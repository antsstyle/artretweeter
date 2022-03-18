<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\CoreDB;
use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Antsstyle\ArtRetweeter\Core\TwitterResponseStatus;

class Users {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function lookupUsersBearerToken($lookupString) {
        $query = "users";
        $params['ids'] = $lookupString;
        $params['user.fields'] = "public_metrics";
        $response = Core::queryTwitterUserAuth($query, $query, "GET", $params, APIKeys::bearer_token);
        $twitterResponseStatus = $response[1];
        if ($twitterResponseStatus->getHttpCode() != TwitterResponseStatus::HTTP_QUERY_OK ||
                $twitterResponseStatus->getTwitterCode() != TwitterResponseStatus::ARTRETWEETER_QUERY_OK) {
            return null;
        }
        $data = $response[0]->data;
        return $data;
    }

    public static function retrieveUserTwitterHandle($userRow) {
        $query = "users/" . $userRow['twitterid'];
        $params['user.fields'] = "username";
        $response = Core::queryTwitterUserAuth($query, "users/:id", "GET", $params, $userRow);
        $twitterResponseStatus = $response[1];
        if ($twitterResponseStatus->getHttpCode() != TwitterResponseStatus::HTTP_QUERY_OK ||
                $twitterResponseStatus->getTwitterCode() != TwitterResponseStatus::ARTRETWEETER_QUERY_OK) {
            return;
        }
        $screenName = $response[0]->data->username;
        $updQuery = "UPDATE users SET screenname=? WHERE twitterid=?";
        $updStmt = CoreDB::getConnection()->prepare($updQuery);
        $updStmt->execute([$screenName, $userRow['twitterid']]);
        return $screenName;
    }

}

Users::initialiseLogger();