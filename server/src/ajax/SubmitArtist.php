<?php

namespace Antsstyle\ArtRetweeter\Ajax;

chdir(dirname(__DIR__, 2));

$dir = getcwd();

require $dir . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\CoreDB;

class SubmitArtist {

    public static function processAjax() {
        Session::checkSession();
        $userTwitterID = filter_input(INPUT_POST, 'userid', FILTER_SANITIZE_NUMBER_INT);
        if ($userTwitterID !== $_SESSION['usertwitterid']) {
            return;
        }

        $userInfo = Core::getUserInfo($userTwitterID);
        if (is_null($userInfo) || $userInfo === false) {
            echo "A database error occurred. Try again later or contact "
            . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.";
            return;
        }

        $userAuth['twitter_id'] = $userInfo['twitterid'];
        $userAuth['access_token'] = $userInfo['accesstoken'];
        $userAuth['access_token_secret'] = $userInfo['accesstokensecret'];

        $artistTwitterHandle = htmlspecialchars($_POST['artisttwitterhandle']);
        if ($artistTwitterHandle === false) {
            echo "Invalid username";
            return;
        } else if (is_null($artistTwitterHandle)) {
            echo "Invalid username";
            return;
        } else if (!preg_match("/^@?[A-Za-z0-9_]{1,15}$/", $artistTwitterHandle)) {
            echo "Invalid username";
            return;
        }

        $operation = htmlspecialchars($_POST['operation']);
        if ($operation === "cancel") {
            $result = CoreDB::cancelArtistSubmission($userAuth, $artistTwitterHandle);
        } else if ($operation === "submit") {
            $result = json_encode(CoreDB::submitArtistForApproval($userAuth, $artistTwitterHandle));
        } else {
            echo "Invalid operation";
            return;
        }

        echo $result;
    }
}

SubmitArtist::processAjax();
