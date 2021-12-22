<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\CoreDB;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Abraham\TwitterOAuth\TwitterOAuth;

class Statuses {

    public static function getStoredTweetsInDB($userAuth) {
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id']) {
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }
        Core::validateUserAuth($userAuth);
        Core::getTweetIDsForUser($userAuth['twitter_id']);
    }

    public static function retrieveTweetMetrics($userAuth) {
        $nextcursor = filter_input(INPUT_POST, 'nextcursor', FILTER_SANITIZE_NUMBER_INT);
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id'] || !$nextcursor) {
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }
        $metricsInDB = getTweetMetricsToRefresh($userAuth['twitter_id'], $nextcursor, "Latest Metrics");
        if (!$metricsInDB) {
            echo Core::encodeStatusInformation(StatusCode::ARTRETWEETER_DATABASE_ERROR, "A server error was encountered attempting to retrieve tweet metrics.");
            exit;
        }
        $idString = $metricsInDB['idstring'];
        if (!$idString) {
            
        } else {
            $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret,
                    $userAuth['access_token'], $userAuth['access_token_secret']);
            $params['id'] = $idString;
            $results = Core::queryTwitterUserAuth($connection, "statuses/lookup", "GET", $params, $userAuth, false, false);
            if ($connection->getLastHttpCode() == 200) {
                Core::insertTweetsAndMetrics($results, $userAuth['twitter_id']);
            }
        }
    }

    public static function getQueueStatus($userAuth) {
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id']) {
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }
        Core::validateUserAuth($userAuth);
        Core::getQueueStatusInDB($userAuth['twitter_id']);
    }

    public static function tweetRetweetStatus($userAuth) {
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id']) {
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }
        Core::validateUserAuth($userAuth);
        Core::getTweetRetweetStatusInDB($userAuth['twitter_id']);
    }

    public static function statusesShow($userAuth, $id, $echoAndExit = true) {
        $params['id'] = $id;
        $params['tweet_mode'] = "extended";

        $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret,
                $userAuth['access_token'], $userAuth['access_token_secret']);
        $results = Core::queryTwitterUserAuth($connection, "statuses/show", "GET", $params, $userAuth, false, false);
        if ($connection->getLastHttpCode() == 200) {
            Core::insertTweetsAndMetrics($results, $userAuth['twitter_id']);
        }
        if ($echoAndExit) {
            echo Core::encodeTwitterResponseInformation($connection, $results);
            exit();
        }
        return $results;
    }

    public static function statusesUserTimeline($userAuth) {
        $screen_name = filter_input(INPUT_POST, 'screen_name', FILTER_SANITIZE_STRING);
        $max_id = filter_input(INPUT_POST, 'max_id', FILTER_SANITIZE_NUMBER_INT);
        $since_id = filter_input(INPUT_POST, 'since_id', FILTER_SANITIZE_NUMBER_INT);
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$screen_name || !$userAuth['twitter_id']) {
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Parameters are not set correctly.");
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

        $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret,
                $userAuth['access_token'], $userAuth['access_token_secret']);
        $results = Core::queryTwitterUserAuth($connection, "statuses/user_timeline", "GET", $params, $userAuth, false, false);
        if ($connection->getLastHttpCode() == 200) {
            Core::insertTweetsAndMetrics($results, $userAuth['twitter_id']);
        }
        echo Core::encodeTwitterResponseInformation($connection, $results);
        exit();
    }

    public static function queueRetweet($userAuth) {
        $tweetID = filter_input(INPUT_POST, 'tweetid', FILTER_SANITIZE_NUMBER_INT);
        $retweetTime = filter_input(INPUT_POST, 'retweettime', FILTER_SANITIZE_NUMBER_INT);
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$tweetID || !$userAuth['twitter_id'] || !$retweetTime) {
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }
        Core::validateUserAuth($userAuth);
        $timeNow = strtotime('+1 hour', time());
        if ($timeNow > $retweetTime) {
            echo Core::encodeStatusInformation("Timestamp is not within valid range.");
            exit;
        }

        Core::queueRetweetInDB($userAuth['twitter_id'], $tweetID, $retweetTime);
    }

    public static function unqueueRetweet($userAuth) {
        $tweetID = filter_input(INPUT_POST, 'tweetid', FILTER_SANITIZE_NUMBER_INT);
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$tweetID || !$userAuth['twitter_id']) {
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }
        Core::validateUserAuth($userAuth);
        Core::unqueueRetweetFromDB($userAuth['twitter_id'], $tweetID);
    }

    public static function statusesUnretweet($userAuth) {
        $id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_NUMBER_INT);
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$id || !$userAuth['twitter_id']) {
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }

        Core::checkUserCanQueueNewRetweet($userAuth['twitter_id']);
        $params['id'] = $id;
        $params['trim_user'] = 1;

        $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret,
                $userAuth['access_token'], $userAuth['access_token_secret']);
        Core::queryTwitterUserAuth($connection, "statuses/unretweet", "POST", $params, $userAuth);
    }

    public static function statusesLookup($userAuth) {
        $id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_STRING);
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$id || !$userAuth['twitter_id']) {
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }

        $params['id'] = $id;
        $params['tweet_mode'] = "extended";

        $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret,
                $userAuth['access_token'], $userAuth['access_token_secret']);
        $results = Core::queryTwitterUserAuth($connection, "statuses/lookup", "POST", $params, $userAuth, false, false);
        if ($connection->getLastHttpCode() == 200) {
            Core::insertTweetsAndMetrics($results, $userAuth['twitter_id']);
        }
        echo Core::encodeTwitterResponseInformation($connection, $results);
        exit();
    }

    function statusesDestroy($userAuth) {
        $id = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_STRING);
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$id || !$userAuth['twitter_id']) {
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }

        $params['id'] = $id;

        $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret,
                $userAuth['access_token'], $userAuth['access_token_secret']);
        Core::queryTwitterUserAuth($connection, "statuses/destroy", "POST", $params, $userAuth);
    }

    public static function deleteTweet($userAuth) {
        $tweetid = filter_input(INPUT_POST, 'tweetid', FILTER_SANITIZE_NUMBER_INT);
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$tweetid || !$userAuth['twitter_id']) {
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }
        $success = CoreDB::$databaseConnection->prepare("UPDATE tweets SET deletedflag=? WHERE tweetid=?")->execute(["Y", $tweetid]);
        echo Core::encodeSuccessInformation($success);
        exit();
    }

    public static function deleteMultipleTweets($userAuth) {
        $tweetids = filter_input(INPUT_POST, 'tweetids', FILTER_SANITIZE_STRING);
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$tweetids || !$userAuth['twitter_id']) {
            echo Core::encodeStatusInformation(StatusCode::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }
        $params = explode(',', $tweetids);
        $tweetCount = count($params);
        array_unshift($params, "Y");
        $query = "UPDATE tweets SET deletedflag=? WHERE tweetid IN (?";
        for ($i = 0; $i < $tweetCount - 1; $i++) {
            $query .= ",?";
        }
        $query .= ")";
        $success = CoreDB::$databaseConnection->prepare($query)->execute($params);
        echo Core::encodeSuccessInformation($success);
        exit();
    }

}
