<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Core\CachedVariables;
use Antsstyle\ArtRetweeter\Core\CoreDB;

class Analysis {

    public static $logger;

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
        $selectUsersStmt = CoreDB::$databaseConnection->prepare($selectUsersQuery);
        $success = $selectUsersStmt->execute(["Y", $currentTime, $minimumTweetMetricsLimit]);
        if (!$success) {
            Analysis::$logger->critical("Failed to get tweets from DB, cannot update metrics.");
            return;
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
            $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
            $success = $selectStmt->execute([$userTwitterID, $metricID]);
            if (!$success) {
                Analysis::$logger->critical("Failed to get tweets from DB, cannot update metrics.");
                return;
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
        CoreDB::$databaseConnection->beginTransaction();
        foreach ($updateParams as $updateParamsForUser) {
            $updateStmt = CoreDB::$databaseConnection->prepare($updateQuery);
            $updateStmt->execute($updateParamsForUser);
        }
        CoreDB::$databaseConnection->commit();
    }

    public static function computeAdaptiveAnalyticsForArtists() {
        $minRTThreshold = CoreDB::getCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_MIN_RT_THRESHOLD);
        if (is_null($minRTThreshold)) {
            Analysis::$logger->critical("Failed to get min RT threshold value from DB - using default.");
            $minRTThreshold = 0.25;
        } else if ($minRTThreshold === false) {
            $minRTThreshold = 0.25;
            CoreDB::updateCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_MIN_RT_THRESHOLD, "0.25");
        }
        $maxRTThreshold = CoreDB::getCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_MAX_RT_THRESHOLD);
        if (is_null($maxRTThreshold)) {
            Analysis::$logger->critical("Failed to get max RT threshold value from DB - using default.");
            $maxRTThreshold = 0.5;
        } else if ($maxRTThreshold === false) {
            $maxRTThreshold = 0.5;
            CoreDB::updateCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_MAX_RT_THRESHOLD, "0.5");
        }

        $lowFollowerThreshold = CoreDB::getCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_LOW_FOLLOWER_THRESHOLD);
        if (is_null($lowFollowerThreshold)) {
            Analysis::$logger->critical("Failed to get low follower threshold value from DB - using default.");
            $lowFollowerThreshold = 0.25;
        } else if ($lowFollowerThreshold === false) {
            $lowFollowerThreshold = 0.25;
            CoreDB::updateCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_LOW_FOLLOWER_THRESHOLD, "0.25");
        }
        $highFollowerThreshold = CoreDB::getCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_HIGH_FOLLOWER_THRESHOLD);
        if (is_null($highFollowerThreshold)) {
            Analysis::$logger->critical("Failed to get high follower threshold value from DB - using default.");
            $highFollowerThreshold = 0.5;
        } else if ($highFollowerThreshold === false) {
            $highFollowerThreshold = 0.5;
            CoreDB::updateCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_HIGH_FOLLOWER_THRESHOLD, "0.5");
        }

        $metricID = CoreDB::getLatestMetricsTypeID();
        if (is_null($metricID)) {
            return;
        }
        $minimumTweetMetricsLimit = 10;
        $currentTime = date("Y-m-d H:i:s");
        $selectUsersQuery = "SELECT twitterid FROM `artists` WHERE (nextanalysis IS NULL OR nextanalysis <= ?) "
                . "AND twitterid IN (SELECT usertwitterid FROM tweets INNER JOIN tweetmetrics ON tweets.tweetid=tweetmetrics.tweetid "
                . "GROUP BY usertwitterid HAVING (COUNT(*) > ?)) ";
        $selectUsersStmt = CoreDB::$databaseConnection->prepare($selectUsersQuery);
        $success = $selectUsersStmt->execute([$currentTime, $minimumTweetMetricsLimit]);
        if (!$success) {
            Analysis::$logger->critical("Failed to get tweets from DB, cannot update metrics.");
            return;
        }
        $updateQuery = "UPDATE artists SET adaptivertthreshold=?, meanrtthreshold=?, nextanalysis=? WHERE twitterid=?";

        $updateParams = [];
        $nextAnalysis = date("Y-m-d H:i:s", strtotime("+7 days"));

        while ($userRow = $selectUsersStmt->fetch()) {
            $selectQuery = "SELECT * FROM tweets INNER JOIN tweetmetrics ON tweets.tweetid=tweetmetrics.tweetid WHERE usertwitterid=?"
                    . " AND retrievalmetric=? ORDER BY retweets";
            $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
            $success = $selectStmt->execute([$userRow['twitterid'], $metricID]);
            if (!$success) {
                Analysis::$logger->critical("Failed to get tweets from DB, cannot update metrics.");
                return;
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
        CoreDB::$databaseConnection->beginTransaction();
        foreach ($updateParams as $updateParamsForUser) {
            $updateStmt = CoreDB::$databaseConnection->prepare($updateQuery);
            $updateStmt->execute($updateParamsForUser);
        }
        CoreDB::$databaseConnection->commit();
    }

}

Analysis::$logger = LogManager::getLogger("Analysis");
