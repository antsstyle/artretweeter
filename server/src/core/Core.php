<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\StatusCode;
use Antsstyle\ArtRetweeter\Core\CachedVariables;
use Antsstyle\ArtRetweeter\Core\CoreDB;
use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Abraham\TwitterOAuth\TwitterOAuth;

class Core {

    public static $logger;

    public static function checkResponseHeadersForErrors($connection, $userTwitterID = null) {
        $headers = $connection->getLastXHeaders();
        $httpCode = $connection->getLastHttpCode();
        if ($httpCode != 200) {
            $requestBody = $connection->getLastBody();
            if (isset($requestBody->errors) && is_array($requestBody->errors)) {
                $error = $requestBody->errors[0];
                $errorCode = $error->code;
                if ($errorCode == StatusCode::TWITTER_INVALID_ACCESS_TOKEN) {
                    Core::$logger->info("Deleting user, invalid access token.");
                    //CoreDB::deleteUser($userTwitterID);
                }
                if ($errorCode == StatusCode::TWITTER_USER_ACCOUNT_LOCKED) {
                    CoreDB::setUserLocked("Y", $userTwitterID);
                }
                if ($httpCode == StatusCode::HTTP_TOO_MANY_REQUESTS) {
                    Core::$logger->critical("Rate limits exceeded, received error 429!");
                }
                if (!(($httpCode >= 500 && $httpCode <= 599) || ($httpCode >= 200 && $httpCode <= 299))) {
                    Core::$logger->error("Response headers contained HTTP code: $httpCode, error code: $errorCode. Response body was:");
                    Core::$logger->error(print_r($connection->getLastBody(), true));
                }
                return new StatusCode($httpCode, $errorCode);
            }
            return new StatusCode($httpCode, StatusCode::ARTRETWEETER_QUERY_OK);
        }
        if (!isset($headers['x_rate_limit_remaining']) || !isset($headers['x_rate_limit_limit']) || !isset($headers['x_rate_limit_reset'])) {
            return new StatusCode(200, 0);
        }
        if ($headers['x_rate_limit_remaining'] == 0) {
            $apiPath = $connection->getLastApiPath();
            Core::$logger->info("Reached rate limit zero. API path was: $apiPath");
            return new StatusCode(200, StatusCode::ARTRETWEETER_RATE_LIMIT_ZERO);
        }
        return new StatusCode(200, 0);
    }

    public static function echoSidebar() {
        echo "<div class=\"sidenav\">
                <button class=\"collapsiblemenuitem\" id=\"mainmenu\"><b>Home</b></button>
                <div class=\"content\">
                    <a href=\"https://antsstyle.com/\">About</a>
                    <a href=\"https://antsstyle.com/apps\">Apps</a>
                </div>
                <br/>
                <button class=\"collapsiblemenuitem activemenuitem\" id=\"artretweetermenu\"><b>ArtRetweeter</b></button>
                <div class=\"content\" style=\"max-height: 100%\">
                    <a href=\"https://antsstyle.com/artretweeter\">Home</a>
                    <a href=\"https://antsstyle.com/artretweeter/info\">Info & FAQ</a> 
                    <a href=\"https://antsstyle.com/artretweeter/artistsettings\">Artist Settings</a> 
                    <a href=\"https://antsstyle.com/artretweeter/artistqueuestatus\">Artist Queue Status</a>
                </div>
                <br/>
                <button class=\"collapsiblemenuitem\" id=\"nftcryptoblockermenu\"><b>NFT Artist & Cryptobro Blocker</b></button>
                <div class=\"content\">
                    <a href=\"https://antsstyle.com/nftcryptoblocker/\">Home</a>
                    <a href=\"https://antsstyle.com/nftcryptoblocker/settings\">Settings</a>
                </div>
            </div>";
    }

