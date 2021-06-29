<?php

namespace ArtRetweeter;

require_once "credentials/apikeys.php";
require_once "credentials/db.php";
require_once "twitteroauth/vendor/autoload.php";

use Abraham\TwitterOAuth\TwitterOAuth;

$options = [
    \PDO::ATTR_DEFAULT_FETCH_MODE => \PDO::FETCH_ASSOC,
];

$database_connection = new \PDO("mysql:host=$servername;dbname=$database;port=$port", $username, $password, $options);

function removeExpiredRetweets() {
    $time1hourago = date('Y-m-d H:i:s', strtotime('-1 hour', time()));
    $insertQuery = "INSERT INTO failedretweets (retweetingusertwitterid,tweetid,retweettime,errorcode,failreason) "
            . "SELECT retweetingusertwitterid, tweetid, retweettime, ?, "
            . "? FROM scheduledretweets WHERE retweettime <= ?";
    $GLOBALS['database_connection']->prepare($insertQuery)
            ->execute([-1, "Missed schedule", $time1hourago]);
    $GLOBALS['database_connection']->prepare("DELETE FROM scheduledretweets WHERE retweettime <= ?")->execute([$time1hourago]);
}

function postScheduledRetweets() {
    $endpoint = "statuses/retweet";
    $time5minsago = date('Y-m-d H:i:s', strtotime('-5 minutes', time()));
    $time5minsfromnow = date('Y-m-d H:i:s', strtotime('+5 minutes', time()));
    $selectQuery = "SELECT * FROM scheduledretweets INNER JOIN users ON "
            . "scheduledretweets.retweetingusertwitterid = users.twitterid WHERE retweettime >= ? "
            . "AND retweettime <= ?";
    $selectStmt = $GLOBALS['database_connection']->prepare($selectQuery);
    $success = $selectStmt->execute([$time5minsago, $time5minsfromnow]);
    if (!$success) {
        return;
    }
    while ($row = $selectStmt->fetch()) {
        $dbid = $row['id'];
        $tweetid = $row['tweetid'];
        $access_token = $row['accesstoken'];
        $access_token_secret = $row['accesstokensecret'];
        $userauth['twitter_id'] = $row['retweetingusertwitterid'];
        $userauth['access_token'] = $access_token;
        $userauth['access_token_secret'] = $access_token_secret;
        $retweetrecordresults = checkRetweetRecordsInDB($userauth['twitter_id'], false);
        if (!$retweetrecordresults) {
            $GLOBALS['database_connection']->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$dbid]);
            continue;
        }
        $params['id'] = $tweetid;
        $params['trim_user'] = 1;
        $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'], $access_token, $access_token_secret);
        $connection->setRetries(1, 1);
        $query_result = queryTwitterUserAuth($connection, $endpoint, "POST", $params, $userauth, false, false);
        $insertFailedRetweetQuery = "INSERT INTO failedretweets (retweetingusertwitterid,tweetid,retweettime,errorcode,failreason) "
                . "SELECT retweetingusertwitterid, tweetid, retweettime, ?, "
                . "? FROM scheduledretweets WHERE id=?";
        if (!$query_result) {
            $GLOBALS['database_connection']->prepare($insertFailedRetweetQuery)
                    ->execute([2, "Internal error whilst attempting to retweet", $dbid]);
        } else {
            if ($query_result->errors) {
                if (is_array($query_result->errors)) {
                    $errcode = $query_result->errors[0]->code;
                    $errmsg = $query_result->errors[0]->message;
                } else {
                    $errcode = $query_result->errors->code;
                    $errmsg = $query_result->errors->message;
                }
                $GLOBALS['database_connection']->prepare($insertFailedRetweetQuery)
                        ->execute([$errcode, $errmsg, $dbid]);
            } else {
                $originalstatus = $query_result->retweeted_status;
                $originaltweetid = $originalstatus->id;
                updateRetweetRecordsInDB($userauth['twitter_id'], $originaltweetid);
            }
        }
        $GLOBALS['database_connection']->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$dbid]);
    }
}

