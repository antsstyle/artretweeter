<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\CachedVariables;
use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Credentials\DB;

class CoreDB {

    const options = [
        \PDO::ATTR_DEFAULT_FETCH_MODE => \PDO::FETCH_ASSOC,
    ];

    public static $databaseConnection;
    public static $logger;

    public static function initialiseConnection() {
        try {
            $params = "mysql:host=" . DB::server_name . ";dbname=" . DB::database . ";port=" . DB::port . ";charset=UTF8MB4";
            CoreDB::$databaseConnection = new \PDO($params, DB::username, DB::password, CoreDB::options);
        } catch (\Exception $e) {
            CoreDB::$logger->error("Failed to create database connection: " . print_r($e, true));
            echo "Failed to create database connection.";
            exit();
        }
        CoreDB::$databaseConnection->setAttribute(\PDO::ATTR_ERRMODE, \PDO::ERRMODE_EXCEPTION);
    }

    public static function getCachedVariable($name) {
        $selectQuery = "SELECT * FROM cachedvariables WHERE name=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$name]);
        if (!$success) {
            CoreDB::$logger->critical("Database error retrieving cached variable with name: $name.");
            return null;
        }
        $row = $selectStmt->fetch();
        if ($row === false) {
            CoreDB::$logger->error("Cached variable with name: $name does not exist.");
            return false;
        }
        return $row['value'];
    }

    public static function updateCachedVariable($name, $value) {
        $row = CoreDB::getCachedVariable($name);
        if ($row === false) {
            $insertQuery = "INSERT INTO cachedvariables (name,value) VALUES (?,?)";
            $insertStmt = CoreDB::$databaseConnection->prepare($insertQuery);
            try {
                $success = $insertStmt->execute([$name, $value]);
                if (!$success) {
                    CoreDB::$logger->error("Could not insert cached variable with name: $name, value: $value");
                }
            } catch (\Exception $e) {
                CoreDB::$logger->error("Could not insert cached variable with name: $name, value: $value. " . print_r($e, true));
            }
        } else {
            $updateQuery = "UPDATE cachedvariables SET value=? WHERE name=?";
            $updateStmt = CoreDB::$databaseConnection->prepare($updateQuery);
            try {
                $success = $updateStmt->execute([$value, $name]);
                if (!$success) {
                    CoreDB::$logger->error("Could not update cached variable with name: $name, value: $value");
                }
            } catch (\Exception $e) {
                CoreDB::$logger->error("Could not insert cached variable with name: $name, value: $value. " . print_r($e, true));
            }
        }
        return $success;
    }

    public static function getLatestMetricsTypeID() {
        $metricTypesSelectQuery = "SELECT * FROM retrievalmetrics";
        $metricTypesSelectStmt = CoreDB::$databaseConnection->prepare($metricTypesSelectQuery);
        $success = $metricTypesSelectStmt->execute();
        if (!$success) {
            CoreDB::$logger->critical("Failed to get metrics type, cannot calculate metrics.");
            return null;
        }
        while ($row = $metricTypesSelectStmt->fetch()) {
            if ($row['description'] === "Latest Metrics") {
                $metricID = $row['id'];
            }
        }
        if (!isset($metricID)) {
            CoreDB::$logger->critical("Could not find ID for latest metrics retrieval metric type - cannot compute adaptive analytics.");
            return null;
        }
        return $metricID;
    }

    public static function submitArtistForApproval($userAuth, $artistTwitterHandle) {
        $selectQuery = "SELECT * FROM artistsubmissions WHERE screenname=? AND submittedbyusertwitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$artistTwitterHandle, $userAuth['twitter_id']]);
        if (!$success) {
            return "A database error occurred. Try again later or contact "
                    . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.";
        }
        $row = $selectStmt->fetch();
        if ($row !== false) {
            $dateSubmitted = $row['datesubmitted'];
            return "You have already submitted this artist for approval (date $dateSubmitted); it is still pending.";
        }
        $artistTwitterObject = Core::getUserTwitterObjectByHandle($userAuth, $artistTwitterHandle);
        if (isset($artistTwitterObject->errors)) {
            $errorMessage = $artistTwitterObject->errors[0]->message;
            return $errorMessage;
        }
        $artistTwitterID = $artistTwitterObject->id;
        $selectQuery = "SELECT * FROM artists WHERE twitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$artistTwitterID]);
        if (!$success) {
            return "A database error occurred. Try again later or contact "
                    . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.";
        }
        $row = $selectStmt->fetch();
        if ($row !== false) {
            return "Artist is already approved for retweeting.";
        }
        $selectQuery = "SELECT * FROM bannedusers WHERE twitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$artistTwitterID]);
        if (!$success) {
            return "A database error occurred. Try again later or contact "
                    . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.";
        }
        $row = $selectStmt->fetch();
        if ($row !== false) {
            $reason = $row['reason'];
            return "Artist is banned. Reason: $reason.";
        }
        $selectQuery = "SELECT * FROM rejectedartists WHERE twitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$artistTwitterID]);
        if (!$success) {
            return "A database error occurred. Try again later or contact "
                    . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.";
        }
        $row = $selectStmt->fetch();
        if ($row !== false) {
            $reason = $row['reason'];
            $date = $row['rejectiondate'];
            return "Artist was previously submitted, but has been rejected. Reason: $reason. Date of rejection: $date.";
        }
        $insertQuery = "INSERT INTO artistsubmissions (screenname,submittedbyusertwitterid) VALUES (?,?)";
        $insertStmt = CoreDB::$databaseConnection->prepare($insertQuery);
        $success = $insertStmt->execute([$artistTwitterHandle, $userAuth['twitter_id']]);
        if ($success) {
            return "Artist submitted for approval successfully.";
        } else {
            return "A database error occurred. Try again later or contact "
                    . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.";
        }
    }

