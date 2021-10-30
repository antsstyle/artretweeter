<?php

namespace ArtRetweeter;

require_once "core.php";

function getStandardDeviation($arr) {
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

function computeAdaptiveAnalytics() {
    $metricTypesSelectQuery = "SELECT * FROM retrievalmetrics";
    $metricTypesSelectStmt = $GLOBALS['databaseConnection']->prepare($metricTypesSelectQuery);
    $success = $metricTypesSelectStmt->execute();
    if (!$success) {
        error_log("Failed to get metrics type, cannot calculate metrics.");
        return;
    }
    while ($row = $metricTypesSelectStmt->fetch()) {
        if ($row['description'] == "Latest Metrics") {
            $metricID = $row['id'];
        }
    }
    if (!isset($metricID)) {
        error_log("Could not find ID for latest metrics retrieval metric type - cannot compute adaptive analytics.");
        return;
    }
    $minimumTweetMetricsLimit = 50;
    $selectUsersQuery = "SELECT usertwitterid FROM `users` INNER JOIN userautomationsettings "
            . "ON users.twitterid=userautomationsettings.usertwitterid WHERE automationenabled=? "
            . "AND usertwitterid IN (SELECT usertwitterid FROM tweets INNER JOIN tweetmetrics ON tweets.id=tweetmetrics.tweetstableid "
            . "GROUP BY usertwitterid HAVING (COUNT(*) > ?)) ";
    $selectUsersStmt = $GLOBALS['databaseConnection']->prepare($selectUsersQuery);
    $success = $selectUsersStmt->execute(["Y", $minimumTweetMetricsLimit]);
    if (!$success) {
        error_log("Failed to get tweets from DB, cannot update metrics.");
        return;
    }
    $userTwitterIDs = [];
    while ($userRow = $selectUsersStmt->fetch()) {
        $userTwitterIDs[] = $userRow['usertwitterid'];
    }

    $updateQuery = "UPDATE userautomationsettings SET adaptivertthreshold=? WHERE usertwitterid=?";
    $updateParams = [];
    foreach ($userTwitterIDs as $userTwitterID) {
        $selectQuery = "SELECT * FROM tweets INNER JOIN tweetmetrics ON tweets.id=tweetmetrics.tweetstableid WHERE usertwitterid=?"
                . " AND retrievalmetric=? ORDER BY retweets";
        $selectStmt = $GLOBALS['databaseConnection']->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID, $metricID]);
        if (!$success) {
            error_log("Failed to get tweets from DB, cannot update metrics.");
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
        $updateParams[] = [$finalRTThreshold, $userTwitterID];
        error_log("Final RT Threshold for $userTwitterID is: $finalRTThreshold");
    }
    $GLOBALS['databaseConnection']->beginTransaction();
    foreach ($updateParams as $updateParamsForUser) {
        $updateStmt = $GLOBALS['databaseConnection']->prepare($updateQuery);
        $updateStmt->execute($updateParamsForUser);
    }
    $GLOBALS['databaseConnection']->commit();
}
