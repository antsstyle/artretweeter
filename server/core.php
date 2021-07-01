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
    $date1HourAfterRTTime = date("Y-m-d H:i:s", strtotime('+1 hour', $retweetTime));
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

    $recordsHourBeforeStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? 
        AND retweettime >= ? AND retweettime <= ?
        ORDER BY retweettime DESC LIMIT ?");
    $recordsHourBeforeStmt->bindValue(1, $userTwitterID);
    $recordsHourBeforeStmt->bindValue(2, $date1HourBeforeRTTime);
    $recordsHourBeforeStmt->bindValue(3, $RTTime);
    $recordsHourBeforeStmt->bindValue(4, $perHourLimit, \PDO::PARAM_INT);
    $recordsHourBeforeSuccess = $recordsHourBeforeStmt->execute();
    if (!$recordsHourBeforeSuccess) {
        error_log("Failed to get retweet records to check hour limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($recordsHourBeforeStmt->errorInfo());
            exit();
        }
        return false;
    }
    $recordsHourBeforeResults = $recordsHourBeforeStmt->fetchAll();

    if ($recordsHourBeforeResults && (count($recordsHourBeforeResults) >= $perHourLimit)) {
        if ($echoAndExit) {
            $timeToResetSeconds = (60 * 60) - (time() - $recordsHourBeforeResults[($perHourLimit - 1)]['retweettime']);
            echo encodeErrorInformation("Too many retweets in 1 hour period.", $timeToResetSeconds);
            exit();
        }
        return false;
    }

    $queueHourBeforeStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM scheduledretweets WHERE retweetingusertwitterid=? 
        AND retweettime >= ? AND retweettime <= ?
        ORDER BY retweettime DESC LIMIT ?");
    $queueHourBeforeStmt->bindValue(1, $userTwitterID);
    $queueHourBeforeStmt->bindValue(2, $date1HourBeforeRTTime);
    $queueHourBeforeStmt->bindValue(3, $RTTime);
    $queueHourBeforeStmt->bindValue(4, $perHourLimit, \PDO::PARAM_INT);
    $queueHourBeforeSuccess = $queueHourBeforeStmt->execute();
    if (!$queueHourBeforeSuccess) {
        error_log("Failed to get scheduled retweets to check hour limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($queueHourBeforeStmt->errorInfo());
            exit();
        }
        return false;
    }
    $queueHourBeforeResults = $queueHourBeforeStmt->fetchAll();
    if (!$recordsHourBeforeResults) {
        $totalCountForHourBefore = count($queueHourBeforeResults);
    } else if (!$queueHourBeforeResults) {
        $totalCountForHourBefore = count($recordsHourBeforeResults);
    } else {
        $totalCountForHourBefore = count($recordsHourBeforeResults) + count($queueHourBeforeResults);
    }

    if ($totalCountForHourBefore >= $perHourLimit) {
        if ($echoAndExit) {
            $timeToResetSeconds = (60 * 60) - (time() - $recordsHourBeforeResults[($perHourLimit - 1)]['retweettime']);
            echo encodeErrorInformation("Too many retweets in 1 hour period.", $timeToResetSeconds);
            exit();
        }
        return false;
    }

    $recordsHourAfterStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? 
        AND retweettime >= ? AND retweettime <= ?
        ORDER BY retweettime DESC LIMIT ?");
    $recordsHourAfterStmt->bindValue(1, $userTwitterID);
    $recordsHourAfterStmt->bindValue(2, $RTTime);
    $recordsHourAfterStmt->bindValue(3, $date1HourAfterRTTime);
    $recordsHourAfterStmt->bindValue(4, $perHourLimit, \PDO::PARAM_INT);
    $recordsHourAfterSuccess = $recordsHourAfterStmt->execute();
    if (!$recordsHourAfterSuccess) {
        error_log("Failed to get retweet records to check hour limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($recordsHourAfterStmt->errorInfo());
            exit();
        }
        return false;
    }
    $recordsHourAfterResults = $recordsHourAfterStmt->fetchAll();

    if ($recordsHourAfterResults && (count($recordsHourAfterResults) >= $perHourLimit)) {
        if ($echoAndExit) {
            $timeToResetSeconds = (60 * 60) - (time() - $recordsHourAfterResults[($perHourLimit - 1)]['retweettime']);
            echo encodeErrorInformation("Too many retweets in 1 hour period.", $timeToResetSeconds);
            exit();
        }
        return false;
    }

    $queueHourAfterStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM scheduledretweets WHERE retweetingusertwitterid=? 
        AND retweettime >= ? AND retweettime <= ?
        ORDER BY retweettime DESC LIMIT ?");
    $queueHourAfterStmt->bindValue(1, $userTwitterID);
    $queueHourAfterStmt->bindValue(2, $RTTime);
    $queueHourAfterStmt->bindValue(3, $date1HourAfterRTTime);
    $queueHourAfterStmt->bindValue(4, $perHourLimit, \PDO::PARAM_INT);
    $queueHourAfterSuccess = $queueHourAfterStmt->execute();
    if (!$queueHourAfterSuccess) {
        error_log("Failed to get scheduled retweets to check hour limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($queueHourAfterStmt->errorInfo());
            exit();
        }
        return false;
    }
    $queueHourAfterResults = $queueHourAfterStmt->fetchAll();
    if (!$recordsHourAfterResults) {
        $totalCountForHourAfter = count($queueHourAfterResults);
    } else if (!$queueHourAfterResults) {
        $totalCountForHourAfter = count($recordsHourAfterResults);
    } else {
        $totalCountForHourAfter = count($recordsHourAfterResults) + count($queueHourAfterResults);
    }

    if ($totalCountForHourAfter >= $perHourLimit) {
        if ($echoAndExit) {
            $timeToResetSeconds = (60 * 60) - (time() - $recordsHourAfterResults[($perHourLimit - 1)]['retweettime']);
            echo encodeErrorInformation("Too many retweets in 1 hour period.", $timeToResetSeconds);
            exit();
        }
        return false;
    }

    $recordsDayBeforeStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? 
        AND retweettime >= ? AND retweettime <= ?
        ORDER BY retweettime DESC LIMIT ?");
    $recordsDayBeforeStmt->bindValue(1, $userTwitterID);
    $recordsDayBeforeStmt->bindValue(2, $date24HoursBeforeRTTime);
    $recordsDayBeforeStmt->bindValue(3, $RTTime);
    $recordsDayBeforeStmt->bindValue(4, $perDayLimit, \PDO::PARAM_INT);
    $recordsDayBeforeSuccess = $recordsDayBeforeStmt->execute();
    if (!$recordsDayBeforeSuccess) {
        error_log("Failed to get retweet records to check day limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($recordsDayBeforeStmt->errorInfo());
            exit();
        }
        return false;
    }
    $recordsDayBeforeResults = $recordsDayBeforeStmt->fetchAll();
    $queueDayBeforeStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM scheduledretweets WHERE 
        retweetingusertwitterid=? AND retweettime >= ? AND retweettime <= ?
        ORDER BY retweettime DESC LIMIT ?");
    $queueDayBeforeStmt->bindValue(1, $userTwitterID);
    $queueDayBeforeStmt->bindValue(2, $date24HoursBeforeRTTime);
    $queueDayBeforeStmt->bindValue(3, $RTTime);
    $queueDayBeforeStmt->bindValue(4, $perDayLimit, \PDO::PARAM_INT);
    $queueDayBeforeSuccess = $queueDayBeforeStmt->execute();
    if (!$queueDayBeforeSuccess) {
        error_log("Failed to get scheduled retweets to check day limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($queueDayBeforeStmt->errorInfo());
            exit();
        }
        return false;
    }
    $queueDayBeforeResults = $queueDayBeforeStmt->fetchAll();

    if (!$recordsDayBeforeResults && !$queueDayBeforeResults) {
        return true;
    } else if (!$recordsDayBeforeResults) {
        $totalCountForDayBefore = count($queueDayBeforeResults);
    } else if ($queueDayBeforeResults) {
        $totalCountForDayBefore = count($recordsDayBeforeResults);
    } else {
        $totalCountForDayBefore = count($recordsDayBeforeResults) + count($queueDayBeforeResults);
    }

    if ($totalCountForDayBefore < $perDayLimit) {
        return true;
    } else if ($totalCountForDayBefore >= $perDayLimit) {
        $timeToResetSeconds = (60 * 60 * 24) - (time() - $recordsDayBeforeResults[($perDayLimit - 1)]['retweettime']);
        if ($echoAndExit) {
            echo encodeErrorInformation("Too many retweets scheduled in 24 hour period.", $timeToResetSeconds);
            exit();
        }
        return false;
    }
    
        $recordsDayAfterStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? 
        AND retweettime >= ? AND retweettime <= ?
        ORDER BY retweettime DESC LIMIT ?");
    $recordsDayAfterStmt->bindValue(1, $userTwitterID);
    $recordsDayAfterStmt->bindValue(2, $RTTime);
    $recordsDayAfterStmt->bindValue(3, $date24HoursAfterRTTime);
    $recordsDayAfterStmt->bindValue(4, $perDayLimit, \PDO::PARAM_INT);
    $recordsDayAfterSuccess = $recordsDayAfterStmt->execute();
    if (!$recordsDayAfterSuccess) {
        error_log("Failed to get retweet records to check day limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($recordsDayAfterStmt->errorInfo());
            exit();
        }
        return false;
    }
    $recordsDayAfterResults = $recordsDayAfterStmt->fetchAll();
    $queueDayAfterStmt = $GLOBALS['databaseConnection']->prepare("SELECT * FROM scheduledretweets WHERE 
        retweetingusertwitterid=? AND retweettime >= ? AND retweettime <= ?
        ORDER BY retweettime DESC LIMIT ?");
    $queueDayAfterStmt->bindValue(1, $userTwitterID);
    $queueDayAfterStmt->bindValue(2, $RTTime);
    $queueDayAfterStmt->bindValue(3, $date24HoursAfterRTTime);
    $queueDayAfterStmt->bindValue(4, $perDayLimit, \PDO::PARAM_INT);
    $queueDayAfterSuccess = $queueDayAfterStmt->execute();
    if (!$queueDayAfterSuccess) {
        error_log("Failed to get scheduled retweets to check day limits.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($queueDayAfterStmt->errorInfo());
            exit();
        }
        return false;
    }
    $queueDayAfterResults = $queueDayAfterStmt->fetchAll();

    if (!$recordsDayAfterResults && !$queueDayAfterResults) {
        return true;
    } else if (!$recordsDayAfterResults) {
        $totalCountForDayAfter = count($queueDayAfterResults);
    } else if ($queueDayAfterResults) {
        $totalCountForDayAfter = count($recordsDayAfterResults);
    } else {
        $totalCountForDayAfter = count($recordsDayAfterResults) + count($queueDayAfterResults);
    }

    if ($totalCountForDayAfter < $perDayLimit) {
        return true;
    } else if ($totalCountForDayAfter >= $perDayLimit) {
        $timeToResetSeconds = (60 * 60 * 24) - (time() - $recordsDayAfterResults[($perDayLimit - 1)]['retweettime']);
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
