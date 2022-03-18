<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\TwitterResponseStatus;
use Antsstyle\ArtRetweeter\Core\CoreDB;
use Antsstyle\ArtRetweeter\Core\MiscTools;
use Antsstyle\ArtRetweeter\Core\RetweetScheduler;
use Antsstyle\ArtRetweeter\Core\OAuth;
use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Abraham\TwitterOAuth\TwitterOAuth;

class Core {

    private static $logger;
    
    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function checkResponseHeadersForErrors($connection, $userTwitterID = null) {
        $twitterResponseStatus = TwitterResponseStatus::initialise()
                ->setHttpCode($connection->getLastHttpCode())
                ->setRequestBody($connection->getLastBody())
                ->setHeaders($connection->getLastXHeaders())
                ->setTwitterAPIPath($connection->getLastApiPath())
                ->setUserTwitterID($userTwitterID);

        $headers = $connection->getLastXHeaders();
        if (isset($headers['x_rate_limit_remaining'])) {
            $twitterResponseStatus->setRateLimitRemaining($headers['x_rate_limit_remaining']);
            $twitterResponseStatus->setRateLimitResetTime($headers['x_rate_limit_reset']);
        }
        $httpCode = $connection->getLastHttpCode();
        if ($httpCode != 200) {
            $requestBody = $connection->getLastBody();
            if (isset($requestBody->detail) && !is_null($requestBody->detail)) {
                $message = $requestBody->detail;
            } else {
                $message = $requestBody->message;
            }
            $twitterResponseStatus->setMessage($message);
            $twitterResponseStatus->setTwitterCode($requestBody->status);
        } else {
            $twitterResponseStatus->setTwitterCode(TwitterResponseStatus::ARTRETWEETER_QUERY_OK);
        }
        return $twitterResponseStatus;
    }

    public static function revalidateTweetsExist($tweetIDsToValidate, $userRow) {
        $endpoint = "tweets";
        $params['ids'] = $tweetIDsToValidate;
        $response = Core::queryTwitterUserAuth($endpoint, $endpoint, "GET", $params, APIKeys::bearer_token);
        $queryResult = $response[0];
        $twitterResponseStatus = $response[1];
        error_log("TWITTER RESP: " . print_r($twitterResponseStatus, true));
        error_log("QUERY RESULT: " . print_r($queryResult, true));
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
        $insertQuery = "INSERT INTO failedretweets (retweetingusertwitterid,tweetid,retweettime,failreason) "
                . "SELECT retweetingusertwitterid, tweetid, retweettime, "
                . "? FROM scheduledretweets WHERE retweettime <= ?";
        CoreDB::getConnection()->prepare($insertQuery)
                ->execute(["Missed schedule", $time1HourAgo]);
        CoreDB::getConnection()->prepare("DELETE FROM scheduledretweets WHERE retweettime <= ?")->execute([$time1HourAgo]);
    }

    public static function deleteRetweet($userRow, $tweetID) {
        $query = "users/" . $userRow['twitterid'] . "/retweets/" . $tweetID;
        Core::queryTwitterUserAuth($query, "users/:id/retweets/:source_tweet_id", "DELETE", [], $userRow);
    }

    public static function postScheduledRetweetsForever() {
        while (true) {
            Core::postScheduledRetweets();
            sleep(15);
        }
    }

