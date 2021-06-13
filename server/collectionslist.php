<?php

require "core.php";
require "twitteroauth/vendor/autoload.php";

use Abraham\TwitterOAuth\TwitterOAuth;

$request_method = filter_var(getenv('REQUEST_METHOD'), FILTER_SANITIZE_STRING);
$access_token = filter_input(INPUT_POST, 'access_token', FILTER_SANITIZE_STRING);
$access_token_secret = filter_input(INPUT_POST, 'access_token_secret', FILTER_SANITIZE_STRING);
$user_id = filter_input(INPUT_POST, 'user_id', FILTER_SANITIZE_STRING);
$userauthtwitterid = filter_input(INPUT_POST, 'user_auth_twitter_id', FILTER_SANITIZE_NUMBER_INT);
$endpoint = "collections/list";

if ($request_method == "POST") {
    if (is_null($access_token) || is_null($access_token_secret) || is_null($user_id) || is_null($userauthtwitterid)) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['user_id'] = $user_id;
    $params['count'] = 200;

    $connection = new TwitterOAuth($consumerkey, $consumersecret, $access_token, $access_token_secret);
    queryTwitterUserAuth($connection, $endpoint, "GET", $params, $userauthtwitterid);
}
