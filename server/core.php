<?php

namespace ArtRetweeter;

require_once "credentials/apikeys.php";
require_once "credentials/db.php";
require_once "twitteroauth/vendor/autoload.php";

use Abraham\TwitterOAuth\TwitterOAuth;

$options = [
    \PDO::ATTR_DEFAULT_FETCH_MODE => \PDO::FETCH_ASSOC,
];

$databaseConnection = new \PDO("mysql:host=$servername;dbname=$database;port=$port", $username, $password, $options);

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
    $endpoint = "statuses/retweet";
    $time5MinsAgo = date('Y-m-d H:i:s', strtotime('-5 minutes', time()));
    $time5MinsFromNow = date('Y-m-d H:i:s', strtotime('+5 minutes', time()));
    $selectQuery = "SELECT * FROM scheduledretweets INNER JOIN users ON "
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
        $queryResult = queryTwitterUserAuth($connection, $endpoint, "POST", $params, $userAuth, false, false);
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
                $originalTweetID = $originalStatus->id;
                updateRetweetRecordsInDB($userAuth['twitter_id'], $originalTweetID);
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
            . "tweetid,UNIX_TIMESTAMP(retweettime) AS rttime FROM failedretweets WHERE retweetingusertwitterid=?");
    $failTableSuccess = $failedRetweetsStmt->execute([$userAuthTwitterID]);
    if (!$failTableSuccess) {
        $returnArray['failedretweets'] = false;
    } else {
        $returnArray['failedretweets'] = $failedRetweetsStmt->fetchAll();
        $GLOBALS['databaseConnection']->prepare("DELETE FROM failedretweets WHERE retweetingusertwitterid=?")->execute([$userAuthTwitterID]);
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

function checkUserCanQueueNewRetweet($userTwitterID, $retweetTime, $echoAndExit = true) {
    $date24HoursBeforeRTTime = date("Y-m-d H:i:s", strtotime('-24 hours', $retweetTime));
    $date1HourBeforeRTTime = date("Y-m-d H:i:s", strtotime('-1 hour', $retweetTime));
    $date24HoursAfterRTTime = date("Y-m-d H:i:s", strtotime('+24 hours', $retweetTime));
    $Date1HourAfterRTTime = date("Y-m-d H:i:s", strtotime('+1 hour', $retweetTime));
    $RTTime = date("Y-m-d H:i:s", $retweetTime);
    $perDayLimit = 10;
    $perHourLimit = 2;

    $recordsNowStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime=?");
    $recordsNowStmt->bindValue(1, $userTwitterID);
    $recordsNowStmt->bindValue(2, $RTTime);
    $recordsNowSuccess = $recordsNowStmt->execute();
    if (!$recordsNowSuccess) {
        error_log("Failed to get retweet records to check current time limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($recordsNowStmt->errorInfo());
            exit();
        }
        return false;
    }
    $recordsNowResults = $recordsNowStmt->fetchAll();
    if ($recordsNowResults) {
        if ($echoAndExit) {
            echo encodeErrorInformation("You cannot schedule two retweets to be posted at the same time.");
            exit();
        }
        return false;
    }

    $queueNowStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM scheduledretweets WHERE retweetingusertwitterid=? AND retweettime=?");
    $queueNowStmt->bindValue(1, $userTwitterID);
    $queueNowStmt->bindValue(2, $RTTime);
    $queueNowSuccess = $queueNowStmt->execute();
    if (!$queueNowSuccess) {
        error_log("Failed to get scheduled retweets to check current time limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($queueNowStmt->errorInfo());
            exit();
        }
        return false;
    }
    $queueNowResults = $queueNowStmt->fetchAll();
    if ($queueNowResults) {
        if ($echoAndExit) {
            echo encodeErrorInformation("You cannot schedule two retweets to be posted at the same time.");
            exit();
        }
        return false;
    }

    $recordsHourStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime >= ? 
                  ORDER BY retweettime DESC LIMIT ?");
    $recordsHourStmt->bindValue(1, $userTwitterID);
    $recordsHourStmt->bindValue(2, $date1HourBeforeRTTime);
    $recordsHourStmt->bindValue(3, $perHourLimit, \PDO::PARAM_INT);
    $recordsHourSuccess = $recordsHourStmt->execute();
    if (!$recordsHourSuccess) {
        error_log("Failed to get retweet records to check hour limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($recordsHourStmt->errorInfo());
            exit();
        }
        return false;
    }
    $recordsHourResults = $recordsHourStmt->fetchAll();

    if ($recordsHourResults && (count($recordsHourResults) >= $perHourLimit)) {
        if ($echoAndExit) {
            $timeToResetSeconds = (60 * 60) - (time() - $recordsHourResults[($perHourLimit - 1)]['retweettime']);
            echo encodeErrorInformation("Too many retweets in 1 hour period.", $timeToResetSeconds);
            exit();
        }
        return false;
    }

    $queueHourStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM scheduledretweets WHERE retweetingusertwitterid=? 
        AND retweettime <= ?
        ORDER BY retweettime DESC LIMIT ?");
    $queueHourStmt->bindValue(1, $userTwitterID);
    $queueHourStmt->bindValue(2, $Date1HourAfterRTTime);
    $queueHourStmt->bindValue(3, $perHourLimit, \PDO::PARAM_INT);
    $queueHourSuccess = $queueHourStmt->execute();
    if (!$queueHourSuccess) {
        error_log("Failed to get scheduled retweets to check hour limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($queueHourStmt->errorInfo());
            exit();
        }
        return false;
    }
    $queueHourResults = $queueHourStmt->fetchAll();
    if (!$recordsHourResults) {
        $totalCountForHour = count($queueHourResults);
    } else if (!$queueHourResults) {
        $totalCountForHour = count($recordsHourResults);
    } else {
        $totalCountForHour = count($recordsHourResults) + count($queueHourResults);
    }

    if ($totalCountForHour >= $perHourLimit) {
        if ($echoAndExit) {
            $timeToResetSeconds = (60 * 60) - (time() - $recordsHourResults[($perHourLimit - 1)]['retweettime']);
            echo encodeErrorInformation("Too many retweets in 1 hour period.", $timeToResetSeconds);
            exit();
        }
        return false;
    }

    $recordsDayStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime >= ?
              ORDER BY retweettime DESC LIMIT ?");
    $recordsDayStmt->bindValue(1, $userTwitterID);
    $recordsDayStmt->bindValue(2, $date24HoursBeforeRTTime);
    $recordsDayStmt->bindValue(3, $perDayLimit, \PDO::PARAM_INT);
    $recordsDaySuccess = $recordsDayStmt->execute();
    if (!$recordsDaySuccess) {
        error_log("Failed to get retweet records to check day limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($recordsDayStmt->errorInfo());
            exit();
        }
        return false;
    }
    $recordsDayResults = $recordsDayStmt->fetchAll();
    $queueDayStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM scheduledretweets WHERE 
        retweetingusertwitterid=? AND retweettime <= ? 
        ORDER BY retweettime DESC LIMIT ?");
    $queueDayStmt->bindValue(1, $userTwitterID);
    $queueDayStmt->bindValue(2, $date24HoursAfterRTTime);
    $queueDayStmt->bindValue(3, $perDayLimit, \PDO::PARAM_INT);
    $queueDaySuccess = $queueDayStmt->execute();
    if (!$queueDaySuccess) {
        error_log("Failed to get scheduled retweets to check day limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($queueDayStmt->errorInfo());
            exit();
        }
        return false;
    }
    $queueDayResults = $queueDayStmt->fetchAll();

    if (!$recordsDayResults && !$queueDayResults) {
        return true;
    } else if (!$recordsDayResults) {
        $totalCountForDay = count($queueDayResults);
    } else if ($queueDayResults) {
        $totalCountForDay = count($recordsDayResults);
    } else {
        $totalCountForDay = count($recordsDayResults) + count($queueDayResults);
    }

    if ($totalCountForDay < $perDayLimit) {
        return true;
    } else if ($totalCountForDay >= $perDayLimit) {
        $timeToResetSeconds = (60 * 60 * 24) - (time() - $recordsDayResults[($perDayLimit - 1)]['retweettime']);
        if ($echoAndExit) {
            echo encodeErrorInformation("Too many retweets scheduled in 24 hour period.", $timeToResetSeconds);
            exit();
        }
        return false;
    }
    return false;
}

function updateRetweetRecordsInDB($userTwitterID, $tweetID) {
    $stmt = $GLOBALS['databaseConnection']->prepare("INSERT INTO retweetrecords (usertwitterid,tweetid,retweettime) 
	VALUES (?,?,?)");
    $currentTime = date("Y-m-d H:i:s", time());
    $success = $stmt->execute([$userTwitterID, $tweetID, $currentTime]);
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
    } else {
        return $result;
    }
}

function removeAccountFromDB($twitterID) {
    $success['queue_cleared'] = $GLOBALS['databaseConnection']
            ->prepare("DELETE FROM scheduledretweets WHERE retweetingusertwitterid=?")
            ->execute([$twitterID]);
    $success['user_cleared'] = $GLOBALS['databaseConnection']
            ->prepare("DELETE FROM users WHERE twitterid=?")
            ->execute([$twitterID]);
    echo encodeDBResponseInformation($success);
}
