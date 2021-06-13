<?php

require "credentials/apikeys.php";
require "credentials/db.php";
require "core.php";
require "twitteroauth/vendor/autoload.php";

use Abraham\TwitterOAuth\TwitterOAuth;

$request_method = filter_var(getenv('REQUEST_METHOD'), FILTER_SANITIZE_STRING);
$access_token = filter_input(INPUT_POST, 'access_token', FILTER_SANITIZE_STRING);
$access_token_secret = filter_input(INPUT_POST, 'access_token_secret', FILTER_SANITIZE_STRING);
$name = filter_input(INPUT_POST, 'name', FILTER_SANITIZE_STRING);
$description = filter_input(INPUT_POST, 'description', FILTER_SANITIZE_STRING);
$timeline_order = filter_input(INPUT_POST, 'timeline_order', FILTER_SANITIZE_STRING);
$userauthtwitterid = filter_input(INPUT_POST, 'user_auth_twitter_id', FILTER_SANITIZE_NUMBER_INT);
$endpoint = "collections/create";

if ($request_method == "POST") {
    if (is_null($access_token) || is_null($access_token_secret) || is_null($name) || is_null($userauthtwitterid)) {
        echo encodeErrorInformation("One or more parameters are not set correctly.");
        exit;
    }

    $params['name'] = $name;

    if (!is_null($description)) {
        $params['description'] = $description;
    }

    if (!is_null($timeline_order)) {
        $params['timeline_order'] = $timeline_order;
    } else {
        $params['timeline_order'] = "curation_reverse_chron";
    }

    $connection = new TwitterOAuth($consumerkey, $consumersecret, $access_token, $access_token_secret);
    queryTwitterUserAuth($connection, $endpoint, "POST", $params, $userauthtwitterid);
}