<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\CachedVariables;
use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Core\RetweetScheduler;
use Antsstyle\ArtRetweeter\Core\TwitterResponseStatus;
use Antsstyle\ArtRetweeter\Credentials\DB;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;

class CoreDB {

    const options = [
        \PDO::ATTR_DEFAULT_FETCH_MODE => \PDO::FETCH_ASSOC,
    ];

    private static $databaseConnection;
    private static $logger;
    
    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }
    
    public static function getConnection() {
        return self::$databaseConnection;
    }

    public static function initialiseConnection() {
        try {
            $params = "mysql:host=" . DB::server_name . ";dbname=" . DB::database . ";port=" . DB::port . ";charset=UTF8MB4";
            self::$databaseConnection = new \PDO($params, DB::username, DB::password, self::options);
        } catch (\Exception $e) {
            self::$logger->error("Failed to create database connection: " . print_r($e, true));
            exit();
        }
        self::$databaseConnection->setAttribute(\PDO::ATTR_ERRMODE, \PDO::ERRMODE_EXCEPTION);
    }

    public static function queueRetweet($userAuthTwitterID, $tweetAuthorID, $tweetID, $retweetTime) {
        if (!RetweetScheduler::checkUserCanQueueNewRetweet($userAuthTwitterID, $retweetTime)) {
            return false;
        }
        if (!RetweetScheduler::checkUserCanRetweetOldTweet($userAuthTwitterID, $retweetTime, $tweetID)) {
            return false;
        }
        $timeString = date('Y-m-d H:i:s', $retweetTime);
        $stmt = self::$databaseConnection->prepare("INSERT INTO scheduledretweets (retweetingusertwitterid,tweetauthorid,tweetid,retweettime,automated) "
                . "VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE retweetingusertwitterid=?, tweetauthorid=?, tweetid=?, retweettime=?, automated=?");
        $success = $stmt->execute([$userAuthTwitterID, $tweetAuthorID, $tweetID, $timeString, "N",
            $userAuthTwitterID, $tweetAuthorID, $tweetID, $timeString, "N"]);
        return $success;
    }

    public static function checkNFTCryptoBlockerCentralDB() {
        $nftCryptoBlockerDBConn = null;
        try {
            $params = "mysql:host=" . DB::server_name . ";dbname=" . DB::nftcryptoblocker_database . ";port=" . DB::port . ";charset=UTF8MB4";
            $nftCryptoBlockerDBConn = new \PDO($params, DB::username, DB::password, self::options);
        } catch (\Exception $e) {
            self::$logger->error("Failed to create database connection: " . print_r($e, true));
            return null;
        }
        $nftCryptoBlockerDBConn->setAttribute(\PDO::ATTR_ERRMODE, \PDO::ERRMODE_EXCEPTION);

        $selectQuery = "SELECT dateadded FROM nftcryptoblockercentralisedblocklist ORDER BY dateadded DESC LIMIT 5";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $selectStmt->execute();
        $latestDates = [];
        while ($row = $selectStmt->fetch()) {
            $latestDates[] = $row['dateadded'];
        }

        $insertQuery = "INSERT INTO nftcryptoblockercentralisedblocklist (blockableusertwitterid,matchedfiltertype,matchedfiltercontent,"
                . "dateadded,addedfrom,markedfordeletion,markedfordeletiondate,markedfordeletionreason,followercount,twitterhandle,matchcount,"
                . "lastcheckeddate) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        $insertStmt = self::$databaseConnection->prepare($insertQuery);
        $offset = 0;
        $rowCount = 1;
        self::$databaseConnection->beginTransaction();
        while ($rowCount > 0) {
            $stmt = $nftCryptoBlockerDBConn->prepare("SELECT * FROM centralisedblocklist ORDER BY dateadded DESC LIMIT 1000 OFFSET $offset");
            $stmt->execute();
            $rowCount = $stmt->rowCount();
            while ($row = $stmt->fetch()) {
                $dateAdded = $row['dateadded'];
                foreach ($latestDates as $latestDate) {
                    if ($latestDate >= $dateAdded) {
                        self::$databaseConnection->commit();
                        return;
                    }
                }
                $insertStmt->execute([$row['blockableusertwitterid'], $row['matchedfiltertype'], $row['matchedfiltercontent'], $row['dateadded'],
                    $row['addedfrom'], $row['markedfordeletion'], $row['markedfordeletiondate'], $row['markedfordeletionreason'],
                    $row['followercount'], $row['twitterhandle'], $row['matchcount'], $row['lastcheckeddate']]);
            }
        }
        self::$databaseConnection->commit();
    }

    public static function getCachedVariable($name) {
        $selectQuery = "SELECT * FROM cachedvariables WHERE name=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$name]);
        if (!$success) {
            self::$logger->critical("Database error retrieving cached variable with name: $name.");
            return null;
        }
        $row = $selectStmt->fetch();
        if ($row === false) {
            self::$logger->error("Cached variable with name: $name does not exist.");
            return false;
        }
        return $row['value'];
    }

    public static function updateCachedVariable($name, $value) {
        $row = self::getCachedVariable($name);
        if ($row === false) {
            $insertQuery = "INSERT INTO cachedvariables (name,value) VALUES (?,?)";
            $insertStmt = self::$databaseConnection->prepare($insertQuery);
            try {
                $success = $insertStmt->execute([$name, $value]);
                if (!$success) {
                    self::$logger->error("Could not insert cached variable with name: $name, value: $value");
                }
            } catch (\Exception $e) {
                self::$logger->error("Could not insert cached variable with name: $name, value: $value. " . print_r($e, true));
            }
        } else {
            $updateQuery = "UPDATE cachedvariables SET value=? WHERE name=?";
            $updateStmt = self::$databaseConnection->prepare($updateQuery);
            try {
                $success = $updateStmt->execute([$value, $name]);
                if (!$success) {
                    self::$logger->error("Could not update cached variable with name: $name, value: $value");
                }
            } catch (\Exception $e) {
                self::$logger->error("Could not insert cached variable with name: $name, value: $value. " . print_r($e, true));
            }
        }
        return $success;
    }

    public static function removeOldArtistSubmissions() {
        $minus30days = date("Y-m-d H:i:s", strtotime("-30 days"));
        $deleteQuery = "DELETE FROM artistsubmissions WHERE status != ? AND datesubmitted < ?";
        $deleteStmt = self::$databaseConnection->prepare($deleteQuery);
        $deleteStmt->execute(["U", $minus30days]);
    }

    public static function checkIfUserIsBanned($userTwitterID) {
        $selectQuery = "SELECT * FROM bannedusers WHERE twitterid=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID]);
        if (!$success) {
            self::$logger->critical("Failed to get user ban info.");
            return null;
        }
        $row = $selectStmt->fetch();
        if ($row !== false) {
            return $row;
        }
        $selectQuery = "SELECT * FROM nftcryptoblockercentralisedblocklist WHERE blockableusertwitterid=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID]);
        if (!$success) {
            self::$logger->critical("Failed to get user ban info.");
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
            $insertStmt = self::$databaseConnection->prepare($insertQuery);
            $success = $insertStmt->execute([$artistTwitterID, $artistScreenName]);
            if (!$success) {
                return "Failed to add artist to DB - check error log.";
            }
            $now = date("Y-m-d H:i:s");
            $updateQuery = "UPDATE artistsubmissions SET status=?, datedecided=? WHERE screenname=?";
            $updateStmt = self::$databaseConnection->prepare($updateQuery);
            $updateStmt->execute(["Y", $now, $artistScreenName]);
            return "Submission approved successfully.";
        } else {
            $now = date("Y-m-d H:i:s");
            $updateQuery = "UPDATE artistsubmissions SET status=?, datedecided=?, rejectionreason=? WHERE screenname=?";
            $updateStmt = self::$databaseConnection->prepare($updateQuery);
            $updateStmt->execute(["N", $now, $reason, $artistScreenName]);
            return "Submission rejected successfully.";
        }
    }

    public static function resetAdminUserLoginAttempts($username) {
        $updateQuery = "UPDATE adminaccounts SET failedloginattempts=0 WHERE username=?";
        $updateStmt = self::$databaseConnection->prepare($updateQuery);
        $updateStmt->execute([$username]);
    }

    public static function incrementAdminUserLoginAttempts($username) {
        $updateQuery = "UPDATE adminaccounts SET failedloginattempts=failedloginattempts+1 WHERE username=?";
        $updateStmt = self::$databaseConnection->prepare($updateQuery);
        $updateStmt->execute([$username]);
    }

    public static function updateUserArtistEligibleTweetsCache($userTwitterID, $cacheArray) {
        $cachedResult = json_encode($cacheArray);
        $insertQuery = "INSERT INTO userartisteligibletweetscache (usertwitterid,cachedresult) VALUES (?,?) "
                . "ON DUPLICATE KEY UPDATE cachedresult=?";
        try {
            $insertStmt = self::$databaseConnection->prepare($insertQuery);
            $insertStmt->execute([$userTwitterID, $cachedResult, $cachedResult]);
        } catch (\PDOException $e) {
            self::$logger->error("Unable to update user artist eligible tweets cache for user twitter ID "
                    . "$userTwitterID: " . print_r($e, true));
        }
    }

    public static function getAllAdminTwitterIDs() {
        $selectQuery = "SELECT * FROM adminaccounts";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute();
        if (!$success) {
            self::$logger->critical("Failed to get all admin info.");
            return null;
        }
        $returnArray = [];
        while ($row = $selectStmt->fetch()) {
            $returnArray[] = $row['twitterid'];
        }
        return $returnArray;
    }

    public static function getAdminInfo($username) {
        $selectQuery = "SELECT * FROM adminaccounts WHERE username=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$username]);
        if (!$success) {
            self::$logger->critical("Failed to get admin info.");
            return null;
        }
        return $selectStmt->fetch();
    }

    public static function getAllPendingArtistSubmissions() {
        $selectQuery = "SELECT screenname,COUNT(screenname) FROM artistsubmissions WHERE status=? GROUP BY screenname";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute(["U"]);
        if (!$success) {
            self::$logger->critical("Failed to get user artist submissions.");
            return null;
        }
        $rows = $selectStmt->fetchAll();
        return $rows;
    }

    public static function getPendingArtistSubmissionsForUser($userTwitterID) {
        $selectQuery = "SELECT * FROM artistsubmissions WHERE artisttwitterid NOT IN (SELECT twitterid FROM artists) "
                . "AND artisttwitterid NOT IN (SELECT artisttwitterid FROM rejectedartistsubmissions) AND submittedbyusertwitterid=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID]);
        if (!$success) {
            self::$logger->critical("Failed to get artist submissions for user ID $userTwitterID - cannot display on webpage.");
            return null;
        }
        $rows = $selectStmt->fetchAll();
        return $rows;
    }

    public static function getLatestMetricsTypeID() {
        $metricTypesSelectQuery = "SELECT * FROM retrievalmetrics";
        $metricTypesSelectStmt = self::$databaseConnection->prepare($metricTypesSelectQuery);
        $success = $metricTypesSelectStmt->execute();
        if (!$success) {
            self::$logger->critical("Failed to get metrics type, cannot calculate metrics.");
            return null;
        }
        while ($row = $metricTypesSelectStmt->fetch()) {
            if ($row['description'] === "Latest Metrics") {
                $metricID = $row['id'];
            }
        }
        if (!isset($metricID)) {
            self::$logger->critical("Could not find ID for latest metrics retrieval metric type - cannot compute adaptive analytics.");
            return null;
        }
        return $metricID;
    }

    public static function cancelArtistSubmission($userRow, $artistTwitterHandle) {
        $deleteQuery = "DELETE FROM artistsubmissions WHERE screenname=? AND submittedbyusertwitterid=?";
        $deleteStmt = self::$databaseConnection->prepare($deleteQuery);
        $success = $deleteStmt->execute([$artistTwitterHandle, $userRow['twitterid']]);
        if ($success) {
            return "Submission cancelled successfully.";
        } else {
            return "A database error occurred. Try again later or contact "
                    . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.";
        }
    }

    public static function submitArtistForApproval($userRow, $artistTwitterHandle) {
        $userInfo = self::getUserInfo($userRow['twitterid']);
        if (is_null($userInfo)) {
            return ["Your user information could not be found. Try logging in again or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }
        if ($userInfo === false) {
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        }
        $paidUser = $userInfo['paiduser'];
        $maxPendingSubmissions = self::getCachedVariable(CachedVariables::MAX_PENDING_SUBMISSIONS_FREE_USER);
        if (is_null($maxPendingSubmissions)) {
            return ["A database error occurred. Try again later or contact "
                . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.", null];
        } else if ($maxPendingSubmissions === false) {
            self::$logger->error("Error: could not find max pending submissions cached variable, defaulting to 5.");
            $maxPendingSubmissions = 5;
        }
        $selectQuery = "SELECT COUNT(*) FROM artistsubmissions WHERE submittedbyusertwitterid=? AND status=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userRow['twitterid'], "U"]);
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
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$artistTwitterHandle, $userRow['twitterid']]);
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
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
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
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
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
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
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
          $selectStmt = self::$databaseConnection->prepare($selectQuery);
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
        $insertStmt = self::$databaseConnection->prepare($insertQuery);
        $success = $insertStmt->execute([$artistTwitterHandle, $userRow['twitterid'], $artistTwitterID]);
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
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID]);
        if (!$success) {
            return null;
        }
        $retweetingCount = $selectStmt->fetchColumn();
        $maxRetweetingLimit = self::getCachedVariable(CachedVariables::MAX_ARTISTS_FREE_USER);
        if (is_null($maxRetweetingLimit) || $maxRetweetingLimit === false) {
            return null;
        }
        $selectQuery = "SELECT paiduser FROM users WHERE twitterid=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
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
            $stmt = self::$databaseConnection->prepare("INSERT INTO userartistretweetsettings (usertwitterid,artisttwitterid) "
                    . "VALUES (?,?)");
        } else {
            $stmt = self::$databaseConnection->prepare("DELETE FROM userartistretweetsettings WHERE usertwitterid=? AND artisttwitterid=?");
        }

        $success = $stmt->execute([$userTwitterID, $artistTwitterID]);
        if (!$success) {
            return null;
        }
        $affectedRows = $stmt->rowCount();
        $artistStmt = self::$databaseConnection->prepare("SELECT * FROM artists WHERE twitterid=?");
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
        $stmt = self::$databaseConnection->prepare("SELECT * FROM userartistautomationsettings WHERE usertwitterid=?");
        $success = $stmt->execute([$userTwitterID]);
        if (!$success) {
            return null;
        }
        $rows = $stmt->fetch();
        return $rows;
    }

    public static function getUserArtistRetweetSettingsCount($userTwitterID) {
        $stmt = self::$databaseConnection->prepare("SELECT COUNT(*) FROM userartistretweetsettings WHERE usertwitterid=?");
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
        $stmt = self::$databaseConnection->prepare("SELECT * FROM userartistretweetsettings INNER JOIN artists ON "
                . "userartistretweetsettings.artisttwitterid=artists.twitterid WHERE usertwitterid=? ORDER BY screenname ASC LIMIT 15 OFFSET $offSet");
        $success = $stmt->execute([$userTwitterID]);
        if (!$success) {
            return null;
        }
        $rows = $stmt->fetchAll();
        return $rows;
    }

    public static function searchArtistsForUser($searchString, $userTwitterID) {
        $stmt = self::$databaseConnection->prepare("SELECT * FROM artists WHERE twitterid NOT IN "
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
        $stmt = self::$databaseConnection->prepare("SELECT *,scheduledretweets.id AS schid, "
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
        $stmt = self::$databaseConnection->prepare("SELECT *,scheduledretweets.id AS schid FROM scheduledretweets "
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

    public static function insertOAuth2UserInformation($accessTokenObject, $userTwitterID) {
        $expiryDate = date("Y-m-d H:i:s", strtotime("+" . ($accessTokenObject->expires_in - 10) . " seconds"));
        $selectQuery = "SELECT twitterid FROM users WHERE twitterid=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $selectStmt->execute([$userTwitterID]);
        if ($selectStmt->fetchColumn() !== false) {
            $insertQuery = "UPDATE users SET accesstoken2=?, refreshtoken=?, oauthtype=?, scope=?, expirydate=? WHERE twitterid=?";
            $success = self::$databaseConnection->prepare($insertQuery)
                    ->execute([$accessTokenObject->access_token, $accessTokenObject->refresh_token,
                "2.0", $accessTokenObject->scope, $expiryDate, $userTwitterID]);
        } else {
            $insertQuery = "INSERT INTO users (twitterid,accesstoken2,refreshtoken,oauthtype,scope,expirydate) VALUES (?,?,?,?,?,?)";
            $success = self::$databaseConnection->prepare($insertQuery)
                    ->execute([$userTwitterID, $accessTokenObject->access_token, $accessTokenObject->refresh_token,
                "2.0", $accessTokenObject->scope, $expiryDate]);
        }
        return $success;
    }

    public static function insertUserInformation($access_token) {
        $accessToken = $access_token['oauth_token'];
        $accessTokenSecret = $access_token['oauth_token_secret'];
        $userTwitterID = $access_token['user_id'];
        $selectQuery = "SELECT twitterid FROM users WHERE twitterid=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $selectStmt->execute([$userTwitterID]);
        if ($selectStmt->fetchColumn() !== false) {
            $insertQuery = "UPDATE users SET accesstoken=?, accesstokensecret=? WHERE twitterid=?";
            $success = self::$databaseConnection->prepare($insertQuery)
                    ->execute([$accessToken, $accessTokenSecret, $userTwitterID]);
        } else {
            $insertQuery = "INSERT INTO users (twitterid,accesstoken,accesstokensecret) VALUES (?,?,?)";
            $success = self::$databaseConnection->prepare($insertQuery)
                    ->execute([$userTwitterID, $accessToken, $accessTokenSecret]);
        }
        return $success;
    }

    public static function rescheduleQueuedTweet($queueID, $userTwitterID, $newTime) {
        $userSelectQuery = "SELECT * FROM userautomationsettings WHERE usertwitterid=?";
        $userSelectStmt = self::$databaseConnection->prepare($userSelectQuery);
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
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $result = $selectStmt->execute([$queueID, $userTwitterID]);
        if (!$result) {
            return "Database error retrieving tweet ID";
        }
        $tweetid = $selectStmt->fetchColumn();
        if (!$tweetid) {
            return "Tweet ID not found";
        }
        $canRequeue = RetweetScheduler::checkUserCanQueueNewRetweet($userTwitterID, $serverTimestamp, $queueID);
        if ($canRequeue) {
            $result = self::updateQueuedRetweet($userTwitterID, $queueID, $serverTimeString);
        } else {
            return $canRequeue;
        }
    }

    // Updates a queue entry without checking if it's allowed or not (uses queue ID, not tweet ID).
    public static function updateQueuedRetweet($userTwitterID, $queueID, $retweetTime) {
        $insertStmt = self::$databaseConnection->prepare("UPDATE scheduledretweets SET retweettime=? WHERE id=? AND retweetingusertwitterid=?");
        $result = $insertStmt->execute([$retweetTime, $queueID, $userTwitterID]);
        if (!$result) {
            return false;
        }
        return $insertStmt->rowCount();
    }

    public static function deleteQueuedRetweet($queueID, $userTwitterID) {
        self::$logger->debug("Deleting queue entry with ID: $queueID and user twitter ID: $userTwitterID");
        $statement = self::$databaseConnection->prepare("DELETE FROM scheduledretweets WHERE id=? AND retweetingusertwitterid=?");
        $result = $statement->execute([$queueID, $userTwitterID]);
        if (!$result) {
            return false;
        }
        $rowCount = $statement->rowCount();
        return $rowCount;
    }

    public static function getUserInfo($userTwitterID) {
        $stmt = self::$databaseConnection->prepare("SELECT * FROM users LEFT JOIN "
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

    public static function commitNonArtistAutomationSettingsInDB($aS) {
        // Get old automation settings: check if they existed, or were disabled. If so, schedule new automated retweets immediately
        $selectQuery = "SELECT * FROM userartistautomationsettings WHERE usertwitterid=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $result = $selectStmt->execute([$aS['usertwitterid']]);
        if (!$result) {
            self::$logger->error("Failed to retrieve previous automation settings");
            return TwitterResponseStatus::ARTRETWEETER_DATABASE_ERROR;
        }
        $oldRow = $selectStmt->fetch();
        $oldSettingsExist = true;
        $automationWasDisabled = false;
        $lastScheduleServerDate = null;
        if (!$oldRow) {
            $oldSettingsExist = false;
            $automationWasDisabled = true;
        } else if ($oldRow && $oldRow['automationenabled'] === "N") {
            $automationWasDisabled = true;
            $lastScheduleServerDate = $oldRow['lastscheduleserverdate'];
        } else {
            $lastScheduleServerDate = $oldRow['lastscheduleserverdate'];
        }
        $selectQuery = "SELECT COUNT(*) AS c FROM scheduledretweets WHERE retweetingusertwitterid=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $result = $selectStmt->execute([$aS['usertwitterid']]);
        if (!$result) {
            self::$logger->error("Failed to retrieve current scheduled retweet count");
            return TwitterResponseStatus::ARTRETWEETER_DATABASE_ERROR;
        }
        $currentScheduledRetweetCount = $selectStmt->fetchColumn();
        $scheduleNewRetweetsNow = false;
        $lastScheduleServerDate = strtotime($lastScheduleServerDate);
        $yesterday = strtotime("-1 day");
        if ($oldRow && $oldRow['automationenabled'] === "N" && $aS['automationenabled'] === "Y" &&
                ($currentScheduledRetweetCount === 0 || (is_null($lastScheduleServerDate) || $lastScheduleServerDate <= $yesterday))) {
            $scheduleNewRetweetsNow = true;
        }
        $userTimeZoneHour = $aS['timezonehouroffset'];
        $userTimeZoneMinute = $aS['timezoneminuteoffset'];
        $userOffsetSeconds = ($userTimeZoneHour * 3600) + ($userTimeZoneMinute * 60);
        $now = new \DateTime();
        $serverTimeZone = new \DateTimeZone(date_default_timezone_get());
        $serverOffsetSeconds = $serverTimeZone->getOffset($now);
        $timeDiffSeconds = $serverOffsetSeconds - $userOffsetSeconds;
        $timeDiffHours = intval(floor($timeDiffSeconds / 3600));
        $hourFlags = MiscTools::shiftString($aS['hourflags'], -$timeDiffHours);
        if (isset($aS['oldtweetcutoffdate'])) {
            $oldTweetCutoffDate = date("Y-m-d H:i:s", strtotime($aS['oldtweetcutoffdate']) + $timeDiffSeconds);
        } else {
            $oldTweetCutoffDate = null;
        }
        if (!isset($aS['imagesenabled'])) {
            $aS['imagesenabled'] = "Y";
        }
        if (!isset($aS['gifsenabled'])) {
            $aS['gifsenabled'] = "N";
        }
        if (!isset($aS['videosenabled'])) {
            $aS['videosenabled'] = "N";
        }
        $query = "INSERT INTO userartistautomationsettings (usertwitterid,automationenabled,dayflags,hourflags,includetext,excludetext"
                . ",oldtweetcutoffdate,oldtweetcutoffdateenabled,includetextenabled,excludetextenabled,timezonehouroffset,"
                . "timezoneminuteoffset,includetextcondition,excludetextcondition,imagesenabled,gifsenabled,videosenabled,settingstype) "
                . "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                . "ON DUPLICATE KEY UPDATE automationenabled=?, dayflags=?, hourflags=?, includetext=?, excludetext=?, "
                . "oldtweetcutoffdate=?, oldtweetcutoffdateenabled=?, includetextenabled=?, excludetextenabled=?, timezonehouroffset=?, "
                . "timezoneminuteoffset=?, includetextcondition=?, excludetextcondition=?, imagesenabled=?, gifsenabled=?, "
                . "videosenabled=?, settingstype=?";
        $stmt = self::$databaseConnection->prepare($query);
        try {
            $success = $stmt->execute([$aS['usertwitterid'], $aS['automationenabled'],
                $aS['dayflags'], $hourFlags, $aS['includetext'], $aS['excludetext'],
                $oldTweetCutoffDate, $aS['oldtweetcutoffdateenabled'],
                $aS['includetextenabled'], $aS['excludetextenabled'], $userTimeZoneHour, $userTimeZoneMinute, $aS['includetextcondition'],
                $aS['excludetextcondition'], $aS['imagesenabled'], $aS['gifsenabled'], $aS['videosenabled'], $aS['settingstype'],
                $aS['automationenabled'], $aS['dayflags'], $hourFlags, $aS['includetext'], $aS['excludetext'],
                $oldTweetCutoffDate, $aS['oldtweetcutoffdateenabled'], $aS['includetextenabled'], $aS['excludetextenabled'], $userTimeZoneHour,
                $userTimeZoneMinute, $aS['includetextcondition'], $aS['excludetextcondition'],
                $aS['imagesenabled'], $aS['gifsenabled'], $aS['videosenabled'], $aS['settingstype']]);
            if (!$success) {
                self::$logger->error("Failed to insert artist automation settings");
                return TwitterResponseStatus::ARTRETWEETER_DATABASE_ERROR;
            } else if ($aS['automationenabled'] === "N") {
                $deleteQuery = "DELETE FROM scheduledretweets WHERE retweetingusertwitterid=? AND automated=?";
                $deleteStmt = self::$databaseConnection->prepare($deleteQuery);
                $success = $deleteStmt->execute([$aS['usertwitterid'], "Y"]);
            } /* else if ($scheduleNewRetweetsNow) {
              $selectStmt = self::$databaseConnection->prepare("SELECT * FROM users WHERE twitterid=?");
              $result = $selectStmt->execute([$aS['usertwitterid']]);
              $userRow = $selectStmt->fetch();
              $userAuth['twitter_id'] = $aS['usertwitterid'];
              $userAuth['access_token'] = $userRow['accesstoken'];
              $userAuth['access_token_secret'] = $userRow['accesstokensecret'];
              Core::scheduleUserArtistRetweets($userRow);
              } */
        } catch (PDOException $e) {
            self::$logger->error("Failed to insert automation settings: " . print_r($e, true));
            return TwitterResponseStatus::ARTRETWEETER_DATABASE_ERROR;
        }
        return TwitterResponseStatus::ARTRETWEETER_QUERY_OK;
    }

    public static function commitAutomationSettingsInDB($aS) {
        // Get old automation settings: check if they existed, or were disabled. If so, schedule new automated retweets immediately
        $selectQuery = "SELECT * FROM userautomationsettings WHERE usertwitterid=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $result = $selectStmt->execute([$aS['usertwitterid']]);
        if (!$result) {
            self::$logger->error("Failed to retrieve previous automation settings");
            return TwitterResponseStatus::ARTRETWEETER_DATABASE_ERROR;
        }
        $oldRow = $selectStmt->fetch();
        $oldSettingsExist = true;
        $automationWasDisabled = false;
        $lastScheduleServerDate = null;
        if (!$oldRow) {
            $oldSettingsExist = false;
            $automationWasDisabled = true;
        } else if ($oldRow && $oldRow['automationenabled'] === "N") {
            $automationWasDisabled = true;
            $lastScheduleServerDate = $oldRow['lastscheduleserverdate'];
        } else {
            $lastScheduleServerDate = $oldRow['lastscheduleserverdate'];
        }
        $selectQuery = "SELECT COUNT(*) AS c FROM scheduledretweets WHERE retweetingusertwitterid=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        $result = $selectStmt->execute([$aS['usertwitterid']]);
        if (!$result) {
            self::$logger->error("Failed to retrieve current scheduled retweet count");
            return TwitterResponseStatus::ARTRETWEETER_DATABASE_ERROR;
        }
        $currentScheduledRetweetCount = $selectStmt->fetchColumn();
        $scheduleNewRetweetsNow = false;
        $lastScheduleServerDate = strtotime($lastScheduleServerDate);
        $yesterday = strtotime("-1 day");
        if ($oldRow && $oldRow['automationenabled'] === "N" && $aS['automationenabled'] === "Y" &&
                ($currentScheduledRetweetCount === 0 || (is_null($lastScheduleServerDate) || $lastScheduleServerDate <= $yesterday))) {
            $scheduleNewRetweetsNow = true;
        }
        $userTimeZoneHour = $aS['timezonehouroffset'];
        $userTimeZoneMinute = $aS['timezoneminuteoffset'];
        $userOffsetSeconds = ($userTimeZoneHour * 3600) + ($userTimeZoneMinute * 60);
        $now = new \DateTime();
        $serverTimeZone = new \DateTimeZone(date_default_timezone_get());
        $serverOffsetSeconds = $serverTimeZone->getOffset($now);
        $timeDiffSeconds = $serverOffsetSeconds - $userOffsetSeconds;
        $timeDiffHours = intval(floor($timeDiffSeconds / 3600));
        $timeDiffMinutes = intval(floor(($timeDiffSeconds % 3600) / 60));
        $hourFlags = MiscTools::shiftString($aS['hourflags'], -$timeDiffHours);
        $minuteFlags = MiscTools::shiftString($aS['minuteflags'], -$timeDiffMinutes / 15);
        if (isset($aS['oldtweetcutoffdate'])) {
            $oldTweetCutoffDate = date("Y-m-d H:i:s", strtotime($aS['oldtweetcutoffdate']) + $timeDiffSeconds);
        } else {
            $oldTweetCutoffDate = null;
        }
        if (!isset($aS['retweetpercent'])) {
            $retweetPercent = null;
        } else {
            $retweetPercent = $aS['retweetpercent'];
        }
        if (!isset($aS['imagesenabled'])) {
            $aS['imagesenabled'] = "Y";
        }
        if (!isset($aS['gifsenabled'])) {
            $aS['gifsenabled'] = "N";
        }
        if (!isset($aS['videosenabled'])) {
            $aS['videosenabled'] = "N";
        }
        $query = "INSERT INTO userautomationsettings (usertwitterid,automationenabled,dayflags,hourflags,minuteflags,includetext,excludetext"
                . ",retweetpercent,oldtweetcutoffdate,oldtweetcutoffdateenabled,includetextenabled,excludetextenabled,timezonehouroffset,"
                . "timezoneminuteoffset,includetextcondition,excludetextcondition,metricsmeasurementtype,imagesenabled,gifsenabled,videosenabled,settingstype) "
                . "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                . "ON DUPLICATE KEY UPDATE automationenabled=?, dayflags=?, hourflags=?, minuteflags=?, includetext=?, excludetext=?, retweetpercent=?,"
                . "oldtweetcutoffdate=?, oldtweetcutoffdateenabled=?, includetextenabled=?, excludetextenabled=?, timezonehouroffset=?, "
                . "timezoneminuteoffset=?, includetextcondition=?, excludetextcondition=?, metricsmeasurementtype=?, imagesenabled=?, gifsenabled=?, "
                . "videosenabled=?, settingstype=?";
        $stmt = self::$databaseConnection->prepare($query);
        try {
            $success = $stmt->execute([$aS['usertwitterid'], $aS['automationenabled'],
                $aS['dayflags'], $hourFlags, $minuteFlags, $aS['includetext'], $aS['excludetext'],
                $retweetPercent, $oldTweetCutoffDate, $aS['oldtweetcutoffdateenabled'],
                $aS['includetextenabled'], $aS['excludetextenabled'], $userTimeZoneHour, $userTimeZoneMinute, $aS['includetextcondition'],
                $aS['excludetextcondition'], $aS['metricsmeasurementtype'], $aS['imagesenabled'], $aS['gifsenabled'], $aS['videosenabled'], $aS['settingstype'],
                $aS['automationenabled'], $aS['dayflags'], $hourFlags, $minuteFlags, $aS['includetext'], $aS['excludetext'], $retweetPercent,
                $oldTweetCutoffDate, $aS['oldtweetcutoffdateenabled'], $aS['includetextenabled'], $aS['excludetextenabled'], $userTimeZoneHour,
                $userTimeZoneMinute, $aS['includetextcondition'], $aS['excludetextcondition'], $aS['metricsmeasurementtype'],
                $aS['imagesenabled'], $aS['gifsenabled'], $aS['videosenabled'], $aS['settingstype']]);
            if (!$success) {
                self::$logger->error("Failed to insert automation settings");
                return TwitterResponseStatus::ARTRETWEETER_DATABASE_ERROR;
            } else if ($aS['automationenabled'] === "N") {
                $deleteQuery = "DELETE FROM scheduledretweets WHERE retweetingusertwitterid=? AND automated=?";
                $deleteStmt = self::$databaseConnection->prepare($deleteQuery);
                $success = $deleteStmt->execute([$aS['usertwitterid'], "Y"]);
            } else if ($scheduleNewRetweetsNow) {
                $selectStmt = self::$databaseConnection->prepare("SELECT * FROM users WHERE twitterid=?");
                $result = $selectStmt->execute([$aS['usertwitterid']]);
                $userRow = $selectStmt->fetch();
                RetweetScheduler::queueAutomatedRetweets($userRow);
            }
        } catch (PDOException $e) {
            self::$logger->error("Failed to insert automation settings: " . print_r($e, true));
            return TwitterResponseStatus::ARTRETWEETER_DATABASE_ERROR;
        }
        return TwitterResponseStatus::ARTRETWEETER_QUERY_OK;
    }

}

CoreDB::initialiseLogger();
CoreDB::initialiseConnection();

