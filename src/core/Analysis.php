<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Core\CachedVariables;
use Antsstyle\ArtRetweeter\DB\CoreDB;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;

class Analysis {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function getMeanRTThresholdForFollowerCount($followerCount, $minRTThreshold, $maxRTThreshold,
            $lowFollowerThreshold, $highFollowerThreshold) {

        $totalDistance = $highFollowerThreshold - $lowFollowerThreshold;
        $thresholdDistance = $maxRTThreshold - $minRTThreshold;
        if ($followerCount >= $highFollowerThreshold) {
            $meanRTThreshold = $minRTThreshold;
        } else if ($followerCount <= $lowFollowerThreshold) {
            $meanRTThreshold = $maxRTThreshold;
        } else {
            $artistDistance = $followerCount - $lowFollowerThreshold;
            $ratio = $totalDistance / $artistDistance;
            $meanRTThreshold = (max(min($minRTThreshold + ($thresholdDistance * $ratio), $maxRTThreshold), $minRTThreshold)) * 100;
            $meanRTThreshold = floor($meanRTThreshold);
        }
        return $meanRTThreshold;
    }

    public static function getStandardDeviation($arr) {
        $numElements = count($arr);
        $variance = 0.0;
        // calculating mean using array_sum() method
        $average = array_sum($arr) / $numElements;
        foreach ($arr as $i) {
            // sum of squares of differences between 
            // all numbers and means.
            $variance += pow(($i - $average), 2);
        }
        return sqrt($variance / $numElements);
    }

