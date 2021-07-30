<?php

namespace ArtRetweeter;

require_once "core.php";

use Abraham\TwitterOAuth\TwitterOAuth;

function getQueueStatus($userAuth) {
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }
    validateUserAuth($userAuth);
    getQueueStatusInDB($userAuth['twitter_id']);
}

function tweetRetweetStatus($userAuth) {
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }
    validateUserAuth($userAuth);
    getTweetRetweetStatusInDB($userAuth['twitter_id']);
}

function statusesShow($userAuth) {
    $id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_NUMBER_INT);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$id || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['id'] = $id;
    $params['tweet_mode'] = "extended";

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "statuses/show", "GET", $params, $userAuth);
}

function statusesUserTimeline($userAuth) {
    $screen_name = filter_input(INPUT_POST, 'screen_name', FILTER_SANITIZE_STRING);
    $max_id = filter_input(INPUT_POST, 'max_id', FILTER_SANITIZE_NUMBER_INT);
    $since_id = filter_input(INPUT_POST, 'since_id', FILTER_SANITIZE_NUMBER_INT);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$screen_name || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['count'] = 200;
    $params['screen_name'] = $screen_name;
    $params['include_rts'] = "false";
    $params['trim_user'] = "true";
    $params['tweet_mode'] = "extended";

    if (!is_null($max_id)) {
        $params['max_id'] = $max_id;
    }
    if (!is_null($since_id)) {
        $params['since_id'] = $since_id;
    }

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "statuses/user_timeline", "GET", $params, $userAuth);
}

function queueRetweet($userAuth) {
    $tweetID = filter_input(INPUT_POST, 'tweetid', FILTER_SANITIZE_NUMBER_INT);
    $retweetTime = filter_input(INPUT_POST, 'retweettime', FILTER_SANITIZE_NUMBER_INT);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$tweetID || !$userAuth['twitter_id'] || !$retweetTime) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }
    validateUserAuth($userAuth);
    $timeNow = strtotime('+1 hour', time());
    if ($timeNow > $retweetTime) {
        echo encodeErrorInformation("Timestamp is not within valid range.");
        exit;
    }

    queueRetweetInDB($userAuth['twitter_id'], $tweetID, $retweetTime);
}

function unqueueRetweet($userAuth) {
    $tweetID = filter_input(INPUT_POST, 'tweetid', FILTER_SANITIZE_NUMBER_INT);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$tweetID || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }
    validateUserAuth($userAuth);
    unqueueRetweetFromDB($userAuth['twitter_id'], $tweetID);
}

function statusesUnretweet($userAuth) {
    $id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_NUMBER_INT);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$id || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    checkUserCanQueueNewRetweet($userAuth['twitter_id']);
    $params['id'] = $id;
    $params['trim_user'] = 1;

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "statuses/unretweet", "POST", $params, $userAuth);
}

function statusesLookup($userAuth) {
    $id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_STRING);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$id || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['id'] = $id;
    $params['tweet_mode'] = "extended";

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "statuses/lookup", "GET", $params, $userAuth);
}

function statusesDestroy($userAuth) {
    $id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_STRING);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$id || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['id'] = $id;

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "statuses/destroy", "POST", $params, $userAuth);
}
