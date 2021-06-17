<?php

require "credentials/apikeys.php";
require "credentials/db.php";

$options = [
    \PDO::ATTR_DEFAULT_FETCH_MODE => \PDO::FETCH_ASSOC,
];

$database_connection = new PDO("mysql:host=$servername;dbname=$database;port=$port", $username, $password, $options);

function removeExpiredRetweets() {
    $time1hourago = date("Y-m-d H:m:s", strtotime('-1 hour', time()));
    $insertQuery = "INSERT INTO failedretweets (retweetinguserid,tweetid,retweettime,errorcode,failreason) "
            . "SELECT retweetinguserid, tweetid, retweettime, ?, "
            . "? FROM scheduledretweets WHERE retweettime <= ?";
    $GLOBALS['database_connection']->prepare($insertQuery)
            ->execute([-1, "Missed schedule", $time1hourago]);
    $GLOBALS['database_connection']->prepare("DELETE FROM scheduledretweets WHERE retweettime <= ?")->execute([$time1hourago]);
}

function postScheduledRetweets() {
    $endpoint = "statuses/retweet";
    $time5minsago = date("Y-m-d H:m:s", strtotime('-5 minutes', time()));
    $time5minsfromnow = date("Y-m-d H:m:s", strtotime('+5 minutes', time()));
    $selectQuery = "SELECT * FROM scheduledretweets INNER JOIN users ON "
            . "scheduledretweets.retweetinguserid = users.twitterid WHERE retweettime >= ?"
            . "AND retweettime <= ?";
    $selectStmt = $GLOBALS['database_connection']->prepare($selectQuery);
    $results = $selectStmt->execute([$time5minsago, $time5minsfromnow]);
    if (empty($results)) {
        return;
    }
    while ($row = $results->fetch(PDO::FETCH_ASSOC)) {
        $dbid = $row['id'];
        $tweetid = $row['tweetid'];
        $access_token = $row['access_token'];
        $access_token_secret = $row['access_token_secret'];
        $userauthtwitterid = $row['retweetinguserid'];
        $retweetrecordresults = checkRetweetRecordsInDB($userauthtwitterid);
        if (!$retweetrecordresults) {
            $GLOBALS['database_connection']->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$dbid]);
            continue;
        }
        $params['id'] = $tweetid;
        $params['trim_user'] = 1;
        $connection = new TwitterOAuth($GLOBALS['consumerkey'], $GLOBALS['consumersecret'], $access_token, $access_token_secret);
        $connection->setRetries(1, 1);
        $queryres = queryTwitterUserAuth($connection, $endpoint, "POST", $params, $userauthtwitterid);
        $insertFailedRetweetQuery = "INSERT INTO failedretweets (retweetinguserid,tweetid,retweettime,errorcode,failreason) "
                . "SELECT retweetinguserid, tweetid, retweettime, ?, "
                . "? FROM scheduledretweets WHERE id=?";
        if (!$queryres) {
            $GLOBALS['database_connection']->prepare($insertFailedRetweetQuery)
                    ->execute([1, "Missed schedule", $dbid]);
        } else {
            // Check if object is error first
            if (isset($queryres['errors'])) {
                $errcode = $queryres['errors'][0]['code'];
                $errmsg = $queryres['errors'][0]['message'];
                $GLOBALS['database_connection']->prepare($insertFailedRetweetQuery)
                        ->execute([$errcode, $errmsg, $dbid]);
            } else {
                $retweetedstatus = $queryres['retweeted_status'];
                $retweetid = $retweetedstatus['id'];
                $retweettime = $retweetedstatus['created_at'];
                updateRetweetRecordsInDB($userauthtwitterid, $retweetid, $retweettime);
            }
        }
        $GLOBALS['database_connection']->prepare("DELETE FROM scheduledretweets WHERE id=?")->execute([$dbid]);
    }
}

function updateAccessToken($response) {
    $stmt = $GLOBALS['database_connection']->prepare("INSERT INTO users (twitterid,accesstoken,accesstokensecret) "
            . "VALUES (?,?,?) ON DUPLICATE KEY UPDATE twitterid=?, accesstoken=?, accesstokensecret=?");
    return $stmt->execute([$response['user_id'], $response['oauth_token'], $response['oauth_token_secret'],
        $response['user_id'], $response['oauth_token'], $response['oauth_token_secret']]);
}

