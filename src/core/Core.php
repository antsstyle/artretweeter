<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\TwitterResponseStatus;
use Antsstyle\ArtRetweeter\DB\CoreDB;
use Antsstyle\ArtRetweeter\DB\UserDB;
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
            if ($httpCode == 401 && !is_null($userTwitterID)) {
                self::$logger->error("User twitter ID $userTwitterID returned 401 error - deleting from ArtRetweeter.");
                UserDB::lockUser($userTwitterID, "Unauthorised", date("Y-m-d H:i:s", strtotime("+1 week")));
            }
        } else {
            $twitterResponseStatus->setTwitterCode(TwitterResponseStatus::ARTRETWEETER_QUERY_OK);
        }
        return $twitterResponseStatus;
    }

    public static function removeExpiredRetweets() {
        $time1HourAgo = date('Y-m-d H:i:s', strtotime('-1 hour', time()));
        $insertQuery = "INSERT INTO failedretweets (retweetingusertwitterid,tweetid,retweettime,failreason) "
                . "SELECT retweetingusertwitterid, tweetid, retweettime, "
                . "? FROM scheduledretweets WHERE retweettime <= ?";
        try {
            CoreDB::getConnection()->prepare($insertQuery)->execute(["Missed schedule", $time1HourAgo]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to insert expired scheduled retweets into failed retweets table: " . print_r($e, true));
            return false;
        }
        try {
            CoreDB::getConnection()->prepare("DELETE FROM scheduledretweets WHERE retweettime <= ?")->execute([$time1HourAgo]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to remove expired scheduled retweets from table: " . print_r($e, true));
            return false;
        }
        return true;
    }

    public static function deleteRetweet($userRow, $tweetID) {
        $query = "users/" . $userRow['twitterid'] . "/retweets/" . $tweetID;
        self::queryTwitterUserAuth($query, "users/:id/retweets/:source_tweet_id", "DELETE", [], $userRow);
    }

    public static function postScheduledRetweetsForever() {
        while (true) {
            self::postScheduledRetweets();
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
        try {
            $selectStmt->execute([$time5MinsAgo, $time5MinsFromNow]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get scheduled retweet information: " . print_r($e, true));
            return false;
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
                try {
                    CoreDB::getConnection()->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$databaseID]);
                } catch (\PDOException $e) {
                    self::$logger->error("Failed to delete scheduled retweet with ID $databaseID from queue: " . print_r($e, true));
                }
                continue;
            }
            $canRetweetThisTweetResults = RetweetScheduler::checkUserCanRetweetOldTweet($row['retweetingusertwitterid'], $retweetTime, $tweetID);
            if (!$canRetweetThisTweetResults) {
                try {
                    CoreDB::getConnection()->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$databaseID]);
                } catch (\PDOException $e) {
                    self::$logger->error("Failed to delete scheduled retweet with ID $databaseID from queue: " . print_r($e, true));
                }
                continue;
            }
            self::deleteRetweet($row, $tweetID);
            $params['tweet_id'] = $tweetID;

            $retweetEndpoint = "users/" . $row['retweetingusertwitterid'] . "/retweets";
            $response = self::queryTwitterUserAuth($retweetEndpoint, "users/:id/retweets", "POST", $params, $row, true);
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
                try {
                    CoreDB::getConnection()->prepare($insertFailedRetweetQuery)
                            ->execute([$errorMessage, $databaseID, json_encode($twitterResponseStatus->getRequestBody())]);
                } catch (\PDOException $e) {
                    self::$logger->error("Failed to insert failed retweet entry: " . print_r($e, true));
                    self::$logger->error("Parameters: Error message: $errorMessage Database ID: $databaseID Twitter response status: "
                            . print_r($twitterResponseStatus->getRequestBody, true));
                }
            } else {
                UserDB::updateRetweetRecords($row['retweetingusertwitterid'], $tweetID, $retweetTime,
                        $tweetAuthorID, $selfRetweet);
            }
            try {
                CoreDB::getConnection()->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$databaseID]);
            } catch (\PDOException $e) {
                self::$logger->error("Failed to delete scheduled retweet with ID $databaseID from queue: " . print_r($e, true));
            }
        }
    }

    public static function queryTwitterUserAuth($endpoint, $endpointTemplate, $type, $params, $userRow, $jsonBody = false) {
        if ($userRow !== APIKeys::bearer_token) {
            $userAuthTwitterID = $userRow['twitterid'];
            UserDB::checkUserRateLimit($userAuthTwitterID, $endpoint);
            $updatedUserRow = UserDB::getUserInfo($userAuthTwitterID);
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
            $twitterStatusResponse = self::checkResponseHeadersForErrors($connection);
        } else {
            $twitterStatusResponse = self::checkResponseHeadersForErrors($connection, $userRow['twitterid']);
            UserDB::updateUserRateLimit($userRow['twitterid'], $endpointTemplate, $connection->getLastXHeaders());
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
        try {
            $selectStmt->execute([$userTwitterID, $startTweetID, $metricsType]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to retrieve tweet metrics from database. User twitter ID: $userTwitterID Starting tweetID: $startTweetID. "
                    . "PDO error: " . print_r($e, true));
            return false;
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

}

Core::initialiseLogger();
