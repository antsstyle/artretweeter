<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Abraham\TwitterOAuth\TwitterOAuth;

class OAuth {

    function oauthAccessToken() {
        $oauth_token = filter_input(INPUT_POST, 'oauth_token', FILTER_SANITIZE_STRING);
        $oauth_verifier = filter_input(INPUT_POST, 'oauth_verifier', FILTER_SANITIZE_STRING);
        if (!$oauth_token || !$oauth_verifier) {
            echo Core::encodeStatusInformation(StatusCodes::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }

        $params['oauth_verifier'] = $oauth_verifier;
        $params['oauth_token'] = $oauth_token;
        $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret);
        $response = $connection->oauth("oauth/access_token", $params);
        if (isset($response['oauth_token'])) {
            $dbop = Core::updateAccessToken($response);
        }
        echo Core::encodeTwitterResponseInformation($connection, $response);
    }

    function oauthRequestToken() {
        $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret);
        $response = $connection->oauth("oauth/request_token", ["oauth_callback" => "oob"]);
        echo Core::encodeTwitterResponseInformation($connection, $response);
    }

    function oauthAuthorize() {
        $oauth_token = filter_input(INPUT_POST, 'oauth_token', FILTER_SANITIZE_STRING);
        if (!$oauth_token) {
            echo Core::encodeStatusInformation(StatusCodes::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }
        $oauth_token_array['oauth_token'] = $oauth_token;
        $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret,);
        $response = $connection->url("oauth/authorize", $oauth_token_array);
        echo Core::encodeTwitterResponseInformation($connection, $response);
    }

    function oauthInvalidateToken($userAuth) {
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id']) {
            echo Core::encodeStatusInformation(StatusCodes::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }
        $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret,
                $userAuth['access_token'], $userAuth['access_token_secret']);
        $response = $connection->oauth("oauth/invalidate_token");
        echo Core::encodeTwitterResponseInformation($connection, $response);
    }

}
