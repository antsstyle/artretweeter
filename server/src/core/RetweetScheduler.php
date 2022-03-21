<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\CachedVariables;
use Antsstyle\ArtRetweeter\DB\AutomationDB;
use Antsstyle\ArtRetweeter\DB\CoreDB;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\LogManager;

class RetweetScheduler {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function getNumTweetsAtExactTime($table, $userTwitterID, $retweetTime) {
        if ($table == "scheduledretweets") {
            $stmt = CoreDB::getConnection()->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime=?");
        } else if ($table == "retweetrecords") {
            $stmt = CoreDB::getConnection()->prepare("SELECT * FROM scheduledretweets WHERE retweetingusertwitterid=? AND retweettime=?");
        } else {
            return false;
        }
        $stmt->bindValue(1, $userTwitterID);
        $stmt->bindValue(2, $retweetTime);
        try {
            $stmt->execute();
        } catch (\PDOException $e) {
            self::$logger->error("Failed to check number of user tweets at exact time. Table: $table User twitter ID: $userTwitterID "
                    . "Retweet time: $retweetTime. PDO error: " . print_r($e, true));
            return false;
        }

        $results = $stmt->fetchAll();
        if (!$results) {
            return 0;
        } else {
            return count($results);
        }
    }

    public static function getNumTweetsInTimeInterval($userTwitterID, $retweetTime, $timeInterval, $limit,
            $rescheduledTweetID = null) {
        $intervalStart = date("Y-m-d H:i:s", strtotime("-" . $timeInterval, $retweetTime));
        $intervalEnd = date("Y-m-d H:i:s", strtotime("+" . $timeInterval, $retweetTime));
        $intervalSizeSeconds = (strtotime('+' . $timeInterval, $retweetTime) - strtotime('-' . $timeInterval, $retweetTime)) / 2;
        $query = "SELECT UNIX_TIMESTAMP(retweettime) AS rttime FROM scheduledretweets WHERE retweetingusertwitterid=? 
            AND retweettime >= ? AND retweettime <= ?";
        if (!is_null($rescheduledTweetID)) {
            $query .= " AND tweetid != ?";
        }
        $query .= " ORDER BY retweettime ASC LIMIT ?";
        $stmt = CoreDB::getConnection()->prepare($query);
        $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
        $stmt->bindValue(2, $intervalStart, \PDO::PARAM_STR);
        $stmt->bindValue(3, $intervalEnd, \PDO::PARAM_STR);
        if (!is_null($rescheduledTweetID)) {
            $stmt->bindValue(4, $rescheduledTweetID, \PDO::PARAM_STR);
            $stmt->bindValue(5, $limit, \PDO::PARAM_INT);
        } else {
            $stmt->bindValue(4, $limit, \PDO::PARAM_INT);
        }
        try {
            $stmt->execute();
        } catch (\PDOException $e) {
            self::$logger->error("Failed to check number of user tweets during time interval. User twitter ID: $userTwitterID "
                    . "Retweet time: $retweetTime Time interval: $timeInterval Limit: $limit Rescheduled Tweet ID: $rescheduledTweetID. "
                    . "PDO error: " . print_r($e, true));
            return false;
        }

        $scheduledTimes = $stmt->fetchAll();
        $query = "SELECT UNIX_TIMESTAMP(scheduledretweettime) AS rttime FROM retweetrecords WHERE usertwitterid=? 
            AND scheduledretweettime >= ? AND scheduledretweettime <= ?";
        if (!is_null($rescheduledTweetID)) {
            $query .= " AND tweetid != ?";
        }
        $query .= " ORDER BY scheduledretweettime ASC LIMIT ?";
        $stmt = CoreDB::getConnection()->prepare($query);
        $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
        $stmt->bindValue(2, $intervalStart, \PDO::PARAM_STR);
        $stmt->bindValue(3, $intervalEnd, \PDO::PARAM_STR);
        if (!is_null($rescheduledTweetID)) {
            $stmt->bindValue(4, $rescheduledTweetID, \PDO::PARAM_STR);
            $stmt->bindValue(5, $limit, \PDO::PARAM_INT);
        } else {
            $stmt->bindValue(4, $limit, \PDO::PARAM_INT);
        }
        try {
            $stmt->execute();
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get record times during time interval. User twitter ID: $userTwitterID "
                    . "Retweet time: $retweetTime Interval start: $intervalStart Interval end: $intervalEnd "
                    . "Limit: $limit Rescheduled Tweet ID: $rescheduledTweetID. "
                    . "PDO error: " . print_r($e, true));
            return false;
        }