function getQueueStatusInDB($userauthtwitterid) {
    $scheduledRetweetsStmt = $GLOBALS['database_connection']->prepare("SELECT * FROM scheduledretweets WHERE retweetingusertwitterid=?");
    $schSuccess = $scheduledRetweetsStmt->execute([$userauthtwitterid]);
    if (!$schSuccess) {
        $returnarray['scheduledretweets'] = false;
    } else {
        $returnarray['scheduledretweets'] = $scheduledRetweetsStmt->fetchAll();
    }
    $failedRetweetsStmt = $GLOBALS['database_connection']->prepare("SELECT * FROM failedretweets WHERE retweetingusertwitterid=?");
    $faiSuccess = $failedRetweetsStmt->execute([$userauthtwitterid]);
    if (!$faiSuccess) {
        $returnarray['failedretweets'] = false;
    } else {
        $returnarray['failedretweets'] = $failedRetweetsStmt->fetchAll();
        $GLOBALS['database_connection']->prepare("DELETE FROM failedretweets WHERE retweetingusertwitterid=?")->execute([$userauthtwitterid]);
    }

    echo encodeDBResponseInformation($returnarray);
}

function queueRetweetInDB($userauthtwitterid, $tweetid, $retweettime) {
    $timeString = date('Y-m-d H:i:s', $retweettime);
    $stmt = $GLOBALS['database_connection']->prepare("INSERT INTO scheduledretweets (retweetingusertwitterid,tweetid,retweettime) "
            . "VALUES (?,?,?) ON DUPLICATE KEY UPDATE retweetingusertwitterid=?, tweetid=?, retweettime=?");
    $success = $stmt->execute([$userauthtwitterid, $tweetid, $timeString,
        $userauthtwitterid, $tweetid, $timeString]);
    echo encodeDBResponseInformation($success);
}

function unqueueRetweetFromDB($userauthtwitterid, $tweetid) {
    $success = $GLOBALS['database_connection']->prepare("DELETE FROM scheduledretweets WHERE retweetingusertwitterid=? AND tweetid=?")
            ->execute([$userauthtwitterid, $tweetid]);
    echo encodeDBResponseInformation($success);
}

function deleteAccount($userauthtwitterid) {
    $success = $GLOBALS['database_connection']->prepare("DELETE FROM users WHERE twitterid=?")
            ->execute([$userauthtwitterid]);
    echo encodeDBResponseInformation($success);
}

function validateUserAuth($userauth) {
    $stmt = $GLOBALS['database_connection']->prepare("SELECT * FROM users WHERE twitterid=? AND accesstoken=? AND accesstokensecret=?");
    $success = $stmt->execute([$userauth['twitter_id'], $userauth['access_token'], $userauth['access_token_secret']]);
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
    $stmt = $GLOBALS['database_connection']->prepare("INSERT INTO users (twitterid,accesstoken,accesstokensecret) "
            . "VALUES (?,?,?) ON DUPLICATE KEY UPDATE twitterid=?, accesstoken=?, accesstokensecret=?");
    return $stmt->execute([$response['user_id'], $response['oauth_token'], $response['oauth_token_secret'],
                $response['user_id'], $response['oauth_token'], $response['oauth_token_secret']]);
}

