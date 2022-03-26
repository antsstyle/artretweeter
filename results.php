<?php

namespace Antsstyle\ArtRetweeter;

require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\TwitterResponseStatus;
use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\DB\UserDB;
use Antsstyle\ArtRetweeter\Core\Config;
use Abraham\TwitterOAuth\TwitterOAuth;

Session::checkSession();

$artRetweeterPage = $_SESSION['artretweeterpage'];

if (!$artRetweeterPage) {
    $location = Config::HOMEPAGE_URL . "error";
    header("Location: $location", true, 302);
    exit();
}

if (!$_SESSION['code_verifier']) {
    $location = Config::HOMEPAGE_URL . "error";
    header("Location: $location", true, 302);
    exit();
}

$code = htmlspecialchars($_GET['code']);
$state = htmlspecialchars($_GET['state']);

$appAuth = base64_encode(APIKeys::twitter_oauth2_client_id . ":" . APIKeys::twitter_oauth2_client_secret);
$curl = curl_init("https://api.twitter.com/2/oauth2/token");
curl_setopt($curl, CURLOPT_RETURNTRANSFER, 1);
curl_setopt($curl, CURLOPT_HTTPHEADER, array(
    'Content-Type: application/x-www-form-urlencoded',
    "Authorization: Basic $appAuth"
));
$postFields = http_build_query(array(
    "code" => $code, "grant_type" => "authorization_code", "client_id" => APIKeys::twitter_oauth2_client_id,
    "redirect_uri" => Config::OAUTH_CALLBACK, "code_verifier" => $_SESSION['code_verifier']
        ));
curl_setopt($curl, CURLOPT_POST, 1);
curl_setopt($curl, CURLOPT_POSTFIELDS, $postFields);
$content = curl_exec($curl);

$accessTokenObject = json_decode($content);

if (isset($accessTokenObject->access_token)) {
    $connection = new TwitterOAuth(APIKeys::twitter_oauth2_client_id, APIKeys::twitter_oauth2_client_secret, null, $accessTokenObject->access_token);
    $connection->setApiVersion('2');
    $connection->setBearer($accessTokenObject->access_token);
    $response = $connection->get("users/me");
    $twitterResponseStatus = Core::checkResponseHeadersForErrors($connection);
    if ($twitterResponseStatus->getHttpCode() != TwitterResponseStatus::HTTP_QUERY_OK ||
            $twitterResponseStatus->getTwitterCode() != TwitterResponseStatus::ARTRETWEETER_QUERY_OK) {
        error_log("Bad response: " . print_r($twitterResponseStatus, true));
        $location = Config::HOMEPAGE_URL . "error";
        header("Location: $location", true, 302);
        exit();
    }
    $username = $response->data->username;
    $userTwitterID = $response->data->id;
    $userBanned = UserDB::checkIfUserIsBanned($userTwitterID);
    if (is_null($userBanned)) {
        $location = Config::HOMEPAGE_URL . "failure";
        header("Location: $location", true, 302);
        exit();
    } else if ($userBanned !== false) {
        if (isset($userBanned['reason'])) {
            $reasonString = htmlspecialchars($userBanned['reason']);
        } else {
            $reasonString = htmlspecialchars($userBanned['matchedfiltertype'] . " : " . $userBanned['matchedfiltercontent']);
        }
        $location = Config::HOMEPAGE_URL . "banned?reason=$reasonString";
        header("Location: $location", true, 302);
        exit();
    }

    $success = UserDB::insertOAuth2UserInformation($accessTokenObject, $userTwitterID);
    if ($success) {
        $_SESSION['usertwitterid'] = $userTwitterID;
        $_SESSION['artretweeterlogin'] = "true";
        if ($_SESSION['artretweeterpage'] === "artists") {
            $location = Config::HOMEPAGE_URL . "artistsettings";
            header("Location: $location", true, 302);
        } else {
            $location = Config::HOMEPAGE_URL . "nonartistsettings";
            header("Location: $location", true, 302);
        }
    } else {
        $location = Config::HOMEPAGE_URL . "failure";
        header("Location: $location", true, 302);
        exit();
    }
} else {
    $location = Config::HOMEPAGE_URL . "failure";
    header("Location: $location", true, 302);
    exit();
}