        $recordTimes = $stmt->fetchAll();
        $allTimes = [];
        foreach ($scheduledTimes as $scheduledTime) {
            $allTimes[] = $scheduledTime['rttime'];
        }
        foreach ($recordTimes as $recordTime) {
            $allTimes[] = $recordTime['rttime'];
        }
        sort($allTimes, SORT_NUMERIC);
        $rollingCounter = 0;
        $firstTimestampCounter = 0;
        $latestTimestamp = $allTimes[0];
        foreach ($allTimes as $time) {
            $latestTimestamp = $time;
            $rollingCounter++;
            if ($rollingCounter == $limit) {
                while (($latestTimestamp - $allTimes[$firstTimestampCounter]) > $intervalSizeSeconds) {
                    $rollingCounter--;
                    $firstTimestampCounter++;
                }
                if ($rollingCounter == $limit) {
                    return "Too many retweets in $timeInterval period.";
                }
            }
        }
        return true;
    }

    public static function scheduleAllUserArtistRetweets() {
        $now = date("Y-m-d H:i:s");
        $selectQuery = "SELECT * FROM users INNER JOIN userartistautomationsettings ON users.twitterid=userartistautomationsettings.usertwitterid "
                . "WHERE automationenabled=? "
                . "AND (nextserverscheduledate IS NULL OR nextserverscheduledate <= ?)";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute(["Y", $now]);
        } catch (\PDOException $e) {
            self::$logger->critical("Failed to get all user artist automation settings: " . print_r($e, true));
            return false;
        }
        $rows = $selectStmt->fetchAll();
        $nonArtistRTThreshold = CoreDB::getCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_MIN_RT_THRESHOLD);
        if (is_null($nonArtistRTThreshold)) {
            self::$logger->critical("Unable to get non-artist retweets threshold - cannot schedule non-artist retweets!");
            return;
        } else if ($nonArtistRTThreshold === false) {
            $nonArtistRTThreshold = 0.25;
            CoreDB::updateCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_MIN_RT_THRESHOLD, "0.25");
        }
        $freeUserMaxDailyLimit = CoreDB::getCachedVariable(CachedVariables::MAX_NON_ARTIST_RETWEETS_PER_DAY_FREE_USER);
        $paidUserMaxDailyLimit = CoreDB::getCachedVariable(CachedVariables::MAX_NON_ARTIST_RETWEETS_PER_DAY_PAID_USER);
        if (is_null($freeUserMaxDailyLimit) || $freeUserMaxDailyLimit === false || is_null($paidUserMaxDailyLimit) || $paidUserMaxDailyLimit === false) {
            self::$logger->critical("Unable to schedule non-artist retweets - could not find user limits in cached variables!");
            return;
        }
        foreach ($rows as $row) {
            self::scheduleUserArtistRetweets($row, $nonArtistRTThreshold, $freeUserMaxDailyLimit, $paidUserMaxDailyLimit);
        }
    }

    public static function scheduleUserArtistRetweets($userRow, $nonArtistRTThreshold, $freeUserMaxDailyLimit, $paidUserMaxDailyLimit) {
        $dayFlags = $userRow['dayflags'];
        $hourFlags = $userRow['hourflags'];
        $dayCount = substr_count($dayFlags, 'Y');
        $hourCount = substr_count($hourFlags, 'Y');
        if ($dayCount == 0 || $hourCount == 0) {
            self::$logger->error("User has invalid artist automation settings, cannot continue.");
            return;
        }
        $today = date("N") - 1;
        $twoDaysFromNow = $today + 2;
        if ($twoDaysFromNow >= 7) {
            $twoDaysFromNow -= 7;
        }
        $dayFlag = substr($dayFlags, $twoDaysFromNow, 1);
        if ($dayFlag !== "Y") {
            return;
        }
        if ($userRow['paiduser'] === "N") {
            $limitPerDay = min($hourCount, $freeUserMaxDailyLimit);
        } else {
            $limitPerDay = min($hourCount, $paidUserMaxDailyLimit);
        }

        $totalCountPerDay = $limitPerDay * ($dayCount / 7);
        $selectQuery = "SELECT * FROM userartistretweetsettings INNER JOIN artists "
                . "ON userartistretweetsettings.artisttwitterid=artists.twitterid WHERE userartistretweetsettings.usertwitterid=? "
                . "ORDER BY totalretweeted ASC, RAND() ";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$userRow['usertwitterid']]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get user artist results for user ID: " . $userRow['usertwitterid'] . ": " . print_r($e, true));
            return false;
        }

        $rows = $selectStmt->fetchAll();
        if (count($rows) === 0) {
            return;
        }
        $totalArtistTweetCount = 0;
        $eligibleTweetsResults = [];
        $cacheArray = [];
        foreach ($rows as $row) {
            $resp = self::getUserArtistEligibleTweets($userRow['usertwitterid'], $row, $nonArtistRTThreshold);
            $tweetRows = $resp[0];
            $eligibleTweetsResults[$row['twitterid']] = count($tweetRows);
            $obj = new \stdClass();
            $obj->artistid = $row['twitterid'];
            $obj->count = $eligibleTweetsResults[$row['twitterid']];
            $obj->params = $resp[1];
            $cacheArray[$row['twitterid']] = $obj;
            self::$logger->debug("Eligible tweets for artist " . $row['twitterid'] . ": " . count($tweetRows));
            $totalArtistTweetCount += count($tweetRows);
            if (count($tweetRows) > 0) {
                $tweetRowsArray[] = $tweetRows;
            }
        }
        CoreDB::updateUserArtistEligibleTweetsCache($userRow['usertwitterid'], $cacheArray);
        if (!isset($tweetRowsArray)) {
            self::$logger->debug("No eligible tweet rows for user, returning.");
            return;
        }
        $tweetsAvailablePerArtist = $totalCountPerDay / $totalArtistTweetCount;
        if ($tweetsAvailablePerArtist >= 1) {
            $maxTweetsPerArtist = 1;
        } else {
            $maxTweetsPerArtist = 2;
        }
        $artistIndex = 0;
        $scheduledTweets = 0;
        $hourIndices = self::getHourFlagIndices($hourFlags);
        self::$logger->debug("Limit per day: $limitPerDay");
        self::$logger->debug("Count of tweet rows array: " . count($tweetRowsArray));
        $endReached = false;
        while ($scheduledTweets < $limitPerDay && ($artistIndex < count($tweetRowsArray)) && !$endReached) {
            $artistRows = $tweetRowsArray[$artistIndex];
            $artistRowCount = count($artistRows);
            for ($i = 0; ($i < $maxTweetsPerArtist && $i < $artistRowCount); $i++) {
                $tweetIDToSchedule = $artistRows[$i]['tweetid'];
                $tweetAuthorID = $artistRows[$i]['usertwitterid'];
                $nextHour = self::getRandomHourToAutomate($hourIndices);
                $minute = rand(0, 59);
                unset($hourIndices[array_search($nextHour, $hourIndices)]);
                $hourIndices = array_values($hourIndices);
                $retweetTime = new \DateTime();
                $retweetTime->add(new \DateInterval('P2D'));
                $retweetTime->setTime($nextHour, $minute);
                $retweetTimeStamp = $retweetTime->getTimestamp();
                AutomationDB::queueRetweet($userRow['usertwitterid'], $tweetAuthorID, $tweetIDToSchedule, $retweetTimeStamp);
                $scheduledTweets++;
                if ($scheduledTweets >= $limitPerDay) {
                    $endReached = true;
                    break;
                }
                if (count($hourIndices) === 0) {
                    $endReached = true;
                    break;
                }
            }
            $artistIndex++;
        }
        if (!is_null($userRow['nextserverscheduledate'])) {
            $scheduleHour = date("H", strtotime($userRow['nextserverscheduledate']));
            $scheduleMinute = date("i", strtotime($userRow['nextserverscheduledate']));
        } else {
            $scheduleHour = date("H");
            $scheduleMinute = date("i");
        }
        $nextServerScheduleDate = new \DateTime();
        $nextServerScheduleDate->add(new \DateInterval('P1D'));
        $nextServerScheduleDate->setTime($scheduleHour, $scheduleMinute);
        $nextServerScheduleDate = date("Y-m-d H:i:s", $nextServerScheduleDate->getTimestamp());
        $updateQuery = "UPDATE userartistautomationsettings SET nextserverscheduledate=? WHERE usertwitterid=?";
        $updateStmt = CoreDB::getConnection()->prepare($updateQuery);
        try {
            $updateStmt->execute([$nextServerScheduleDate, $userRow['usertwitterid']]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to update next server schedule date field in userartistautomationsettings for user twitter ID: "
                    . $userRow['usertwitterid'] . ". PDO error: " . print_r($e, true));
            return false;
        }
        return true;
    }

    public static function checkUserCanRetweetOldTweet($userTwitterID, $retweetTime, $tweetID) {
        $date30DaysAgo = date("Y-m-d H:i:s", strtotime('-30 days', $retweetTime));
        $date1YearAgo = date("Y-m-d H:i:s", strtotime('-1 year', $retweetTime));
        // Limit users to retweeting the same tweet only once per month, and not more than 3 times per year
        $per30DaysLimit = 1;
        $perYearLimit = 3;
        $records30DaysStmt = CoreDB::getConnection()->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime >= ? 
        AND tweetid=? ORDER BY retweettime DESC LIMIT ?");
        $records30DaysStmt->bindValue(1, $userTwitterID);
        $records30DaysStmt->bindValue(2, $date30DaysAgo);
        $records30DaysStmt->bindValue(3, $tweetID);
        $records30DaysStmt->bindValue(4, $per30DaysLimit, \PDO::PARAM_INT);
        try {
            $records30DaysStmt->execute();
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get retweet records to check monthly limits: " . print_r($e, true));
            return false;
        }

        $records30DaysResults = $records30DaysStmt->fetchAll();
        if ($records30DaysResults && (count($records30DaysResults) >= $per30DaysLimit)) {
            return false;
        }


        $recordsYearStmt = CoreDB::getConnection()->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime >= ? 
        AND tweetid=? ORDER BY retweettime DESC LIMIT ?");
        $recordsYearStmt->bindValue(1, $userTwitterID);
        $recordsYearStmt->bindValue(2, $date1YearAgo);
        $recordsYearStmt->bindValue(3, $tweetID);
        $recordsYearStmt->bindValue(4, $perYearLimit, \PDO::PARAM_INT);
        try {
            $recordsYearStmt->execute();
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get retweet records to check yearly limits: " . print_r($e, true));
            return false;
        }

        $recordsYearResults = $recordsYearStmt->fetchAll();
        if ($recordsYearResults && (count($recordsYearResults) >= $perYearLimit)) {
            return false;
        }
        return true;
    }

    public static function checkUserCanQueueNewRetweet($userTwitterID, $retweetTime, $rescheduledTweetID = null) {
        $RTTime = date("Y-m-d H:i:s", $retweetTime);
        $perDayLimit = 10;
        $perHourLimit = 2;

        $recordsNowResults = self::getNumTweetsAtExactTime("retweetrecords", $userTwitterID, $RTTime);
        if ($recordsNowResults === false) {
            return "A database error occurred. Try logging in again or contact "
                    . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.";
        } else if ($recordsNowResults > 0) {
            return "You cannot schedule two retweets to be posted at the same time.";
        }

        $queueNowResults = self::getNumTweetsAtExactTime("scheduledretweets", $userTwitterID, $RTTime);
        if ($queueNowResults === false) {
            return "A database error occurred. Try logging in again or contact "
                    . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.";
        } else if ($queueNowResults > 0) {
            return "You cannot schedule two retweets to be posted at the same time.";
        }

        $result = self::getNumTweetsInTimeInterval($userTwitterID, $retweetTime, "1 hour", $perHourLimit, $rescheduledTweetID);
        if ($result !== true) {
            return $result;
        }
        $result = self::getNumTweetsInTimeInterval($userTwitterID, $retweetTime, "24 hours", $perDayLimit, $rescheduledTweetID);
        if ($result !== true) {
            return $result;
        }
        return true;
    }

    public static function scheduleAutomatedRetweets() {
        $query = "SELECT * FROM users INNER JOIN userautomationsettings ON users.twitterid=userautomationsettings.usertwitterid "
                . "WHERE automationenabled=?";
        $stmt = CoreDB::getConnection()->prepare($query);
        try {
            $stmt->execute(["Y"]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to acquire list of users to schedule automated retweets for, cannot continue: " . print_r($e, true));
            return;
        }

        while ($row = $stmt->fetch()) {
            self::queueAutomatedRetweets($row);
        }
    }

    public static function queueAutomatedRetweets($userRow) {
        $selectSettingsQuery = "SELECT * FROM userautomationsettings WHERE usertwitterid=?";
        $updateStmt = CoreDB::getConnection()->prepare($selectSettingsQuery);
        try {
            $updateStmt->execute([$userRow['twitterid']]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get user ID to queue automated retweets for, cannot continue: " . print_r($e, true));
            return;
        }
        $settingsRow = $updateStmt->fetch();
        if (!$settingsRow) {
            self::$logger->error("No automation settings are available for this user, cannot continue.");
            return;
        }
        $lastScheduleServerDate = $settingsRow['lastscheduleserverdate'];
        if (!is_null($lastScheduleServerDate)) {
            $lastScheduleServerDate = strtotime($lastScheduleServerDate);
            $yesterday = strtotime("-1 day +15 minutes");
            if ($lastScheduleServerDate > $yesterday) {
                return;
            }
        }
        $dayFlags = $settingsRow['dayflags'];
        $hourFlags = $settingsRow['hourflags'];
        $minuteFlags = $settingsRow['minuteflags'];
        $dayCount = substr_count($dayFlags, 'Y');
        $hourCount = substr_count($hourFlags, 'Y');
        $minuteCount = substr_count($minuteFlags, 'Y');
        if ($dayCount == 0 || $hourCount == 0 || $minuteCount == 0) {
            self::$logger->error("User has invalid automation settings, cannot continue.");
            return;
        }
        $today = date("N") - 1;
        $twoDaysFromNow = $today + 2;
        if ($twoDaysFromNow >= 7) {
            $twoDaysFromNow -= 7;
        }
        $dayFlag = substr($dayFlags, $twoDaysFromNow, 1);
        if ($dayFlag !== "Y") {
            return;
        }
        $limitPerDay = min($hourCount, 10);
        $totalCountPerDay = $limitPerDay * ($dayCount / 7);

        $selectMeanStatsQuery = "SELECT AVG(retweets) AS avgrts, UNIX_TIMESTAMP(MAX(createdat)) AS maxcr, UNIX_TIMESTAMP(MIN(createdat)) AS mincr "
                . "FROM tweets INNER JOIN tweetmetrics ON tweets.tweetid=tweetmetrics.tweetid WHERE usertwitterid=?";
        $selectMeanStatsParams[] = $settingsRow['usertwitterid'];
        if ($settingsRow['oldtweetscutoffdateenabled'] == 'Y') {
            $selectMeanStatsQuery .= " AND createdat >= ?";
            $selectMeanStatsParams[] = $settingsRow['oldtweetscutoffdate'];
        }
        if ($settingsRow['includetextenabled'] === 'Y' && !is_null($settingsRow['includetext'])) {
            if ($settingsRow['includetextcondition'] === "This exact phrase") {
                $selectMeanStatsQuery .= " AND fulltweettext LIKE ?";
                $selectMeanStatsParams[] = "%" . $settingsRow['includetext'] . "%";
            } else {
                $includedWords = explode(" ", $settingsRow['includetext']);
                $selectMeanStatsQuery .= " AND (";
                foreach ($includedWords as $includedWord) {
                    if ($includedWord == "") {
                        continue;
                    }
                    if ($settingsRow['includetextcondition'] == "All of these words") {
                        $selectMeanStatsQuery .= "fulltweettext LIKE ? AND ";
                    } else {
                        $selectMeanStatsQuery .= "fulltweettext LIKE ? OR ";
                    }
                    $selectMeanStatsParams[] = "%" . $includedWord . "%";
                }
                if ($settingsRow['includetextcondition'] == "All of these words") {
                    $selectMeanStatsQuery = substr($selectMeanStatsQuery, 0, -5);
                } else {
                    $selectMeanStatsQuery = substr($selectMeanStatsQuery, 0, -4);
                }
                $selectMeanStatsQuery .= ")";
            }
        }
        if ($settingsRow['excludetextenabled'] == 'Y' && !is_null($settingsRow['excludetext'])) {
            if ($settingsRow['excludetextcondition'] === "This exact phrase") {
                $selectMeanStatsQuery .= " AND fulltweettext NOT LIKE ?";
                $selectMeanStatsParams[] = "%" . $settingsRow['excludetext'] . "%";
            } else {
                $excludedWords = explode(" ", $settingsRow['excludetext']);
                $selectMeanStatsQuery .= " AND (";
                foreach ($excludedWords as $excludedWord) {
                    if ($excludedWord == "") {
                        continue;
                    }
                    if ($settingsRow['excludetextcondition'] === "All of these words") {
                        $selectMeanStatsQuery .= "fulltweettext NOT LIKE ? AND ";
                    } else {
                        $selectMeanStatsQuery .= "fulltweettext NOT LIKE ? OR ";
                    }
                    $selectMeanStatsParams[] = "%" . $excludedWord . "%";
                }
                if ($settingsRow['excludetextcondition'] === "All of these words") {
                    $selectMeanStatsQuery = substr($selectMeanStatsQuery, 0, -5);
                } else {
                    $selectMeanStatsQuery = substr($selectMeanStatsQuery, 0, -4);
                }
                $selectMeanStatsQuery .= ")";
            }
        }
        if ($settingsRow['blockedhandlesenabled'] === 'Y') {
            $blockedHandlesStmt = CoreDB::getConnection()->prepare("SELECT * FROM userblockedhandles WHERE usertwitterid=?");
            try {
                $blockedHandlesStmt->execute([$userRow['twitterid']]);
            } catch (\PDOException $e) {
                self::$logger->error("Failed to get user blocked handles: " . print_r($e, true));
                return false;
            }

            $blockedHandles = $blockedHandlesStmt->fetchAll();
            $selectMeanStatsQuery .= " AND fulltweettext NOT REGEXP ?";
            $regexp = "";
            foreach ($blockedHandles as $blockedHandle) {
                $regexp .= $blockedHandle['blockedhandle'] . '|';
            }
            if (strlen($regexp) > 0) {
                $regexp = substr($regexp, 0, -1);
            }
            self::$logger->debug("Regexp: $regexp");
            $selectMeanStatsParams[] = $regexp;
        }

        $selectMeanStatsStmt = CoreDB::getConnection()->prepare($selectMeanStatsQuery);
        try {
            $selectMeanStatsStmt->execute($selectMeanStatsParams);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get mean statistics for user, cannot continue with automated retweet scheduling: " . print_r($e, true));
            return false;
        }

        $statsRow = $selectMeanStatsStmt->fetch();
        $meanRTs = $statsRow['avgrts'];
        if ($settingsRow['metricsmeasurementtype'] == "Mean Average" && $settingsRow['retweetpercent'] != null) {
            $retweetPercent = $settingsRow['retweetpercent'] / 100;
            $retweetThreshold = $meanRTs * $retweetPercent;
        } else if ($settingsRow['metricsmeasurementtype'] == "Adaptive") {
            $retweetThreshold = max(($meanRTs * 0.2), $settingsRow['adaptivertthreshold']);
        } else {
            $userTwitterID = $userRow['twitterid'];
            self::$logger->error("Incorrect automation settings for user ID: $userTwitterID. Settings row:");
            self::$logger->error(print_r($settingsRow, true));
            return false;
        }

        $minCreatedAt = $statsRow['mincr'];
        $maxCreatedAt = $statsRow['maxcr'];
        $seconds = $maxCreatedAt - $minCreatedAt;
        $daysBetweenTweets = max(ceil($seconds / (60 * 60 * 24)), 1);
        $oneYearAgo = date("Y-m-d H:i:s", strtotime("-1 year"));
        $oneMonthAgo = date("Y-m-d H:i:s", strtotime("-1 month"));
        $selectTweetsQuery = str_replace("AVG(retweets) AS avgrts, UNIX_TIMESTAMP(MAX(createdat)) AS maxcr, UNIX_TIMESTAMP(MIN(createdat)) AS mincr",
                "*", $selectMeanStatsQuery);
        $selectTweetsQuery .= " AND retweets >=? AND tweets.tweetid NOT IN (SELECT scheduledretweets.tweetid FROM scheduledretweets WHERE retweetingusertwitterid=?) "
                . "AND tweets.tweetid NOT IN (SELECT retweetrecords.tweetid FROM retweetrecords WHERE usertwitterid=? AND scheduledretweettime >= ?) "
                . "AND tweets.tweetid NOT IN (SELECT retweetrecords.tweetid FROM retweetrecords WHERE usertwitterid=? AND scheduledretweettime >= ? "
                . "GROUP BY retweetrecords.tweetid HAVING COUNT(retweetrecords.tweetid) > ?) "
                . "ORDER BY RAND()";
        $selectMeanStatsParams[] = $retweetThreshold;
        $selectMeanStatsParams[] = $userRow['twitterid'];
        $selectMeanStatsParams[] = $userRow['twitterid'];
        $selectMeanStatsParams[] = $oneMonthAgo;
        $selectMeanStatsParams[] = $userRow['twitterid'];
        $selectMeanStatsParams[] = $oneYearAgo;
        $selectMeanStatsParams[] = 2;
        $selectTweetsStmt = CoreDB::getConnection()->prepare($selectTweetsQuery);
        try {
            $selectTweetsStmt->execute($selectMeanStatsParams);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get tweets for user, cannot continue with automated retweet scheduling: " . print_r($e, true));
            return false;
        }

        $tweetRows = $selectTweetsStmt->fetchAll();
        $tweetCount = count($tweetRows);

        $tweetsPerDay = ceil($tweetCount / $daysBetweenTweets);
        $minuteValues = self::getMinuteValues($minuteFlags);
        $numScheduled = 0;
        if ($tweetCount == 0) {
            return true;
        }
        $scheduleType = "Random";
        if ($scheduleType == "Random") {
            // Schedule randomly, once per hour
            $hourIndices = self::getHourFlagIndices($hourFlags);
            while ($numScheduled < 10 && $numScheduled < $tweetCount && $numScheduled < $tweetsPerDay && count($hourIndices) > 0) {
                $nextHour = self::getRandomHourToAutomate($hourIndices);
                unset($hourIndices[array_search($nextHour, $hourIndices)]);
                $hourIndices = array_values($hourIndices);
                $minuteValue = self::getNextMinute($minuteValues);
                $retweetTime = new \DateTime();
                $retweetTime->add(new \DateInterval('P2D'));
                $retweetTime->setTime($nextHour, $minuteValue);
                $retweetTimeStamp = $retweetTime->getTimestamp();
                AutomationDB::queueRetweet($userRow['twitterid'], $userRow['twitterid'], $tweetRows[$numScheduled]['tweetid'], $retweetTimeStamp);
                $numScheduled++;
            }
        } else {
            $start = strpos($hourFlags, "Y");
            $end = strrpos($hourFlags, "Y");
            $maxDist = $end - $start;
            $nextHour = $start;
            $latestUsed = null;
            $interval = $maxDist / $totalCountPerDay;
            while ($numScheduled < $tweetsPerDay && $numScheduled < $tweetCount && $numScheduled < 10) {
                $minuteValue = self::getNextMinute($minuteValues);
                $retweetTime = new \DateTime();
                $retweetTime->add(new \DateInterval('P2D'));
                $retweetTime->setTime($nextHour, $minuteValue);
                $retweetTimeStamp = $retweetTime->getTimestamp();
                AutomationDB::queueRetweet($userRow['twitterid'], $userRow['twitterid'], $tweetRows[$numScheduled]['tweetid'], $retweetTimeStamp);
                $numScheduled++;
                $latestUsed = $nextHour;
                $nextHour = self::getNextHourToAutomate($hourFlags, $latestUsed, $interval);
            }
        }
        $now = date("Y-m-d H:i:s");
        $updateQuery = "UPDATE userautomationsettings SET lastscheduleserverdate=? WHERE usertwitterid=?";
        $updateStmt = CoreDB::getConnection()->prepare($updateQuery);
        try {
            $updateStmt->execute([$now, $userRow['twitterid']]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to update user last schedule server date: " . print_r($e, true));
            return false;
        }
        return true;
    }

    public static function getUserArtistEligibleTweets($userTwitterID, $artistRow, $nonArtistRTThreshold) {
        $selectMeanStatsQuery = "SELECT AVG(retweets) AS avgrts, UNIX_TIMESTAMP(MAX(createdat)) AS maxcr, UNIX_TIMESTAMP(MIN(createdat)) AS mincr "
                . "FROM tweets INNER JOIN tweetmetrics ON tweets.tweetid=tweetmetrics.tweetid WHERE usertwitterid=?";
        $selectMeanStatsParams[] = $artistRow['twitterid'];

        $oldTweetsCutoffDate = date("Y-m-d H:i:s", strtotime("-5 years"));
        $selectMeanStatsQuery .= " AND createdat >= ?";
        $selectMeanStatsParams[] = $oldTweetsCutoffDate;
        if ($artistRow['includetextenabled'] === 'Y' && !is_null($artistRow['includetext'])) {
            if ($artistRow['includetextcondition'] === "This exact phrase") {
                $selectMeanStatsQuery .= " AND fulltweettext LIKE ?";
                $selectMeanStatsParams[] = "%" . $artistRow['includetext'] . "%";
            } else {
                $includedWords = explode(" ", $artistRow['includetext']);
                $selectMeanStatsQuery .= " AND (";
                foreach ($includedWords as $includedWord) {
                    if ($includedWord == "") {
                        continue;
                    }
                    if ($artistRow['includetextcondition'] == "All of these words") {
                        $selectMeanStatsQuery .= "fulltweettext LIKE ? AND ";
                    } else {
                        $selectMeanStatsQuery .= "fulltweettext LIKE ? OR ";
                    }
                    $selectMeanStatsParams[] = "%" . $includedWord . "%";
                }
                if ($artistRow['includetextcondition'] == "All of these words") {
                    $selectMeanStatsQuery = substr($selectMeanStatsQuery, 0, -5);
                } else {
                    $selectMeanStatsQuery = substr($selectMeanStatsQuery, 0, -4);
                }
                $selectMeanStatsQuery .= ")";
            }
        }
        if ($artistRow['excludetextenabled'] == 'Y' && !is_null($artistRow['excludetext'])) {
            if ($artistRow['excludetextcondition'] === "This exact phrase") {
                $selectMeanStatsQuery .= " AND fulltweettext NOT LIKE ?";
                $selectMeanStatsParams[] = "%" . $artistRow['excludetext'] . "%";
            } else {
                $excludedWords = explode(" ", $artistRow['excludetext']);
                $selectMeanStatsQuery .= " AND (";
                foreach ($excludedWords as $excludedWord) {
                    if ($excludedWord == "") {
                        continue;
                    }
                    if ($artistRow['excludetextcondition'] === "All of these words") {
                        $selectMeanStatsQuery .= "fulltweettext NOT LIKE ? AND ";
                    } else {
                        $selectMeanStatsQuery .= "fulltweettext NOT LIKE ? OR ";
                    }
                    $selectMeanStatsParams[] = "%" . $excludedWord . "%";
                }
                if ($artistRow['excludetextcondition'] === "All of these words") {
                    $selectMeanStatsQuery = substr($selectMeanStatsQuery, 0, -5);
                } else {
                    $selectMeanStatsQuery = substr($selectMeanStatsQuery, 0, -4);
                }
                $selectMeanStatsQuery .= ")";
            }
        }
        $selectMeanStatsStmt = CoreDB::getConnection()->prepare($selectMeanStatsQuery);
        try {
            $selectMeanStatsStmt->execute($selectMeanStatsParams);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get mean statistics for user, cannot continue with automated retweet scheduling: " . print_r($e, true));
            return false;
        }

        $statsRow = $selectMeanStatsStmt->fetch();
        $meanRTs = $statsRow['avgrts'];

        $retweetThreshold = max(($meanRTs * $nonArtistRTThreshold), $artistRow['adaptivertthreshold']);

        $oneYearAgo = date("Y-m-d H:i:s", strtotime("-1 year"));
        $oneMonthAgo = date("Y-m-d H:i:s", strtotime("-1 month"));
        $selectTweetsQuery = str_replace("AVG(retweets) AS avgrts, UNIX_TIMESTAMP(MAX(createdat)) AS maxcr, UNIX_TIMESTAMP(MIN(createdat)) AS mincr",
                "*", $selectMeanStatsQuery);
        $selectTweetsQuery .= " AND retweets >=? AND tweets.tweetid NOT IN (SELECT scheduledretweets.tweetid FROM scheduledretweets WHERE retweetingusertwitterid=?) "
                . "AND tweets.tweetid NOT IN (SELECT retweetrecords.tweetid FROM retweetrecords WHERE usertwitterid=? AND scheduledretweettime >= ?) "
                . "AND tweets.tweetid NOT IN (SELECT retweetrecords.tweetid FROM retweetrecords WHERE usertwitterid=? AND scheduledretweettime >= ? "
                . "GROUP BY retweetrecords.tweetid HAVING COUNT(retweetrecords.tweetid) > ?) "
                . "ORDER BY RAND()";
        $selectMeanStatsParams[] = $retweetThreshold;
        $selectMeanStatsParams[] = $userTwitterID;
        $selectMeanStatsParams[] = $userTwitterID;
        $selectMeanStatsParams[] = $oneMonthAgo;
        $selectMeanStatsParams[] = $userTwitterID;
        $selectMeanStatsParams[] = $oneYearAgo;
        $selectMeanStatsParams[] = 2;
        $selectTweetsStmt = CoreDB::getConnection()->prepare($selectTweetsQuery);
        try {
            $selectTweetsStmt->execute($selectMeanStatsParams);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get tweets for user, cannot continue with automated retweet scheduling: " . print_r($e, true));
            return false;
        }

        $tweetRows = $selectTweetsStmt->fetchAll();
        return [$tweetRows, $selectMeanStatsParams];
    }

    public static function getHourFlagIndices($hourFlags) {
        $hourFlagIndices = [];
        for ($i = 0; $i < strlen($hourFlags); $i++) {
            if ($hourFlags[$i] == "Y") {
                $hourFlagIndices[] = $i;
            }
        }
        return $hourFlagIndices;
    }

    public static function getRandomHourToAutomate($hourFlagIndices) {
        $validIndices = count($hourFlagIndices);
        if ($validIndices == 0) {
            return null;
        }
        return $hourFlagIndices[rand(0, $validIndices - 1)];
    }

    public static function getNextHourToAutomate($hourFlags, $latestUsed, $interval) {
        $finalHour = strrpos($hourFlags, "Y");
        if ($latestUsed == $finalHour) {
            return null;
        }
        $newTime = intval(floor($latestUsed + $interval));
        while ($newTime < 24) {
            if ($hourFlags[$newTime] == "Y") {
                return $newTime;
            }
            $newTime++;
        }
        return null;
    }

    public static function getMinuteValues($minuteFlags) {
        if ($minuteFlags[0] == "Y") {
            $minuteValues[] = 0;
        }
        if ($minuteFlags[1] == "Y") {
            $minuteValues[] = 15;
        }
        if ($minuteFlags[2] == "Y") {
            $minuteValues[] = 30;
        }
        if ($minuteFlags[3] == "Y") {
            $minuteValues[] = 45;
        }
        return $minuteValues;
    }

    public static function getNextMinute($minuteValues) {
        if (count($minuteValues) == 1) {
            return $minuteValues[0];
        } else {
            return $minuteValues[rand(0, count($minuteValues) - 1)];
        }
    }

}

RetweetScheduler::initialiseLogger();

