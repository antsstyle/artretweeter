<?php

namespace Antsstyle\ArtRetweeter\DB;

use Antsstyle\ArtRetweeter\DB\CoreDB;
use Antsstyle\ArtRetweeter\DB\UserDB;
use Antsstyle\ArtRetweeter\Core\CachedVariables;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Core\TwitterResponseStatus;

class ArtistDB {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function processArtistSubmission($artistScreenName, $type, $reason) {
        if ($type !== "rejection" && $type !== "approval") {
            return "Unknown approval type.";
        }

        if ($type === "approval") {
            $params['user.fields'] = "protected";
            $endpoint = "users/by/username/" . $artistScreenName;
            $response = Core::queryTwitterUserAuth($endpoint, "users/by/username/:username", "GET", $params, APIKeys::bearer_token);
            $queryResult = $response[0];
            $twitterResponseStatus = $response[1];
            if (!is_null($twitterResponseStatus->getMessage())) {
                return $twitterResponseStatus->getMessage();
            }
            $artistTwitterID = $queryResult->data->id;
            $protected = $queryResult->data->protected;
            if ($protected) {
                return "This artist's profile is protected - their tweets cannot be retweeted.";
            }
            $insertQuery = "INSERT INTO artists (twitterid,screenname) VALUES (?,?)";
            $insertStmt = CoreDB::getConnection()->prepare($insertQuery);
            try {
                $insertStmt->execute([$artistTwitterID, $artistScreenName]);
            } catch (\PDOException $e) {
                self::$logger->error("Failed to add artist to DB: " . print_r($e, true));
                return "Failed to add artist to DB - check error log.";
            }

            $now = date("Y-m-d H:i:s");
            $updateQuery = "UPDATE artistsubmissions SET status=?, datedecided=? WHERE screenname=?";
            $updateStmt = CoreDB::getConnection()->prepare($updateQuery);
            try {
                $updateStmt->execute(["Y", $now, $artistScreenName]);
            } catch (\PDOException $e) {
                self::$logger->error("Failed to approve artist submission: " . print_r($e, true));
                return "Failed to approve artist submission, check error log.";
            }
            $selectQuery = "SELECT * FROM artistsubmissions WHERE screenname=? AND addtortlistonapproval=?";
            $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
            try {
                $selectStmt->execute([$artistScreenName, "Y"]);
            } catch (\PDOException $e) {
                self::$logger->error("Failed to get list of users to enable artist RTs for: " . print_r($e, true));
                return "Artist submission approved but failed to enable RTs for users, check error log.";
            }
            while ($row = $selectStmt->fetch()) {
                UserDB::updateArtistForUser($row['submittedbyusertwitterid'], $row['artisttwitterid'], "Enable");
            }
            return "Submission approved successfully.";
        } else {
            $now = date("Y-m-d H:i:s");
            $updateQuery = "UPDATE artistsubmissions SET status=?, datedecided=?, rejectionreason=? WHERE screenname=?";
            $updateStmt = CoreDB::getConnection()->prepare($updateQuery);
            try {
                $updateStmt->execute(["N", $now, $reason, $artistScreenName]);
                return "Submission rejected successfully.";
            } catch (\PDOException $e) {
                self::$logger->error("Failed to reject artist submission: " . print_r($e, true));
                return "Failed to reject artist submission, check error log.";
            }
        }
    }

