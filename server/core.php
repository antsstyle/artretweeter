<?php

namespace ArtRetweeter;

require_once "credentials/apikeys.php";
require_once "coredb.php";
require_once "twitteroauth/vendor/autoload.php";

use Abraham\TwitterOAuth\TwitterOAuth;

function getTweetIDsForUser($userAuthTwitterID) {
    $selectQuery = "SELECT id,tweetid FROM tweets WHERE usertwitterid=? AND deletedflag=? ORDER BY id ASC";
    $selectStmt = $GLOBALS['databaseConnection']->prepare($selectQuery);
    $success = $selectStmt->execute([$userAuthTwitterID, "N"]);
    if (!$success) {
        $returnArray['tweetids'] = false;
        echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, $returnArray);
    } else {
        $returnArray['tweetids'] = $selectStmt->fetchAll();
        echo encodeStatusInformation(StatusCodes::QUERY_OK, $returnArray);
    }
}

function removeExpiredRetweets() {
    $time1HourAgo = date('Y-m-d H:i:s', strtotime('-1 hour', time()));
    $insertQuery = "INSERT INTO failedretweets (retweetingusertwitterid,tweetid,retweettime,errorcode,failreason) "
            . "SELECT retweetingusertwitterid, tweetid, retweettime, ?, "
            . "? FROM scheduledretweets WHERE retweettime <= ?";
    $GLOBALS['databaseConnection']->prepare($insertQuery)
            ->execute([-1, "Missed schedule", $time1HourAgo]);
    $GLOBALS['databaseConnection']->prepare("DELETE FROM scheduledretweets WHERE retweettime <= ?")->execute([$time1HourAgo]);
}

function deleteRetweet($userAuth, $tweetID) {
    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    $connection->setApiVersion('2');
    $connection->setRetries(1, 1);
    $query = "users/" . $userAuth['twitter_id'] . "/retweets/" . $tweetID;
    $connection->delete($query);
}

function postScheduledRetweets() {
    $retweetEndpoint = "statuses/retweet";
    $time5MinsAgo = date('Y-m-d H:i:s', strtotime('-5 minutes', time()));
    $time5MinsFromNow = date('Y-m-d H:i:s', strtotime('+5 minutes', time()));
    $selectQuery = "SELECT scheduledretweets.id,tweetid,retweettime,accesstoken,accesstokensecret,"
            . "retweetingusertwitterid FROM scheduledretweets INNER JOIN users ON "
            . "scheduledretweets.retweetingusertwitterid = users.twitterid WHERE retweettime >= ? "
            . "AND retweettime <= ?";
    $selectStmt = $GLOBALS['databaseConnection']->prepare($selectQuery);
    $success = $selectStmt->execute([$time5MinsAgo, $time5MinsFromNow]);
    if (!$success) {
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
        $retweetRecordResults = checkUserCanQueueNewRetweet($userAuth['twitter_id'], $retweetTime, false);
        if (!$retweetRecordResults) {
            $GLOBALS['databaseConnection']->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$databaseID]);
            continue;
        }
        $canRetweetThisTweetResults = checkUserCanRetweetOldTweet($userAuth['twitter_id'], $retweetTime, $tweetID, false);
        if (!$canRetweetThisTweetResults) {
            $GLOBALS['databaseConnection']->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$databaseID]);
            continue;
        }
        deleteRetweet($userAuth, $tweetID);
        $params['id'] = $tweetID;
        $params['trim_user'] = 1;
        $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'], $accessToken, $accessTokenSecret);
        $connection->setRetries(1, 1);
        $queryResult = queryTwitterUserAuth($connection, $retweetEndpoint, "POST", $params, $userAuth, false, false);
        $insertFailedRetweetQuery = "INSERT INTO failedretweets (retweetingusertwitterid,tweetid,retweettime,errorcode,failreason) "
                . "SELECT retweetingusertwitterid, tweetid, retweettime, ?, "
                . "? FROM scheduledretweets WHERE id=?";
        if (!$queryResult) {
            $GLOBALS['databaseConnection']->prepare($insertFailedRetweetQuery)
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
                $GLOBALS['databaseConnection']->prepare($insertFailedRetweetQuery)
                        ->execute([$errorCode, $errorMessage, $databaseID]);
            } else {
                $originalStatus = $queryResult->retweeted_status;
                $retweetedStatusID = $queryResult->id;
                $originalTweetID = $originalStatus->id;
                updateRetweetRecordsInDB($userAuth['twitter_id'], $originalTweetID, $retweetedStatusID, $retweetTime);
            }
        }
        $GLOBALS['databaseConnection']->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$databaseID]);
    }
}

function getQueueStatusInDB($userAuthTwitterID) {
    $scheduledRetweetsStmt = $GLOBALS['databaseConnection']->prepare("SELECT retweetingusertwitterid,"
            . "tweetid,UNIX_TIMESTAMP(retweettime) AS rttime,automated FROM scheduledretweets WHERE retweetingusertwitterid=?");
    $scheduledTableSuccess = $scheduledRetweetsStmt->execute([$userAuthTwitterID]);
    if (!$scheduledTableSuccess) {
        $returnArray['scheduledretweets'] = false;
    } else {
        $returnArray['scheduledretweets'] = $scheduledRetweetsStmt->fetchAll();
    }
    $failedRetweetsStmt = $GLOBALS['databaseConnection']->prepare("SELECT retweetingusertwitterid,"
            . "tweetid,UNIX_TIMESTAMP(retweettime) AS rttime,errorcode,failreason FROM failedretweets WHERE retweetingusertwitterid=?");
    $failTableSuccess = $failedRetweetsStmt->execute([$userAuthTwitterID]);
    if (!$failTableSuccess) {
        $returnArray['failedretweets'] = false;
    } else {
        $returnArray['failedretweets'] = $failedRetweetsStmt->fetchAll();
        $GLOBALS['databaseConnection']->prepare("DELETE FROM failedretweets WHERE retweetingusertwitterid=?")->execute([$userAuthTwitterID]);
    }
    if ($failTableSuccess && $scheduledTableSuccess) {
        echo encodeStatusInformation(StatusCodes::QUERY_OK, $returnArray);
    } else {
        echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, $returnArray);
    }
}

