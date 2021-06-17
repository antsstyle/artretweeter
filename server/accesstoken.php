<?php

require "core.php";
require "twitteroauth/vendor/autoload.php";

use Abraham\TwitterOAuth\TwitterOAuth;

$request_method = filter_var(getenv('REQUEST_METHOD'), FILTER_SANITIZE_STRING);
$oauth_token = filter_input(INPUT_POST, 'oauth_token', FILTER_SANITIZE_STRING);
$oauth_verifier = filter_input(INPUT_POST, 'oauth_verifier', FILTER_SANITIZE_STRING);

if ($request_method == "POST") {
    if (is_null($oauth_token) || is_null($oauth_verifier)) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['oauth_verifier'] = $oauth_verifier;
    $params['oauth_token'] = $oauth_token;
    $connection = new TwitterOAuth($consumerkey, $consumersecret);
    $response = $connection->oauth("oauth/access_token", $params);
    if (isset($response['oauth_token'])) {
        $dbop = updateAccessToken($response);
    }
    echo encodeTwitterResponseInformation($connection, $response, $dbop);
}