function checkRetweetRecordsInDB($usertwitterid, $echo = true) {
    $datelimit24hour = date("Y-m-d H:m:s", strtotime('-24 hours', time()));
    $datelimit1hour = date("Y-m-d H:m:s", strtotime('-1 hour', time()));
    $perdaylimit = 10;
    $perhourlimit = 2;
    $stmt = $GLOBALS['database_connection']->prepare("SELECT * FROM retweetrecords WHERE userid=? AND retweettime >= ?
              ORDER BY retweettime DESC LIMIT ?");
    $results = $stmt->execute([$usertwitterid, $datelimit24hour, $perdaylimit])->fetchAll();

    if (empty($results)) {
        return true;
    } else if (count($results) == $perdaylimit) {
        $timetoresetseconds = (60 * 60 * 24) - (time() - $results[9]['retweettime']);
        if ($echo) {
            echo encodeErrorInformation("Too many retweets in 24 hour period.", $timetoresetseconds);
        }
        return false;
    } else if (count($results) > 1) {
        $stmt = $GLOBALS['database_connection']->prepare("SELECT * FROM retweetrecords WHERE userid=? AND retweettime >= ? 
                  ORDER BY retweettime DESC LIMIT ?");
        $results = $stmt->execute([$usertwitterid, $datelimit1hour, $perhourlimit])->fetchAll();
        if (empty($results) || count($results) < $perhourlimit) {
            return true;
        } else {
            $timetoresetseconds = (60 * 60) - (time() - $results[1]['retweettime']);
            if ($echo) {
                echo encodeErrorInformation("Too many retweets in 1 hour period.", $timetoresetseconds);
            }
            return false;
        }
    }
}

function updateRetweetRecordsInDB($usertwitterid, $tweetid, $retweettime) {
    $stmt = $GLOBALS['database_connection']->prepare("INSERT INTO retweetrecords (userid,tweetid,retweettime) 
	VALUES (?,?,?)");
    $success = $stmt->execute([$usertwitterid, $tweetid, $retweettime]);
    return $success;
}

function encodeResponseInformation($connection, $response, $dbOpSuccessful = null) {
    $results['response'] = $response;
    $results['headers'] = $connection->getLastXHeaders();
    $results['httpcode'] = $connection->getLastHttpCode();
    if (!is_null($dbOpSuccessful)) {
        $results['dbop'] = $dbOpSuccessful;
    }
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
    $stmt = $GLOBALS['database_connection']->prepare("INSERT INTO ratelimitrecords (userid,endpoint,maxlimit,remaininglimit,resettime,timetoresetseconds) 
	VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE userid=?, endpoint=?, maxlimit=?, remaininglimit=?, resettime=?, timetoresetseconds=?");
    $success = $stmt->execute([$usertwitterid, $endpoint, $maxlimit, $remaininglimit, $resettime, $timetoresetseconds,
        $usertwitterid, $endpoint, $maxlimit, $remaininglimit, $resettime, $timetoresetseconds]);
    return $success;
}

function checkUserRateLimitInDB($usertwitterid, $endpoint, $echo = true) {
    $stmt = $GLOBALS['database_connection']->prepare("SELECT * FROM ratelimitrecords WHERE userid=? AND endpoint=?");
    $stmt->execute([$usertwitterid, $endpoint]);
    $result = $stmt->fetch();

    if ($result && $result['remaininglimit'] < 5) {
        if ($echo) {
            echo encodeErrorInformation("Rate limit reached.", $result['timetoresetseconds']);
        } else {
            return false;
        }
    } else {
        return true;
    }
}

function queryTwitterUserAuth($connection, $endpoint, $httpRequestType, $params, $userauthtwitterid, $paramsisjsondata = false,
        $echo = true) {
    if (!checkUserRateLimitInDB($userauthtwitterid, $endpoint)) {
        return false;
    }
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
        echo encodeResponseInformation($connection, $result);
    } else {
        return $result;
    }
}