    public static function submitArtistForApproval($userRow, $artistTwitterHandle, $autoRetweetOnApproval) {
        $userInfo = UserDB::getUserInfo($userRow['twitterid']);
        if (is_null($userInfo)) {
            return ["Your user information could not be found. Try logging in again or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }
        if ($userInfo === false) {
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }
        $paidUser = $userInfo['paiduser'];
        $maxPendingSubmissions = CoreDB::getCachedVariable(CachedVariables::MAX_PENDING_SUBMISSIONS_FREE_USER);
        if (is_null($maxPendingSubmissions)) {
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        } else if ($maxPendingSubmissions === false) {
            self::$logger->error("Error: could not find max pending submissions cached variable, defaulting to 5.");
            $maxPendingSubmissions = 5;
        }
        $selectQuery = "SELECT COUNT(*) FROM artistsubmissions WHERE submittedbyusertwitterid=? AND status=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$userRow['twitterid'], "U"]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get count of artist submissions from user: " . print_r($e, true));
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }

        $count = $selectStmt->fetchColumn();
        if ($count >= $maxPendingSubmissions && $paidUser !== "Y") {
            return ["You already have $maxPendingSubmissions submissions pending. You cannot submit any "
                . "more until some of them are approved or rejected.", null];
        }
        $selectQuery = "SELECT * FROM artistsubmissions WHERE screenname=? AND submittedbyusertwitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$artistTwitterHandle, $userRow['twitterid']]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get artist submissions for user: " . print_r($e, true));
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }

        $row = $selectStmt->fetch();
        if ($row !== false) {
            $dateSubmitted = $row['datesubmitted'];
            return ["You have already submitted this artist for approval (date $dateSubmitted); it is still pending.", null];
        }
        $params['user.fields'] = "protected";
        $endpoint = "users/by/username/" . $artistTwitterHandle;
        $response = Core::queryTwitterUserAuth($endpoint, "users/by/username/:username", "GET", $params, $userRow);
        $queryResult = $response[0];
        $twitterResponseStatus = $response[1];
        if ($twitterResponseStatus->getHttpCode() != TwitterResponseStatus::HTTP_QUERY_OK) {
            $errorMessage = $twitterResponseStatus->getMessage();
            return [$errorMessage, null];
        }
        $artistTwitterID = $queryResult->data->id;
        $protected = $queryResult->data->protected;
        if ($protected) {
            return ["This artist's profile is protected - their tweets cannot be retweeted.", null];
        }
        $selectQuery = "SELECT * FROM artists WHERE twitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$artistTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get artist from database: " . print_r($e, true));
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }

        $row = $selectStmt->fetch();
        if ($row !== false) {
            return ["Artist is already approved for retweeting.", null];
        }
        $selectQuery = "SELECT * FROM bannedusers WHERE twitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$artistTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get banned users from database: " . print_r($e, true));
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }

        $row = $selectStmt->fetch();
        if ($row !== false) {
            $reason = $row['reason'];
            return ["Artist is banned. Reason: $reason.", null];
        }
        $selectQuery = "SELECT * FROM nftcryptoblockercentralisedblocklist WHERE blockableusertwitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$artistTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get banned NFT/crypto users from database: " . print_r($e, true));
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }

        $row = $selectStmt->fetch();
        if ($row !== false) {
            $type = $row['matchedfiltertype'];
            $content = $row['matchedfiltercontent'];
            return ["Artist was detected as using NFTs or crypto, they cannot be submitted. "
                . "<br/>Match type: $type Match content: $content", null];
        }

        $insertQuery = "INSERT INTO artistsubmissions (screenname,submittedbyusertwitterid,artisttwitterid,addtortlistonapproval) VALUES (?,?,?,?)";
        $insertStmt = CoreDB::getConnection()->prepare($insertQuery);
        try {
            $insertStmt->execute([$artistTwitterHandle, $userRow['twitterid'], $artistTwitterID, $autoRetweetOnApproval]);
            return ["Artist submitted for approval successfully.", $artistTwitterID];
        } catch (\PDOException $e) {
            self::$logger->error("Failed to insert artist submission: " . print_r($e, true));
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }
    }

    public static function cancelArtistSubmission($userRow, $artistTwitterHandle) {
        $deleteQuery = "DELETE FROM artistsubmissions WHERE screenname=? AND submittedbyusertwitterid=?";
        $deleteStmt = CoreDB::getConnection()->prepare($deleteQuery);
        try {
            $deleteStmt->execute([$artistTwitterHandle, $userRow['twitterid']]);
            return "Submission cancelled successfully.";
        } catch (\PDOException $e) {
            self::$logger->error("Failed to cancel artist submission: " . print_r($e, true));
            return "A database error occurred. Try again later or contact "
                    . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.";
        }
    }

    public static function getAllPendingArtistSubmissions() {
        $selectQuery = "SELECT screenname,MIN(datesubmitted) AS min, COUNT(screenname) AS submissioncount "
                . "FROM artistsubmissions WHERE status=? GROUP BY screenname";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute(["U"]);
        } catch (\PDOException $e) {
            self::$logger->critical("Failed to get user artist submissions: " . print_r($e, true));
            return null;
        }

        $rows = $selectStmt->fetchAll();
        return $rows;
    }

    public static function removeOldArtistSubmissions() {
        $minus30days = date("Y-m-d H:i:s", strtotime("-30 days"));
        $deleteQuery = "DELETE FROM artistsubmissions WHERE status != ? AND datesubmitted < ?";
        $deleteStmt = CoreDB::getConnection()->prepare($deleteQuery);
        try {
            $deleteStmt->execute(["U", $minus30days]);
        } catch (\PDOException $e) {
            self::$logger->error("FAiled to delete old artist submissions: " . print_r($e, true));
            return false;
        }
        return true;
    }

}

ArtistDB::initialiseLogger();
