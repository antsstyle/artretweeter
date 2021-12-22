<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Credentials\DB;

class CoreDB {

    const options = [
        \PDO::ATTR_DEFAULT_FETCH_MODE => \PDO::FETCH_ASSOC,
    ];

    public static $databaseConnection;

    public static function initialiseConnection() {
        try {
            $params = "mysql:host=" . DB::server_name . ";dbname=" . DB::database . ";port=" . DB::port . ";charset=UTF8MB4";
            CoreDB::$databaseConnection = new \PDO($params, DB::username, DB::password, CoreDB::options);
        } catch (\Exception $e) {
            error_log("Failed to create database connection.");
            echo "Failed to create database connection.";
            exit();
        }
        CoreDB::$databaseConnection->setAttribute(\PDO::ATTR_ERRMODE, \PDO::ERRMODE_EXCEPTION);
    }

    public static function getUserRetweetQueue($userTwitterID) {
        $stmt = CoreDB::$databaseConnection->prepare("SELECT *,scheduledretweets.id AS schid FROM scheduledretweets "
                . "INNER JOIN tweets ON scheduledretweets.tweetid=tweets.tweetid "
                . "WHERE retweetingusertwitterid=? ORDER BY retweettime ASC");
        $success = $stmt->execute([$userTwitterID]);
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
        $canRequeue = Core::checkUserCanQueueNewRetweet($userTwitterID, $serverTimestamp, false, $queueID);
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
        error_log("Deleting queue entry with ID: $queueID and user twitter ID: $userTwitterID");
        $statement = CoreDB::$databaseConnection->prepare("DELETE FROM scheduledretweets WHERE id=? AND retweetingusertwitterid=?");
        $result = $statement->execute([$queueID, $userTwitterID]);
        if (!$result) {
            return false;
        }
        $rowCount = $statement->rowCount();
        return $rowCount;
    }

}

CoreDB::initialiseConnection();

