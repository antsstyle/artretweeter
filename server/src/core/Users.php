<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\CoreDB;
use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Abraham\TwitterOAuth\TwitterOAuth;

class Users {

    public static $logger;

    public static function lookupUsersBearerToken($lookupString) {
        $connection = new TwitterOAuth(APIKeys::twitter_consumer_key, APIKeys::twitter_consumer_secret,
                null, APIKeys::bearer_token);
        $connection->setApiVersion('2');
        $connection->setRetries(1, 1);
        $query = "users";
        $params['ids'] = $lookupString;
        $params['user.fields'] = "public_metrics";
        $response = $connection->get($query, $params);
        $statusCode = Core::checkResponseHeadersForErrors($connection);
        if ($statusCode->httpCode != StatusCode::HTTP_QUERY_OK || $statusCode->twitterCode != StatusCode::ARTRETWEETER_QUERY_OK) {
            return null;
        }
        $data = $response->data;
        return $data;
    }

    public static function retrieveUserTwitterHandle($userAuth) {
        Core::validateUserAuth($userAuth);
        $connection = new TwitterOAuth(APIKeys::twitter_consumer_key, APIKeys::twitter_consumer_secret,
                $userAuth['access_token'], $userAuth['access_token_secret']);
        $connection->setApiVersion('2');
        $connection->setRetries(1, 1);
        $query = "users/" . $userAuth['twitter_id'];
        $params['user.fields'] = "username";
        $response = $connection->get($query, $params);
        $statusCode = Core::checkResponseHeadersForErrors($connection);
        if ($statusCode->httpCode != StatusCode::HTTP_QUERY_OK || $statusCode->twitterCode != StatusCode::ARTRETWEETER_QUERY_OK) {
            return;
        }
        $screenName = $response->data->username;
        $updQuery = "UPDATE users SET screenname=? WHERE twitterid=?";
        $updStmt = CoreDB::$databaseConnection->prepare($updQuery);
        $updStmt->execute([$screenName, $userAuth['twitter_id']]);
        return $screenName;
    }

}

Users::$logger = LogManager::getLogger("Users");
