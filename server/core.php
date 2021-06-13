<?php

require "credentials/apikeys.php";
require "credentials/db.php";

$options = [
    \PDO::ATTR_DEFAULT_FETCH_MODE => \PDO::FETCH_ASSOC,
];

$database_connection = new PDO("mysql:host=$servername;dbname=$database;port=$port", $username, $password, $options);

function encodeResponseInformation($connection, $response) {
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
    $stmt = $GLOBALS['database_connection']->prepare("INSERT INTO ratelimitrecords (userid,endpoint,maxlimit,remaininglimit,resettime,timetoresetseconds) 
	VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE userid=?, endpoint=?, maxlimit=?, remaininglimit=?, resettime=?, timetoresetseconds=?");
    $success = $stmt->execute([$usertwitterid, $endpoint, $maxlimit, $remaininglimit, $resettime, $timetoresetseconds,
        $usertwitterid, $endpoint, $maxlimit, $remaininglimit, $resettime, $timetoresetseconds]);
    return $success;
}

function checkUserRateLimitInDB($usertwitterid, $endpoint) {
    $stmt = $GLOBALS['database_connection']->prepare("SELECT * FROM ratelimitrecords WHERE userid=? AND endpoint=?");
    $stmt->execute([$usertwitterid, $endpoint]);
    $result = $stmt->fetch();

    if ($result && $result['remaininglimit'] < 5) {
        echo encodeErrorInformation("Rate limit reached.", $result['timetoresetseconds']);
        return false;
    } else {
        return true;
    }
}

function queryTwitterUserAuth($connection, $endpoint, $httpRequestType, $params, $userauthtwitterid, $paramsisjsondata = false) {
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
    echo encodeResponseInformation($connection, $result);
}