    public static function computeAdaptiveAnalyticsForUsers() {
        $metricID = CoreDB::getLatestMetricsTypeID();
        if (is_null($metricID)) {
            return;
        }
        $minimumTweetMetricsLimit = 50;
        $currentTime = date("Y-m-d H:i:s");
        $selectUsersQuery = "SELECT usertwitterid FROM `users` INNER JOIN userautomationsettings "
                . "ON users.twitterid=userautomationsettings.usertwitterid WHERE automationenabled=? AND (nextanalysis IS NULL OR nextanalysis <= ?) "
                . "AND usertwitterid IN (SELECT usertwitterid FROM tweets INNER JOIN tweetmetrics ON tweets.tweetid=tweetmetrics.tweetid "
                . "GROUP BY usertwitterid HAVING (COUNT(*) > ?)) ";
        $selectUsersStmt = CoreDB::getConnection()->prepare($selectUsersQuery);
        try {
            $selectUsersStmt->execute(["Y", $currentTime, $minimumTweetMetricsLimit]);
        } catch (\PDOException $e) {
            self::$logger->critical("Failed to get tweets from DB, cannot update metrics: " . print_r($e, true));
            return false;
        }

        $userTwitterIDs = [];
        while ($userRow = $selectUsersStmt->fetch()) {
            $userTwitterIDs[] = $userRow['usertwitterid'];
        }
        $updateQuery = "UPDATE userautomationsettings SET adaptivertthreshold=?, nextanalysis=? WHERE usertwitterid=?";
        $updateParams = [];
        $nextAnalysis = date("Y-m-d H:i:s", strtotime("+7 days"));
        foreach ($userTwitterIDs as $userTwitterID) {
            $selectQuery = "SELECT * FROM tweets INNER JOIN tweetmetrics ON tweets.tweetid=tweetmetrics.tweetid WHERE usertwitterid=?"
                    . " AND retrievalmetric=? ORDER BY retweets";
            $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
            try {
                $selectStmt->execute([$userTwitterID, $metricID]);
            } catch (\PDOException $e) {
                self::$logger->critical("Failed to get tweets from DB, cannot update metrics: " . print_r($e, true));
                return false;
            }

            $retweetValues = [];
            while ($row = $selectStmt->fetch()) {
                $retweetValues[] = $row['retweets'];
            }
            $countRetweets = count($retweetValues);
            $tenths = $countRetweets / 10.0;
            $rem = $countRetweets - (floor($tenths) * 10);
            $currentIndex = 0;
            $segmentedStats = [];
            $segment = [];
            for ($i = 0; $i < 10; $i++) {
                $segmentSize = floor($tenths);
                if ($rem > $i) {
                    $segmentSize++;
                }
                for ($j = $currentIndex; $j < ($currentIndex + $segmentSize); $j++) {
                    $segment[] = $retweetValues[$j];
                }
                $segmentedStats[] = $segment;
                $segment = [];
                $currentIndex += $segmentSize;
            }

            $segmentedSeries = [];
            $segmentedDividedSeries = [];
            for ($i = 0; $i < count($segmentedStats); $i++) {
                $segment = $segmentedStats[$i];
                $diff = $segment[count($segment) - 1] - $segment[0];
                if ($segment[0] == 0) {
                    $dividedDiff = -1;
                } else {
                    $dividedDiff = $segment[count($segment) - 1] / $segment[0];
                }
                $segmentedSeries[] = $diff;
                $segmentedDividedSeries[] = $dividedDiff;
            }

            $segmentDiffSeries = [];
            $segmentDiffDataPoints = [];
            for ($i = 0; $i < (count($segmentedSeries) - 1); $i++) {
                $tanoppositeadjacent = rad2deg(atan(($segmentedSeries[$i + 1] - $segmentedSeries[$i]) / 1000));
                $segmentDiffSeries[] = $tanoppositeadjacent;
                $segmentDiffDataPoints[] = [$segmentedSeries[$i + 1], $segmentedSeries[$i]];
            }

            $tanMax = max($segmentDiffSeries);
            $tanMin = -1;
            for ($i = 0; $i < count($segmentDiffSeries); $i++) {
                $yVal = $segmentDiffSeries[$i];
                if ($yVal == $tanMax && $i > 0) {
                    $tanMin = $segmentDiffSeries[$i - 1];
                    $tanMINDataPoints = $segmentDiffDataPoints[$i - 1];
                }
            }
            $finalRTThreshold = $tanMINDataPoints[1];
            $updateParams[] = [$finalRTThreshold, $nextAnalysis, $userTwitterID];
        }
        try {
            CoreDB::getConnection()->beginTransaction();
            foreach ($updateParams as $updateParamsForUser) {
                $updateStmt = CoreDB::getConnection()->prepare($updateQuery);
                try {
                    $updateStmt->execute($updateParamsForUser);
                } catch (\PDOException $e) {
                    self::$logger->error("Failed to update analytics for user. Params were: " . print_r($updateParamsForUser, true)
                            . " PDO error: " . print_r($e, true));
                }
            }
            CoreDB::getConnection()->commit();
        } catch (\PDOException $e) {
            self::$logger->error("Failed to commit transaction for user analytics: " . print_r($e, true));
            return false;
        }
        return true;
    }

