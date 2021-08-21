<?php

namespace ArtRetweeter;

require_once "credentials/apikeys.php";
require_once "credentials/db.php";
require_once "twitteroauth/vendor/autoload.php";

use Abraham\TwitterOAuth\TwitterOAuth;

$options = [
    \PDO::ATTR_DEFAULT_FETCH_MODE => \PDO::FETCH_ASSOC,
];

try {
    $databaseConnection = new \PDO("mysql:host=$servername;dbname=$database;port=$port", $username, $password, $options);
} catch (Exception $e) {
    echo encodeErrorInformation("Failed to create database connection.");
    exit();
}

function getTweetIDsForUser($userAuthTwitterID) {
    $selectQuery = "SELECT tweetid FROM tweets WHERE usertwitterid=? AND deletedflag=?";
    $selectStmt = $GLOBALS['databaseConnection']->prepare($selectQuery);
    $success = $selectStmt->execute([$userAuthTwitterID, "N"]);
    if (!$success) {
        $returnArray['tweetids'] = false;
    } else {
        $returnArray['tweetids'] = $selectStmt->fetchAll();
    }

    echo encodeDBResponseInformation($returnArray);
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
            . "tweetid,UNIX_TIMESTAMP(retweettime) AS rttime FROM scheduledretweets WHERE retweetingusertwitterid=?");
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

    echo encodeDBResponseInformation($returnArray);
}

function getTweetRetweetStatusInDB($userAuthTwitterID) {
    $recordsStmt = $GLOBALS['databaseConnection']->prepare("SELECT tweetid,UNIX_TIMESTAMP(scheduledretweettime) AS rttime FROM retweetrecords "
            . "WHERE usertwitterid=?");
    $recordsSuccess = $recordsStmt->execute([$userAuthTwitterID]);
    if (!$recordsSuccess) {
        $returnArray['retweetrecords'] = false;
    } else {
        $returnArray['retweetrecords'] = $recordsStmt->fetchAll();
    }

    echo encodeDBResponseInformation($returnArray);
}

function queueRetweetInDB($userAuthTwitterID, $tweetID, $retweetTime) {
    checkUserCanQueueNewRetweet($userAuthTwitterID, $retweetTime);
    checkUserCanRetweetOldTweet($userAuthTwitterID, $retweetTime, $tweetID);
    $timeString = date('Y-m-d H:i:s', $retweetTime);
    $stmt = $GLOBALS['databaseConnection']->prepare("INSERT INTO scheduledretweets (retweetingusertwitterid,tweetid,retweettime) "
            . "VALUES (?,?,?) ON DUPLICATE KEY UPDATE retweetingusertwitterid=?, tweetid=?, retweettime=?");
    $success = $stmt->execute([$userAuthTwitterID, $tweetID, $timeString,
        $userAuthTwitterID, $tweetID, $timeString]);
    echo encodeDBResponseInformation($success);
}

function unqueueRetweetFromDB($userAuthTwitterID, $tweetID) {
    $success = $GLOBALS['databaseConnection']->prepare("DELETE FROM scheduledretweets WHERE retweetingusertwitterid=? AND tweetid=?")
            ->execute([$userAuthTwitterID, $tweetID]);
    echo encodeDBResponseInformation($success);
}

function deleteAccount($userAuthTwitterID) {
    $success = $GLOBALS['databaseConnection']->prepare("DELETE FROM users WHERE twitterid=?")
            ->execute([$userAuthTwitterID]);
    echo encodeDBResponseInformation($success);
}

