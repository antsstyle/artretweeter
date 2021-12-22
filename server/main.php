<?php

require __DIR__ . '/vendor/autoload.php';
;

use Antsstyle\ArtRetweeter\Core\Accounts;
use Antsstyle\ArtRetweeter\Core\Collections;
use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\OAuth;
use Antsstyle\ArtRetweeter\Core\StatusCode;
use Antsstyle\ArtRetweeter\Core\Statuses;

class Main {

    public static function processRequest() {

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
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Invalid request type.");
            exit();
        }

        if (!$artretweeter_endpoint && !$twitter_endpoint) {
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "No endpoint was specified.");
            exit();
        }

        if ($artretweeter_endpoint) {
            switch ($artretweeter_endpoint) {
                case "accounts/commitautomationsettings":
                    Accounts::commitAutomationSettings($userAuth);
                    break;
                case "accounts/getautomationsettings":
                    Accounts::getAutomationSettings($userAuth);
                    break;
                case "accounts/remove":
                    Accounts::removeAccount($userAuth);
                    break;
                case "retweets/queuestatus":
                    Statuses::getQueueStatus($userAuth);
                    break;
                case "retweets/queue":
                    Statuses::queueRetweet($userAuth);
                    break;
                case "retweets/tweetretweetstatus":
                    Statuses::tweetRetweetStatus($userAuth);
                    break;
                case "retweets/unqueue":
                    Statuses::unqueueRetweet($userAuth);
                    break;
                case "tweets/deletemultipletweets":
                    Statuses::deleteMultipleTweets($userAuth);
                    break;
                case "tweets/deletetweet":
                    Statuses::deleteTweet($userAuth);
                    break;
                case "tweets/getstoredtweetids":
                    Statuses::getStoredTweetsInDB($userAuth);
                    break;
            }
            exit();
        }

        switch ($twitter_endpoint) {
            case "collections/create":
                Collections::collectionsCreate($userAuth);
                break;
            case "collections/destroy":
                Collections::collectionsDestroy($userAuth);
                break;
            case "collections/entries":
                Collections::collectionsEntries($userAuth);
                break;
            case "collections/entries/add":
                Collections::collectionsEntriesAdd($userAuth);
                break;
            case "collections/entries/curate":
                Collections::collectionsEntriesCurate($userAuth);
                break;
            case "collections/entries/move":
                Collections::collectionsEntriesMove($userAuth);
                break;
            case "collections/entries/remove":
                Collections::collectionsEntriesRemove($userAuth);
                break;
            case "collections/list":
                Collections::collectionsList($userAuth);
                break;
            case "collections/show":
                Collections::collectionsShow($userAuth);
                break;
            case "oauth/access_token":
                OAuth::oauthAccessToken();
                break;
            case "oauth/authorize":
                OAuth::oauthAuthorize();
                break;
            case "oauth/invalidate_token":
                OAuth::oauthInvalidateToken($userAuth);
                break;
            case "oauth/request_token":
                OAuth::oauthRequestToken();
                break;
            case "statuses/destroy":
                Statuses::statusesDestroy($userAuth);
                break;
            case "statuses/lookup":
                Statuses::statusesLookup($userAuth);
                break;
            case "statuses/show":
                $id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_NUMBER_INT);
                if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$id || !$userAuth['twitter_id']) {
                    echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Parameters are not set correctly.");
                    exit;
                }
                Statuses::statusesShow($userAuth, $id);
                break;
            case "statuses/unretweet":
                Statuses::statusesUnretweet($userAuth);
                break;
            case "statuses/user_timeline":
                Statuses::statusesUserTimeline($userAuth);
                break;
            default:
                echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Invalid endpoint, or endpoint not supported.");
                break;
        }
    }

}

Main::processRequest();
