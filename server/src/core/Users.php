<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\CoreDB;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Abraham\TwitterOAuth\TwitterOAuth;

class Users {

    public static function retrieveUserTwitterHandle($userAuth) {
        Core::validateUserAuth($userAuth);
        $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret,
                $userAuth['access_token'], $userAuth['access_token_secret']);
        $connection->setApiVersion('2');
        $connection->setRetries(1, 1);
        $query = "users/" . $userAuth['twitter_id'];
        $params['user.fields'] = "username";
        $response = $connection->get($query, $params);
        $statusCode = Core::checkResponseHeadersForErrors($connection);
        if ($statusCode->httpCode != StatusCode::HTTP_QUERY_OK || $statusCode->twitterCode != StatusCode::ARTRETWEETER_QUERY_OK) {
            error_log(print_r($statusCode, true));
            return;
        }
        $screenName = $response->data->username;
        error_log(print_r($response->data, true));
        $updQuery = "UPDATE users SET screenname=? WHERE twitterid=?";
        $updStmt = CoreDB::$databaseConnection->prepare($updQuery);
        $updStmt->execute([$screenName, $userAuth['twitter_id']]);
        return $screenName;
    }

}
