<?php

require_once "accounts.php";
require_once "collections.php";
require_once "core.php";
require_once "oauth.php";
require_once "statuses.php";

$request_method = filter_var(getenv('REQUEST_METHOD'), FILTER_SANITIZE_STRING);
$access_token = filter_input(INPUT_POST, 'access_token', FILTER_SANITIZE_STRING);
$access_token_secret = filter_input(INPUT_POST, 'access_token_secret', FILTER_SANITIZE_STRING);
$userAuthTwitterID = filter_input(INPUT_POST, 'user_auth_twitter_id', FILTER_SANITIZE_NUMBER_INT);
$twitter_endpoint = filter_input(INPUT_POST, 'twitter_endpoint', FILTER_SANITIZE_STRING);
$artretweeter_endpoint = filter_input(INPUT_POST, 'artretweeter_endpoint', FILTER_SANITIZE_STRING);

$userAuth['twitter_id'] = $userAuthTwitterID;
$userAuth['access_token'] = $access_token;
$userAuth['access_token_secret'] = $access_token_secret;

if ($request_method != "POST") {
    echo ArtRetweeter\encodeErrorInformation("Invalid request type.");
    exit();
}

if (!$artretweeter_endpoint && !$twitter_endpoint) {
    echo ArtRetweeter\encodeErrorInformation("No endpoint was specified.");
    exit();
}

if ($artretweeter_endpoint) {
    switch ($artretweeter_endpoint) {
        case "accounts/remove":
            ArtRetweeter\removeAccount($userAuth);
            break;
        case "retweets/queuestatus":
            ArtRetweeter\getQueueStatus($userAuth);
            break;
        case "retweets/queue":
            ArtRetweeter\queueRetweet($userAuth);
            break;
        case "retweets/tweetretweetstatus":
            ArtRetweeter\tweetRetweetStatus($userAuth);
            break;
        case "retweets/unqueue":
            ArtRetweeter\unqueueRetweet($userAuth);
            break;
        case "tweets/deletetweet":
            ArtRetweeter\deleteTweet($userAuth);
            break;
    }
    exit();
}

switch ($twitter_endpoint) {
    case "collections/create":
        ArtRetweeter\collectionsCreate($userAuth);
        break;
    case "collections/destroy":
        ArtRetweeter\collectionsDestroy($userAuth);
        break;
    case "collections/entries":
        ArtRetweeter\collectionsEntries($userAuth);
        break;
    case "collections/entries/add":
        ArtRetweeter\collectionsEntriesAdd($userAuth);
        break;
    case "collections/entries/curate":
        ArtRetweeter\collectionsEntriesCurate($userAuth);
        break;
    case "collections/entries/move":
        ArtRetweeter\collectionsEntriesMove($userAuth);
        break;
    case "collections/entries/remove":
        ArtRetweeter\collectionsEntriesRemove($userAuth);
        break;
    case "collections/list":
        ArtRetweeter\collectionsList($userAuth);
        break;
    case "collections/show":
        ArtRetweeter\collectionsShow($userAuth);
        break;
    case "oauth/access_token":
        ArtRetweeter\oauthAccessToken();
        break;
    case "oauth/authorize":
        ArtRetweeter\oauthAuthorize();
        break;
    case "oauth/invalidate_token":
        ArtRetweeter\oauthInvalidateToken($userAuth);
        break;
    case "oauth/request_token":
        ArtRetweeter\oauthRequestToken();
        break;
    case "statuses/destroy":
        ArtRetweeter\statusesDestroy($userAuth);
        break;
    case "statuses/lookup":
        ArtRetweeter\statusesLookup($userAuth);
        break;
    case "statuses/show":
        ArtRetweeter\statusesShow($userAuth);
        break;
    case "statuses/unretweet":
        ArtRetweeter\statusesUnretweet($userAuth);
        break;
    case "statuses/user_timeline":
        ArtRetweeter\statusesUserTimeline($userAuth);
        break;
    default:
        echo ArtRetweeter\encodeErrorInformation("Invalid endpoint, or endpoint not supported.");
        break;
}