function getTweetRetweetStatusInDB($userAuthTwitterID) {
    $recordsStmt = $GLOBALS['databaseConnection']->prepare("SELECT tweetid,UNIX_TIMESTAMP(scheduledretweettime) AS rttime FROM retweetrecords "
            . "WHERE usertwitterid=?");
    $recordsSuccess = $recordsStmt->execute([$userAuthTwitterID]);
    if (!$recordsSuccess) {
        $returnArray['retweetrecords'] = false;
        echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, $returnArray);
    } else {
        $returnArray['retweetrecords'] = $recordsStmt->fetchAll();
        echo encodeStatusInformation(StatusCodes::QUERY_OK, $returnArray);
    }
}

function queueRetweetInDB($userAuthTwitterID, $tweetID, $retweetTime, $automated = false) {
    if (!checkUserCanQueueNewRetweet($userAuthTwitterID, $retweetTime, !$automated)) {
        return false;
    }
    if (!checkUserCanRetweetOldTweet($userAuthTwitterID, $retweetTime, $tweetID, !$automated)) {
        return false;
    }
    $timeString = date('Y-m-d H:i:s', $retweetTime);
    $stmt = $GLOBALS['databaseConnection']->prepare("INSERT INTO scheduledretweets (retweetingusertwitterid,tweetid,retweettime,automated) "
            . "VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE retweetingusertwitterid=?, tweetid=?, retweettime=?, automated=?");
    $success = $stmt->execute([$userAuthTwitterID, $tweetID, $timeString, $automated ? "Y" : "N",
        $userAuthTwitterID, $tweetID, $timeString, $automated ? "Y" : "N"]);
    if (!$automated) {
        encodeSuccessInformation($success);
    } else {
        return $success;
    }
}

function unqueueRetweetFromDB($userAuthTwitterID, $tweetID) {
    $success = $GLOBALS['databaseConnection']->prepare("DELETE FROM scheduledretweets WHERE retweetingusertwitterid=? AND tweetid=?")
            ->execute([$userAuthTwitterID, $tweetID]);
    echo encodeSuccessInformation($success);
}

function deleteAccount($userAuthTwitterID) {
    $success = $GLOBALS['databaseConnection']->prepare("DELETE FROM users WHERE twitterid=?")
            ->execute([$userAuthTwitterID]);
    echo encodeSuccessInformation($success);
}

function validateUserAuth($userAuth) {
    $bannedStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM bannedusers WHERE twitterid=?");
    $success = $bannedStmt->execute([$userAuth['twitter_id']]);
    if (!$success) {
        echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, "Database error executing query.");
        exit();
    }
    $bannedResults = $bannedStmt->fetch();
    if ($bannedResults) {
        $banReason = $bannedResults['reason'];
        echo encodeStatusInformation(StatusCodes::USER_BANNED, "This user is banned from ArtRetweeter. Reason: $banReason");
        exit();
    }
    $stmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM users WHERE twitterid=? AND accesstoken=? AND accesstokensecret=?");
    $success = $stmt->execute([$userAuth['twitter_id'], $userAuth['access_token'], $userAuth['access_token_secret']]);
    if (!$success) {
        echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, "Database error executing query.");
        exit();
    }
    $results = $stmt->fetch();
    if (!$results) {
        echo encodeStatusInformation(StatusCodes::INVALID_ACCESS_TOKEN, "Invalid or nonexistent access token.");
        exit();
    }
}

function updateAccessToken($response) {
    $stmt = $GLOBALS['databaseConnection']->prepare("INSERT INTO users (twitterid,accesstoken,accesstokensecret) "
            . "VALUES (?,?,?) ON DUPLICATE KEY UPDATE twitterid=?, accesstoken=?, accesstokensecret=?");
    return $stmt->execute([$response['user_id'], $response['oauth_token'], $response['oauth_token_secret'],
                $response['user_id'], $response['oauth_token'], $response['oauth_token_secret']]);
}

function checkUserCanRetweetOldTweet($userTwitterID, $retweetTime, $tweetID, $echoAndExit = true) {
    $date30DaysAgo = date("Y-m-d H:i:s", strtotime('-30 days', $retweetTime));
    $date1YearAgo = date("Y-m-d H:i:s", strtotime('-1 year', $retweetTime));
    // Limit users to retweeting the same tweet only once per month, and not more than 3 times per year
    $per30DaysLimit = 1;
    $perYearLimit = 3;
    $records30DaysStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime >= ? 
        AND tweetid=? ORDER BY retweettime DESC LIMIT ?");
    $records30DaysStmt->bindValue(1, $userTwitterID);
    $records30DaysStmt->bindValue(2, $date30DaysAgo);
    $records30DaysStmt->bindValue(3, $tweetID);
    $records30DaysStmt->bindValue(4, $per30DaysLimit, \PDO::PARAM_INT);
    $records30DaysSuccess = $records30DaysStmt->execute();
    if (!$records30DaysSuccess) {
        error_log("Failed to get retweet records to check monthly limits.");
        if ($echoAndExit) {
            echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, $records30DaysSuccess->errorInfo());
            exit();
        }
        return false;
    }
    $records30DaysResults = $records30DaysStmt->fetchAll();
    if ($records30DaysResults && (count($records30DaysResults) >= $per30DaysLimit)) {
        if ($echoAndExit) {
            echo encodeStatusInformation(StatusCodes::ARTRETWEETER_RATE_LIMIT_EXCEEDED,
                    "Too many retweets of this tweet in the last 30 days.");
            exit();
        }
        return false;
    }


    $recordsYearStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime >= ? 
        AND tweetid=? ORDER BY retweettime DESC LIMIT ?");
    $recordsYearStmt->bindValue(1, $userTwitterID);
    $recordsYearStmt->bindValue(2, $date1YearAgo);
    $recordsYearStmt->bindValue(3, $tweetID);
    $recordsYearStmt->bindValue(4, $perYearLimit, \PDO::PARAM_INT);
    $recordsYearSuccess = $recordsYearStmt->execute();
    if (!$recordsYearSuccess) {
        error_log("Failed to get retweet records to check yearly limits.");
        if ($echoAndExit) {
            echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, $recordsYearStmt->errorInfo());
            exit();
        }
        return false;
    }
    $recordsYearResults = $recordsYearStmt->fetchAll();
    if ($recordsYearResults && (count($recordsYearResults) >= $perYearLimit)) {
        echo encodeStatusInformation(StatusCodes::ARTRETWEETER_RATE_LIMIT_EXCEEDED,
                "Too many retweets of this tweet in the last 12 months.");
        exit();
    }
    return true;
}

