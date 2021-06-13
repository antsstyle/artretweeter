<?php

require "core.php";
require "twitteroauth/vendor/autoload.php";

use Abraham\TwitterOAuth\TwitterOAuth;

$request_method = filter_var(getenv('REQUEST_METHOD'), FILTER_SANITIZE_STRING);
$access_token = filter_input(INPUT_POST, 'access_token', FILTER_SANITIZE_STRING);
$access_token_secret = filter_input(INPUT_POST, 'access_token_secret', FILTER_SANITIZE_STRING);
$json_data = json_decode(filter_input(INPUT_POST, 'json_data', FILTER_SANITIZE_URL), true);
$userauthtwitterid = filter_input(INPUT_POST, 'user_auth_twitter_id', FILTER_SANITIZE_NUMBER_INT);
$endpoint = "collections/entries/curate";

if ($request_method == "POST") {
    if (is_null($access_token) || is_null($access_token_secret) || is_null($json_data) || is_null($userauthtwitterid)) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $connection = new TwitterOAuth($consumerkey, $consumersecret, $access_token, $access_token_secret);
    queryTwitterUserAuth($connection, $endpoint, "POST", $json_data, $userauthtwitterid, true);
}