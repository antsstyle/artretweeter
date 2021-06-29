<?php

namespace ArtRetweeter;
require_once "core.php";

$request_method = filter_var(getenv('REQUEST_METHOD'), FILTER_SANITIZE_STRING);
$access_token = filter_input(INPUT_POST, 'access_token', FILTER_SANITIZE_STRING);
$access_token_secret = filter_input(INPUT_POST, 'access_token_secret', FILTER_SANITIZE_STRING);
$userAuthTwitterID = filter_input(INPUT_POST, 'user_auth_twitter_id', FILTER_SANITIZE_NUMBER_INT);

if ($request_method == "POST") {
    if (is_null($access_token) || is_null($access_token_secret) || is_null($userAuthTwitterID)) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $userAuth['twitter_id'] = $userAuthTwitterID;
    $userAuth['access_token'] = $access_token;
    $userAuth['access_token_secret'] = $access_token_secret;

    validateUserAuth($userAuth);

    removeAccount($userAuthTwitterID);
}