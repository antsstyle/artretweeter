<?php

namespace Antsstyle\ArtRetweeter;

require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Antsstyle\ArtRetweeter\Core\CoreDB;
use Antsstyle\ArtRetweeter\Core\Config;
use Abraham\TwitterOAuth\TwitterOAuth;

Session::checkSession();

$artRetweeterPage = $_SESSION['artretweeterpage'];

if (!$artRetweeterPage) {
    $location = Config::HOMEPAGE_URL . "error";
    header("Location: $location", true, 302);
    exit();
}

if (!$_SESSION['oauth_token']) {
    $location = Config::HOMEPAGE_URL . "error";
    header("Location: $location", true, 302);
    exit();
}

if ($_SESSION['usertwitterid']) {
    $location = Config::HOMEPAGE_URL . "loginsuccess";
    header("Location: $location", true, 302);
    exit();
}

$request_token = [];
$request_token['oauth_token'] = $_SESSION['oauth_token'];
$request_token['oauth_token_secret'] = $_SESSION['oauth_token_secret'];

$requestOAuthToken = htmlspecialchars($_GET['oauth_token']);
$requestOAuthVerifier = htmlspecialchars($_GET['oauth_verifier']);

if ($request_token['oauth_token'] !== $requestOAuthToken) {
    // Show error, redirect user back to homepage
    error_log("Non-matching OAuth tokens - aborting.");
    exit();
}

$connection = new TwitterOAuth(APIKeys::twitter_consumer_key, APIKeys::twitter_consumer_secret,
        $request_token['oauth_token'], $request_token['oauth_token_secret']);
try {
    $access_token = $connection->oauth("oauth/access_token", ["oauth_verifier" => $requestOAuthVerifier]);
} catch (\Exception $e) {
    error_log("Could not get access token");
    error_log(print_r($e, true));
    $location = Config::HOMEPAGE_URL . "failure";
    header("Location: $location", true, 302);
    exit();
}

if (isset($access_token)) {

    $userTwitterID = $access_token['user_id'];
    $userBanned = CoreDB::checkIfUserIsBanned($userTwitterID);
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

    $success = CoreDB::insertUserInformation($access_token);
    if ($success) {
        $_SESSION['usertwitterid'] = $access_token['user_id'];
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
}

