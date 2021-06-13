<?php

require "credentials/apikeys.php";
require "credentials/db.php";
require "core.php";
require "twitteroauth/vendor/autoload.php";

use Abraham\TwitterOAuth\TwitterOAuth;

$request_method = filter_var(getenv('REQUEST_METHOD'), FILTER_SANITIZE_STRING);
$oauth_token = filter_input(INPUT_POST, 'oauth_token', FILTER_SANITIZE_STRING);

if ($request_method == "POST") {
    if (is_null($oauth_token)) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }
    $oauth_token_array['oauth_token'] = $oauth_token;
    $connection = new TwitterOAuth($consumerkey, $consumersecret);
    $response = $connection->url("oauth/authorize", $oauth_token_array);
    echo encodeResponseInformation($connection, $response);
}