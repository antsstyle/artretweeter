<?php

namespace ArtRetweeter;

require_once "core.php";

use Abraham\TwitterOAuth\TwitterOAuth;

function oauthAccessToken() {
    $oauth_token = filter_input(INPUT_POST, 'oauth_token', FILTER_SANITIZE_STRING);
    $oauth_verifier = filter_input(INPUT_POST, 'oauth_verifier', FILTER_SANITIZE_STRING);
    if (!$oauth_token || !$oauth_verifier) {
        echo encodeStatusInformation(StatusCodes::INVALID_INPUT, "Parameters are not set correctly.");
        exit;
    }

    $params['oauth_verifier'] = $oauth_verifier;
    $params['oauth_token'] = $oauth_token;
    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret']);
    $response = $connection->oauth("oauth/access_token", $params);
    if (isset($response['oauth_token'])) {
        $dbop = updateAccessToken($response);
    }
    echo encodeTwitterResponseInformation($connection, $response);
}

function oauthRequestToken() {
    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret']);
    $response = $connection->oauth("oauth/request_token", ["oauth_callback" => "oob"]);
    echo encodeTwitterResponseInformation($connection, $response);
}

function oauthAuthorize() {
    $oauth_token = filter_input(INPUT_POST, 'oauth_token', FILTER_SANITIZE_STRING);
    if (!$oauth_token) {
        echo encodeStatusInformation(StatusCodes::INVALID_INPUT, "Parameters are not set correctly.");
        exit;
    }
    $oauth_token_array['oauth_token'] = $oauth_token;
    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],);
    $response = $connection->url("oauth/authorize", $oauth_token_array);
    echo encodeTwitterResponseInformation($connection, $response);
}

function oauthInvalidateToken($userAuth) {
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id']) {
        echo encodeStatusInformation(StatusCodes::INVALID_INPUT, "Parameters are not set correctly.");
        exit;
    }
    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    $response = $connection->oauth("oauth/invalidate_token");
    echo encodeTwitterResponseInformation($connection, $response);
}