function validateUserAuth($userAuth) {
    $stmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM users WHERE twitterid=? AND accesstoken=? AND accesstokensecret=?");
    $success = $stmt->execute([$userAuth['twitter_id'], $userAuth['access_token'], $userAuth['access_token_secret']]);
    if (!$success) {
        echo encodeDBResponseInformation("Database error executing query.");
        exit();
    }
    $results = $stmt->fetch();
    if (!$results) {
        echo encodeDBResponseInformation("Invalid or nonexistent access token.");
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
            echo encodeDBResponseInformation($records30DaysSuccess->errorInfo());
            exit();
        }
        return false;
    }
    $records30DaysResults = $records30DaysStmt->fetchAll();
    if ($records30DaysResults && (count($records30DaysResults) >= $per30DaysLimit)) {
        if ($echoAndExit) {
            $timeToResetSeconds = (60 * 60 * 24 * 30) - (time() - $records30DaysResults[($per30DaysLimit - 1)]['retweettime']);
            echo encodeErrorInformation("Too many retweets of this tweet in the last 30 days.", $timeToResetSeconds);
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
            echo encodeDBResponseInformation($recordsYearStmt->errorInfo());
            exit();
        }
        return false;
    }
    $recordsYearResults = $recordsYearStmt->fetchAll();
    if ($recordsYearResults && (count($recordsYearResults) >= $perYearLimit)) {
        $timeToResetSeconds = (60 * 60 * 24 * 30 * 365) - (time() - $recordsYearResults[($perYearLimit - 1)]['retweettime']);
        echo encodeErrorInformation("Too many retweets of this tweet in the last 12 months.", $timeToResetSeconds);
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
        echo encodeErrorInformation("Internal server error - an invalid table was specified for retrieving tweets at an exact time.");
        exit();
    } else {
        return false;
    }
    $stmt->bindValue(1, $userTwitterID);
    $stmt->bindValue(2, $retweetTime);
    $success = $stmt->execute();
    if (!$success) {
        if ($echoAndExit) {
            echo encodeDBResponseInformation($stmt->errorInfo());
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
    $RTTime = date("Y-m-d H:i:s", $retweetTime);
    $intervalSizeSeconds = (strtotime('+' . $timeInterval, $retweetTime) - strtotime('-' . $timeInterval, $retweetTime)) / 2;
    $stmt = $GLOBALS['databaseConnection']->prepare("SELECT COUNT(*) FROM scheduledretweets WHERE retweetingusertwitterid=? 
            AND retweettime >= ? AND retweettime <= ?
            ORDER BY retweettime DESC LIMIT ?");
    $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
    $stmt->bindValue(2, $intervalStart, \PDO::PARAM_STR);
    $stmt->bindValue(3, $RTTime, \PDO::PARAM_STR);
    $stmt->bindValue(4, $limit, \PDO::PARAM_INT);
    $success = $stmt->execute();
    if (!$success) {
        if ($echoAndExit) {
            echo encodeDBResponseInformation($stmt->errorInfo());
            exit();
        }
        return $stmt->errorInfo();
    }
    $scheduledCount = $stmt->fetchColumn();
    $stmt = $GLOBALS['databaseConnection']->prepare("SELECT COUNT(*) FROM retweetrecords WHERE usertwitterid=? 
            AND scheduledretweettime >= ? AND scheduledretweettime <= ?
            ORDER BY scheduledretweettime DESC LIMIT ?");
    $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
    $stmt->bindValue(2, $intervalStart, \PDO::PARAM_STR);
    $stmt->bindValue(3, $RTTime, \PDO::PARAM_STR);
    $stmt->bindValue(4, $limit, \PDO::PARAM_INT);
    $success = $stmt->execute();
    if (!$success) {
        if ($echoAndExit) {
            echo encodeDBResponseInformation($stmt->errorInfo());
            exit();
        }
        return $stmt->errorInfo();
    }
    $recordCount = $stmt->fetchColumn();
    $rescount = $recordCount + $scheduledCount;
    if ($rescount >= $limit) {
        if ($echoAndExit) {
            echo encodeErrorInformation("Too many retweets in 1 hour period.");
            exit();
        }
        return false;
    }

    $stmt = $GLOBALS['databaseConnection']->prepare("SELECT COUNT(*) FROM scheduledretweets WHERE retweetingusertwitterid=? 
            AND retweettime >= ? AND retweettime <= ?
            ORDER BY retweettime DESC LIMIT ?");
    $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
    $stmt->bindValue(2, $RTTime, \PDO::PARAM_STR);
    $stmt->bindValue(3, $intervalEnd, \PDO::PARAM_STR);
    $stmt->bindValue(4, $limit, \PDO::PARAM_INT);
    $success = $stmt->execute();
    if (!$success) {
        if ($echoAndExit) {
            echo encodeDBResponseInformation($stmt->errorInfo());
            exit();
        }
        return $stmt->errorInfo();
    }
    $scheduledCount = $stmt->fetchColumn();
    $stmt = $GLOBALS['databaseConnection']->prepare("SELECT COUNT(*) FROM retweetrecords WHERE usertwitterid=? 
            AND scheduledretweettime >= ? AND scheduledretweettime <= ?
            ORDER BY scheduledretweettime DESC LIMIT ?");
    $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
    $stmt->bindValue(2, $RTTime, \PDO::PARAM_STR);
    $stmt->bindValue(3, $intervalEnd, \PDO::PARAM_STR);
    $stmt->bindValue(4, $limit, \PDO::PARAM_INT);
    $success = $stmt->execute();
    if (!$success) {
        if ($echoAndExit) {
            echo encodeDBResponseInformation($stmt->errorInfo());
            exit();
        }
        return $stmt->errorInfo();
    }
    $recordCount = $stmt->fetchColumn();
    $rescount = $recordCount + $scheduledCount;
    if ($rescount >= $limit) {
        if ($echoAndExit) {
            echo encodeErrorInformation("Too many retweets in 1 hour period.");
            exit();
        }
        return false;
    }


    $stmt = $GLOBALS['databaseConnection']->prepare("SELECT retweettime AS rttime FROM scheduledretweets WHERE retweetingusertwitterid=? 
            AND retweettime >= ? ORDER BY retweettime ASC LIMIT 1");
    $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
    $stmt->bindValue(2, $intervalStart, \PDO::PARAM_STR);
    $success = $stmt->execute();
    if (!$success) {
        if ($echoAndExit) {
            echo encodeDBResponseInformation($stmt->errorInfo());
            exit();
        }
        return $stmt->errorInfo();
    }
    $row = $stmt->fetch();
    if (!$row) {
        return true;
    }
    $earliestTimestamp1 = $row['rttime'];
    $stmt = $GLOBALS['databaseConnection']->prepare("SELECT scheduledretweettime AS rttime FROM retweetrecords WHERE usertwitterid=? 
            AND scheduledretweettime >= ? ORDER BY scheduledretweettime ASC LIMIT 1");
    $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
    $stmt->bindValue(2, $intervalStart, \PDO::PARAM_STR);
    $success = $stmt->execute();
    if (!$success) {
        if ($echoAndExit) {
            echo encodeDBResponseInformation($stmt->errorInfo());
            exit();
        }
        return $stmt->errorInfo();
    }
    $row = $stmt->fetch();
    if (!$row) {
        return true;
    }
    $earliestTimestamp2 = $row['rttime'];
    if (!$earliestTimestamp1) {
        $earliestTimestampFinal = $earliestTimestamp2;
    } else if (!$earliestTimestamp2) {
        $earliestTimestampFinal = $earliestTimestamp1;
    } else if ($earliestTimestamp1 < $earliestTimestamp2) {
        $earliestTimestampFinal = $earliestTimestamp1;
    } else {
        $earliestTimestampFinal = $earliestTimestamp2;
    }


    $stmt = $GLOBALS['databaseConnection']->prepare("SELECT retweettime AS rttime FROM scheduledretweets WHERE retweetingusertwitterid=? 
            AND retweettime <= ? ORDER BY retweettime DESC LIMIT 1");
    $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
    $stmt->bindValue(2, $intervalEnd, \PDO::PARAM_STR);
    $success = $stmt->execute();
    if (!$success) {
        if ($echoAndExit) {
            echo encodeDBResponseInformation($stmt->errorInfo());
            exit();
        }
        return $stmt->errorInfo();
    }
    $row = $stmt->fetch();
    if (!$row) {
        return true;
    }
    $latestTimestamp1 = $row['rttime'];
    $stmt = $GLOBALS['databaseConnection']->prepare("SELECT scheduledretweettime AS rttime FROM retweetrecords WHERE usertwitterid=? 
            AND scheduledretweettime <= ? ORDER BY scheduledretweettime DESC LIMIT 1");
    $stmt->bindValue(1, $userTwitterID, \PDO::PARAM_STR);
    $stmt->bindValue(2, $intervalEnd, \PDO::PARAM_STR);
    $success = $stmt->execute();
    if (!$success) {
        if ($echoAndExit) {
            echo encodeDBResponseInformation($stmt->errorInfo());
            exit();
        }
        return $stmt->errorInfo();
    }
    $row = $stmt->fetch();
    if (!$row) {
        return true;
    }
    $latestTimestamp2 = $row['rttime'];
    if (!$latestTimestamp1) {
        $latestTimestampFinal = $latestTimestamp2;
    } else if (!$latestTimestamp2) {
        $latestTimestampFinal = $latestTimestamp1;
    } else if ($latestTimestamp1 < $latestTimestamp2) {
        $latestTimestampFinal = $latestTimestamp1;
    } else {
        $latestTimestampFinal = $latestTimestamp2;
    }

    $earliestTimeString = strtotime($earliestTimestampFinal);
    $latestTimeString = strtotime($latestTimestampFinal);
    $diff = $latestTimeString - $earliestTimeString;
    if ($diff <= $intervalSizeSeconds) {
        if ($echoAndExit) {
            echo encodeErrorInformation("Too many retweets in $timeInterval period.");
            exit();
        }
        return true;
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
            echo encodeErrorInformation("You cannot schedule two retweets to be posted at the same time.");
            exit();
        }
        return false;
    }

    $queueNowResults = getNumTweetsAtExactTime("scheduledretweets", $userTwitterID, $RTTime, $echoAndExit);
    if ($queueNowResults) {
        if ($echoAndExit) {
            echo encodeErrorInformation("You cannot schedule two retweets to be posted at the same time.");
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

function encodeErrorInformation($msg, $timeToResetSeconds = null) {
    $results['artretweetererrors'] = $msg;
    if (!is_null($timeToResetSeconds)) {
        $results['timetoresetseconds'] = $timeToResetSeconds;
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
            echo encodeDBResponseInformation($stmt->errorInfo());
            exit();
        }
        return true;
    }
    $result = $stmt->fetch();

    if ($result && $result['remaininglimit'] < 5) {
        if ($echoAndExit) {
            echo encodeErrorInformation("Rate limit reached.", $result['timetoresetseconds']);
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
    updateUserRateLimitInDB($userAuthTwitterID, $endpoint, $connection->getLastXHeaders());
    if ($echoAndExit) {
        echo encodeTwitterResponseInformation($connection, $result);
        exit();
    } else {
        return $result;
    }
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
            ->prepare("DELETE FROM tweetmetrics WHERE twitterid=?")
            ->execute([$twitterID]);
    echo encodeDBResponseInformation($success);
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
    }
    $GLOBALS['databaseConnection']->commit();
}