    public static function computeAdaptiveAnalyticsForArtists() {
        $minRTThreshold = CoreDB::getCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_MIN_RT_THRESHOLD);
        if (is_null($minRTThreshold)) {
            self::$logger->critical("Failed to get min RT threshold value from DB - using default.");
            $minRTThreshold = 0.25;
        } else if ($minRTThreshold === false) {
            $minRTThreshold = 0.25;
            CoreDB::updateCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_MIN_RT_THRESHOLD, "0.25");
        }
        $maxRTThreshold = CoreDB::getCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_MAX_RT_THRESHOLD);
        if (is_null($maxRTThreshold)) {
            self::$logger->critical("Failed to get max RT threshold value from DB - using default.");
            $maxRTThreshold = 0.5;
        } else if ($maxRTThreshold === false) {
            $maxRTThreshold = 0.5;
            CoreDB::updateCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_MAX_RT_THRESHOLD, "0.5");
        }

        $lowFollowerThreshold = CoreDB::getCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_LOW_FOLLOWER_THRESHOLD);
        if (is_null($lowFollowerThreshold)) {
            self::$logger->critical("Failed to get low follower threshold value from DB - using default.");
            $lowFollowerThreshold = 0.25;
        } else if ($lowFollowerThreshold === false) {
            $lowFollowerThreshold = 0.25;
            CoreDB::updateCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_LOW_FOLLOWER_THRESHOLD, "0.25");
        }
        $highFollowerThreshold = CoreDB::getCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_HIGH_FOLLOWER_THRESHOLD);
        if (is_null($highFollowerThreshold)) {
            self::$logger->critical("Failed to get high follower threshold value from DB - using default.");
            $highFollowerThreshold = 0.5;
        } else if ($highFollowerThreshold === false) {
            $highFollowerThreshold = 0.5;
            CoreDB::updateCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_HIGH_FOLLOWER_THRESHOLD, "0.5");
        }

        $metricID = CoreDB::getLatestMetricsTypeID();
        if (is_null($metricID)) {
            return;
        }
        $currentTime = date("Y-m-d H:i:s");
        $selectUsersQuery = "SELECT twitterid,(SELECT count(*) FROM tweets INNER JOIN tweetmetrics "
                . "ON tweets.tweetid=tweetmetrics.tweetid WHERE tweets.usertwitterid=artists.twitterid AND "
                . "tweetmetrics.retrievalmetric=?) AS tweetcount FROM `artists` WHERE (nextanalysis IS NULL OR nextanalysis <= ?) LIMIT 1000";
        $selectUsersStmt = CoreDB::getConnection()->prepare($selectUsersQuery);
        try {
            $selectUsersStmt->execute([$metricID, $currentTime]);
        } catch (\PDOException $e) {
            self::$logger->critical("Failed to get tweets from DB, cannot update metrics: " . print_r($e, true));
            return false;
        }

        $nextAnalysis = date("Y-m-d H:i:s", strtotime("+7 days"));
        $artistIDsString = "";
        $artistIDsCount = 0;
        $query = "users";
        $params['user.fields'] = "public_metrics";
        $consecutiveErrors = 0;
        $rateLimitRemaining = 1;

        $userRows = $selectUsersStmt->fetchAll();
        $updateQuery = "UPDATE artists SET followercount=? WHERE twitterid=?";
        $updateStmt = CoreDB::getConnection()->prepare($updateQuery);
        foreach ($userRows as $userRow) {
            if ($rateLimitRemaining === 0) {
                self::$logger->debug("No rate limit left - aborting artist analysis.");
                return;
            }
            $artistIDsString .= $userRow['twitterid'] .= ",";
            $artistIDsCount++;
            if ($artistIDsCount === 100) {
                if ($consecutiveErrors === 3) {
                    self::$logger->error("$consecutiveErrors errors trying to get artist info from Twitter. Artist params: $artistIDsString");
                    $artistIDsString = "";
                    continue;
                }
                $artistIDsCount = 0;
                $artistIDsString = substr($artistIDsString, 0, -1);
                $params['ids'] = $artistIDsString;
                $response = Core::queryTwitterUserAuth($query, $query, "GET", $params, APIKeys::bearer_token);
                $data = $response[0];
                $twitterResponseStatus = $response[1];
                if ($twitterResponseStatus->getHttpCode() != 200) {
                    if ($twitterResponseStatus->getHttpCode() == 429) {
                        self::$logger->critical("429 error encountered during artist analysis, aborting.");
                        return;
                    }
                    $consecutiveErrors++;
                    continue;
                } else {
                    $consecutiveErrors = 0;
                }
                $rateLimitRemaining = $twitterResponseStatus->getRateLimitRemaining();
                foreach ($data as $userEntry) {
                    $userTwitterID = $userEntry->id;
                    $followersCount = $userEntry->public_metrics->followers_count;
                    try {
                        $updateStmt->execute([$followersCount, $userTwitterID]);
                    } catch (\PDOException $e) {
                        self::$logger->critical("Failed to update follower count for user twitter ID: $userTwitterID. PDO error: " . print_r($e, true));
                    }
                }
                $artistIDsString = "";
            }
        }
        $updateQuery = "UPDATE artists SET adaptivertthreshold=?, meanrtthreshold=?, nextanalysis=? WHERE twitterid=?";

        $updateParams = [];
        foreach ($userRows as $userRow) {
            $selectQuery = "SELECT * FROM tweets INNER JOIN tweetmetrics ON tweets.tweetid=tweetmetrics.tweetid WHERE usertwitterid=?"
                    . " AND retrievalmetric=? ORDER BY retweets";
            $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
            try {
                $selectStmt->execute([$userRow['twitterid'], $metricID]);
            } catch (\PDOException $e) {
                self::$logger->critical("Failed to get tweets from DB, cannot update metrics: " . print_r($e, true));
                return false;
            }

            $retweetValues = [];
            while ($row = $selectStmt->fetch()) {
                $retweetValues[] = $row['retweets'];
            }
            $countRetweets = count($retweetValues);
            $tenths = $countRetweets / 10.0;
            $rem = $countRetweets - (floor($tenths) * 10);
            $currentIndex = 0;
            $segmentedStats = [];
            $segment = [];
            for ($i = 0; $i < 10; $i++) {
                $segmentSize = floor($tenths);
                if ($rem > $i) {
                    $segmentSize++;
                }
                for ($j = $currentIndex; $j < ($currentIndex + $segmentSize); $j++) {
                    $segment[] = $retweetValues[$j];
                }
                $segmentedStats[] = $segment;
                $segment = [];
                $currentIndex += $segmentSize;
            }

            $segmentedSeries = [];
            $segmentedDividedSeries = [];
            for ($i = 0; $i < count($segmentedStats); $i++) {
                $segment = $segmentedStats[$i];
                $diff = $segment[count($segment) - 1] - $segment[0];
                if ($segment[0] == 0) {
                    $dividedDiff = -1;
                } else {
                    $dividedDiff = $segment[count($segment) - 1] / $segment[0];
                }
                $segmentedSeries[] = $diff;
                $segmentedDividedSeries[] = $dividedDiff;
            }

            $segmentDiffSeries = [];
            $segmentDiffDataPoints = [];
            for ($i = 0; $i < (count($segmentedSeries) - 1); $i++) {
                $tanoppositeadjacent = rad2deg(atan(($segmentedSeries[$i + 1] - $segmentedSeries[$i]) / 1000));
                $segmentDiffSeries[] = $tanoppositeadjacent;
                $segmentDiffDataPoints[] = [$segmentedSeries[$i + 1], $segmentedSeries[$i]];
            }

            $tanMax = max($segmentDiffSeries);
            $tanMin = -1;
            for ($i = 0; $i < count($segmentDiffSeries); $i++) {
                $yVal = $segmentDiffSeries[$i];
                if ($yVal == $tanMax && $i > 0) {
                    $tanMin = $segmentDiffSeries[$i - 1];
                    $tanMINDataPoints = $segmentDiffDataPoints[$i - 1];
                }
            }
            $finalRTThreshold = $tanMINDataPoints[1];
            if (!is_null($userRow['followercount'])) {
                $meanRTThreshold = Analysis::getMeanRTThresholdForFollowerCount($userRow['followercount'], $minRTThreshold, $maxRTThreshold,
                                $lowFollowerThreshold, $highFollowerThreshold);
            } else {
                $meanRTThreshold = null;
            }

            $updateParams[] = [$finalRTThreshold, $meanRTThreshold, $nextAnalysis, $userRow['twitterid']];
        }
        try {
            CoreDB::getConnection()->beginTransaction();
            foreach ($updateParams as $updateParamsForUser) {
                $updateStmt = CoreDB::getConnection()->prepare($updateQuery);
                try {
                    $updateStmt->execute($updateParamsForUser);
                } catch (\PDOException $e) {
                    self::$logger->error("Failed to update artist analytics. Update params: " . print_r($updateParamsForUser, true)
                            . " PDO error: " . print_r($e, true));
                }
            }
            CoreDB::getConnection()->commit();
        } catch (\PDOException $e) {
            self::$logger->error("Failed to commit artist analytics transaction: " . print_r($e, true));
            return false;
        }
        return true;
    }

}

Analysis::initialiseLogger();
