<?php

namespace ArtRetweeter;

require_once "core.php";

use Abraham\TwitterOAuth\TwitterOAuth;

function getQueueStatus($userauth) {
    if (!$userauth['access_token'] || !$userauth['access_token_secret'] || !$userauth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }
    validateUserAuth($userauth);
    getQueueStatusInDB($userauth['twitter_id']);
}

function statusesShow($userauth) {
    $id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_NUMBER_INT);
    if (!$userauth['access_token'] || !$userauth['access_token_secret'] || !$id || !$userauth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['id'] = $id;
    $params['tweet_mode'] = "extended";

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userauth['access_token'], $userauth['access_token_secret']);
    queryTwitterUserAuth($connection, "statuses/show", "GET", $params, $userauth);
}

function statusesUserTimeline($userauth) {
    $screen_name = filter_input(INPUT_POST, 'screen_name', FILTER_SANITIZE_STRING);
    $max_id = filter_input(INPUT_POST, 'max_id', FILTER_SANITIZE_NUMBER_INT);
    if (!$userauth['access_token'] || !$userauth['access_token_secret'] || !$screen_name || !$userauth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['count'] = 200;
    $params['screen_name'] = $screen_name;

    if (!is_null($max_id)) {
        $params['max_id'] = $max_id;
    }

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userauth['access_token'], $userauth['access_token_secret']);
    queryTwitterUserAuth($connection, "statuses/user_timeline", "GET", $params, $userauth);
}

function queueRetweet($userauth) {
    $tweet_id = filter_input(INPUT_POST, 'tweetid', FILTER_SANITIZE_NUMBER_INT);
    $retweettime = filter_input(INPUT_POST, 'retweettime', FILTER_SANITIZE_NUMBER_INT);
    if (!$userauth['access_token'] || !$userauth['access_token_secret'] || !$tweet_id || !$userauth['twitter_id'] || !$retweettime) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }
    validateUserAuth($userauth);
    $timeNow = strtotime('+1 hour', time());
    if ($timeNow > $retweettime) {
        echo encodeErrorInformation("Timestamp is not within valid range.");
        exit;
    }

    queueRetweetInDB($userauth['twitter_id'], $tweet_id, $retweettime);
}

function unqueueRetweet($userauth) {
    $tweet_id = filter_input(INPUT_POST, 'tweetid', FILTER_SANITIZE_NUMBER_INT);
    if (!$userauth['access_token'] || !$userauth['access_token_secret'] || !$tweet_id || !$userauth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }
    validateUserAuth($userauth);
    unqueueRetweetFromDB($userauth['twitter_id'], $tweet_id);
}

function statusesUnretweet($userauth) {
    $id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_NUMBER_INT);
    if (!$userauth['access_token'] || !$userauth['access_token_secret'] || !$id || !$userauth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    checkRetweetRecordsInDB($userauth['twitter_id']);
    $params['id'] = $id;
    $params['trim_user'] = 1;

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userauth['access_token'], $userauth['access_token_secret']);
    queryTwitterUserAuth($connection, "statuses/unretweet", "POST", $params, $userauth);
}

function statusesRetweet($userauth) {
    $id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_NUMBER_INT);
    if (!$userauth['access_token'] || !$userauth['access_token_secret'] || !$id || !$userauth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $retweetrecordresults = checkRetweetRecordsInDB($userauth['twitter_id']);
    if (!$retweetrecordresults) {
        exit;
    }

    $params['id'] = $id;
    $params['trim_user'] = 1;

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userauth['access_token'], $userauth['access_token_secret']);
    queryTwitterUserAuth($connection, "statuses/retweet", "POST", $params, $userauth);
}

function statusesLookup($userauth) {
    $id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_STRING);
    if (!$userauth['access_token'] || !$userauth['access_token_secret'] || !$id || !$userauth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['id'] = $id;
    $params['tweet_mode'] = "extended";

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userauth['access_token'], $userauth['access_token_secret']);
    queryTwitterUserAuth($connection, "statuses/lookup", "GET", $params, $userauth);
}