function checkRetweetRecordsInDB($usertwitterid, $echoAndExit = true) {
    $datelimit24hour = date("Y-m-d H:i:s", strtotime('-24 hours', time()));
    $datelimit1hour = date("Y-m-d H:i:s", strtotime('-1 hour', time()));
    $perdaylimit = 10;
    $perhourlimit = 2;
    $stmt = $GLOBALS['database_connection']->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime >= ?
              ORDER BY retweettime DESC LIMIT ?");
    $stmt->bindValue(1, $usertwitterid);
    $stmt->bindValue(2, $datelimit24hour);
    $stmt->bindValue(3, 5, \PDO::PARAM_INT);
    $success = $stmt->execute();
    if (!$success) {
        error_log("Failed to get retweet records.");
        if ($echoAndExit) {
            echo encodeDBResponseInformation($stmt->errorInfo());
            exit();
        }
        return true;
    }
    $results = $stmt->fetchAll();
    if (!$results) {
        return true;
    } else if (count($results) < $perhourlimit) {
        return true;
    } else if (count($results) == $perdaylimit) {
        $timetoresetseconds = (60 * 60 * 24) - (time() - $results[9]['retweettime']);
        if ($echoAndExit) {
            echo encodeErrorInformation("Too many retweets in 24 hour period.", $timetoresetseconds);
            exit();
        }
        return false;
    } else if (count($results) >= $perhourlimit) {
        $stmt = $GLOBALS['database_connection']->prepare("SELECT * FROM retweetrecords WHERE usertwitterid=? AND retweettime >= ? 
                  ORDER BY retweettime DESC LIMIT ?");
        $success = $stmt->execute([$usertwitterid, $datelimit1hour, $perhourlimit]);
        if (!$success) {
            error_log("Failed to get retweet records.");
            if ($echoAndExit) {
                echo encodeDBResponseInformation($stmt->errorInfo());
                exit();
            }
            return false;
        }
        $results = $stmt->fetchAll();
        if (!$results || (count($results) < $perhourlimit)) {
            return true;
        } else {
            $timetoresetseconds = (60 * 60) - (time() - $results[1]['retweettime']);
            if ($echoAndExit) {
                echo encodeErrorInformation("Too many retweets in 1 hour period.", $timetoresetseconds);
                exit();
            }
            return false;
        }
    }
}

function updateRetweetRecordsInDB($usertwitterid, $tweetid) {
    $stmt = $GLOBALS['database_connection']->prepare("INSERT INTO retweetrecords (usertwitterid,tweetid,retweettime) 
	VALUES (?,?,?)");
    $current_time = date("Y-m-d H:i:s", time());
    $success = $stmt->execute([$usertwitterid, $tweetid, $current_time]);
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

function encodeErrorInformation($msg, $timetoresetseconds = null) {
    $results['artretweetererrors'] = $msg;
    if (!is_null($timetoresetseconds)) {
        $results['timetoresetseconds'] = $timetoresetseconds;
    }
    return json_encode($results);
}

function updateUserRateLimitInDB($usertwitterid, $endpoint, $headers) {
    if (!isset($headers['x_rate_limit_remaining']) || !isset($headers['x_rate_limit_limit']) || !isset($headers['x_rate_limit_reset'])) {
        return true;
    }
    $maxlimit = $headers['x_rate_limit_limit'];
    $remaininglimit = $headers['x_rate_limit_remaining'];
    $resettime = date('Y-m-d H:i:s', $headers['x_rate_limit_reset']);
    $timetoresetseconds = $headers['x_rate_limit_reset'] - time();
    $stmt = $GLOBALS['database_connection']->prepare("INSERT INTO ratelimitrecords (usertwitterid,endpoint,maxlimit,remaininglimit,resettime,timetoresetseconds) 
	VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE usertwitterid=?, endpoint=?, maxlimit=?, remaininglimit=?, resettime=?, timetoresetseconds=?");
    $success = $stmt->execute([$usertwitterid, $endpoint, $maxlimit, $remaininglimit, $resettime, $timetoresetseconds,
        $usertwitterid, $endpoint, $maxlimit, $remaininglimit, $resettime, $timetoresetseconds]);
    return $success;
}

function checkUserRateLimitInDB($usertwitterid, $endpoint, $echoAndExit = true) {
    $stmt = $GLOBALS['database_connection']->prepare("SELECT * FROM ratelimitrecords WHERE usertwitterid=? AND endpoint=?");
    $success = $stmt->execute([$usertwitterid, $endpoint]);
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

function queryTwitterUserAuth($connection, $endpoint, $httpRequestType, $params, $userauth, $paramsisjsondata = false,
        $echo = true) {
    $userauthtwitterid = $userauth['twitter_id'];
    validateUserAuth($userauth);
    checkUserRateLimitInDB($userauthtwitterid, $endpoint, $echo);
    if ($httpRequestType == 'GET') {
        if ($paramsisjsondata) {
            $result = $connection->get($endpoint, $params, true);
        } else {
            $result = $connection->get($endpoint, $params);
        }
    } else {
        if ($paramsisjsondata) {
            $result = $connection->post($endpoint, $params, true);
        } else {
            $result = $connection->post($endpoint, $params);
        }
    }
    updateUserRateLimitInDB($userauthtwitterid, $endpoint, $connection->getLastXHeaders());
    if ($echo) {
        echo encodeTwitterResponseInformation($connection, $result);
    } else {
        return $result;
    }
}

function removeAccountFromDB($twitter_id) {
    $success['queue_cleared'] = $GLOBALS['database_connection']
            ->prepare("DELETE FROM scheduledretweets WHERE retweetingusertwitterid=?")
            ->execute([$twitter_id]);
    $success['user_cleared'] = $GLOBALS['database_connection']
            ->prepare("DELETE FROM users WHERE twitterid=?")
            ->execute([$twitter_id]);
    echo encodeDBResponseInformation($success);
}
