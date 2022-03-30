<?php

namespace Antsstyle\ArtRetweeter\DB;

use Antsstyle\ArtRetweeter\Core\CachedVariables;
use Antsstyle\ArtRetweeter\DB\CoreDB;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Antsstyle\ArtRetweeter\Core\MiscTools;
use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Core\RetweetScheduler;
use Antsstyle\ArtRetweeter\Core\TwitterResponseStatus;

class UserDB {

    private static $logger;

    public static function updateUserAddArtistsViewMode($userTwitterID, $viewMode) {
        $updateQuery = "UPDATE userartistautomationsettings SET addartistsview=? WHERE usertwitterid=?";
        $updateStmt = CoreDB::getConnection()->prepare($updateQuery);
        try {
            $updateStmt->execute([$viewMode, $userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Unable to update add artists view mode for user twitter ID $userTwitterID" . print_r($e, true));
            return false;
        }
        return true;
    }

    public static function lockUser($userTwitterID, $lockReason, $deletionDate = null) {
        if (is_null($lockReason) || !is_string($lockReason) || $lockReason === "") {
            self::$logger->critical("Attempting to lock user twitter ID $userTwitterID with invalid lock reason - aborting lock.");
            return false;
        }
        if ($lockReason === "Unauthorised" && is_null($deletionDate)) {
            self::$logger->critical("Attempting to lock user twitter ID $userTwitterID for unauthorised token, but deletion date is null - aborting lock.");
            return false;
        }
        $updateStmt = CoreDB::getConnection()->prepare("UPDATE users SET locked=?, deletiondate=?, lockreason=? WHERE twitterid=?");
        try {
            $updateStmt->execute(["Y", $deletionDate, $lockReason, $userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Unable to lock user twitter ID $userTwitterID, could not update users table: " . print_r($e, true));
            return false;
        }
        $deleteStmt = CoreDB::getConnection()->prepare("DELETE FROM scheduledretweets WHERE retweetingusertwitterid=?");
        try {
            $deleteStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Unable to lock user twitter ID $userTwitterID, could not delete scheduled retweets: " . print_r($e, true));
            return false;
        }
    }

    public static function deleteUser($userTwitterID) {
        $deleteStmt = CoreDB::getConnection()->prepare("DELETE FROM scheduledretweets WHERE retweetingusertwitterid=?");
        try {
            $deleteStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Unable to delete user twitter ID $userTwitterID, could not delete scheduled retweets: " . print_r($e, true));
            return false;
        }
        $selectStmt = CoreDB::getConnection()->prepare("SELECT * FROM artists WHERE twitterid=?");
        try {
            $selectStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Unable to delete user twitter ID $userTwitterID, could not check for artist table presence: " . print_r($e, true));
            return false;
        }
        $artistRow = $selectStmt->fetch();
        if ($artistRow === false) {
            $deleteStmt = CoreDB::getConnection()->prepare("DELETE FROM tweets WHERE usertwitterid=?");
            try {
                $deleteStmt->execute([$userTwitterID]);
            } catch (\PDOException $e) {
                self::$logger->error("Unable to delete user twitter ID $userTwitterID, could not delete tweets: " . print_r($e, true));
                return false;
            }
        }

        $deleteStmt = CoreDB::getConnection()->prepare("DELETE FROM users WHERE twitterid=?");
        try {
            $deleteStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Unable to delete user twitter ID $userTwitterID, could not delete from users table: " . print_r($e, true));
            return false;
        }
        return true;
    }

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function searchArtistsForUser($searchString, $userTwitterID) {
        $stmt = CoreDB::getConnection()->prepare("SELECT * FROM artists WHERE twitterid NOT IN "
                . "(SELECT artisttwitterid FROM userartistretweetsettings WHERE usertwitterid=?) AND screenname LIKE ?");
        try {
            $stmt->execute([$userTwitterID, $searchString]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to search artists for user: " . print_r($e, true));
            return null;
        }

        $rows = $stmt->fetchAll();
        $returnArray['resultcount'] = $stmt->rowCount();
        $returnArray['rows'] = $rows;
        return $returnArray;
    }

    public static function getUserNonArtistRetweetQueue($userTwitterID) {
        $stmt = CoreDB::getConnection()->prepare("SELECT *,scheduledretweets.id AS schid, "
                . "(SELECT screenname FROM artists WHERE artists.twitterid=tweets.usertwitterid) AS screenname FROM scheduledretweets "
                . "INNER JOIN tweets ON scheduledretweets.tweetid=tweets.tweetid WHERE retweetingusertwitterid=? "
                . "AND tweetauthorid != ? ORDER BY retweettime ASC");
        try {
            $stmt->execute([$userTwitterID, $userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get user non-artist retweet queue: " . print_r($e, true));
            return false;
        }

        $rows = $stmt->fetchAll();
        if ($rows) {
            return $rows;
        }
        return null;
    }

    public static function getUserRetweetQueue($userTwitterID) {
        $stmt = CoreDB::getConnection()->prepare("SELECT *,scheduledretweets.id AS schid FROM scheduledretweets "
                . "INNER JOIN tweets ON scheduledretweets.tweetid=tweets.tweetid "
                . "WHERE retweetingusertwitterid=? AND tweetauthorid=? ORDER BY retweettime ASC");
        try {
            $stmt->execute([$userTwitterID, $userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get user retweet queue: " . print_r($e, true));
            return false;
        }

        $rows = $stmt->fetchAll();
        if ($rows) {
            return $rows;
        }
        return null;
    }

    public static function getUserArtistAutomationSettings($userTwitterID) {
        $selectStmt = CoreDB::getConnection()->prepare("SELECT * FROM userartistautomationsettings WHERE usertwitterid=?");
        try {
            $selectStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get user artist automation settings for user twitter ID $userTwitterID: " . print_r($e, true));
            return null;
        }

        return $selectStmt->fetch();
    }

    public static function getUserArtistRetweetSettings($userTwitterID, $pageNum = 1, $pageCount = 10) {
        if (!is_numeric($pageNum)) {
            $pageNum = 1;
        }
        if (!is_numeric($pageCount)) {
            $pageCount = 10;
        }
        $offSet = ($pageNum - 1) * $pageCount;
        $stmt = CoreDB::getConnection()->prepare("SELECT * FROM userartistretweetsettings INNER JOIN artists ON "
                . "userartistretweetsettings.artisttwitterid=artists.twitterid WHERE usertwitterid=? "
                . "ORDER BY screenname ASC LIMIT $pageCount OFFSET $offSet");
        try {
            $stmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get user artist retweet settings: " . print_r($e, true));
            return null;
        }

        $rows = $stmt->fetchAll();
        return $rows;
    }

    public static function checkUserRateLimit($userTwitterID, $endpoint) {
        $stmt = CoreDB::getConnection()->prepare("SELECT * FROM ratelimitrecords WHERE usertwitterid=? AND endpoint=?");
        try {
            $stmt->execute([$userTwitterID, $endpoint]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get user rate limit records: " . print_r($e, true));
            return true;
        }

        $result = $stmt->fetch();

        if ($result && $result['remaininglimit'] < 5) {
            return false;
        } else {
            return true;
        }
    }

    public static function updateUserRateLimit($userTwitterID, $endpoint, $headers) {
        if (!isset($headers['x_rate_limit_remaining']) || !isset($headers['x_rate_limit_limit']) || !isset($headers['x_rate_limit_reset'])) {
            return true;
        }
        if ($userTwitterID === APIKeys::bearer_token) {
            return true;
        }
        $maxLimit = $headers['x_rate_limit_limit'];
        $remainingLimit = $headers['x_rate_limit_remaining'];
        $resetTime = date('Y-m-d H:i:s', $headers['x_rate_limit_reset']);
        $timeToResetSeconds = $headers['x_rate_limit_reset'] - time();
        $stmt = CoreDB::getConnection()->prepare("INSERT INTO ratelimitrecords (usertwitterid,endpoint,maxlimit,remaininglimit,resettime,timetoresetseconds) 
	VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE usertwitterid=?, endpoint=?, maxlimit=?, remaininglimit=?, resettime=?, timetoresetseconds=?");
        try {
            $stmt->execute([$userTwitterID, $endpoint, $maxLimit, $remainingLimit, $resetTime, $timeToResetSeconds,
                $userTwitterID, $endpoint, $maxLimit, $remainingLimit, $resetTime, $timeToResetSeconds]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to insert user rate limit records: " . print_r($e, true));
            return false;
        }
        return true;
    }

    public static function updateRetweetRecords($userTwitterID, $tweetID, $scheduledTime, $tweetAuthorID, $selfRetweet) {
        $stmt = CoreDB::getConnection()->prepare("INSERT INTO retweetrecords (usertwitterid,tweetid,retweettime,scheduledretweettime) 
	VALUES (?,?,?,?)");
        $currentTime = date("Y-m-d H:i:s", time());
        try {
            $stmt->execute([$userTwitterID, $tweetID, $currentTime, $scheduledTime]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to insert entry into retweet records: " . print_r($e, true));
        }

        if (!$selfRetweet) {
            $stmt = CoreDB::getConnection()->prepare("UPDATE userartistretweetsettings SET totalretweeted=totalretweeted+1 WHERE usertwitterid=? "
                    . "AND artisttwitterid=?");
            try {
                $stmt->execute([$userTwitterID, $tweetAuthorID]);
            } catch (\PDOException $e) {
                self::$logger->error("Failed to update user artist retweet settings with new totalretweeted count: " . print_r($e, true));
            }
        }
    }

    public static function getUserInfo($userTwitterID) {
        $stmt = CoreDB::getConnection()->prepare("SELECT * FROM users LEFT JOIN "
                . "userautomationsettings ON users.twitterid=userautomationsettings.usertwitterid WHERE twitterid=?");
        try {
            $stmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get user info from DB: " . print_r($e, true));
            return false;
        }

        $row = $stmt->fetch();
        if ($row) {
            return $row;
        }
        return null;
    }

    public static function insertOAuth2UserInformation($accessTokenObject, $userTwitterID) {
        $expiryDate = date("Y-m-d H:i:s", strtotime("+" . ($accessTokenObject->expires_in - 10) . " seconds"));
        $selectQuery = "SELECT twitterid FROM users WHERE twitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get user twitter ID from user table: " . print_r($e, true));
            return false;
        }

        if ($selectStmt->fetchColumn() !== false) {
            $insertQuery = "UPDATE users SET accesstoken2=?, refreshtoken=?, oauthtype=?, scope=?, expirydate=?, locked=? WHERE twitterid=?";
            try {
                CoreDB::getConnection()->prepare($insertQuery)
                        ->execute([$accessTokenObject->access_token, $accessTokenObject->refresh_token,
                            "2.0", $accessTokenObject->scope, $expiryDate, "N", $userTwitterID]);
            } catch (\PDOException $e) {
                self::$logger->critical("Failed to update user row with OAuth2 credentials: " . print_r($e, true));
                return false;
            }
        } else {
            $insertQuery = "INSERT INTO users (twitterid,accesstoken2,refreshtoken,oauthtype,scope,expirydate,locked) VALUES (?,?,?,?,?,?,?)";
            try {
                CoreDB::getConnection()->prepare($insertQuery)
                        ->execute([$userTwitterID, $accessTokenObject->access_token, $accessTokenObject->refresh_token,
                            "2.0", $accessTokenObject->scope, $expiryDate, "N"]);
            } catch (\PDOException $e) {
                self::$logger->critical("Failed to insert user row with OAuth2 credentials: " . print_r($e, true));
                return false;
            }
        }
        return true;
    }

    public static function getPendingArtistSubmissionsForUser($userTwitterID) {
        $selectQuery = "SELECT * FROM artistsubmissions WHERE artisttwitterid NOT IN (SELECT twitterid FROM artists) "
                . "AND artisttwitterid NOT IN (SELECT artisttwitterid FROM rejectedartistsubmissions) AND submittedbyusertwitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->critical("Failed to get artist submissions for user ID $userTwitterID - cannot display on webpage: " . print_r($e, true));
            return null;
        }

        $rows = $selectStmt->fetchAll();
        return $rows;
    }

    public static function checkIfUserIsBanned($userTwitterID) {
        $selectQuery = "SELECT * FROM bannedusers WHERE twitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->critical("Failed to get user ban info: " . print_r($e, true));
            return null;
        }

        $row = $selectStmt->fetch();
        if ($row !== false) {
            return $row;
        }
        $selectQuery = "SELECT * FROM nftcryptoblockercentralisedblocklist WHERE blockableusertwitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->critical("Failed to get user ban info: " . print_r($e, true));
            return null;
        }

        $row = $selectStmt->fetch();
        if ($row !== false) {
            return $row;
        }
        return false;
    }

    public static function updateArtistForUser($userTwitterID, $artistTwitterID, $operation) {
        if ($operation !== "Enable" && $operation !== "Disable") {
            return null;
        }
        if ($userTwitterID === $artistTwitterID) {
            return "You cannot use this page to add retweets for yourself. Use the artists settings page for that.";
        }
        $selectQuery = "SELECT COUNT(*) AS retweetingcount FROM userartistretweetsettings WHERE usertwitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get retweeting count from userartistretweetsettings: " . print_r($e, true));
            return null;
        }

        $retweetingCount = $selectStmt->fetchColumn();
        $maxRetweetingLimit = CoreDB::getCachedVariable(CachedVariables::MAX_ARTISTS_FREE_USER);
        if (is_null($maxRetweetingLimit) || $maxRetweetingLimit === false) {
            return null;
        }
        $selectQuery = "SELECT paiduser FROM users WHERE twitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get paiduser info for user from DB: " . print_r($e, true));
            return null;
        }

        $paidUser = $selectStmt->fetchColumn();
        if ($retweetingCount >= $maxRetweetingLimit && $operation === "Enable" && $paidUser !== "Y") {
            return "You are already retweeting the maximum number of artists allowed. To increase your limit, you can "
                    . "<a href=\"" . Config::HOMEPAGE_URL . "subscribe\">subscribe</a>.";
        }

        if ($operation === "Enable") {
            $stmt = CoreDB::getConnection()->prepare("INSERT INTO userartistretweetsettings (usertwitterid,artisttwitterid) "
                    . "VALUES (?,?)");
        } else {
            $stmt = CoreDB::getConnection()->prepare("DELETE FROM userartistretweetsettings WHERE usertwitterid=? AND artisttwitterid=?");
        }

        try {
            $stmt->execute([$userTwitterID, $artistTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to change user artist retweet settings: " . print_r($e, true));
            return null;
        }

        $affectedRows = $stmt->rowCount();
        $artistStmt = CoreDB::getConnection()->prepare("SELECT * FROM artists WHERE twitterid=?");
        try {
            $artistStmt->execute([$artistTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to select artist from DB: " . print_r($e, true));
            return null;
        }

        $artistRow = $artistStmt->fetch();
        $returnArray['screenname'] = $artistRow['screenname'];
        $returnArray['artisttwitterid'] = $artistRow['twitterid'];
        $returnArray['affectedrows'] = $affectedRows;
        return $returnArray;
    }

    public static function getUserArtistRetweetSettingsCount($userTwitterID) {
        $stmt = CoreDB::getConnection()->prepare("SELECT COUNT(*) FROM userartistretweetsettings WHERE usertwitterid=?");
        try {
            $stmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to retrieve user artist retweet settings count: " . print_r($e, true));
            return null;
        }

        return $stmt->fetchColumn();
    }

}

UserDB::initialiseLogger();
