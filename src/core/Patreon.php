<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Credentials\AdminUserAuth;
use Antsstyle\ArtRetweeter\DB\AdminDB;
use Antsstyle\ArtRetweeter\DB\CoreDB;
use Antsstyle\ArtRetweeter\Core\LogManager;
use Patreon\API;

class Patreon {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function getCampaignTiers() {
        $api_client = new API(AdminUserAuth::patreon_creator_access_token);
        $response = $api_client->get_data("campaigns?page%5Bsize%5D=100&include=tiers&fields"
                . urlencode('[campaign]') . "=created_at&fields"
                . urlencode('[tier]') . "=amount_cents,description,published,title,description");
        if (!is_array($response)) {
            $error = json_decode($response);
            if (!is_null($error)) {
                // Deal with error
                return;
            }
        }
        $firstCampaignTiers = $response['data'][0]['relationships']['tiers']['data'];
        foreach ($firstCampaignTiers as $tierData) {
            $tierIDs[$tierData['id']] = $tierData['id'];
        }
        $includedInfo = $response['included'];
        foreach ($includedInfo as $tierDetails) {
            if (!array_key_exists($tierDetails['id'], $tierIDs)) {
                continue;
            }
            $params = [$tierDetails['id'], $tierDetails['attributes']['title'], $tierDetails['attributes']['description'],
                $tierDetails['attributes']['amount_cents'], $tierDetails['attributes']['published'], $tierDetails['attributes']['title'],
                $tierDetails['attributes']['description'], $tierDetails['attributes']['amount_cents'], $tierDetails['attributes']['published']];
            $insertQuery = "INSERT INTO patreontiers (patreon_id,title,description,amount_cents,published) VALUES (?,?,?,?,?) ON DUPLICATE KEY "
                    . "UPDATE title=?,description=?,amount_cents=?,published=?";
            $insertStmt = CoreDB::getConnection()->prepare($insertQuery);
            try {
                $insertStmt->execute($params);
            } catch (\PDOException $e) {
                self::$logger->error("Failed to insert Patreon campaign tier. Params: " . print_r($params, true)
                        . " PDO error: " . print_r($e, true));
            }
        }
    }

    public static function getPatrons() {
        $adminUserIDs = AdminDB::getAllAdminTwitterIDs();
        if (is_null($adminUserIDs)) {
            self::$logger->emergency("Unable to get admin user IDs, cannot update patron statuses!");
            return false;
        }
        $paidTierID = CoreDB::getCachedVariable(CachedVariables::PATREON_PAID_TIER_ID);
        if (is_null($paidTierID) || $paidTierID === false) {
            self::$logger->emergency("Unable to get patron information, no paid tier ID is known!");
            return false;
        }
        $api_client = new API(AdminUserAuth::patreon_creator_access_token);
        $campaigns = $api_client->fetch_campaigns();
        $firstCampaign = $campaigns['data'][0]['id'];
        $error = 1;
        $i = 0;
        $maxIterations = 30;
        while ($i < $maxIterations) {
            $i++;
            $response = $api_client->get_data("campaigns/{$firstCampaign}/members?page%5Bsize%5D=100&include=user,currently_entitled_tiers&fields"
                    . urlencode('[member]') . "=full_name,is_follower,last_charge_date,last_charge_status,lifetime_support_cents,currently_entitled_amount_cents,patron_status&fields"
                    . urlencode('[tier]') . "=amount_cents,created_at,description,discord_role_ids,edited_at,patron_count,published,published_at,requires_shipping,title,url&fields"
                    . urlencode('[user]') . "=social_connections");
            if (!is_array($response)) {
                $error = json_decode($response);
                if (!is_null($error)) {
                    self::$logger->error("Error received from Patreon API, not registering new patron information.");
                    break;
                }
            }


            $updateParams = [];

            $data = $response['data'];
            foreach ($data as $dataEntry) {
                $userID = $dataEntry['relationships']['user']['data']['id'];
                $currentlyEntitledTiers = $dataEntry['relationships']['currently_entitled_tiers']['data'];
                $isEligible = false;
                foreach ($currentlyEntitledTiers as $tier) {
                    if ($tier['id'] === $paidTierID) {
                        $isEligible = true;
                        break;
                    }
                }
                // Check if contains eligible tier - get tier info from DB first
                if ($isEligible) {
                    $updateParams[$userID] = "Y";
                } else {
                    $updateParams[$userID] = "N";
                }
            }
            $included = $response['included'];
            foreach ($included as $includedEntry) {
                $userPatreonID = $includedEntry['id'];
                $twitterUserID = $includedEntry['attributes']['social_connections']['twitter']['user_id'];
                if (!is_null($twitterUserID) && array_key_exists($userPatreonID, $updateParams) && !in_array($twitterUserID, $adminUserIDs)) {
                    $updateQuery = "UPDATE users SET patreonid=?, paiduser=? WHERE twitterid=?";
                    $updateStmt = CoreDB::getConnection()->prepare($updateQuery);
                    try {
                        $updateStmt->execute([$userPatreonID, $updateParams[$userPatreonID], $twitterUserID]);
                    } catch (\PDOException $e) {
                        self::$logger->error("Failed to update users table with patreon information. User patreon ID: $userPatreonID, "
                                . "Paid user: " . $updateParams[$userPatreonID] . ", User Twitter ID: $twitterUserID. PDO error: " . print_r($e, true));
                    }
                }
            }
            $metapagination = $response['meta']['pagination'];
            if (!isset($metapagination['cursors']['next'])) {
                break;
            }
        }
    }

    public static function processWebhookEvent($data, $event) {
        switch ($event) {
            case "members:create":
                break;
            case "members:update":
                break;
            case "members:delete":
                break;
            case "members:pledge:create":
                break;
            case "members:pledge:update":
                break;
            case "members:pledge:delete":
                break;
            default:
                break;
        }
        error_log("Patreon webhook received: " . print_r(json_decode($data), true));
    }

}

Patreon::initialiseLogger();
