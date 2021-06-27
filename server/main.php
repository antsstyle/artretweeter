<?php

require_once "accounts.php";
require_once "collections.php";
require_once "core.php";
require_once "oauth.php";
require_once "statuses.php";

$request_method = filter_var(getenv('REQUEST_METHOD'), FILTER_SANITIZE_STRING);
$access_token = filter_input(INPUT_POST, 'access_token', FILTER_SANITIZE_STRING);
$access_token_secret = filter_input(INPUT_POST, 'access_token_secret', FILTER_SANITIZE_STRING);
$userauthtwitterid = filter_input(INPUT_POST, 'user_auth_twitter_id', FILTER_SANITIZE_NUMBER_INT);
$twitter_endpoint = filter_input(INPUT_POST, 'twitter_endpoint', FILTER_SANITIZE_STRING);
$artretweeter_endpoint = filter_input(INPUT_POST, 'artretweeter_endpoint', FILTER_SANITIZE_STRING);

$userauth['twitter_id'] = $userauthtwitterid;
$userauth['access_token'] = $access_token;
$userauth['access_token_secret'] = $access_token_secret;

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
            ArtRetweeter\removeAccount($userauth);
            break;
        case "retweets/queuestatus":
            ArtRetweeter\getQueueStatus($userauth);
            break;
        case "retweets/queue":
            ArtRetweeter\queueRetweet($userauth);
            break;
        case "retweets/unqueue":
            ArtRetweeter\unqueueRetweet($userauth);
            break;
    }
    exit();
}

switch ($twitter_endpoint) {
    case "collections/create":
        ArtRetweeter\collectionsCreate($userauth);
        break;
    case "collections/destroy":
        ArtRetweeter\collectionsDestroy($userauth);
        break;
    case "collections/entries":
        ArtRetweeter\collectionsEntries($userauth);
        break;
    case "collections/entries/add":
        ArtRetweeter\collectionsEntriesAdd($userauth);
        break;
    case "collections/entries/curate":
        ArtRetweeter\collectionsEntriesCurate($userauth);
        break;
    case "collections/entries/move":
        ArtRetweeter\collectionsEntriesMove($userauth);
        break;
    case "collections/list":
        ArtRetweeter\collectionsList($userauth);
        break;
    case "collections/show":
        ArtRetweeter\collectionsShow($userauth);
        break;
    case "oauth/access_token":
        ArtRetweeter\oauthAccessToken();
        break;
    case "oauth/authorize":
        ArtRetweeter\oauthAuthorize();
        break;
    case "oauth/invalidate_token":
        ArtRetweeter\oauthInvalidateToken($userauth);
        break;
    case "oauth/request_token":
        ArtRetweeter\oauthRequestToken();
        break;
    case "statuses/lookup":
        ArtRetweeter\statusesLookup($userauth);
        break;
    case "statuses/retweet":
        ArtRetweeter\statusesRetweet($userauth);
        break;
    case "statuses/show":
        ArtRetweeter\statusesShow($userauth);
        break;
    case "statuses/unretweet":
        ArtRetweeter\statusesUnretweet($userauth);
        break;
    case "statuses/user_timeline":
        ArtRetweeter\statusesUserTimeline($userauth);
        break;
    default:
        echo ArtRetweeter\encodeErrorInformation("Invalid endpoint, or endpoint not supported.");
        break;
}