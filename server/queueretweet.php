<?php

require "core.php";

$request_method = filter_var(getenv('REQUEST_METHOD'), FILTER_SANITIZE_STRING);
$access_token = filter_input(INPUT_POST, 'access_token', FILTER_SANITIZE_STRING);
$access_token_secret = filter_input(INPUT_POST, 'access_token_secret', FILTER_SANITIZE_STRING);
$userauthtwitterid = filter_input(INPUT_POST, 'user_auth_twitter_id', FILTER_SANITIZE_NUMBER_INT);
$tweetid = filter_input(INPUT_POST, 'tweetid', FILTER_SANITIZE_NUMBER_INT);

if ($request_method == "POST") {
    if (is_null($access_token) || is_null($access_token_secret) || is_null($userauthtwitterid)
            || is_null($tweetid)) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    if (!validateAccessToken($userauthtwitterid, $accesstoken, $accesstokensecret)) {
        echo encodeErrorInformation("Invalid access token for this user ID.");
        exit;
    }

    unqueueRetweet($userauthtwitterid, $tweetid);
}