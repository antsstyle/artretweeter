<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Antsstyle\ArtRetweeter\DB\UserDB;
use Antsstyle\ArtRetweeter\Core\LogManager;

class OAuth {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function base64url_encode($plainText) {
        $base64 = base64_encode($plainText);
        $base64 = trim($base64, "=");
        $base64url = strtr($base64, '+/', '-_');
        return ($base64url);
    }

    public static function generatePKCEVerifierAndChallenge() {
        $random = bin2hex(openssl_random_pseudo_bytes(128));
        $verifier = OAuth::base64url_encode(pack('H*', $random));
        $challenge = OAuth::base64url_encode(pack('H*', hash('sha256', $verifier)));
        return [$verifier, $challenge];
    }

    public static function getRefreshTokenForUser($userAuth, $errorCount = 0) {
        $userTwitterID = $userAuth['twitterid'];
        $userInfo = UserDB::getUserInfo($userTwitterID);
        if (is_null($userInfo) || $userInfo === false) {
            self::$logger->critical("Unable to get user info from DB, cannot refresh token for user twitter ID $userTwitterID");
            return false;
        }
        $refreshToken = $userInfo['refreshtoken'];
        if (is_null($refreshToken)) {
            self::$logger->critical("Refresh token in DB is null, cannot refresh token for user twitter ID $userTwitterID");
            return false;
        }
        if ($errorCount >= 3) {
            self::$logger->error("Three errors refreshing access token for user twitter ID $userTwitterID");
            return false;
        }
        $appAuth = base64_encode(APIKeys::twitter_oauth2_client_id . ":" . APIKeys::twitter_oauth2_client_secret);
        $curl = curl_init("https://api.twitter.com/2/oauth2/token");
        curl_setopt($curl, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($curl, CURLOPT_HTTPHEADER, array(
            'Content-Type: application/x-www-form-urlencoded',
            "Authorization: Basic $appAuth"
        ));
        $postFields = http_build_query(array(
            "refresh_token" => $refreshToken, "grant_type" => "refresh_token", "client_id" => APIKeys::twitter_oauth2_client_id
        ));
        curl_setopt($curl, CURLOPT_POST, 1);
        curl_setopt($curl, CURLOPT_POSTFIELDS, $postFields);
        $content = curl_exec($curl);
        $accessTokenObject = json_decode($content);
        if (isset($accessTokenObject->access_token)) {
            $success = UserDB::insertOAuth2UserInformation($accessTokenObject, $userTwitterID);
            if (!$success) {
                self::$logger->emergency("Failed to update user refresh token info for user twitter ID $userTwitterID!");
            }
            return $accessTokenObject->access_token;
        } else {
            error_log("New access token could not be obtained for user ID $userTwitterID");
            error_log("Response was: " . print_r($accessTokenObject, true));
            error_log("USER AUTH WAS: " . print_r($userAuth, true));
            usleep(100000);
            OAuth::getRefreshTokenForUser($userAuth, $errorCount + 1);
        }
    }

}

OAuth::initialiseLogger();