    public static function updateArtistForUser($userTwitterID, $artistTwitterID, $operation) {
        if ($operation !== "Enable" && $operation !== "Disable") {
            return null;
        }
        if ($userTwitterID === $artistTwitterID) {
            return "You cannot use this page to add retweets for yourself. Use the artists settings page for that.";
        }
        $selectQuery = "SELECT COUNT(*) AS retweetingcount FROM userartistretweetsettings WHERE usertwitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID]);
        if (!$success) {
            return null;
        }
        $retweetingCount = $selectStmt->fetchColumn();
        $maxRetweetingLimit = CoreDB::getCachedVariable(CachedVariables::MAX_ARTISTS_FREE_USER);
        if (is_null($maxRetweetingLimit) || $maxRetweetingLimit === false) {
            return null;
        }
        $selectQuery = "SELECT paiduser FROM users WHERE twitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID]);
        if (!$success) {
            return null;
        }
        $paidUser = $selectStmt->fetchColumn();
        if ($retweetingCount >= $maxRetweetingLimit && $operation === "Enable" && $paidUser !== "Y") {
            return "You are already retweeting the maximum number of artists allowed. To increase your limit, you can "
                    . "<a href=\"" . Config::HOMEPAGE_URL . "subscribe\">subscribe</a>.";
        }

        if ($operation === "Enable") {
            $stmt = CoreDB::$databaseConnection->prepare("INSERT INTO userartistretweetsettings (usertwitterid,artisttwitterid) "
                    . "VALUES (?,?)");
        } else {
            $stmt = CoreDB::$databaseConnection->prepare("DELETE FROM userartistretweetsettings WHERE usertwitterid=? AND artisttwitterid=?");
        }

        $success = $stmt->execute([$userTwitterID, $artistTwitterID]);
        if (!$success) {
            return null;
        }
        $affectedRows = $stmt->rowCount();
        $artistStmt = CoreDB::$databaseConnection->prepare("SELECT * FROM artists WHERE twitterid=?");
        $success = $artistStmt->execute([$artistTwitterID]);
        if (!$success) {
            return null;
        }
        $artistRow = $artistStmt->fetch();
        $returnArray['screenname'] = $artistRow['screenname'];
        $returnArray['artisttwitterid'] = $artistRow['twitterid'];
        $returnArray['followercount'] = $artistRow['followercount'];
        $returnArray['affectedrows'] = $affectedRows;
        return $returnArray;
    }

    public static function getUserArtistAutomationSettings($userTwitterID) {
        $stmt = CoreDB::$databaseConnection->prepare("SELECT * FROM userartistautomationsettings WHERE usertwitterid=?");
        $success = $stmt->execute([$userTwitterID]);
        if (!$success) {
            return null;
        }
        $rows = $stmt->fetch();
        return $rows;
    }

    public static function getUserArtistRetweetSettings($userTwitterID) {
        $stmt = CoreDB::$databaseConnection->prepare("SELECT * FROM userartistretweetsettings INNER JOIN artists ON "
                . "userartistretweetsettings.artisttwitterid=artists.twitterid WHERE usertwitterid=?");
        $success = $stmt->execute([$userTwitterID]);
        if (!$success) {
            return null;
        }
        $rows = $stmt->fetchAll();
        return $rows;
    }

    public static function searchArtistsForUser($searchString, $userTwitterID) {
        $stmt = CoreDB::$databaseConnection->prepare("SELECT * FROM artists WHERE twitterid NOT IN "
                . "(SELECT artisttwitterid FROM userartistretweetsettings WHERE usertwitterid=?) AND screenname LIKE ?");
        $success = $stmt->execute([$userTwitterID, $searchString]);
        if (!$success) {
            return null;
        }
        $rows = $stmt->fetchAll();
        $returnArray['resultcount'] = $stmt->rowCount();
        $returnArray['rows'] = $rows;
        return $returnArray;
    }