    public static function postScheduledRetweets() {
        $time5MinsAgo = date('Y-m-d H:i:s', strtotime('-10 seconds', time()));
        $time5MinsFromNow = date('Y-m-d H:i:s', strtotime('+2 minutes', time()));
        $selectQuery = "SELECT scheduledretweets.id AS schid,tweetid,retweettime,accesstoken,accesstokensecret,"
                . "retweetingusertwitterid,tweetauthorid,accesstoken2,expirydate,refreshtoken,users.twitterid,oauthtype"
                . " FROM scheduledretweets INNER JOIN users ON "
                . "scheduledretweets.retweetingusertwitterid = users.twitterid WHERE retweettime >= ? "
                . "AND retweettime <= ?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        $success = $selectStmt->execute([$time5MinsAgo, $time5MinsFromNow]);
        if (!$success) {
            self::$logger->error("Failed to get scheduled retweet information!");
            return;
        }
        while ($row = $selectStmt->fetch()) {
            $databaseID = $row['schid'];
            $tweetID = $row['tweetid'];
            $retweetTime = $row['retweettime'];
            $tweetAuthorID = $row['tweetauthorid'];
            if ($row['retweetingusertwitterid'] === $tweetAuthorID) {
                $selfRetweet = true;
            } else {
                $selfRetweet = false;
            }
            $retweetRecordResults = RetweetScheduler::checkUserCanQueueNewRetweet($row['retweetingusertwitterid'], $retweetTime);
            if (!$retweetRecordResults) {
                CoreDB::getConnection()->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$databaseID]);
                continue;
            }
            $canRetweetThisTweetResults = RetweetScheduler::checkUserCanRetweetOldTweet($row['retweetingusertwitterid'], $retweetTime, $tweetID);
            if (!$canRetweetThisTweetResults) {
                CoreDB::getConnection()->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$databaseID]);
                continue;
            }
            Core::deleteRetweet($row, $tweetID);
            $params['tweet_id'] = $tweetID;

            $retweetEndpoint = "users/" . $row['retweetingusertwitterid'] . "/retweets";
            $response = Core::queryTwitterUserAuth($retweetEndpoint, "users/:id/retweets", "POST", $params, $row, true);
            $twitterResponseStatus = $response[1];
            $insertFailedRetweetQuery = "INSERT INTO failedretweets (retweetingusertwitterid,tweetid,retweettime,failreason,requestbodyjson) "
                    . "SELECT retweetingusertwitterid, tweetid, retweettime, "
                    . "?, ? FROM scheduledretweets WHERE id=?";

            if ($twitterResponseStatus->getHttpCode() != TwitterResponseStatus::HTTP_QUERY_OK) {
                $errorMessage = $twitterResponseStatus->getMessage();
                self::$logger->error("Error message for failed retweet is null! Tweet ID: $tweetID");
                self::$logger->error("Twitter response status: " . print_r($twitterResponseStatus, true));
                self::$logger->error("Data row: " . print_r($row, true));
                if (is_null($errorMessage)) {
                    $errorMessage = "Unknown error";
                }
                CoreDB::getConnection()->prepare($insertFailedRetweetQuery)
                        ->execute([$errorMessage, $databaseID, json_encode($twitterResponseStatus->getRequestBody())]);
            } else {
                Core::updateRetweetRecordsInDB($row['retweetingusertwitterid'], $tweetID, $retweetTime,
                        $tweetAuthorID, $selfRetweet);
            }

            CoreDB::getConnection()->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$databaseID]);
        }
    }

    public static function updateAccessToken($response) {
        $stmt = CoreDB::getConnection()->prepare("INSERT INTO users (twitterid,accesstoken,accesstokensecret) "
                . "VALUES (?,?,?) ON DUPLICATE KEY UPDATE twitterid=?, accesstoken=?, accesstokensecret=?");
        return $stmt->execute([$response['user_id'], $response['oauth_token'], $response['oauth_token_secret'],
                    $response['user_id'], $response['oauth_token'], $response['oauth_token_secret']]);
    }

    public static function updateRetweetRecordsInDB($userTwitterID, $tweetID, $scheduledTime, $tweetAuthorID, $selfRetweet) {
        $stmt = CoreDB::getConnection()->prepare("INSERT INTO retweetrecords (usertwitterid,tweetid,retweettime,scheduledretweettime) 
	VALUES (?,?,?,?)");
        $currentTime = date("Y-m-d H:i:s", time());
        $stmt->execute([$userTwitterID, $tweetID, $currentTime, $scheduledTime]);
        if (!$selfRetweet) {
            $stmt = CoreDB::getConnection()->prepare("UPDATE userartistretweetsettings SET totalretweeted=totalretweeted+1 WHERE usertwitterid=? "
                    . "AND artisttwitterid=?");
            $stmt->execute([$userTwitterID, $tweetAuthorID]);
        }
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
        $stmt = CoreDB::getConnection()->prepare("INSERT INTO ratelimitrecords (usertwitterid,endpoint,maxlimit,remaininglimit,resettime,timetoresetseconds) 
	VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE usertwitterid=?, endpoint=?, maxlimit=?, remaininglimit=?, resettime=?, timetoresetseconds=?");
        $success = $stmt->execute([$userTwitterID, $endpoint, $maxLimit, $remainingLimit, $resetTime, $timeToResetSeconds,
            $userTwitterID, $endpoint, $maxLimit, $remainingLimit, $resetTime, $timeToResetSeconds]);
        return $success;
    }

    public static function checkUserRateLimitInDB($userTwitterID, $endpoint) {
        $stmt = CoreDB::getConnection()->prepare("SELECT * FROM ratelimitrecords WHERE usertwitterid=? AND endpoint=?");
        $success = $stmt->execute([$userTwitterID, $endpoint]);
        if (!$success) {
            self::$logger->error("Failed to get user rate limit records.");
            return true;
        }
        $result = $stmt->fetch();

        if ($result && $result['remaininglimit'] < 5) {
            return false;
        } else {
            return true;
        }
    }

    public static function queryTwitterUserAuth($endpoint, $endpointTemplate, $type, $params, $userRow, $jsonBody = false) {
        if ($userRow !== APIKeys::bearer_token) {
            $userAuthTwitterID = $userRow['twitterid'];
            Core::checkUserRateLimitInDB($userAuthTwitterID, $endpoint);
            $updatedUserRow = CoreDB::getUserInfo($userAuthTwitterID);
            if (is_null($updatedUserRow) || $updatedUserRow === false) {
                self::$logger->critical("Unable to get user row for user twitter ID $userAuthTwitterID - unable to perform query.");
                self::$logger->critical("Endpoint: $endpoint   Type: $type     Params: " . print_r($params, true));
                self::$logger->critical("Initial user row: " . print_r($userRow, true));
                return false;
            } else {
                $userRow = $updatedUserRow;
            }
        }

        if ($userRow === APIKeys::bearer_token) {
            $connection = new TwitterOAuth(APIKeys::twitter_consumer_key, APIKeys::twitter_consumer_secret,
                    null, APIKeys::bearer_token);
        } else if ($userRow['oauthtype'] === "1.0a") {
            $connection = new TwitterOAuth(APIKeys::twitter_consumer_key, APIKeys::twitter_consumer_secret,
                    $userRow['accesstoken'], $userRow['accesstokensecret']);
        } else {
            $tokenExpiry = $userRow['expirydate'];
            $now = date("Y-m-d H:i:s");
            if ($now >= $tokenExpiry) {
                $accesstoken2 = OAuth::getRefreshTokenForUser($userRow);
            } else {
                $accesstoken2 = $userRow['accesstoken2'];
            }
            $connection = new TwitterOAuth(APIKeys::twitter_consumer_key, APIKeys::twitter_consumer_secret,
                    null, $accesstoken2);
        }
        $connection->setApiVersion('2');
        if ($type === "DELETE") {
            $result = $connection->delete($endpoint, $params);
        } else if ($type === 'GET') {
            $result = $connection->get($endpoint, $params, $jsonBody);
        } else {
            $result = $connection->post($endpoint, $params, $jsonBody);
        }
        if ($userRow === APIKeys::bearer_token) {
            $twitterStatusResponse = Core::checkResponseHeadersForErrors($connection);
        } else {
            $twitterStatusResponse = Core::checkResponseHeadersForErrors($connection, $userRow['twitterid']);
            Core::updateUserRateLimitInDB($userRow['twitterid'], $endpointTemplate, $connection->getLastXHeaders());
        }
        return [$result, $twitterStatusResponse];
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
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID, $startTweetID, $metricsType]);
        if (!$success) {
            self::$logger->error("Failed to retrieve tweet metrics from database. User twitter ID: $userTwitterID     Starting tweetID: $startTweetID");
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

    public static function getNonArtistAutomationSettings($userTwitterID) {
        $selectQuery = "SELECT * FROM userartistautomationsettings WHERE usertwitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
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
            $row['hourflags'] = MiscTools::shiftString($row['hourflags'], $timeDiffHours);
            return $row;
        }
    }

    public static function getAutomationSettings($userTwitterID) {
        $selectQuery = "SELECT * FROM userautomationsettings WHERE usertwitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
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
            $row['hourflags'] = MiscTools::shiftString($row['hourflags'], $timeDiffHours);
            $row['minuteflags'] = MiscTools::shiftString($row['minuteflags'], $timeDiffMinutes / 15);
            return $row;
        }
    }

}

Core::initialiseLogger();
