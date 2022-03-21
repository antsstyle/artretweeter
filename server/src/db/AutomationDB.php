<?php

namespace Antsstyle\ArtRetweeter\DB;

use Antsstyle\ArtRetweeter\DB\CoreDB;
use Antsstyle\ArtRetweeter\Core\MiscTools;
use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Core\RetweetScheduler;
use Antsstyle\ArtRetweeter\Core\TwitterResponseStatus;

class AutomationDB {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function getNonArtistAutomationSettings($userTwitterID) {
        $selectQuery = "SELECT * FROM userartistautomationsettings WHERE usertwitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get user non-artist automation settings: " . print_r($e, true));
            return false;
        }

        $row = $selectStmt->fetch();
        if (!$row) {
            return null;
        } else {
            $userTimeZoneHour = $row['timezonehouroffset'];
            $userTimeZoneMinute = $row['timezoneminuteoffset'];
            $userOffsetSeconds = ($userTimeZoneHour * 3600) + ($userTimeZoneMinute * 60);
            $now = new \DateTime();
            $serverTimeZone = new \DateTimeZone(date_default_timezone_get());
            $serverOffsetSeconds = $serverTimeZone->getOffset($now);
            $timeDiffSeconds = $serverOffsetSeconds - $userOffsetSeconds;
            $timeDiffHours = intval(floor($timeDiffSeconds / 3600));
            if ($row['oldtweetcutoffdate'] !== null) {
                $oldTweetCutoffDate = date("Y-m-d H:i:s", strtotime($row['oldtweetcutoffdate']) - $timeDiffSeconds);
                $row['oldtweetcutoffdate'] = $oldTweetCutoffDate;
            }
            $row['hourflags'] = MiscTools::shiftString($row['hourflags'], $timeDiffHours);
            return $row;
        }
    }

    public static function getAutomationSettings($userTwitterID) {
        $selectQuery = "SELECT * FROM userautomationsettings WHERE usertwitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get user automation settings: " . print_r($e, true));
            return false;
        }

        $row = $selectStmt->fetch();
        if (!$row) {
            return null;
        } else {
            $userTimeZoneHour = $row['timezonehouroffset'];
            $userTimeZoneMinute = $row['timezoneminuteoffset'];
            $userOffsetSeconds = ($userTimeZoneHour * 3600) + ($userTimeZoneMinute * 60);
            $now = new \DateTime();
            $serverTimeZone = new \DateTimeZone(date_default_timezone_get());
            $serverOffsetSeconds = $serverTimeZone->getOffset($now);
            $timeDiffSeconds = $serverOffsetSeconds - $userOffsetSeconds;
            $timeDiffHours = intval(floor($timeDiffSeconds / 3600));
            $timeDiffMinutes = intval(floor(($timeDiffSeconds % 3600) / 60));
            if ($row['oldtweetcutoffdate'] !== null) {
                $oldTweetCutoffDate = date("Y-m-d H:i:s", strtotime($row['oldtweetcutoffdate']) - $timeDiffSeconds);
                $row['oldtweetcutoffdate'] = $oldTweetCutoffDate;
            }
            $row['hourflags'] = MiscTools::shiftString($row['hourflags'], $timeDiffHours);
            $row['minuteflags'] = MiscTools::shiftString($row['minuteflags'], $timeDiffMinutes / 15);
            return $row;
        }
    }

    public static function commitNonArtistAutomationSettings($aS) {
        // Get old automation settings: check if they existed, or were disabled. If so, schedule new automated retweets immediately
        $selectQuery = "SELECT * FROM userartistautomationsettings WHERE usertwitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$aS['usertwitterid']]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to retrieve previous automation settings: " . print_r($e, true));
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
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$aS['usertwitterid']]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to retrieve current scheduled retweet count: " . print_r($e, true));
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
        $stmt = CoreDB::getConnection()->prepare($query);
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
                $deleteStmt = CoreDB::getConnection()->prepare($deleteQuery);
                $success = $deleteStmt->execute([$aS['usertwitterid'], "Y"]);
            } /* else if ($scheduleNewRetweetsNow) {
              $selectStmt = CoreDB::getConnection()->prepare("SELECT * FROM users WHERE twitterid=?");
              $result = $selectStmt->execute([$aS['usertwitterid']]);
              $userRow = $selectStmt->fetch();
              $userAuth['twitter_id'] = $aS['usertwitterid'];
              $userAuth['access_token'] = $userRow['accesstoken'];
              $userAuth['access_token_secret'] = $userRow['accesstokensecret'];
              Core::scheduleUserArtistRetweets($userRow);
              } */
        } catch (\PDOException $e) {
            self::$logger->error("Failed to insert automation settings: " . print_r($e, true));
            return TwitterResponseStatus::ARTRETWEETER_DATABASE_ERROR;
        }
        return TwitterResponseStatus::ARTRETWEETER_QUERY_OK;
    }

    public static function commitAutomationSettings($aS) {
        // Get old automation settings: check if they existed, or were disabled. If so, schedule new automated retweets immediately
        $selectQuery = "SELECT * FROM userautomationsettings WHERE usertwitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$aS['usertwitterid']]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to retrieve previous automation settings: " . print_r($e, true));
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
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$aS['usertwitterid']]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to retrieve current scheduled retweet count: " . print_r($e, true));
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
        $stmt = CoreDB::getConnection()->prepare($query);
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
                $deleteStmt = CoreDB::getConnection()->prepare($deleteQuery);
                $success = $deleteStmt->execute([$aS['usertwitterid'], "Y"]);
            } else if ($scheduleNewRetweetsNow) {
                $selectStmt = CoreDB::getConnection()->prepare("SELECT * FROM users WHERE twitterid=?");
                $selectStmt->execute([$aS['usertwitterid']]);
                $userRow = $selectStmt->fetch();
                RetweetScheduler::queueAutomatedRetweets($userRow);
            }
        } catch (\PDOException $e) {
            self::$logger->error("Failed to insert automation settings: " . print_r($e, true));
            return TwitterResponseStatus::ARTRETWEETER_DATABASE_ERROR;
        }
        return TwitterResponseStatus::ARTRETWEETER_QUERY_OK;
    }

    public static function queueRetweet($userAuthTwitterID, $tweetAuthorID, $tweetID, $retweetTime) {
        if (!RetweetScheduler::checkUserCanQueueNewRetweet($userAuthTwitterID, $retweetTime)) {
            return false;
        }
        if (!RetweetScheduler::checkUserCanRetweetOldTweet($userAuthTwitterID, $retweetTime, $tweetID)) {
            return false;
        }
        $timeString = date('Y-m-d H:i:s', $retweetTime);
        $stmt = CoreDB::getConnection()->prepare("INSERT INTO scheduledretweets (retweetingusertwitterid,tweetauthorid,tweetid,retweettime,automated) "
                . "VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE retweetingusertwitterid=?, tweetauthorid=?, tweetid=?, retweettime=?, automated=?");
        try {
            $stmt->execute([$userAuthTwitterID, $tweetAuthorID, $tweetID, $timeString, "N",
                $userAuthTwitterID, $tweetAuthorID, $tweetID, $timeString, "N"]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to queue retweet: " . print_r($e, true));
            return false;
        }
        return true;
    }

    public static function rescheduleQueuedTweet($queueID, $userTwitterID, $newTime) {
        $userSelectQuery = "SELECT * FROM userautomationsettings WHERE usertwitterid=?";
        $userSelectStmt = CoreDB::getConnection()->prepare($userSelectQuery);
        try {
            $userSelectStmt->execute([$userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to retrieve user automation settings: " . print_r($e, true));
            return "Database error retrieving user automation settings";
        }

        $userRow = $userSelectStmt->fetch();
        if (!$userRow) {
            return "User automation settings not found";
        }
        $serverTimeString = MiscTools::convertUserTimeStringToServerTime($newTime, $userRow['timezonehouroffset'], $userRow['timezoneminuteoffset']);
        $serverTimestamp = strtotime($serverTimeString);
        $selectQuery = "SELECT tweetid FROM scheduledretweets WHERE id=? AND retweetingusertwitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$queueID, $userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get tweet ID from scheduled retweets table: " . print_r($e, true));
            return "Database error retrieving tweet ID";
        }

        $tweetid = $selectStmt->fetchColumn();
        if (!$tweetid) {
            return "Tweet ID not found";
        }
        $canRequeue = RetweetScheduler::checkUserCanQueueNewRetweet($userTwitterID, $serverTimestamp, $queueID);
        if ($canRequeue) {
            self::updateQueuedRetweet($userTwitterID, $queueID, $serverTimeString);
        } else {
            return $canRequeue;
        }
    }

    // Updates a queue entry without checking if it's allowed or not (uses queue ID, not tweet ID).
    public static function updateQueuedRetweet($userTwitterID, $queueID, $retweetTime) {
        $insertStmt = CoreDB::getConnection()->prepare("UPDATE scheduledretweets SET retweettime=? WHERE id=? AND retweetingusertwitterid=?");
        try {
            $insertStmt->execute([$retweetTime, $queueID, $userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to update scheduled retweet with new tweet time: " . print_r($e, true));
            return false;
        }

        return $insertStmt->rowCount();
    }

    public static function deleteQueuedRetweet($queueID, $userTwitterID) {
        self::$logger->debug("Deleting queue entry with ID: $queueID and user twitter ID: $userTwitterID");
        $statement = CoreDB::getConnection()->prepare("DELETE FROM scheduledretweets WHERE id=? AND retweetingusertwitterid=?");
        try {
            $statement->execute([$queueID, $userTwitterID]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to delete queued retweet: " . print_r($e, true));
            return false;
        }

        $rowCount = $statement->rowCount();
        return $rowCount;
    }

}

AutomationDB::initialiseLogger();