function getNumTweetsAtExactTime($table, $userTwitterID, $retweetTime, $echoAndExit = true) {
    if ($table == "scheduledretweets") {
        $stmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime=?");
    } else if ($table == "retweetrecords") {
        $stmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM scheduledretweets WHERE retweetingusertwitterid=? AND retweettime=?");
    } else if ($echoAndExit) {
        echo encodeStatusInformation("Internal server error - an invalid table was specified for retrieving tweets at an exact time.");
        exit();
    } else {
        return false;
    }
    $stmt->bindValue(1, $userTwitterID);
    $stmt->bindValue(2, $retweetTime);
    $success = $stmt->execute();
    if (!$success) {
        if ($echoAndExit) {
            echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, $stmt->errorInfo());
            exit();
        }
        return $stmt->errorInfo();
    }
    $results = $stmt->fetchAll();
    if (!$results) {
        return 0;
    } else {
        return count($results);
    }
}

function getNumTweetsInTimeInterval($userTwitterID, $retweetTime, $timeInterval, $limit, $echoAndExit = true) {
    $intervalStart = date("Y-m-d H:i:s", strtotime("-" . $timeInterval, $retweetTime));
    $intervalEnd = date("Y-m-d H:i:s", strtotime("+" . $timeInterval, $retweetTime));
    $intervalSizeSeconds = (strtotime('+' . $timeInterval, $retweetTime) - strtotime('-' . $timeInterval, $retweetTime)) / 2;
    $stmt = $GLOBALS['databaseConnection']->prepare("SELECT UNIX_TIMESTAMP(retweettime) AS rttime FROM scheduledretweets WHERE retweetingusertwitterid=? 
            AND retweettime >= ? AND retweettime <= ?
            ORDER BY retweettime ASC LIMIT ?");
    $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
    $stmt->bindValue(2, $intervalStart, \PDO::PARAM_STR);
    $stmt->bindValue(3, $intervalEnd, \PDO::PARAM_STR);
    $stmt->bindValue(4, $limit, \PDO::PARAM_INT);
    $success = $stmt->execute();
    if (!$success) {
        if ($echoAndExit) {
            echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, $stmt->errorInfo());
            exit();
        }
        return $stmt->errorInfo();
    }
    $scheduledTimes = $stmt->fetchAll();
    $stmt = $GLOBALS['databaseConnection']->prepare("SELECT UNIX_TIMESTAMP(scheduledretweettime) AS rttime FROM retweetrecords WHERE usertwitterid=? 
            AND scheduledretweettime >= ? AND scheduledretweettime <= ?
            ORDER BY scheduledretweettime ASC LIMIT ?");
    $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
    $stmt->bindValue(2, $intervalStart, \PDO::PARAM_STR);
    $stmt->bindValue(3, $intervalEnd, \PDO::PARAM_STR);
    $stmt->bindValue(4, $limit, \PDO::PARAM_INT);
    $success = $stmt->execute();
    if (!$success) {
        if ($echoAndExit) {
            echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, $stmt->errorInfo());
            exit();
        }
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
                if ($echoAndExit) {
                    echo encodeStatusInformation(StatusCodes::ARTRETWEETER_RATE_LIMIT_EXCEEDED,
                            "Too many retweets in $timeInterval period.");
                    exit();
                }
                return false;
            }
        }
    }
    return true;
}

function checkUserCanQueueNewRetweet($userTwitterID, $retweetTime, $echoAndExit = true) {
    $RTTime = date("Y-m-d H:i:s", $retweetTime);
    $perDayLimit = 10;
    $perHourLimit = 2;

    $recordsNowResults = getNumTweetsAtExactTime("retweetrecords", $userTwitterID, $RTTime, $echoAndExit);
    if ($recordsNowResults) {
        if ($echoAndExit) {
            echo encodeStatusInformation(StatusCodes::ARTRETWEETER_RATE_LIMIT_EXCEEDED,
                    "You cannot schedule two retweets to be posted at the same time.");
            exit();
        }
        return false;
    }

    $queueNowResults = getNumTweetsAtExactTime("scheduledretweets", $userTwitterID, $RTTime, $echoAndExit);
    if ($queueNowResults) {
        if ($echoAndExit) {
            echo encodeStatusInformation(StatusCodes::ARTRETWEETER_RATE_LIMIT_EXCEEDED,
                    "You cannot schedule two retweets to be posted at the same time.");
            exit();
        }
        return false;
    }

    getNumTweetsInTimeInterval($userTwitterID, $retweetTime, "1 hour", $perHourLimit, $echoAndExit);
    getNumTweetsInTimeInterval($userTwitterID, $retweetTime, "24 hours", $perDayLimit, $echoAndExit);

    return true;
}