    public static function scheduleAllUserArtistRetweets() {
        $now = date("Y-m-d H:i:s");
        $selectQuery = "SELECT * FROM userartistautomationsettings WHERE automationenabled=? "
                . "AND (nextserverscheduledate IS NULL OR nextserverscheduledate <= ?)";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute(["Y", $now]);
        if (!$success) {
            Core::$logger->critical("Failed to get all user artist automation settings!");
            return;
        }
        $rows = $selectStmt->fetchAll();
        error_log("Num users: " . count($rows));
        $nonArtistRTThreshold = CoreDB::getCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_THRESHOLD);
        if (is_null($nonArtistRTThreshold)) {
            Core::$logger->critical("Unable to get non-artist retweets threshold - cannot schedule non-artist retweets!");
            return;
        } else if ($nonArtistRTThreshold === false) {
            $nonArtistRTThreshold = 0.25;
            CoreDB::updateCachedVariable(CachedVariables::NON_ARTIST_RETWEET_METRICS_THRESHOLD, "0.25");
        }
        foreach ($rows as $row) {
            Core::scheduleUserArtistRetweets($row, $nonArtistRTThreshold);
        }
    }

    public static function scheduleUserArtistRetweets($userRow, $nonArtistRTThreshold) {
        $userMaxDailyLimit = 10;
        $dayFlags = $userRow['dayflags'];
        $hourFlags = $userRow['hourflags'];
        $dayCount = substr_count($dayFlags, 'Y');
        $hourCount = substr_count($hourFlags, 'Y');
        if ($dayCount == 0 || $hourCount == 0) {
            Core::$logger->error("User has invalid artist automation settings, cannot continue.");
            return;
        }
        error_log("User day flags: $dayFlags     User hour flags: $hourFlags");
        $today = date("N") - 1;
        $twoDaysFromNow = $today + 2;
        if ($twoDaysFromNow >= 7) {
            $twoDaysFromNow -= 7;
        }
        $dayFlag = substr($dayFlags, $twoDaysFromNow, 1);
        if ($dayFlag !== "Y") {
            Core::$logger->debug("Two days from now: $twoDaysFromNow. Today: $today. Dayflags: $dayFlags");
            return;
        }
        $limitPerDay = min($hourCount, $userMaxDailyLimit);
        $totalCountPerDay = $limitPerDay * ($dayCount / 7);
        $selectQuery = "SELECT * FROM userartistretweetsettings INNER JOIN artists "
                . "ON userartistretweetsettings.artisttwitterid=artists.twitterid WHERE userartistretweetsettings.usertwitterid=? "
                . "ORDER BY totalretweeted ASC, RAND()";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userRow['usertwitterid']]);
        if (!$success) {
            Core::$logger->error("Failed to get user artist results for user ID: " . $userRow['usertwitterid']);
            return;
        }
        $rows = $selectStmt->fetchAll();
        if (count($rows) === 0) {
            return;
        }
        $totalArtistTweetCount = 0;
        foreach ($rows as $row) {
            $tweetRows = Core::getUserArtistEligibleTweets($userRow['usertwitterid'], $row, $nonArtistRTThreshold);
            error_log("Eligible tweets for artist " . $row['twitterid'] . ": " . count($tweetRows));
            $totalArtistTweetCount += count($tweetRows);
            if (count($tweetRows) > 0) {
                $tweetRowsArray[] = $tweetRows;
            }
        }
        $tweetsAvailablePerArtist = $totalCountPerDay / $totalArtistTweetCount;
        if ($tweetsAvailablePerArtist >= 1) {
            $maxTweetsPerArtist = 1;
        } else {
            $maxTweetsPerArtist = 2;
        }
        $artistIndex = 0;
        $scheduledTweets = 0;
        $hourIndices = Core::getHourFlagIndices($hourFlags);
        error_log("Limit per day: $limitPerDay");
        error_log("Count of tweet rows array: " . count($tweetRowsArray));
        $endReached = false;
        while ($scheduledTweets < $limitPerDay && ($artistIndex < count($tweetRowsArray)) && !$endReached) {
            $artistRows = $tweetRowsArray[$artistIndex];
            for ($i = 0; $i < $maxTweetsPerArtist; $i++) {
                $tweetIDToSchedule = $artistRows[$i]['tweetid'];
                $tweetAuthorID = $artistRows[$i]['usertwitterid'];
                $nextHour = Core::getRandomHourToAutomate($hourIndices);
                $minute = rand(0, 59);
                unset($hourIndices[array_search($nextHour, $hourIndices)]);
                $hourIndices = array_values($hourIndices);
                $retweetTime = new \DateTime();
                $retweetTime->add(new \DateInterval('P2D'));
                $retweetTime->setTime($nextHour, $minute);
                $retweetTimeStamp = $retweetTime->getTimestamp();
                Core::queueRetweetInDB($userRow['usertwitterid'], $tweetAuthorID, $tweetIDToSchedule, $retweetTimeStamp);
                $scheduledTweets++;
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
        $updateStmt = CoreDB::$databaseConnection->prepare($updateQuery);
        $success = $updateStmt->execute([$nextServerScheduleDate, $userRow['usertwitterid']]);
    }

    public static function convertServerTimeStringToUserTime($timeString, $userHourOffset, $userMinuteOffset) {
        $userOffsetSeconds = ($userHourOffset * 3600) + ($userMinuteOffset * 60);
        $now = new \DateTime();
        $serverTimeZone = new \DateTimeZone(date_default_timezone_get());
        $serverOffsetSeconds = $serverTimeZone->getOffset($now);
        $timeDiffSeconds = $serverOffsetSeconds - $userOffsetSeconds;
        $timeString = date("Y-m-d H:i:s", strtotime($timeString) - $timeDiffSeconds);
        return $timeString;
    }

    public static function convertUserTimeStringToServerTime($timeString, $userHourOffset, $userMinuteOffset) {
        $userOffsetSeconds = ($userHourOffset * 3600) + ($userMinuteOffset * 60);
        $now = new \DateTime();
        $serverTimeZone = new \DateTimeZone(date_default_timezone_get());
        $serverOffsetSeconds = $serverTimeZone->getOffset($now);
        $timeDiffSeconds = $serverOffsetSeconds - $userOffsetSeconds;
        $timeString = date("Y-m-d H:i:s", strtotime($timeString) + $timeDiffSeconds);
        return $timeString;
    }

    public static function removeExpiredRetweets() {
        $time1HourAgo = date('Y-m-d H:i:s', strtotime('-1 hour', time()));
        $insertQuery = "INSERT INTO failedretweets (retweetingusertwitterid,tweetid,retweettime,errorcode,failreason) "
                . "SELECT retweetingusertwitterid, tweetid, retweettime, ?, "
                . "? FROM scheduledretweets WHERE retweettime <= ?";
        CoreDB::$databaseConnection->prepare($insertQuery)
                ->execute([-1, "Missed schedule", $time1HourAgo]);
        CoreDB::$databaseConnection->prepare("DELETE FROM scheduledretweets WHERE retweettime <= ?")->execute([$time1HourAgo]);
    }

    public static function deleteRetweet($userAuth, $tweetID) {
        $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret,
                $userAuth['access_token'], $userAuth['access_token_secret']);
        $connection->setApiVersion('2');
        $connection->setRetries(1, 1);
        $query = "users/" . $userAuth['twitter_id'] . "/retweets/" . $tweetID;
        $connection->delete($query);
    }

    public static function getUserTwitterObjectByHandle($userAuth, $twitterHandle) {
        $params['screen_name'] = $twitterHandle;
        $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret,
                $userAuth['access_token'], $userAuth['access_token_secret']);
        $connection->setRetries(1, 1);
        $queryResult = Core::queryTwitterUserAuth($connection, "users/show", "GET", $params, $userAuth);
        return $queryResult;
    }

    public static function postScheduledRetweets() {
        $retweetEndpoint = "statuses/retweet";
        $time5MinsAgo = date('Y-m-d H:i:s', strtotime('-5 minutes', time()));
        $time5MinsFromNow = date('Y-m-d H:i:s', strtotime('+5 minutes', time()));
        $selectQuery = "SELECT scheduledretweets.id,tweetid,retweettime,accesstoken,accesstokensecret,"
                . "retweetingusertwitterid,tweetauthorid FROM scheduledretweets INNER JOIN users ON "
                . "scheduledretweets.retweetingusertwitterid = users.twitterid WHERE retweettime >= ? "
                . "AND retweettime <= ?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$time5MinsAgo, $time5MinsFromNow]);
        if (!$success) {
            Core::$logger->error("Failed to get scheduled retweet information!");
            return;
        }
        while ($row = $selectStmt->fetch()) {
            $databaseID = $row['id'];
            $tweetID = $row['tweetid'];
            $retweetTime = $row['retweettime'];
            $accessToken = $row['accesstoken'];
            $accessTokenSecret = $row['accesstokensecret'];
            $userAuth['twitter_id'] = $row['retweetingusertwitterid'];
            $userAuth['access_token'] = $accessToken;
            $userAuth['access_token_secret'] = $accessTokenSecret;
            $tweetAuthorID = $row['tweetauthorid'];
            if ($row['retweetingusertwitterid'] === $tweetAuthorID) {
                $selfRetweet = true;
            } else {
                $selfRetweet = false;
            }
            $retweetRecordResults = Core::checkUserCanQueueNewRetweet($userAuth['twitter_id'], $retweetTime);
            if (!$retweetRecordResults) {
                CoreDB::$databaseConnection->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$databaseID]);
                continue;
            }
            $canRetweetThisTweetResults = Core::checkUserCanRetweetOldTweet($userAuth['twitter_id'], $retweetTime, $tweetID);
            if (!$canRetweetThisTweetResults) {
                CoreDB::$databaseConnection->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$databaseID]);
                continue;
            }
            Core::deleteRetweet($userAuth, $tweetID);
            $params['id'] = $tweetID;
            $params['trim_user'] = 1;
            $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret, $accessToken, $accessTokenSecret);
            $connection->setRetries(1, 1);
            $queryResult = Core::queryTwitterUserAuth($connection, $retweetEndpoint, "POST", $params, $userAuth);
            $insertFailedRetweetQuery = "INSERT INTO failedretweets (retweetingusertwitterid,tweetid,retweettime,errorcode,failreason) "
                    . "SELECT retweetingusertwitterid, tweetid, retweettime, ?, "
                    . "? FROM scheduledretweets WHERE id=?";
            if (!$queryResult) {
                CoreDB::$databaseConnection->prepare($insertFailedRetweetQuery)
                        ->execute([2, "Internal error whilst attempting to retweet", $databaseID]);
            } else {
                if ($queryResult->errors) {
                    if (is_array($queryResult->errors)) {
                        $errorCode = $queryResult->errors[0]->code;
                        $errorMessage = $queryResult->errors[0]->message;
                    } else {
                        $errorCode = $queryResult->errors->code;
                        $errorMessage = $queryResult->errors->message;
                    }
                    CoreDB::$databaseConnection->prepare($insertFailedRetweetQuery)
                            ->execute([$errorCode, $errorMessage, $databaseID]);
                } else {
                    $originalStatus = $queryResult->retweeted_status;
                    $retweetedStatusID = $queryResult->id;
                    $originalTweetID = $originalStatus->id;
                    Core::updateRetweetRecordsInDB($userAuth['twitter_id'], $originalTweetID, $retweetedStatusID, $retweetTime,
                            $tweetAuthorID, $selfRetweet);
                }
            }
            CoreDB::$databaseConnection->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$databaseID]);
        }
    }

    public static function queueRetweetInDB($userAuthTwitterID, $tweetAuthorID, $tweetID, $retweetTime) {
        if (!Core::checkUserCanQueueNewRetweet($userAuthTwitterID, $retweetTime)) {
            return false;
        }
        if (!Core::checkUserCanRetweetOldTweet($userAuthTwitterID, $retweetTime, $tweetID)) {
            return false;
        }
        $timeString = date('Y-m-d H:i:s', $retweetTime);
        $stmt = CoreDB::$databaseConnection->prepare("INSERT INTO scheduledretweets (retweetingusertwitterid,tweetauthorid,tweetid,retweettime,automated) "
                . "VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE retweetingusertwitterid=?, tweetauthorid=?, tweetid=?, retweettime=?, automated=?");
        $success = $stmt->execute([$userAuthTwitterID, $tweetAuthorID, $tweetID, $timeString, "N",
            $userAuthTwitterID, $tweetAuthorID, $tweetID, $timeString, "N"]);

        return $success;
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

    public static function validateUserAuth($userAuth) {
        if ($userAuth === APIKeys::bearer_token) {
            return true;
        }
        $bannedStmt = CoreDB::$databaseConnection->prepare("SELECT * FROM bannedusers WHERE twitterid=?");
        $success = $bannedStmt->execute([$userAuth['twitter_id']]);
        if (!$success) {
            return false;
        }
        $bannedResults = $bannedStmt->fetch();
        if ($bannedResults) {
            return false;
        }
        $stmt = CoreDB::$databaseConnection->prepare("SELECT * FROM users WHERE twitterid=? AND accesstoken=? AND accesstokensecret=?");
        $success = $stmt->execute([$userAuth['twitter_id'], $userAuth['access_token'], $userAuth['access_token_secret']]);
        if (!$success) {
            return false;
        }
        $results = $stmt->fetch();
        if (!$results) {
            return false;
        }
    }

    public static function updateAccessToken($response) {
        $stmt = CoreDB::$databaseConnection->prepare("INSERT INTO users (twitterid,accesstoken,accesstokensecret) "
                . "VALUES (?,?,?) ON DUPLICATE KEY UPDATE twitterid=?, accesstoken=?, accesstokensecret=?");
        return $stmt->execute([$response['user_id'], $response['oauth_token'], $response['oauth_token_secret'],
                    $response['user_id'], $response['oauth_token'], $response['oauth_token_secret']]);
    }

    public static function checkUserCanRetweetOldTweet($userTwitterID, $retweetTime, $tweetID) {
        $date30DaysAgo = date("Y-m-d H:i:s", strtotime('-30 days', $retweetTime));
        $date1YearAgo = date("Y-m-d H:i:s", strtotime('-1 year', $retweetTime));
        // Limit users to retweeting the same tweet only once per month, and not more than 3 times per year
        $per30DaysLimit = 1;
        $perYearLimit = 3;
        $records30DaysStmt = CoreDB::$databaseConnection->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime >= ? 
        AND tweetid=? ORDER BY retweettime DESC LIMIT ?");
        $records30DaysStmt->bindValue(1, $userTwitterID);
        $records30DaysStmt->bindValue(2, $date30DaysAgo);
        $records30DaysStmt->bindValue(3, $tweetID);
        $records30DaysStmt->bindValue(4, $per30DaysLimit, \PDO::PARAM_INT);
        $records30DaysSuccess = $records30DaysStmt->execute();
        if (!$records30DaysSuccess) {
            Core::$logger->error("Failed to get retweet records to check monthly limits.");
            return false;
        }
        $records30DaysResults = $records30DaysStmt->fetchAll();
        if ($records30DaysResults && (count($records30DaysResults) >= $per30DaysLimit)) {
            return false;
        }


        $recordsYearStmt = CoreDB::$databaseConnection->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime >= ? 
        AND tweetid=? ORDER BY retweettime DESC LIMIT ?");
        $recordsYearStmt->bindValue(1, $userTwitterID);
        $recordsYearStmt->bindValue(2, $date1YearAgo);
        $recordsYearStmt->bindValue(3, $tweetID);
        $recordsYearStmt->bindValue(4, $perYearLimit, \PDO::PARAM_INT);
        $recordsYearSuccess = $recordsYearStmt->execute();
        if (!$recordsYearSuccess) {
            Core::$logger->error("Failed to get retweet records to check yearly limits.");
            return false;
        }
        $recordsYearResults = $recordsYearStmt->fetchAll();
        if ($recordsYearResults && (count($recordsYearResults) >= $perYearLimit)) {
            return false;
        }
        return true;
    }

    public static function getNumTweetsAtExactTime($table, $userTwitterID, $retweetTime) {
        if ($table == "scheduledretweets") {
            $stmt = CoreDB::$databaseConnection->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime=?");
        } else if ($table == "retweetrecords") {
            $stmt = CoreDB::$databaseConnection->prepare("SELECT * FROM scheduledretweets WHERE retweetingusertwitterid=? AND retweettime=?");
        } else {
            return false;
        }
        $stmt->bindValue(1, $userTwitterID);
        $stmt->bindValue(2, $retweetTime);
        $success = $stmt->execute();
        if (!$success) {
            return $stmt->errorInfo();
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
        $stmt = CoreDB::$databaseConnection->prepare($query);
        $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
        $stmt->bindValue(2, $intervalStart, \PDO::PARAM_STR);
        $stmt->bindValue(3, $intervalEnd, \PDO::PARAM_STR);
        if (!is_null($rescheduledTweetID)) {
            $stmt->bindValue(4, $rescheduledTweetID, \PDO::PARAM_STR);
            $stmt->bindValue(5, $limit, \PDO::PARAM_INT);
        } else {
            $stmt->bindValue(4, $limit, \PDO::PARAM_INT);
        }
        $success = $stmt->execute();
        if (!$success) {
            return $stmt->errorInfo();
        }
        $scheduledTimes = $stmt->fetchAll();
        $query = "SELECT UNIX_TIMESTAMP(scheduledretweettime) AS rttime FROM retweetrecords WHERE usertwitterid=? 
            AND scheduledretweettime >= ? AND scheduledretweettime <= ?";
        if (!is_null($rescheduledTweetID)) {
            $query .= " AND tweetid != ?";
        }
        $query .= " ORDER BY scheduledretweettime ASC LIMIT ?";
        $stmt = CoreDB::$databaseConnection->prepare($query);
        $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
        $stmt->bindValue(2, $intervalStart, \PDO::PARAM_STR);
        $stmt->bindValue(3, $intervalEnd, \PDO::PARAM_STR);
        if (!is_null($rescheduledTweetID)) {
            $stmt->bindValue(4, $rescheduledTweetID, \PDO::PARAM_STR);
            $stmt->bindValue(5, $limit, \PDO::PARAM_INT);
        } else {
            $stmt->bindValue(4, $limit, \PDO::PARAM_INT);
        }
        $success = $stmt->execute();
        if (!$success) {
            return $stmt->errorInfo();
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

    public static function checkUserCanQueueNewRetweet($userTwitterID, $retweetTime, $rescheduledTweetID = null) {
        $RTTime = date("Y-m-d H:i:s", $retweetTime);
        $perDayLimit = 10;
        $perHourLimit = 2;

        $recordsNowResults = Core::getNumTweetsAtExactTime("retweetrecords", $userTwitterID, $RTTime);
        if ($recordsNowResults) {
            return "You cannot schedule two retweets to be posted at the same time.";
        }

        $queueNowResults = Core::getNumTweetsAtExactTime("scheduledretweets", $userTwitterID, $RTTime);
        if ($queueNowResults) {
            return "You cannot schedule two retweets to be posted at the same time.";
        }

        $result = Core::getNumTweetsInTimeInterval($userTwitterID, $retweetTime, "1 hour", $perHourLimit, $rescheduledTweetID);
        if ($result !== true) {
            return $result;
        }
        $result = Core::getNumTweetsInTimeInterval($userTwitterID, $retweetTime, "24 hours", $perDayLimit, $rescheduledTweetID);
        if ($result !== true) {
            return $result;
        }
        return true;
    }

    public static function updateRetweetRecordsInDB($userTwitterID, $tweetID, $retweetID, $scheduledTime, $tweetAuthorID, $selfRetweet) {
        $stmt = CoreDB::$databaseConnection->prepare("INSERT INTO retweetrecords (usertwitterid,tweetid,retweetid,retweettime,scheduledretweettime) 
	VALUES (?,?,?,?,?)");
        $currentTime = date("Y-m-d H:i:s", time());
        $stmt->execute([$userTwitterID, $tweetID, $retweetID, $currentTime, $scheduledTime]);
        if (!$selfRetweet) {
            $stmt = CoreDB::$databaseConnection->prepare("UPDATE userartistretweetsettings SET totalretweeted=totalretweeted+1 WHERE usertwitterid=? "
                    . "AND artisttwitterid=?");
            $stmt->execute([$userTwitterID, $tweetAuthorID]);
        }
    }

    public static function encodeDBResponseInformation($dbResult) {
        $results['dbresult'] = $dbResult;
        return json_encode($results);
    }

    public static function encodeTwitterResponseInformation($connection, $response) {
        $results['response'] = $response;
        $results['headers'] = $connection->getLastXHeaders();
        $results['httpcode'] = $connection->getLastHttpCode();
        return json_encode($results);
    }

    public static function updateUserRateLimitInDB($userTwitterID, $endpoint, $headers) {
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
        $stmt = CoreDB::$databaseConnection->prepare("INSERT INTO ratelimitrecords (usertwitterid,endpoint,maxlimit,remaininglimit,resettime,timetoresetseconds) 
	VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE usertwitterid=?, endpoint=?, maxlimit=?, remaininglimit=?, resettime=?, timetoresetseconds=?");
        $success = $stmt->execute([$userTwitterID, $endpoint, $maxLimit, $remainingLimit, $resetTime, $timeToResetSeconds,
            $userTwitterID, $endpoint, $maxLimit, $remainingLimit, $resetTime, $timeToResetSeconds]);
        return $success;
    }

    public static function checkUserRateLimitInDB($userTwitterID, $endpoint) {
        $stmt = CoreDB::$databaseConnection->prepare("SELECT * FROM ratelimitrecords WHERE usertwitterid=? AND endpoint=?");
        $success = $stmt->execute([$userTwitterID, $endpoint]);
        if (!$success) {
            Core::$logger->error("Failed to get user rate limit records.");
            return true;
        }
        $result = $stmt->fetch();

        if ($result && $result['remaininglimit'] < 5) {
            return false;
        } else {
            return true;
        }
    }

    public static function queryTwitterUserAuth($connection, $endpoint, $httpRequestType, $params, $userAuth) {
        if ($userAuth !== APIKeys::bearer_token) {
            $userAuthTwitterID = $userAuth['twitter_id'];
            Core::validateUserAuth($userAuth);
            Core::checkUserRateLimitInDB($userAuthTwitterID, $endpoint);
        }
        if ($httpRequestType == 'GET') {
            $result = $connection->get($endpoint, $params);
        } else {
            $result = $connection->post($endpoint, $params);
        }
        if ($userAuth !== APIKeys::bearer_token) {
            if (Core::checkIfUserAccessTokenInvalid($result)) {
                Core::$logger->error("Invalid access token detected for user ID: $userAuthTwitterID, disabling any automation.");
                $query = "UPDATE userautomationsettings SET automationenabled=? WHERE usertwitterid=?";
                $stmt = CoreDB::$databaseConnection->prepare($query);
                $stmt->execute(["N", $userAuthTwitterID]);
                $deleteAutomatedRetweetsQuery = "DELETE FROM scheduledretweets WHERE retweetingusertwitterid=? AND automated=?";
                $stmt = CoreDB::$databaseConnection->prepare($deleteAutomatedRetweetsQuery);
                $stmt->execute([$userAuthTwitterID, "N"]);
            } else {
                Core::updateUserRateLimitInDB($userAuthTwitterID, $endpoint, $connection->getLastXHeaders());
            }
        }
        return $result;
    }

    public static function checkIfUserAccessTokenInvalid($result) {
        if (is_object($result)) {
            if (is_array($result->errors)) {
                $errorCode = $result->errors[0]->code;
                return ($errorCode == 89);
            } else if (is_object($result->errors)) {
                $errorCode = $result->errors->code;
                return ($errorCode == 89);
            }
        }
        return false;
    }

    public static function getTweetMetricsToRefresh($userTwitterID, $startTweetID, $metricsType) {
        $selectQuery = "SELECT *,(SELECT description FROM retrievalmetrics WHERE retrievalmetrics.id=tweetmetrics.retrievalmetric) "
                . "AS description FROM tweetmetrics WHERE usertwitterid=? AND tweetid > ? "
                . "AND retrievalmetric=(SELECT id FROM retrievalmetrics WHERE description=?) "
                . "ORDER BY tweetid ASC LIMIT 101";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID, $startTweetID, $metricsType]);
        if (!$success) {
            Core::$logger->error("Failed to retrieve tweet metrics from database. User twitter ID: $userTwitterID     Starting tweetID: $startTweetID");
            return;
        }
        $rows = $selectStmt->fetchAll();
        if (!$rows) {
            $results['moreentries'] = false;
        }
        $idString = "";
        $time1weekago = date("Y-m-d H:i:s", strtotime("-1 week"));
        $existingMetrics = array();
        for ($i = 0; $i < 100; $i++) {
            $tweetmetric = $rows[$i];
            $type = $tweetmetric['description'];
            $retrievedTime = $tweetmetric['retrievedtime'];
            if (($retrievedTime <= $time1weekago) || $type == "12 Hour Metrics" || $type == "24 Hour Metrics") {
                array_push($existingMetrics, $tweetmetric);
            } else {
                $idString = $idString . $tweetmetric['tweetid'] . ",";
            }
        }
        if ($idString) {
            $idString = substr($idString, 0, -1);
        }

        $results['idstring'] = $idString;
        $results['existingmetrics'] = $existingMetrics;
        $results['moreentries'] = (count($rows) > 100);
        $results['nextcursor'] = end($rows);
        return $results;
    }

    public static function fixTweetText() {
        $selectQuery = "SELECT * FROM tweets WHERE tweetjson IS NOT NULL AND fixed=? LIMIT 25000";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute(["N"]);
        if (!$success) {
            Core::$logger->error("Failed to get tweets to fix");
            return;
        }
        while ($row = $selectStmt->fetch()) {
            $rowID = $row['id'];
            $tweet = json_decode($row['tweetjson']);
            $fullText = $tweet->full_text;
            $media = $tweet->entities->media;
            for ($j = 0; $j < count($media); $j++) {
                $url = $media[$j]->url;
                $fullText = str_replace($url, "", $fullText);
            }
            $urls = $tweet->entities->urls;
            for ($j = 0; $j < count($urls); $j++) {
                $url = $urls[$j]->url;
                $extendedUrl = $urls[$j]->expanded_url;
                $fullText = str_replace($url, $extendedUrl, $fullText);
            }
            $updateQuery = "UPDATE tweets SET fulltweettext=?, fixed=? WHERE id=?";
            $updateStmt = CoreDB::$databaseConnection->prepare($updateQuery);
            try {
                $success = $updateStmt->execute([$fullText, "Y", $rowID]);
            } catch (\Exception $e) {
                error_log("Tweet ID: " . $tweet->id);
                error_log("Full tweet text too long: $fullText");
            }
        }
    }

    public static function insertTweetsAndMetrics($tweets, $userTwitterID, $imagesEnabled = "Y", $gifsEnabled = "N", $videosEnabled = "N") {
        $selectQuery = "SELECT id FROM retrievalmetrics WHERE description=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute(["Latest Metrics"]);
        if (!$success) {
            Core::$logger->error("Failed to get metrics type, cannot insert tweets.");
            return;
        }
        $metricID = $selectStmt->fetchColumn();
        if (!$metricID) {
            Core::$logger->error("No metrics type found, cannot insert tweets.");
            return;
        }

        $selectQuery = "SELECT tweetid FROM tweets WHERE usertwitterid=? AND deletedflag=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID, "Y"]);
        if (!$success) {
            Core::$logger->error("Failed to get delete-flagged tweets for user, cannot insert tweets.");
            return;
        }

        while ($row = $selectStmt->fetch()) {
            $deletedTweetsArray[$row['tweetid']] = 1;
        }
        CoreDB::$databaseConnection->beginTransaction();
        $timeNow = date("Y-m-d H:i:s");
        foreach ($tweets as $tweet) {
            $created_at = date("Y-m-d H:i:s", strtotime($tweet->created_at));
            if (!$tweet->extended_entities) {
                continue;
            }
            if (!$tweet->extended_entities->media) {
                continue;
            }
            $mediaType = $tweet->extended_entities->media[0]->type;
            if ($mediaType === "photo" && $imagesEnabled !== "Y") {
                continue;
            }
            if ($mediaType === "animated_gif" && $gifsEnabled !== "Y") {
                continue;
            }
            if ($mediaType === "video" && $videosEnabled !== "Y") {
                continue;
            }
            if ($tweet->retweeted_status) {
                continue;
            }
            if ($tweet->in_reply_to_user_id && ($tweet->in_reply_to_user_id != $userTwitterID)) {
                continue;
            }
            if (isset($deletedTweetsArray) && isset($deletedTweetsArray[$tweet->id])) {
                continue;
            }
            $fullText = $tweet->full_text;
            $media = $tweet->entities->media;
            for ($j = 0; $j < count($media); $j++) {
                $url = $media[$j]->url;
                $fullText = str_replace($url, "", $fullText);
            }
            $urls = $tweet->entities->urls;
            for ($j = 0; $j < count($urls); $j++) {
                $url = $urls[$j]->url;
                $extendedUrl = $urls[$j]->expanded_url;
                $fullText = str_replace($url, $extendedUrl, $fullText);
            }
            $tweetJson = json_encode($tweet);
            try {
                $stmt = CoreDB::$databaseConnection->prepare("INSERT INTO tweets
            (tweetid,usertwitterid,createdat,fulltweettext,tweetjson,mediatype) 
	VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE fulltweettext=?");
                $stmt->execute([$tweet->id, $userTwitterID, $created_at, $fullText, $tweetJson, $mediaType, $fullText]);
            } catch (\PDOException $e) {
                Core::$logger->error("Failed to insert tweet: " . print_r($e, true));
            }
            if ($stmt->rowCount()) {
                try {
                    $stmt = CoreDB::$databaseConnection->prepare("INSERT INTO tweetmetrics
                (tweetstableid,retrievedtime,retrievalmetric,likes,retweets) VALUES (LAST_INSERT_ID(),?,?,?,?) 
                ON DUPLICATE KEY UPDATE likes=?,retweets=?,retrievedtime=?");
                    $stmt->execute([$timeNow, $metricID, $tweet->favorite_count, $tweet->retweet_count, $tweet->favorite_count,
                        $tweet->retweet_count, $timeNow]);
                } catch (\PDOException $e) {
                    Core::$logger->error("Failed to insert tweet metrics for new tweet: " . print_r($e, true));
                }
            } else {
                $stmt = CoreDB::$databaseConnection->prepare("SELECT id FROM tweets WHERE tweetid=?");
                $stmt->execute([$tweet->id]);
                $tweetsTableID = $stmt->fetchColumn();
                try {
                    $stmt = CoreDB::$databaseConnection->prepare("INSERT INTO tweetmetrics
                (tweetstableid,retrievedtime,retrievalmetric,likes,retweets) VALUES (?,?,?,?,?) 
                ON DUPLICATE KEY UPDATE likes=?,retweets=?,retrievedtime=?");
                    $stmt->execute([$tweetsTableID, $timeNow, $metricID, $tweet->favorite_count, $tweet->retweet_count, $tweet->favorite_count,
                        $tweet->retweet_count, $timeNow]);
                } catch (\PDOException $e) {
                    Core::$logger->error("Failed to insert tweet metrics for existing tweet: " . print_r($e, true));
                }
            }
            if (!isset($highestID)) {
                $highestID = $tweet->id;
            } else if ($tweet->id > $highestID) {
                $highestID = $tweet->id;
            }
            if (!isset($lowestID)) {
                $lowestID = $tweet->id;
            } else if ($tweet->id < $lowestID) {
                $lowestID = $tweet->id;
            }
        }
        CoreDB::$databaseConnection->commit();
        $returnArray = [];
        if (isset($highestID)) {
            $returnArray['highestid'] = $highestID;
        }
        if (isset($lowestID)) {
            $returnArray['lowestid'] = $lowestID;
        }
        if (count($returnArray) == 0) {
            $returnArray['novalidresults'] = true;
        }
        return $returnArray;
    }

    public static function scheduleAutomatedRetweets() {
        $query = "SELECT * FROM users INNER JOIN userautomationsettings ON users.twitterid=userautomationsettings.usertwitterid "
                . "WHERE automationenabled=?";
        $stmt = CoreDB::$databaseConnection->prepare($query);
        $success = $stmt->execute(["Y"]);
        if (!$success) {
            Core::$logger->error("Failed to acquire list of users to schedule automated retweets for, cannot continue.");
            return;
        }
        while ($row = $stmt->fetch()) {
            $userAuth['twitter_id'] = $row['usertwitterid'];
            $userAuth['access_token'] = $row['accesstoken'];
            $userAuth['access_token_secret'] = $row['accesstokensecret'];
            Core::queueAutomatedRetweets($userAuth);
        }
    }

    public static function getAllNewTweetsForAllUsers() {
        $query = "SELECT * FROM users INNER JOIN userautomationsettings ON users.twitterid=userautomationsettings.usertwitterid "
                . "WHERE automationenabled=?";
        $stmt = CoreDB::$databaseConnection->prepare($query);
        $success = $stmt->execute(["Y"]);
        if (!$success) {
            Core::$logger->error("Failed to acquire list of users to schedule automated retweets for, cannot continue.");
            return;
        }
        while ($row = $stmt->fetch()) {
            Core::getAllNewTweetsForUser($row);
        }
    }

    public static function getAllNewTweetsForAllArtists() {
        $now = date("Y-m-d H:i:s");
        $query = "SELECT * FROM artists WHERE (nextcheckdate IS NULL OR nextcheckdate < ?)";
        $stmt = CoreDB::$databaseConnection->prepare($query);
        $success = $stmt->execute([$now]);
        if (!$success) {
            Core::$logger->error("Failed to acquire list of artists to get tweets for, cannot continue.");
            return;
        }
        while ($row = $stmt->fetch()) {
            Core::getAllNewTweetsForArtist($row);
        }
    }

    public static function getAllNewTweetsForArtist($artistRow) {
        $params['count'] = 200;
        $params['user_id'] = $artistRow['twitterid'];
        $params['include_rts'] = "false";
        $params['trim_user'] = "true";
        $params['tweet_mode'] = "extended";
        if ($artistRow['oldtweetlimitretrieved'] === "N" && !is_null($artistRow['maxid'])) {
            $params['max_id'] = $artistRow['maxid'];
        }
        if (!is_null($artistRow['sinceid'])) {
            $params['since_id'] = $artistRow['sinceid'];
        }
        $resultCount = 1;
        $queryCount = 0;
        $reachedEnd = false;
        $reachedEnd = false;
        while ($resultCount != 0 && !$reachedEnd) {
            $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret,
                    null, APIKeys::bearer_token);
            $results = Core::queryTwitterUserAuth($connection, "statuses/user_timeline", "GET", $params, APIKeys::bearer_token);
            $queryCount++;
            if ($connection->getLastHttpCode() == 200) {
                $resultCount = count($results);
                if ($resultCount == 0) {
                    $reachedEnd = true;
                } else {
                    $returnArray = Core::insertTweetsAndMetrics($results, $artistRow['twitterid'], $artistRow['imagesenabled'],
                                    $artistRow['gifsenabled'], $artistRow['videosenabled']);
                    if (!$returnArray) {
                        Core::$logger->error("Empty or nonexistent return array - breaking loop.");
                        break;
                    }
                    if (isset($returnArray['novalidresults']) && $returnArray['novalidresults']) {
                        break;
                    }
                    if ($artistRow['oldtweetlimitretrieved'] == "N") {
                        $params['max_id'] = $returnArray['lowestid'] - 1;
                    } else {
                        $params['since_id'] = $returnArray['highestid'];
                    }
                }
            } else {
                break;
            }
            if ($queryCount > 35) {
                Core::$logger->critical("35 user_timeline queries!");
                break;
            }
        }
        $nextCheckDateRatio = ceil(24 / max($queryCount, 1));
        $nextCheckDate = date("Y-m-d H:i:s", strtotime("+$nextCheckDateRatio hours"));
        $query = "UPDATE artists SET ";
        $updParams = [];
        if ($reachedEnd) {
            $query .= "oldtweetlimitretrieved=?, maxid=?, nextcheckdate=? ";
            array_push($updParams, "Y", $params['max_id'], $nextCheckDate);
        } else if (isset($params['max_id'])) {
            $query .= "maxid=?, nextcheckdate=? ";
            array_push($updParams, $params['max_id'], $nextCheckDate);
        } else {
            $query .= "sinceid=?, nextcheckdate=? ";
            array_push($updParams, $params['since_id'], $nextCheckDate);
        }
        $query .= "WHERE twitterid=?";
        array_push($updParams, $artistRow['twitterid']);

        $stmt = CoreDB::$databaseConnection->prepare($query);
        return $stmt->execute($updParams);
    }

    public static function getAllNewTweetsForUser($userRow) {
        $params['count'] = 200;
        $params['user_id'] = $userRow['usertwitterid'];
        $params['include_rts'] = "false";
        $params['trim_user'] = "true";
        $params['tweet_mode'] = "extended";
        $resultCount = 1;
        $queryCount = 0;
        $reachedEnd = false;
        $userAuth['twitter_id'] = $userRow['usertwitterid'];
        $userAuth['access_token'] = $userRow['accesstoken'];
        $userAuth['access_token_secret'] = $userRow['accesstokensecret'];
        if ($userRow['oldtweetlimitretrieved'] == "Y") {
            if ($userRow['sinceid'] != null) {
                $params['since_id'] = $userRow['sinceid'];
            }
        } else {
            if ($userRow['maxid'] != null) {
                $params['max_id'] = $userRow['maxid'];
            }
        }
        $reachedEnd = false;
        while ($resultCount != 0 && !$reachedEnd) {
            $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret,
                    $userAuth['access_token'], $userAuth['access_token_secret']);
            $results = Core::queryTwitterUserAuth($connection, "statuses/user_timeline", "GET", $params, $userAuth);
            $queryCount++;
            if ($connection->getLastHttpCode() == 200) {
                $resultCount = count($results);
                if ($resultCount == 0) {
                    $reachedEnd = true;
                } else {
                    $returnArray = Core::insertTweetsAndMetrics($results, $userRow['usertwitterid'], $userRow['imagesenabled'],
                                    $userRow['gifsenabled'], $userRow['videosenabled']);
                    if (!$returnArray) {
                        Core::$logger->error("Empty or nonexistent return array - breaking loop.");
                        break;
                    }
                    if (isset($returnArray['novalidresults']) && $returnArray['novalidresults']) {
                        break;
                    }
                    if ($userRow['oldtweetlimitretrieved'] == "N") {
                        $params['max_id'] = $returnArray['lowestid'] - 1;
                    } else {
                        $params['since_id'] = $returnArray['highestid'];
                    }
                }
            } else {
                break;
            }
            if ($queryCount > 35) {
                Core::$logger->critical("35 user_timeline queries!");
                break;
            }
        }
        $query = "UPDATE users SET ";
        $updParams = [];
        if ($reachedEnd) {
            $query .= "oldtweetlimitretrieved=?, maxid=? ";
            array_push($updParams, "Y", $params['max_id']);
        } else if (isset($params['max_id'])) {
            $query .= "maxid=? ";
            $updParams[] = $params['max_id'];
        } else {
            $query .= "sinceid=? ";
            $updParams[] = $params['since_id'];
        }
        $query .= "WHERE twitterid=? AND accesstoken=? AND accesstokensecret=?";
        array_push($updParams, $userAuth['twitter_id'],
                $userAuth['access_token'], $userAuth['access_token_secret']);

        $stmt = CoreDB::$databaseConnection->prepare($query);
        return $stmt->execute($updParams);
    }

    public static function getNonArtistAutomationSettings($userTwitterID) {
        $selectQuery = "SELECT * FROM userartistautomationsettings WHERE usertwitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID]);
        if (!$success) {
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
            $row['hourflags'] = Core::shiftString($row['hourflags'], $timeDiffHours);
            return $row;
        }
    }

    public static function getAutomationSettings($userTwitterID) {
        $selectQuery = "SELECT * FROM userautomationsettings WHERE usertwitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID]);
        if (!$success) {
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
            $row['hourflags'] = Core::shiftString($row['hourflags'], $timeDiffHours);
            $row['minuteflags'] = Core::shiftString($row['minuteflags'], $timeDiffMinutes / 15);
            return $row;
        }
    }

    public static function shiftString($string, $shiftCount) {
        if ($shiftCount == 0) {
            return $string;
        }
        if ($shiftCount > 24 || $shiftCount < -24) {
            return $string;
        }
        if ($shiftCount < 0) {
            while ($shiftCount < 0) {
                $string = $string[strlen($string) - 1] . substr($string, 0, strlen($string) - 1);
                $shiftCount++;
            }
        } else {
            while ($shiftCount > 0) {
                $string = substr($string, 1, strlen($string)) . $string[0];
                $shiftCount--;
            }
        }
        return $string;
    }

    public static function commitNonArtistAutomationSettingsInDB($aS) {
        // Get old automation settings: check if they existed, or were disabled. If so, schedule new automated retweets immediately
        $selectQuery = "SELECT * FROM userartistautomationsettings WHERE usertwitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $result = $selectStmt->execute([$aS['usertwitterid']]);
        if (!$result) {
            Core::$logger->error("Failed to retrieve previous automation settings");
            return StatusCode::ARTRETWEETER_DATABASE_ERROR;
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
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $result = $selectStmt->execute([$aS['usertwitterid']]);
        if (!$result) {
            Core::$logger->error("Failed to retrieve current scheduled retweet count");
            return StatusCode::ARTRETWEETER_DATABASE_ERROR;
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
        $hourFlags = Core::shiftString($aS['hourflags'], -$timeDiffHours);
        if (isset($aS['oldtweetcutoffdate'])) {
            $oldTweetCutoffDate = date("Y-m-d H:i:s", strtotime($aS['oldtweetcutoffdate']) + $timeDiffSeconds);
        } else {
            $oldTweetCutoffDate = null;
        }
        /**
         * TEMPORARY: Remove when Java client is retired
         */
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
                . "timezoneminuteoffset,includetextcondition,excludetextcondition,imagesenabled,gifsenabled,videosenabled) "
                . "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                . "ON DUPLICATE KEY UPDATE automationenabled=?, dayflags=?, hourflags=?, includetext=?, excludetext=?, "
                . "oldtweetcutoffdate=?, oldtweetcutoffdateenabled=?, includetextenabled=?, excludetextenabled=?, timezonehouroffset=?, "
                . "timezoneminuteoffset=?, includetextcondition=?, excludetextcondition=?, imagesenabled=?, gifsenabled=?, "
                . "videosenabled=?";
        $stmt = CoreDB::$databaseConnection->prepare($query);
        try {
            $success = $stmt->execute([$aS['usertwitterid'], $aS['automationenabled'],
                $aS['dayflags'], $hourFlags, $aS['includetext'], $aS['excludetext'],
                $oldTweetCutoffDate, $aS['oldtweetcutoffdateenabled'],
                $aS['includetextenabled'], $aS['excludetextenabled'], $userTimeZoneHour, $userTimeZoneMinute, $aS['includetextcondition'],
                $aS['excludetextcondition'], $aS['imagesenabled'], $aS['gifsenabled'], $aS['videosenabled'],
                $aS['automationenabled'], $aS['dayflags'], $hourFlags, $aS['includetext'], $aS['excludetext'],
                $oldTweetCutoffDate, $aS['oldtweetcutoffdateenabled'], $aS['includetextenabled'], $aS['excludetextenabled'], $userTimeZoneHour,
                $userTimeZoneMinute, $aS['includetextcondition'], $aS['excludetextcondition'],
                $aS['imagesenabled'], $aS['gifsenabled'], $aS['videosenabled']]);
            if (!$success) {
                Core::$logger->error("Failed to insert artist automation settings");
                return StatusCode::ARTRETWEETER_DATABASE_ERROR;
            } else if ($aS['automationenabled'] === "N") {
                $deleteQuery = "DELETE FROM scheduledretweets WHERE retweetingusertwitterid=? AND automated=?";
                $deleteStmt = CoreDB::$databaseConnection->prepare($deleteQuery);
                $success = $deleteStmt->execute([$aS['usertwitterid'], "Y"]);
            } /* else if ($scheduleNewRetweetsNow) {
              $selectStmt = CoreDB::$databaseConnection->prepare("SELECT * FROM users WHERE twitterid=?");
              $result = $selectStmt->execute([$aS['usertwitterid']]);
              $userRow = $selectStmt->fetch();
              $userAuth['twitter_id'] = $aS['usertwitterid'];
              $userAuth['access_token'] = $userRow['accesstoken'];
              $userAuth['access_token_secret'] = $userRow['accesstokensecret'];
              Core::scheduleUserArtistRetweets($userRow);
              } */
        } catch (PDOException $e) {
            Core::$logger->error("Failed to insert automation settings: " . print_r($e, true));
            return StatusCode::ARTRETWEETER_DATABASE_ERROR;
        }
        return StatusCode::ARTRETWEETER_QUERY_OK;
    }

    public static function commitAutomationSettingsInDB($aS) {
        // Get old automation settings: check if they existed, or were disabled. If so, schedule new automated retweets immediately
        $selectQuery = "SELECT * FROM userautomationsettings WHERE usertwitterid=?";
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $result = $selectStmt->execute([$aS['usertwitterid']]);
        if (!$result) {
            Core::$logger->error("Failed to retrieve previous automation settings");
            return StatusCode::ARTRETWEETER_DATABASE_ERROR;
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
        $selectStmt = CoreDB::$databaseConnection->prepare($selectQuery);
        $result = $selectStmt->execute([$aS['usertwitterid']]);
        if (!$result) {
            Core::$logger->error("Failed to retrieve current scheduled retweet count");
            return StatusCode::ARTRETWEETER_DATABASE_ERROR;
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
        $hourFlags = Core::shiftString($aS['hourflags'], -$timeDiffHours);
        $minuteFlags = Core::shiftString($aS['minuteflags'], -$timeDiffMinutes / 15);
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
        /**
         * TEMPORARY: Remove when Java client is retired
         */
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
                . "timezoneminuteoffset,includetextcondition,excludetextcondition,metricsmeasurementtype,imagesenabled,gifsenabled,videosenabled) "
                . "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                . "ON DUPLICATE KEY UPDATE automationenabled=?, dayflags=?, hourflags=?, minuteflags=?, includetext=?, excludetext=?, retweetpercent=?,"
                . "oldtweetcutoffdate=?, oldtweetcutoffdateenabled=?, includetextenabled=?, excludetextenabled=?, timezonehouroffset=?, "
                . "timezoneminuteoffset=?, includetextcondition=?, excludetextcondition=?, metricsmeasurementtype=?, imagesenabled=?, gifsenabled=?, "
                . "videosenabled=?";
        $stmt = CoreDB::$databaseConnection->prepare($query);
        try {
            $success = $stmt->execute([$aS['usertwitterid'], $aS['automationenabled'],
                $aS['dayflags'], $hourFlags, $minuteFlags, $aS['includetext'], $aS['excludetext'],
                $retweetPercent, $oldTweetCutoffDate, $aS['oldtweetcutoffdateenabled'],
                $aS['includetextenabled'], $aS['excludetextenabled'], $userTimeZoneHour, $userTimeZoneMinute, $aS['includetextcondition'],
                $aS['excludetextcondition'], $aS['metricsmeasurementtype'], $aS['imagesenabled'], $aS['gifsenabled'], $aS['videosenabled'],
                $aS['automationenabled'], $aS['dayflags'], $hourFlags, $minuteFlags, $aS['includetext'], $aS['excludetext'], $retweetPercent,
                $oldTweetCutoffDate, $aS['oldtweetcutoffdateenabled'], $aS['includetextenabled'], $aS['excludetextenabled'], $userTimeZoneHour,
                $userTimeZoneMinute, $aS['includetextcondition'], $aS['excludetextcondition'], $aS['metricsmeasurementtype'],
                $aS['imagesenabled'], $aS['gifsenabled'], $aS['videosenabled']]);
            Core::$logger->debug("Automation enabled:");
            Core::$logger->debug($aS['automationenabled']);
            Core::$logger->debug("Automation was disabled: $automationWasDisabled");
            if (!$success) {
                Core::$logger->error("Failed to insert automation settings");
                return StatusCode::ARTRETWEETER_DATABASE_ERROR;
            } else if ($aS['automationenabled'] === "N") {
                $deleteQuery = "DELETE FROM scheduledretweets WHERE retweetingusertwitterid=? AND automated=?";
                $deleteStmt = CoreDB::$databaseConnection->prepare($deleteQuery);
                $success = $deleteStmt->execute([$aS['usertwitterid'], "Y"]);
            } else if ($scheduleNewRetweetsNow) {
                $selectStmt = CoreDB::$databaseConnection->prepare("SELECT * FROM users WHERE twitterid=?");
                $result = $selectStmt->execute([$aS['usertwitterid']]);
                $userRow = $selectStmt->fetch();
                $userAuth['twitter_id'] = $aS['usertwitterid'];
                $userAuth['access_token'] = $userRow['accesstoken'];
                $userAuth['access_token_secret'] = $userRow['accesstokensecret'];
                Core::queueAutomatedRetweets($userAuth);
            }
        } catch (PDOException $e) {
            Core::$logger->error("Failed to insert automation settings: " . print_r($e, true));
            return StatusCode::ARTRETWEETER_DATABASE_ERROR;
        }
        return StatusCode::ARTRETWEETER_QUERY_OK;
    }

    public static function getUserArtistEligibleTweets($userTwitterID, $artistRow, $nonArtistRTThreshold) {
        $selectMeanStatsQuery = "SELECT AVG(retweets) AS avgrts, UNIX_TIMESTAMP(MAX(createdat)) AS maxcr, UNIX_TIMESTAMP(MIN(createdat)) AS mincr "
                . "FROM tweets INNER JOIN tweetmetrics ON tweets.id=tweetmetrics.tweetstableid WHERE usertwitterid=?";
        $selectMeanStatsParams[] = $artistRow['twitterid'];

        $oldTweetsCutoffDate = date("Y-m-d H:i:s", strtotime("-1 year"));
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
        $selectMeanStatsStmt = CoreDB::$databaseConnection->prepare($selectMeanStatsQuery);
        $success = $selectMeanStatsStmt->execute($selectMeanStatsParams);
        if (!$success) {
            Core::$logger->error("Failed to get mean statistics for user, cannot continue with automated retweet scheduling.");
            return;
        }
        $statsRow = $selectMeanStatsStmt->fetch();
        $meanRTs = $statsRow['avgrts'];

        $retweetThreshold = max(($meanRTs * $nonArtistRTThreshold), $artistRow['adaptivertthreshold']);

        $oneYearAgo = date("Y-m-d H:i:s", strtotime("-1 year"));
        $oneMonthAgo = date("Y-m-d H:i:s", strtotime("-1 month"));
        $selectTweetsQuery = str_replace("AVG(retweets) AS avgrts, UNIX_TIMESTAMP(MAX(createdat)) AS maxcr, UNIX_TIMESTAMP(MIN(createdat)) AS mincr",
                "*", $selectMeanStatsQuery);
        $selectTweetsQuery .= " AND retweets >=? AND tweetid NOT IN (SELECT tweetid FROM scheduledretweets WHERE retweetingusertwitterid=?) "
                . "AND tweetid NOT IN (SELECT tweetid FROM retweetrecords WHERE usertwitterid=? AND scheduledretweettime >= ?) "
                . "AND tweetid NOT IN (SELECT tweetid FROM retweetrecords WHERE usertwitterid=? AND scheduledretweettime >= ? "
                . "GROUP BY tweetid HAVING COUNT(tweetid) > ?) "
                . "ORDER BY RAND()";
        $selectMeanStatsParams[] = $retweetThreshold;
        $selectMeanStatsParams[] = $userTwitterID;
        $selectMeanStatsParams[] = $userTwitterID;
        $selectMeanStatsParams[] = $oneMonthAgo;
        $selectMeanStatsParams[] = $userTwitterID;
        $selectMeanStatsParams[] = $oneYearAgo;
        $selectMeanStatsParams[] = 2;
        $selectTweetsStmt = CoreDB::$databaseConnection->prepare($selectTweetsQuery);
        $success = $selectTweetsStmt->execute($selectMeanStatsParams);
        if (!$success) {
            Core::$logger->error("Failed to get tweets for user, cannot continue with automated retweet scheduling.");
            return;
        }
        $tweetRows = $selectTweetsStmt->fetchAll();
        return $tweetRows;
    }

    public static function queueAutomatedRetweets($userAuth) {
        $selectSettingsQuery = "SELECT * FROM userautomationsettings WHERE usertwitterid=?";
        $updateStmt = CoreDB::$databaseConnection->prepare($selectSettingsQuery);
        $success = $updateStmt->execute([$userAuth['twitter_id']]);
        if (!$success) {
            Core::$logger->error("Failed to get user ID to queue automated retweets for, cannot continue.");
            return;
        }
        $settingsRow = $updateStmt->fetch();
        if (!$settingsRow) {
            Core::$logger->error("No automation settings are available for this user, cannot continue.");
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
            Core::$logger->error("User has invalid automation settings, cannot continue.");
            return;
        }
        $today = date("N") - 1;
        $twoDaysFromNow = $today + 2;
        if ($twoDaysFromNow > 7) {
            $twoDaysFromNow -= 7;
        }
        $dayFlag = substr($dayFlags, $twoDaysFromNow, 1);
        if ($dayFlag !== "Y") {
            Core::$logger->debug("Two days from now: $twoDaysFromNow. Today: $today. Dayflags: $dayFlags");
            return;
        }
        $limitPerDay = min($hourCount, 10);
        $totalCountPerDay = $limitPerDay * ($dayCount / 7);

        $selectMeanStatsQuery = "SELECT AVG(retweets) AS avgrts, UNIX_TIMESTAMP(MAX(createdat)) AS maxcr, UNIX_TIMESTAMP(MIN(createdat)) AS mincr "
                . "FROM tweets INNER JOIN tweetmetrics ON tweets.id=tweetmetrics.tweetstableid WHERE usertwitterid=?";
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
            $blockedHandlesStmt = CoreDB::$databaseConnection->prepare("SELECT * FROM userblockedhandles WHERE usertwitterid=?");
            $success = $blockedHandlesStmt->execute([$userAuth['twitter_id']]);
            if (!$success) {
                Core::$logger->error("Failed to get user blocked handles, cannot continue.");
                return;
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
            Core::$logger->debug("Regexp: $regexp");
            $selectMeanStatsParams[] = $regexp;
        }

        $selectMeanStatsStmt = CoreDB::$databaseConnection->prepare($selectMeanStatsQuery);
        $success = $selectMeanStatsStmt->execute($selectMeanStatsParams);
        if (!$success) {
            Core::$logger->error("Failed to get mean statistics for user, cannot continue with automated retweet scheduling.");
            return;
        }
        $statsRow = $selectMeanStatsStmt->fetch();
        $meanRTs = $statsRow['avgrts'];
        if ($settingsRow['metricsmeasurementtype'] == "Mean Average" && $settingsRow['retweetpercent'] != null) {
            $retweetPercent = $settingsRow['retweetpercent'] / 100;
            $retweetThreshold = $meanRTs * $retweetPercent;
        } else if ($settingsRow['metricsmeasurementtype'] == "Adaptive") {
            $retweetThreshold = max(($meanRTs * 0.2), $settingsRow['adaptivertthreshold']);
        } else {
            $userTwitterID = $userAuth['twitter_id'];
            Core::$logger->error("Incorrect automation settings for user ID: $userTwitterID. Settings row:");
            Core::$logger->error(print_r($settingsRow, true));
            return;
        }

        $minCreatedAt = $statsRow['mincr'];
        $maxCreatedAt = $statsRow['maxcr'];
        $seconds = $maxCreatedAt - $minCreatedAt;
        $daysBetweenTweets = max(ceil($seconds / (60 * 60 * 24)), 1);
        $oneYearAgo = date("Y-m-d H:i:s", strtotime("-1 year"));
        $oneMonthAgo = date("Y-m-d H:i:s", strtotime("-1 month"));
        $selectTweetsQuery = str_replace("AVG(retweets) AS avgrts, UNIX_TIMESTAMP(MAX(createdat)) AS maxcr, UNIX_TIMESTAMP(MIN(createdat)) AS mincr",
                "*", $selectMeanStatsQuery);
        $selectTweetsQuery .= " AND retweets >=? AND tweetid NOT IN (SELECT tweetid FROM scheduledretweets WHERE retweetingusertwitterid=?) "
                . "AND tweetid NOT IN (SELECT tweetid FROM retweetrecords WHERE usertwitterid=? AND scheduledretweettime >= ?) "
                . "AND tweetid NOT IN (SELECT tweetid FROM retweetrecords WHERE usertwitterid=? AND scheduledretweettime >= ? "
                . "GROUP BY tweetid HAVING COUNT(tweetid) > ?) "
                . "ORDER BY RAND()";
        $selectMeanStatsParams[] = $retweetThreshold;
        $selectMeanStatsParams[] = $userAuth['twitter_id'];
        $selectMeanStatsParams[] = $userAuth['twitter_id'];
        $selectMeanStatsParams[] = $oneMonthAgo;
        $selectMeanStatsParams[] = $userAuth['twitter_id'];
        $selectMeanStatsParams[] = $oneYearAgo;
        $selectMeanStatsParams[] = 2;
        $selectTweetsStmt = CoreDB::$databaseConnection->prepare($selectTweetsQuery);
        //error_log("Select tweets query: $selectTweetsQuery");
        $success = $selectTweetsStmt->execute($selectMeanStatsParams);
        if (!$success) {
            Core::$logger->error("Failed to get tweets for user, cannot continue with automated retweet scheduling.");
            return;
        }
        $tweetRows = $selectTweetsStmt->fetchAll();
        $tweetCount = count($tweetRows);

        $tweetsPerDay = ceil($tweetCount / $daysBetweenTweets);
        $minuteValues = Core::getMinuteValues($minuteFlags);
        $numScheduled = 0;
        if ($tweetCount == 0) {
            return;
        }
        $scheduleType = "Random";
        if ($scheduleType == "Random") {
            // Schedule randomly, once per hour
            $hourIndices = Core::getHourFlagIndices($hourFlags);
            //error_log("Count of hour indices: $countOfHours");
            while ($numScheduled < 10 && $numScheduled < $tweetCount && $numScheduled < $tweetsPerDay && count($hourIndices) > 0) {
                $nextHour = Core::getRandomHourToAutomate($hourIndices);
                //error_log("Next hour: $nextHour");
                unset($hourIndices[array_search($nextHour, $hourIndices)]);
                $hourIndices = array_values($hourIndices);
                //error_log(print_r($hourIndices, true));
                $minuteValue = Core::getNextMinute($minuteValues);
                $retweetTime = new \DateTime();
                $retweetTime->add(new \DateInterval('P2D'));
                //error_log("Next hour: $nextHour    Next minute: $minuteValue");
                $retweetTime->setTime($nextHour, $minuteValue);
                $retweetTimeStamp = $retweetTime->getTimestamp();
                Core::queueRetweetInDB($userAuth['twitter_id'], $userAuth['twitter_id'], $tweetRows[$numScheduled]['tweetid'], $retweetTimeStamp);
                $numScheduled++;
            }
        } else {
            $start = strpos($hourFlags, "Y");
            $end = strrpos($hourFlags, "Y");
            $maxDist = $end - $start;
            $nextHour = $start;
            $latestUsed = null;
            $interval = $maxDist / $totalCountPerDay;
            //error_log("Time interval: $interval");
            while ($numScheduled < $tweetsPerDay && $numScheduled < $tweetCount && $numScheduled < 10) {
                $minuteValue = Core::getNextMinute($minuteValues);
                $retweetTime = new \DateTime();
                $retweetTime->add(new \DateInterval('P2D'));
                $retweetTime->setTime($nextHour, $minuteValue);
                $retweetTimeStamp = $retweetTime->getTimestamp();
                Core::queueRetweetInDB($userAuth['twitter_id'], $userAuth['twitter_id'], $tweetRows[$numScheduled]['tweetid'], $retweetTimeStamp);
                $numScheduled++;
                $latestUsed = $nextHour;
                $nextHour = Core::getNextHourToAutomate($hourFlags, $latestUsed, $interval);
            }
        }
        $now = date("Y-m-d H:i:s");
        $updateQuery = "UPDATE userautomationsettings SET lastscheduleserverdate=? WHERE usertwitterid=?";
        $updateStmt = CoreDB::$databaseConnection->prepare($updateQuery);
        $success = $updateStmt->execute([$now, $userAuth['twitter_id']]);
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

    public static function beginInitialAutomationOnServer($userAuth) {
        
    }

}

Core::$logger = LogManager::getLogger("Core");
