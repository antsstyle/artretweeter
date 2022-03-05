<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\CachedVariables;
use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Credentials\DB;
use Abraham\TwitterOAuth\TwitterOAuth;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;

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
            exit();
        }
        CoreDB::$databaseConnection->setAttribute(\PDO::ATTR_ERRMODE, \PDO::ERRMODE_EXCEPTION);
    }

    public static function checkNFTCryptoBlockerCentralDB() {
        $nftCryptoBlockerDBConn = null;
        try {
            $params = "mysql:host=" . DB::server_name . ";dbname=" . DB::nftcryptoblocker_database . ";port=" . DB::port . ";charset=UTF8MB4";
            $nftCryptoBlockerDBConn = new \PDO($params, DB::username, DB::password, CoreDB::options);
        } catch (\Exception $e) {
            CoreDB::$logger->error("Failed to create database connection: " . print_r($e, true));
            return null;
        }
        $nftCryptoBlockerDBConn->setAttribute(\PDO::ATTR_ERRMODE, \PDO::ERRMODE_EXCEPTION);

        $selectQuery = "SELECT dateadded FROM nftcryptoblockercentralisedblocklist ORDER BY dateadded DESC LIMIT 5";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $selectStmt->execute();
        $latestDates = [];
        while ($row = $selectStmt->fetch()) {
            $latestDates[] = $row['dateadded'];
        }

        $insertQuery = "INSERT INTO nftcryptoblockercentralisedblocklist (blockableusertwitterid,matchedfiltertype,matchedfiltercontent,"
                . "dateadded,addedfrom,markedfordeletion,markedfordeletiondate,markedfordeletionreason,followercount,twitterhandle,matchcount,"
                . "lastcheckeddate) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        $insertStmt = CoreDB::$databaseConnection->prepare($insertQuery);
        $offset = 0;
        $rowCount = 1;
        CoreDB::$databaseConnection->beginTransaction();
        while ($rowCount > 0) {
            $stmt = $nftCryptoBlockerDBConn->prepare("SELECT * FROM centralisedblocklist ORDER BY dateadded DESC LIMIT 1000 OFFSET $offset");
            $stmt->execute();
            $rowCount = $stmt->rowCount();
            while ($row = $stmt->fetch()) {
                $dateAdded = $row['dateadded'];
                foreach ($latestDates as $latestDate) {
                    if ($latestDate >= $dateAdded) {
                        CoreDB::$databaseConnection->commit();
                        return;
                    }
                }
                $insertStmt->execute([$row['blockableusertwitterid'], $row['matchedfiltertype'], $row['matchedfiltercontent'], $row['dateadded'],
                    $row['addedfrom'], $row['markedfordeletion'], $row['markedfordeletiondate'], $row['markedfordeletionreason'],
                    $row['followercount'], $row['twitterhandle'], $row['matchcount'], $row['lastcheckeddate']]);
            }
        }
        CoreDB::$databaseConnection->commit();
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

    public static function removeOldArtistSubmissions() {
        $now = date("Y-m-d H:i:s");
        $deleteQuery = "DELETE FROM artistsubmissions WHERE status != ? AND datesubmitted < ?";
        $deleteStmt = CoreDB::$databaseConnection->prepare($deleteQuery);
        $deleteStmt->execute(["U", $now]);
    }

    public static function checkIfUserIsBanned($userTwitterID) {
        $selectQuery = "SELECT * FROM bannedusers WHERE twitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID]);
        if (!$success) {
            CoreDB::$logger->critical("Failed to get user ban info.");
            return null;
        }
        $row = $selectStmt->fetch();
        if ($row !== false) {
            return $row;
        }
        $selectQuery = "SELECT * FROM nftcryptoblockercentralisedblocklist WHERE blockableusertwitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID]);
        if (!$success) {
            CoreDB::$logger->critical("Failed to get user ban info.");
            return null;
        }
        $row = $selectStmt->fetch();
        if ($row !== false) {
            return $row;
        }
        return false;
    }

    public static function processArtistSubmission($artistScreenName, $type, $reason) {
        if ($type !== "rejection" && $type !== "approval") {
            return "Unknown approval type.";
        }

        if ($type === "approval") {
            $params['user.fields'] = "protected";
            $connection = new TwitterOAuth(APIKeys::twitter_consumer_key, APIKeys::twitter_consumer_secret,
                    null, APIKeys::bearer_token);
            $connection->setApiVersion('2');
            $connection->setRetries(1, 1);
            $endpoint = "users/by/username/" . $artistScreenName;
            $queryResult = Core::queryTwitterUserAuth($connection, $endpoint, "GET", $params, APIKeys::bearer_token);
            if (isset($queryResult->errors)) {
                $errorMessage = $queryResult->errors[0]->detail;
                return $errorMessage;
            }
            $artistTwitterID = $queryResult->data->id;
            $protected = $queryResult->data->protected;
            if ($protected) {
                return "This artist's profile is protected - their tweets cannot be retweeted.";
            }
            $insertQuery = "INSERT INTO artists (twitterid,screenname) VALUES (?,?)";
            $insertStmt = CoreDB::$databaseConnection->prepare($insertQuery);
            $success = $insertStmt->execute([$artistTwitterID, $artistScreenName]);
            if (!$success) {
                return "Failed to add artist to DB - check error log.";
            }
            $now = date("Y-m-d H:i:s");
            $updateQuery = "UPDATE artistsubmissions SET status=?, datedecided=? WHERE screenname=?";
            $updateStmt = CoreDB::$databaseConnection->prepare($updateQuery);
            $updateStmt->execute(["Y", $now, $artistScreenName]);
            return "Submission approved successfully.";
        } else {
            $now = date("Y-m-d H:i:s");
            $updateQuery = "UPDATE artistsubmissions SET status=?, datedecided=?, rejectionreason=? WHERE screenname=?";
            $updateStmt = CoreDB::$databaseConnection->prepare($updateQuery);
            $updateStmt->execute(["N", $now, $reason, $artistScreenName]);
            return "Submission rejected successfully.";
        }
    }

    public static function resetAdminUserLoginAttempts($username) {
        $updateQuery = "UPDATE adminaccounts SET failedloginattempts=0 WHERE username=?";
        $updateStmt = CoreDB::$databaseConnection->prepare($updateQuery);
        $updateStmt->execute([$username]);
    }

    public static function incrementAdminUserLoginAttempts($username) {
        $updateQuery = "UPDATE adminaccounts SET failedloginattempts=failedloginattempts+1 WHERE username=?";
        $updateStmt = CoreDB::$databaseConnection->prepare($updateQuery);
        $updateStmt->execute([$username]);
    }

    public static function getAdminInfo($username) {
        $selectQuery = "SELECT * FROM adminaccounts WHERE username=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$username]);
        if (!$success) {
            CoreDB::$logger->critical("Failed to get admin info.");
            return null;
        }
        return $selectStmt->fetch();
    }

    public static function getAllPendingArtistSubmissions() {
        $selectQuery = "SELECT screenname,COUNT(screenname) FROM artistsubmissions WHERE status=? GROUP BY screenname";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute(["U"]);
        if (!$success) {
            CoreDB::$logger->critical("Failed to get user artist submissions.");
            return null;
        }
        $rows = $selectStmt->fetchAll();
        return $rows;
    }

    public static function getPendingArtistSubmissionsForUser($userTwitterID) {
        $selectQuery = "SELECT * FROM artistsubmissions WHERE artisttwitterid NOT IN (SELECT twitterid FROM artists) "
                . "AND artisttwitterid NOT IN (SELECT artisttwitterid FROM rejectedartistsubmissions) AND submittedbyusertwitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID]);
        if (!$success) {
            CoreDB::$logger->critical("Failed to get artist submissions for user ID $userTwitterID - cannot display on webpage.");
            return null;
        }
        $rows = $selectStmt->fetchAll();
        return $rows;
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

    public static function cancelArtistSubmission($userAuth, $artistTwitterHandle) {
        $deleteQuery = "DELETE FROM artistsubmissions WHERE screenname=? AND submittedbyusertwitterid=?";
        $deleteStmt = CoreDB::$databaseConnection->prepare($deleteQuery);
        $success = $deleteStmt->execute([$artistTwitterHandle, $userAuth['twitter_id']]);
        if ($success) {
            return "Submission cancelled successfully.";
        } else {
            return "A database error occurred. Try again later or contact "
                    . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.";
        }
    }

    public static function submitArtistForApproval($userAuth, $artistTwitterHandle) {
        $userInfo = CoreDB::getUserInfo($userAuth['twitter_id']);
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
            CoreDB::$logger->error("Error: could not find max pending submissions cached variable, defaulting to 5.");
            $maxPendingSubmissions = 5;
        }
        $selectQuery = "SELECT COUNT(*) FROM artistsubmissions WHERE submittedbyusertwitterid=? AND status=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userAuth['twitter_id'], "U"]);
        if (!$success) {
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }
        $count = $selectStmt->fetchColumn();
        if ($count >= $maxPendingSubmissions && $paidUser !== "Y") {
            return ["You already have $maxPendingSubmissions submissions pending. You cannot submit any "
                . "more until some of them are approved or rejected.", null];
        }
        $selectQuery = "SELECT * FROM artistsubmissions WHERE screenname=? AND submittedbyusertwitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$artistTwitterHandle, $userAuth['twitter_id']]);
        if (!$success) {
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }
        $row = $selectStmt->fetch();
        if ($row !== false) {
            $dateSubmitted = $row['datesubmitted'];
            return ["You have already submitted this artist for approval (date $dateSubmitted); it is still pending.", null];
        }
        $params['user.fields'] = "protected";
        $connection = new TwitterOAuth(APIKeys::twitter_consumer_key, APIKeys::twitter_consumer_secret,
                $userAuth['access_token'], $userAuth['access_token_secret']);
        $connection->setApiVersion('2');
        $connection->setRetries(1, 1);
        $endpoint = "users/by/username/" . $artistTwitterHandle;
        $queryResult = Core::queryTwitterUserAuth($connection, $endpoint, "GET", $params, $userAuth);
        if (isset($queryResult->errors)) {
            $errorMessage = $queryResult->errors[0]->detail;
            return [$errorMessage, null];
        }
        $artistTwitterID = $queryResult->data->id;
        $protected = $queryResult->data->protected;
        if ($protected) {
            return ["This artist's profile is protected - their tweets cannot be retweeted.", null];
        }
        $selectQuery = "SELECT * FROM artists WHERE twitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$artistTwitterID]);
        if (!$success) {
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }
        $row = $selectStmt->fetch();
        if ($row !== false) {
            return ["Artist is already approved for retweeting.", null];
        }
        $selectQuery = "SELECT * FROM bannedusers WHERE twitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$artistTwitterID]);
        if (!$success) {
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }
        $row = $selectStmt->fetch();
        if ($row !== false) {
            $reason = $row['reason'];
            return ["Artist is banned. Reason: $reason.", null];
        }
        $selectQuery = "SELECT * FROM nftcryptoblockercentralisedblocklist WHERE blockableusertwitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$artistTwitterID]);
        if (!$success) {
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
        /*
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
          } */
        $insertQuery = "INSERT INTO artistsubmissions (screenname,submittedbyusertwitterid,artisttwitterid) VALUES (?,?,?)";
        $insertStmt = CoreDB::$databaseConnection->prepare($insertQuery);
        $success = $insertStmt->execute([$artistTwitterHandle, $userAuth['twitter_id'], $artistTwitterID]);
        if ($success) {
            return ["Artist submitted for approval successfully.", $artistTwitterID];
        } else {
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
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

    public static function getUserArtistRetweetSettingsCount($userTwitterID) {
        $stmt = CoreDB::$databaseConnection->prepare("SELECT COUNT(*) FROM userartistretweetsettings WHERE usertwitterid=?");
        $success = $stmt->execute([$userTwitterID]);
        if (!$success) {
            return null;
        }
        return $stmt->fetchColumn();
    }

    public static function getUserArtistRetweetSettings($userTwitterID, $pageNum = 1) {
        if (!is_numeric($pageNum)) {
            $pageNum = 1;
        }
        $offSet = ($pageNum - 1) * 15;
        $stmt = CoreDB::$databaseConnection->prepare("SELECT * FROM userartistretweetsettings INNER JOIN artists ON "
                . "userartistretweetsettings.artisttwitterid=artists.twitterid WHERE usertwitterid=? ORDER BY screenname ASC LIMIT 15 OFFSET $offSet");
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
        $selectQuery = "SELECT twitterid FROM users WHERE twitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $selectStmt->execute([$userTwitterID]);
        if ($selectStmt->fetchColumn() !== false) {
            $insertQuery = "UPDATE users SET accesstoken=?, accesstokensecret=? WHERE twitterid=?";
            $success = CoreDB::$databaseConnection->prepare($insertQuery)
                    ->execute([$accessToken, $accessTokenSecret, $userTwitterID]);
        } else {
            $insertQuery = "INSERT INTO users (twitterid,accesstoken,accesstokensecret) VALUES (?,?,?)";
            $success = CoreDB::$databaseConnection->prepare($insertQuery)
                    ->execute([$userTwitterID, $accessToken, $accessTokenSecret]);
        }
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

    public static function getUserInfo($userTwitterID) {
        $stmt = CoreDB::$databaseConnection->prepare("SELECT * FROM users LEFT JOIN "
                . "userautomationsettings ON users.twitterid=userautomationsettings.usertwitterid WHERE twitterid=?");
        $success = $stmt->execute([$userTwitterID]);
        if (!$success) {
            return false;
        }
        $row = $stmt->fetch();
        if ($row) {
            return $row;
        }
        return null;
    }

}

CoreDB::$logger = LogManager::getLogger("CoreDB");
CoreDB::initialiseConnection();