function updateRetweetRecordsInDB($userTwitterID, $tweetID, $retweetID, $scheduledTime) {
    $stmt = $GLOBALS['databaseConnection']->prepare("INSERT INTO retweetrecords (usertwitterid,tweetid,retweetid,retweettime,scheduledretweettime) 
	VALUES (?,?,?,?,?)");
    $currentTime = date("Y-m-d H:i:s", time());
    $success = $stmt->execute([$userTwitterID, $tweetID, $retweetID, $currentTime, $scheduledTime]);
    return $success;
}

function encodeDBResponseInformation($dbResult) {
    $results['dbresult'] = $dbResult;
    return json_encode($results);
}

function encodeTwitterResponseInformation($connection, $response) {
    $results['response'] = $response;
    $results['headers'] = $connection->getLastXHeaders();
    $results['httpcode'] = $connection->getLastHttpCode();
    return json_encode($results);
}

function encodeSuccessInformation($success) {
    if ($success) {
        echo encodeStatusInformation(StatusCodes::QUERY_OK);
    } else {
        echo encodeStatusInformation(StatusCodes::DATABASE_ERROR);
    }
}

function encodeStatusInformation($code, $msg = null) {
    $results['statuscode'] = $code;
    if ($msg == null) {
        $results['message'] = StatusCodes::getStatusMessage($code);
    } else {
        $results['message'] = $msg;
    }
    return json_encode($results);
}

function updateUserRateLimitInDB($userTwitterID, $endpoint, $headers) {
    if (!isset($headers['x_rate_limit_remaining']) || !isset($headers['x_rate_limit_limit']) || !isset($headers['x_rate_limit_reset'])) {
        return true;
    }
    $maxLimit = $headers['x_rate_limit_limit'];
    $remainingLimit = $headers['x_rate_limit_remaining'];
    $resetTime = date('Y-m-d H:i:s', $headers['x_rate_limit_reset']);
    $timeToResetSeconds = $headers['x_rate_limit_reset'] - time();
    $stmt = $GLOBALS['databaseConnection']->prepare("INSERT INTO ratelimitrecords (usertwitterid,endpoint,maxlimit,remaininglimit,resettime,timetoresetseconds) 
	VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE usertwitterid=?, endpoint=?, maxlimit=?, remaininglimit=?, resettime=?, timetoresetseconds=?");
    $success = $stmt->execute([$userTwitterID, $endpoint, $maxLimit, $remainingLimit, $resetTime, $timeToResetSeconds,
        $userTwitterID, $endpoint, $maxLimit, $remainingLimit, $resetTime, $timeToResetSeconds]);
    return $success;
}

function checkUserRateLimitInDB($userTwitterID, $endpoint, $echoAndExit = true) {
    $stmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM ratelimitrecords WHERE usertwitterid=? AND endpoint=?");
    $success = $stmt->execute([$userTwitterID, $endpoint]);
    if (!$success) {
        error_log("Failed to get user rate limit records.");
        if ($echoAndExit) {
            echo encodeErrorInformation(StatusCodes::DATABASE_ERROR, $stmt->errorInfo());
            exit();
        }
        return true;
    }
    $result = $stmt->fetch();

    if ($result && $result['remaininglimit'] < 5) {
        if ($echoAndExit) {
            echo encodeStatusInformation(StatusCodes::TWITTER_RATE_LIMIT_EXCEEDED);
            exit();
        }
        return false;
    } else {
        return true;
    }
}

function queryTwitterUserAuth($connection, $endpoint, $httpRequestType, $params, $userAuth, $paramsIsJsonData = false,
        $echoAndExit = true) {
    $userAuthTwitterID = $userAuth['twitter_id'];
    validateUserAuth($userAuth);
    checkUserRateLimitInDB($userAuthTwitterID, $endpoint, $echoAndExit);
    if ($httpRequestType == 'GET') {
        if ($paramsIsJsonData) {
            $result = $connection->get($endpoint, $params, true);
        } else {
            $result = $connection->get($endpoint, $params);
        }
    } else {
        if ($paramsIsJsonData) {
            $result = $connection->post($endpoint, $params, true);
        } else {
            $result = $connection->post($endpoint, $params);
        }
    }
    if (checkIfUserAccessTokenInvalid($result)) {
        error_log("Invalid access token detected for user ID: $userAuthTwitterID, disabling any automation.");
        $query = "UPDATE userautomationsettings SET automationenabled=? WHERE usertwitterid=?";
        $stmt = $GLOBALS['databaseConnection']->prepare($query);
        $stmt->execute(["N", $userAuthTwitterID]);
        $deleteAutomatedRetweetsQuery = "DELETE FROM scheduledretweets WHERE retweetingusertwitterid=? AND automated=?";
        $stmt = $GLOBALS['databaseConnection']->prepare($deleteAutomatedRetweetsQuery);
        $stmt->execute([$userAuthTwitterID, "N"]);
    } else {
        updateUserRateLimitInDB($userAuthTwitterID, $endpoint, $connection->getLastXHeaders());
    }
    if ($echoAndExit) {
        echo encodeTwitterResponseInformation($connection, $result);
        exit();
    } else {
        return $result;
    }
}

function checkIfUserAccessTokenInvalid($result) {
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

function removeAccountFromDB($twitterID) {
    $success['failure_queue_cleared'] = $GLOBALS['databaseConnection']
            ->prepare("DELETE FROM failedretweets WHERE retweetingusertwitterid=?")
            ->execute([$twitterID]);
    $success['schedule_queue_cleared'] = $GLOBALS['databaseConnection']
            ->prepare("DELETE FROM scheduledretweets WHERE retweetingusertwitterid=?")
            ->execute([$twitterID]);
    $success['user_cleared'] = $GLOBALS['databaseConnection']
            ->prepare("DELETE FROM users WHERE twitterid=?")
            ->execute([$twitterID]);
    $success['metrics_cleared'] = $GLOBALS['databaseConnection']
            ->prepare("DELETE FROM tweetmetrics WHERE tweetstableid IN (SELECT id FROM tweets WHERE usertwitterid=?)")
            ->execute([$twitterID]);
    $success['tweets_cleared'] = $GLOBALS['databaseConnection']
            ->prepare("DELETE FROM tweets WHERE usertwitterid=?")
            ->execute([$twitterID]);
    $success['automation_settings_cleared'] = $GLOBALS['databaseConnection']
            ->prepare("DELETE FROM userautomationsettings WHERE usertwitterid=?")
            ->execute([$twitterID]);
    encodeSuccessInformation($success);
}

function getTweetMetricsToRefresh($userTwitterID, $startTweetID, $metricsType) {
    $selectQuery = "SELECT *,(SELECT description FROM retrievalmetrics WHERE retrievalmetrics.id=tweetmetrics.retrievalmetric) "
            . "AS description FROM tweetmetrics WHERE usertwitterid=? AND tweetid > ? "
            . "AND retrievalmetric=(SELECT id FROM retrievalmetrics WHERE description=?) "
            . "ORDER BY tweetid ASC LIMIT 101";
    $selectStmt = $GLOBALS['databaseConnection']->prepare($selectQuery);
    $success = $selectStmt->execute([$userTwitterID, $startTweetID, $metricsType]);
    if (!$success) {
        error_log("Failed to retrieve tweet metrics from database. User twitter ID: $userTwitterID     Starting tweetID: $startTweetID");
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

function insertTweetsAndMetrics($tweets, $userTwitterID) {
    $selectQuery = "SELECT id FROM retrievalmetrics WHERE description=?";
    $selectStmt = $GLOBALS['databaseConnection']->prepare($selectQuery);
    $success = $selectStmt->execute(["Latest Metrics"]);
    if (!$success) {
        error_log("Failed to get metrics type, cannot insert tweets.");
        return;
    }
    $metricID = $selectStmt->fetchColumn();
    if (!$metricID) {
        error_log("No metrics type found, cannot insert tweets.");
        return;
    }

    $selectQuery = "SELECT tweetid FROM tweets WHERE usertwitterid=? AND deletedflag=?";
    $selectStmt = $GLOBALS['databaseConnection']->prepare($selectQuery);
    $success = $selectStmt->execute([$userTwitterID, "Y"]);
    if (!$success) {
        error_log("Failed to get delete-flagged tweets for user, cannot insert tweets.");
        return;
    }

    while ($row = $selectStmt->fetch()) {
        $deletedTweetsArray[$row['tweetid']] = 1;
    }
    $GLOBALS['databaseConnection']->beginTransaction();
    $timeNow = date("Y-m-d H:i:s");
    foreach ($tweets as $tweet) {
        $created_at = date("Y-m-d H:i:s", strtotime($tweet->created_at));
        if (!$tweet->extended_entities) {
            continue;
        }
        if (!$tweet->extended_entities->media) {
            continue;
        }
        if ($tweet->extended_entities->media[0]->type != "photo") {
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
        try {
            $stmt = $GLOBALS['databaseConnection']->prepare("INSERT IGNORE INTO tweets
            (tweetid,usertwitterid,createdat,fulltweettext) 
	VALUES (?,?,?,?)");
            $stmt->execute([$tweet->id, $userTwitterID, $created_at, $tweet->full_text]);
        } catch (PDOException $e) {
            error_log("Failed to insert tweet: $e");
        }
        if ($stmt->rowCount()) {
            try {
                $stmt = $GLOBALS['databaseConnection']->prepare("INSERT INTO tweetmetrics
                (tweetstableid,retrievedtime,retrievalmetric,likes,retweets) VALUES (LAST_INSERT_ID(),?,?,?,?) 
                ON DUPLICATE KEY UPDATE likes=?,retweets=?,retrievedtime=?");
                $stmt->execute([$timeNow, $metricID, $tweet->favorite_count, $tweet->retweet_count, $tweet->favorite_count,
                    $tweet->retweet_count, $timeNow]);
            } catch (PDOException $e) {
                error_log("Failed to insert tweet metrics for new tweet: $e");
            }
        } else {
            $stmt = $GLOBALS['databaseConnection']->prepare("SELECT id FROM tweets WHERE tweetid=?");
            $stmt->execute([$tweet->id]);
            $tweetsTableID = $stmt->fetchColumn();
            try {
                $stmt = $GLOBALS['databaseConnection']->prepare("INSERT INTO tweetmetrics
                (tweetstableid,retrievedtime,retrievalmetric,likes,retweets) VALUES (?,?,?,?,?) 
                ON DUPLICATE KEY UPDATE likes=?,retweets=?,retrievedtime=?");
                $stmt->execute([$tweetsTableID, $timeNow, $metricID, $tweet->favorite_count, $tweet->retweet_count, $tweet->favorite_count,
                    $tweet->retweet_count, $timeNow]);
            } catch (PDOException $e) {
                error_log("Failed to insert tweet metrics for existing tweet: $e");
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
    $GLOBALS['databaseConnection']->commit();
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

function getTweetMetrics() {
    $selectQuery = "SELECT * FROM userautomationsettings WHERE automationenabled=?";
    $selectStmt = $GLOBALS['databaseConnection']->prepare($selectQuery);
    $success = $selectStmt->execute(["Y"]);
    if (!$success) {
        error_log("Failed to get users to retrieve tweets for, cannot continue.");
        return;
    }
    $params['count'] = 200;
    $params['include_rts'] = "false";
    $params['trim_user'] = "true";
    $params['tweet_mode'] = "extended";
    while ($row = $selectStmt->fetch()) {
        $params['user_id'] = $row['twitterid'];
        $userAuth['twitter_id'] = $row['twitterid'];
        $userAuth['access_token'] = $row['accesstoken'];
        $userAuth['access_token_secret'] = $row['accesstokensecret'];
        if ($row['oldtweetlimitretrieved']) {
            $params['since_id'] = $row['sinceid'];
            unset($params['max_id']);
        } else {
            $params['max_id'] = $row['maxid'];
            unset($params['since_id']);
        }
        $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
                $row['accesstoken'], $row['accesstokensecret']);
        $results = queryTwitterUserAuth($connection, "statuses/user_timeline", "GET", $params, $userAuth, false, false);
        if ($connection->getLastHttpCode() == 200) {
            insertTweetsAndMetrics($results, $userAuth['twitter_id']);
        }
    }
}

function scheduleAutomatedRetweets() {
    $query = "SELECT * FROM users INNER JOIN userautomationsettings ON users.twitterid=userautomationsettings.usertwitterid "
            . "WHERE automationenabled=?";
    $stmt = $GLOBALS['databaseConnection']->prepare($query);
    $success = $stmt->execute(["Y"]);
    if (!$success) {
        error_log("Failed to acquire list of users to schedule automated retweets for, cannot continue.");
        return;
    }
    while ($row = $stmt->fetch()) {
        $userAuth['twitter_id'] = $row['usertwitterid'];
        $userAuth['access_token'] = $row['accesstoken'];
        $userAuth['access_token_secret'] = $row['accesstokensecret'];
        queueAutomatedRetweets($userAuth);
    }
}

function getAllNewTweetsForAllUsers() {
    $query = "SELECT * FROM users INNER JOIN userautomationsettings ON users.twitterid=userautomationsettings.usertwitterid "
            . "WHERE automationenabled=?";
    $stmt = $GLOBALS['databaseConnection']->prepare($query);
    $success = $stmt->execute(["Y"]);
    if (!$success) {
        error_log("Failed to acquire list of users to schedule automated retweets for, cannot continue.");
        return;
    }
    while ($row = $stmt->fetch()) {
        getAllNewTweetsForUser($row);
    }
}

function getAllNewTweetsForUser($row) {
    $params['count'] = 200;
    $params['user_id'] = $row['usertwitterid'];
    $params['include_rts'] = "false";
    $params['trim_user'] = "true";
    $params['tweet_mode'] = "extended";
    $resultCount = 1;
    $queryCount = 0;
    $reachedEnd = false;
    $userAuth['twitter_id'] = $row['usertwitterid'];
    $userAuth['access_token'] = $row['accesstoken'];
    $userAuth['access_token_secret'] = $row['accesstokensecret'];
    if ($row['oldtweetlimitretrieved'] == "Y") {
        if ($row['sinceid'] != null) {
            $params['since_id'] = $row['sinceid'];
        }
    } else {
        if ($row['maxid'] != null) {
            $params['max_id'] = $row['maxid'];
        }
    }
    $reachedEnd = false;
    while ($resultCount != 0 && !$reachedEnd) {
        $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
                $userAuth['access_token'], $userAuth['access_token_secret']);
        $results = queryTwitterUserAuth($connection, "statuses/user_timeline", "GET", $params, $userAuth, false, false);
        $queryCount++;
        if ($connection->getLastHttpCode() == 200) {
            $resultCount = count($results);
            if ($resultCount == 0) {
                error_log("Reached end.");
                $reachedEnd = true;
            } else {
                $returnArray = insertTweetsAndMetrics($results, $userAuth['twitter_id']);
                if (!$returnArray) {
                    error_log("Empty or nonexistent return array - breaking loop.");
                    break;
                }
                if (isset($returnArray['novalidresults']) && $returnArray['novalidresults']) {
                    error_log("No more valid results, breaking loop.");
                    break;
                }
                if ($row['oldtweetlimitretrieved'] == "N") {
                    $params['max_id'] = $returnArray['lowestid'] - 1;
                } else {
                    $params['since_id'] = $returnArray['highestid'];
                }
            }
        } else {
            break;
        }
        if ($queryCount > 35) {
            error_log("35 user_timeline queries!");
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

    $stmt = $GLOBALS['databaseConnection']->prepare($query);
    return $stmt->execute($updParams);
}

function getAutomationSettingsInDB($userAuth) {
    $selectQuery = "SELECT * FROM userautomationsettings WHERE usertwitterid=?";
    $selectStmt = $GLOBALS['databaseConnection']->prepare($selectQuery);
    $success = $selectStmt->execute([$userAuth['twitter_id']]);
    if (!$success) {
        echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, $selectStmt->errorInfo());
        return;
    }
    $row = $selectStmt->fetch();
    if (!$row) {
        echo encodeStatusInformation(StatusCodes::QUERY_OK, $row);
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
        $row['hourflags'] = shiftString($row['hourflags'], $timeDiffHours);
        $row['minuteflags'] = shiftString($row['minuteflags'], $timeDiffMinutes / 15);
        echo encodeStatusInformation(StatusCodes::QUERY_OK, $row);
    }
}

function shiftString($string, $shiftCount) {
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

function commitAutomationSettingsInDB($aS) {
    // Get old automation settings: check if they existed, or were disabled. If so, schedule new automated retweets immediately

    $userTimeZoneHour = $aS['timezonehouroffset'];
    $userTimeZoneMinute = $aS['timezoneminuteoffset'];
    $userOffsetSeconds = ($userTimeZoneHour * 3600) + ($userTimeZoneMinute * 60);
    $now = new \DateTime();
    $serverTimeZone = new \DateTimeZone(date_default_timezone_get());
    $serverOffsetSeconds = $serverTimeZone->getOffset($now);
    $timeDiffSeconds = $serverOffsetSeconds - $userOffsetSeconds;
    $timeDiffHours = intval(floor($timeDiffSeconds / 3600));
    $timeDiffMinutes = intval(floor(($timeDiffSeconds % 3600) / 60));
    $hourFlags = shiftString($aS['hourflags'], -$timeDiffHours);
    $minuteFlags = shiftString($aS['minuteflags'], -$timeDiffMinutes / 15);
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
    $query = "INSERT INTO userautomationsettings (usertwitterid,automationenabled,dayflags,hourflags,minuteflags,includedtext,excludedtext"
            . ",retweetpercent,oldtweetcutoffdate,oldtweetcutoffdateenabled,includedtextenabled,excludedtextenabled,timezonehouroffset,"
            . "timezoneminuteoffset,includetextcondition,excludetextcondition) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
            . "ON DUPLICATE KEY UPDATE automationenabled=?, dayflags=?, hourflags=?, minuteflags=?, includedtext=?, excludedtext=?, retweetpercent=?,"
            . "oldtweetcutoffdate=?, oldtweetcutoffdateenabled=?, includedtextenabled=?, excludedtextenabled=?, timezonehouroffset=?, "
            . "timezoneminuteoffset=?, includetextcondition=?, excludetextcondition=?";
    $stmt = $GLOBALS['databaseConnection']->prepare($query);
    try {
        $success = $stmt->execute([$aS['usertwitterid'], $aS['automationenabled'],
            $aS['dayflags'], $hourFlags, $minuteFlags, $aS['includedtext'], $aS['excludedtext'],
            $retweetPercent, $oldTweetCutoffDate, $aS['oldtweetcutoffdateenabled'],
            $aS['includedtextenabled'], $aS['excludedtextenabled'], $userTimeZoneHour, $userTimeZoneMinute, $aS['includetextcondition'],
            $aS['excludetextcondition'], $aS['automationenabled'], $aS['dayflags'], $hourFlags, $minuteFlags,
            $aS['includedtext'], $aS['excludedtext'], $retweetPercent, $oldTweetCutoffDate, $aS['oldtweetcutoffdateenabled'],
            $aS['includedtextenabled'], $aS['excludedtextenabled'], $userTimeZoneHour, $userTimeZoneMinute, $aS['includetextcondition'],
            $aS['excludetextcondition']]);
        if (!$success) {
            error_log("Failed to insert automation settings");
            echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, $stmt->errorInfo());
            return;
        } else if ($aS['automationenabled'] == "N") {
            $deleteQuery = "DELETE FROM scheduledretweets WHERE retweetingusertwitterid=? AND automated=?";
            $deleteStmt = $GLOBALS['databaseConnection']->prepare($deleteQuery);
            $success = $deleteStmt->execute([$aS['usertwitterid'], "Y"]);
        }
    } catch (PDOException $e) {
        error_log("Failed to insert automation settings: $e");
        echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, $e);
        return;
    }
    encodeSuccessInformation($success);
}

function queueAutomatedRetweets($userAuth) {
    $selectSettingsQuery = "SELECT * FROM userautomationsettings WHERE usertwitterid=?";
    $selectSettingsStmt = $GLOBALS['databaseConnection']->prepare($selectSettingsQuery);
    $success = $selectSettingsStmt->execute([$userAuth['twitter_id']]);
    if (!$success) {
        error_log("Failed to get user ID to queue automated retweets for, cannot continue.");
        return;
    }
    $settingsRow = $selectSettingsStmt->fetch();
    if (!$settingsRow) {
        error_log("No automation settings are available for this user, cannot continue.");
        return;
    }

    $dayFlags = $settingsRow['dayflags'];
    $hourFlags = $settingsRow['hourflags'];
    $minuteFlags = $settingsRow['minuteflags'];
    $dayCount = substr_count($dayFlags, 'Y');
    $hourCount = substr_count($hourFlags, 'Y');
    $minuteCount = substr_count($minuteFlags, 'Y');
    if ($dayCount == 0 || $hourCount == 0 || $minuteCount == 0) {
        error_log("User has invalid automation settings, cannot continue.");
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
    if ($settingsRow['includedtextenabled'] == 'Y') {
        if ($settingsRow['includedtextcondition'] == "This exact phrase") {
            $selectMeanStatsQuery .= " AND fulltweettext LIKE ?";
            $selectMeanStatsParams[] = "%" . $settingsRow['includedText'] . "%";
        } else {
            $includedWords = explode(" ", $settingsRow['includedtext']);
            $selectMeanStatsQuery .= " AND (";
            foreach ($includedWords as $includedWord) {
                if ($settingsRow['includedtextcondition'] == "All of these words") {
                    $selectMeanStatsQuery .= "fulltweettext LIKE ? AND ";
                } else {
                    $selectMeanStatsQuery .= "fulltweettext LIKE ? OR ";
                }
                $selectMeanStatsParams[] = "%" . $includedWord . "%";
            }
            if ($settingsRow['includedtextcondition'] == "All of these words") {
                $selectMeanStatsQuery = substr($selectMeanStatsQuery, 0, -5);
            } else {
                $selectMeanStatsQuery = substr($selectMeanStatsQuery, 0, -4);
            }
            $selectMeanStatsQuery .= ")";
        }
    }
    if ($settingsRow['excludedtextenabled'] == 'Y') {
        if ($settingsRow['excludedtextcondition'] == "This exact phrase") {
            $selectMeanStatsQuery .= " AND fulltweettext NOT LIKE ?";
            $selectMeanStatsParams[] = "%" . $settingsRow['excludedText'] . "%";
        } else {
            $excludedWords = explode(" ", $settingsRow['excludedtext']);
            $selectMeanStatsQuery .= " AND (";
            foreach ($excludedWords as $excludedWord) {
                if ($settingsRow['excludedtextcondition'] == "All of these words") {
                    $selectMeanStatsQuery .= "fulltweettext NOT LIKE ? AND ";
                } else {
                    $selectMeanStatsQuery .= "fulltweettext NOT LIKE ? OR ";
                }
                $selectMeanStatsParams[] = "%" . $excludedWord . "%";
            }
            if ($settingsRow['excludedtextcondition'] == "All of these words") {
                $selectMeanStatsQuery = substr($selectMeanStatsQuery, 0, -5);
            } else {
                $selectMeanStatsQuery = substr($selectMeanStatsQuery, 0, -4);
            }
            $selectMeanStatsQuery .= ")";
        }
    }

    $selectMeanStatsStmt = $GLOBALS['databaseConnection']->prepare($selectMeanStatsQuery);
    //error_log($selectMeanStatsQuery);
    $success = $selectMeanStatsStmt->execute($selectMeanStatsParams);
    if (!$success) {
        error_log("Failed to get mean statistics for user, cannot continue with automated retweet scheduling.");
        return;
    }
    /* $selectMedianStatsQuery = "SET @rowindex := -1; SELECT AVG(retweets) AS medianrts FROM (SELECT @rowindex:=@rowindex + 1 AS rowindex,
      retweets FROM tweets INNER JOIN tweetmetrics ON tweets.id=tweetmetrics.tweetstableid WHERE usertwitterid=? ORDER BY retweets) AS g WHERE
      g.rowindex IN (FLOOR(@rowindex / 2) , CEIL(@rowindex / 2))";
      $selectMedianStatsStmt = $GLOBALS['databaseConnection']->prepare($selectMedianStatsQuery);
      $success = $selectMedianStatsStmt->execute($userAuth['twitter_id']);
      if (!$success) {
      error_log("Failed to get median statistics for user, cannot continue with automated retweet scheduling.");
      return;
      }
      $medianRow = $selectMedianStatsStmt->fetch(); */
    //$medianRTs = $medianRow['medianrts'];
    $statsRow = $selectMeanStatsStmt->fetch();
    $meanRTs = $statsRow['avgrts'];
    if ($settingsRow['metricsmeasurementtype'] == "Mean Average" && $settingsRow['retweetpercent'] != null) {
        $retweetPercent = $settingsRow['retweetpercent'] / 100;
        $retweetThreshold = $meanRTs * $retweetPercent;
    }
    else if ($settingsRow['metricsmeasurementtype'] == "Adaptive") {
        $retweetThreshold = max(($meanRTs * 0.2), $settingsRow['adaptivertthreshold']);
    } else {
        $userTwitterID = $userAuth['twitter_id'];
        $settingType = $settingsRow['metricsmeasurementtype'];
        error_log("Incorrect automation settings for user ID: $userTwitterID. Settings row:");
        error_log(print_r($settingsRow, true));
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
    $selectTweetsStmt = $GLOBALS['databaseConnection']->prepare($selectTweetsQuery);
    //error_log("Select tweets query: $selectTweetsQuery");
    $success = $selectTweetsStmt->execute($selectMeanStatsParams);
    if (!$success) {
        error_log("Failed to get tweets for user, cannot continue with automated retweet scheduling.");
        return;
    }
    $tweetRows = $selectTweetsStmt->fetchAll();
    $tweetCount = count($tweetRows);

    //error_log("Total tweet count returned: $tweetCount");
    $tweetsPerDay = ceil($tweetCount / $daysBetweenTweets);
    $minuteValues = getMinuteValues($minuteFlags);
    $numScheduled = 0;
    //error_log("Tweets per day: $tweetsPerDay     Total count per day: $totalCountPerDay");
    if ($tweetCount == 0) {
        return;
    }
    $scheduleType = "Random";
    if ($scheduleType == "Random") {
        // Schedule randomly, once per hour
        $hourIndices = getHourFlagIndices($hourFlags);
        //error_log("Count of hour indices: $countOfHours");
        while ($numScheduled < 10 && $numScheduled < $tweetCount && $numScheduled < $tweetsPerDay && count($hourIndices) > 0) {
            $nextHour = getRandomHourToAutomate($hourIndices);
            //error_log("Next hour: $nextHour");
            unset($hourIndices[array_search($nextHour, $hourIndices)]);
            $hourIndices = array_values($hourIndices);
            //error_log(print_r($hourIndices, true));
            $minuteValue = getNextMinute($minuteValues);
            $retweetTime = new \DateTime();
            $retweetTime->add(new \DateInterval('P2D'));
            //error_log("Next hour: $nextHour    Next minute: $minuteValue");
            $retweetTime->setTime($nextHour, $minuteValue);
            $retweetTimeStamp = $retweetTime->getTimestamp();
            queueRetweetInDB($userAuth['twitter_id'], $tweetRows[$numScheduled]['tweetid'], $retweetTimeStamp, true);
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
            $minuteValue = getNextMinute($minuteValues);
            $retweetTime = new \DateTime();
            $retweetTime->add(new \DateInterval('P2D'));
            $retweetTime->setTime($nextHour, $minuteValue);
            $retweetTimeStamp = $retweetTime->getTimestamp();
            queueRetweetInDB($userAuth['twitter_id'], $tweetRows[$numScheduled]['tweetid'], $retweetTimeStamp, true);
            $numScheduled++;
            $latestUsed = $nextHour;
            $nextHour = getNextHourToAutomate($hourFlags, $latestUsed, $interval);
        }
    }
}

function getHourFlagIndices($hourFlags) {
    $hourFlagIndices = [];
    for ($i = 0; $i < strlen($hourFlags); $i++) {
        if ($hourFlags[$i] == "Y") {
            $hourFlagIndices[] = $i;
        }
    }
    return $hourFlagIndices;
}

function getRandomHourToAutomate($hourFlagIndices) {
    $validIndices = count($hourFlagIndices);
    if ($validIndices == 0) {
        return null;
    }
    return $hourFlagIndices[rand(0, $validIndices - 1)];
}

function getNextHourToAutomate($hourFlags, $latestUsed, $interval) {
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

function getMinuteValues($minuteFlags) {
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

function getNextMinute($minuteValues) {
    if (count($minuteValues) == 1) {
        return $minuteValues[0];
    } else {
        return $minuteValues[rand(0, count($minuteValues) - 1)];
    }
}

function beginInitialAutomationOnServer($userAuth) {
    
}