    public static function getUserNonArtistRetweetQueue($userTwitterID) {
        $stmt = CoreDB::$databaseConnection->prepare("SELECT *,scheduledretweets.id AS schid, "
                . "(SELECT screenname FROM artists WHERE artists.twitterid=tweets.usertwitterid) AS screenname FROM scheduledretweets "
                . "INNER JOIN tweets ON scheduledretweets.tweetid=tweets.tweetid WHERE retweetingusertwitterid=? "
                . "AND tweetauthorid != ? ORDER BY retweettime ASC");
        $success = $stmt->execute([$userTwitterID, $userTwitterID]);
        if (!$success) {
            return false;
        }
        $rows = $stmt->fetchAll();
        if ($rows) {
            return $rows;
        }
        return null;
    }

    public static function getUserRetweetQueue($userTwitterID) {
        $stmt = CoreDB::$databaseConnection->prepare("SELECT *,scheduledretweets.id AS schid FROM scheduledretweets "
                . "INNER JOIN tweets ON scheduledretweets.tweetid=tweets.tweetid "
                . "WHERE retweetingusertwitterid=? AND tweetauthorid=? ORDER BY retweettime ASC");
        $success = $stmt->execute([$userTwitterID, $userTwitterID]);
        if (!$success) {
            return false;
        }
        $rows = $stmt->fetchAll();
        if ($rows) {
            return $rows;
        }
        return null;
    }

    public static function insertUserInformation($access_token) {
        $accessToken = $access_token['oauth_token'];
        $accessTokenSecret = $access_token['oauth_token_secret'];
        $userTwitterID = $access_token['user_id'];
        $insertQuery = "INSERT INTO users (twitterid,accesstoken,accesstokensecret) VALUES (?,?,?) ON DUPLICATE KEY UPDATE "
                . "accesstoken=?, accesstokensecret=? ";
        $success = CoreDB::$databaseConnection->prepare($insertQuery)
                ->execute([$userTwitterID, $accessToken, $accessTokenSecret, $accessToken, $accessTokenSecret]);
        return $success;
    }

    public static function rescheduleQueuedTweet($queueID, $userTwitterID, $newTime) {
        $userSelectQuery = "SELECT * FROM userautomationsettings WHERE usertwitterid=?";
        $userSelectStmt = CoreDB::$databaseConnection->prepare($userSelectQuery);
        $result = $userSelectStmt->execute([$userTwitterID]);
        if (!$result) {
            return "Database error retrieving user automation settings";
        }
        $userRow = $userSelectStmt->fetch();
        if (!$userRow) {
            return "User automation settings not found";
        }
        $serverTimeString = Core::convertUserTimeStringToServerTime($newTime, $userRow['timezonehouroffset'], $userRow['timezoneminuteoffset']);
        $serverTimestamp = strtotime($serverTimeString);
        $selectQuery = "SELECT tweetid FROM scheduledretweets WHERE id=? AND retweetingusertwitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $result = $selectStmt->execute([$queueID, $userTwitterID]);
        if (!$result) {
            return "Database error retrieving tweet ID";
        }
        $tweetid = $selectStmt->fetchColumn();
        if (!$tweetid) {
            return "Tweet ID not found";
        }
        $canRequeue = Core::checkUserCanQueueNewRetweet($userTwitterID, $serverTimestamp, $queueID);
        if ($canRequeue) {
            $result = CoreDB::updateQueuedRetweet($userTwitterID, $queueID, $serverTimeString);
        } else {
            return $canRequeue;
        }
    }

    // Updates a queue entry without checking if it's allowed or not (uses queue ID, not tweet ID).
    public static function updateQueuedRetweet($userTwitterID, $queueID, $retweetTime) {
        $insertStmt = CoreDB::$databaseConnection->prepare("UPDATE scheduledretweets SET retweettime=? WHERE id=? AND retweetingusertwitterid=?");
        $result = $insertStmt->execute([$retweetTime, $queueID, $userTwitterID]);
        if (!$result) {
            return false;
        }
        return $insertStmt->rowCount();
    }

    public static function deleteQueuedRetweet($queueID, $userTwitterID) {
        CoreDB::$logger->debug("Deleting queue entry with ID: $queueID and user twitter ID: $userTwitterID");
        $statement = CoreDB::$databaseConnection->prepare("DELETE FROM scheduledretweets WHERE id=? AND retweetingusertwitterid=?");
        $result = $statement->execute([$queueID, $userTwitterID]);
        if (!$result) {
            return false;
        }
        $rowCount = $statement->rowCount();
        return $rowCount;
    }

}

CoreDB::$logger = LogManager::getLogger("CoreDB");
CoreDB::initialiseConnection();

