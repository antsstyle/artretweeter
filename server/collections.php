<?php

namespace ArtRetweeter;

require_once "core.php";

use Abraham\TwitterOAuth\TwitterOAuth;

function collectionsDestroy($userAuth) {
    $collection_id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_STRING);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$collection_id || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['id'] = $collection_id;

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "collections/destroy", "POST", $params, $userAuth);
}

function collectionsCreate($userAuth) {
    $name = filter_input(INPUT_POST, 'name', FILTER_SANITIZE_STRING);
    $description = filter_input(INPUT_POST, 'description', FILTER_SANITIZE_STRING);
    $timeline_order = filter_input(INPUT_POST, 'timeline_order', FILTER_SANITIZE_STRING);

    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$name || !$userAuth['twitter_id'] || !$timeline_order || !$description) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['name'] = $name;
    $params['description'] = $description;
    $params['timeline_order'] = $timeline_order;

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "collections/create", "POST", $params, $userAuth);
}

function collectionsShow($userAuth) {
    $collection_id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_STRING);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$collection_id || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['id'] = $collection_id;

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "collections/show", "GET", $params, $userAuth);
}

function collectionsEntriesCurate($userAuth) {
    $json_data = json_decode(filter_input(INPUT_POST, 'json_data', FILTER_SANITIZE_URL), true);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$json_data || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "collections/entries/curate", "POST", $json_data, $userAuth, true);
}

function collectionsEntriesAdd($userAuth) {
    $collection_id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_STRING);
    $tweet_id = filter_input(INPUT_POST, 'tweet_id', FILTER_SANITIZE_NUMBER_INT);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$collection_id || !$userAuth['twitter_id'] || !$tweet_id) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['id'] = $collection_id;
    $params['tweet_id'] = $tweet_id;

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "collections/entries/add", "POST", $params, $userAuth);
}

function collectionsEntriesRemove($userAuth) {
    $collection_id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_STRING);
    $tweet_id = filter_input(INPUT_POST, 'tweet_id', FILTER_SANITIZE_NUMBER_INT);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$collection_id || !$userAuth['twitter_id'] || !$tweet_id) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['id'] = $collection_id;
    $params['tweet_id'] = $tweet_id;

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "collections/entries/remove", "POST", $params, $userAuth);
}

function collectionsEntriesMove($userAuth) {
    $collection_id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_STRING);
    $tweet_id = filter_input(INPUT_POST, 'tweet_id', FILTER_SANITIZE_NUMBER_INT);
    $relative_to_tweet_id = filter_input(INPUT_POST, 'relative_to', FILTER_SANITIZE_NUMBER_INT);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$collection_id || !$userAuth['twitter_id'] || !$tweet_id
            || !$relative_to_tweet_id) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['id'] = $collection_id;
    $params['tweet_id'] = $tweet_id;
    $params['relative_to'] = $relative_to_tweet_id;

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "collections/entries/move", "POST", $params, $userAuth);
}

function collectionsEntries($userAuth) {
    $id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_STRING);
    $max_position = filter_input(INPUT_POST, 'max_position', FILTER_SANITIZE_NUMBER_INT);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$id || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['count'] = 200;
    $params['id'] = $id;

    if ($max_position) {
        $params['max_position'] = $max_position;
    }

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "collections/entries", "GET", $params, $userAuth);
}

function collectionsList($userAuth) {
    $user_id = filter_input(INPUT_POST, 'user_id', FILTER_SANITIZE_STRING);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$user_id || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    $params['user_id'] = $user_id;
    $params['count'] = 200;

    $connection = new TwitterOAuth($GLOBALS['consumer_key'], $GLOBALS['consumer_secret'],
            $userAuth['access_token'], $userAuth['access_token_secret']);
    queryTwitterUserAuth($connection, "collections/list", "GET", $params, $userAuth);
}