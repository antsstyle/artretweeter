<?php

namespace Antsstyle\ArtRetweeter\Ajax;

chdir(dirname(__DIR__, 2));

$dir = getcwd();

require $dir . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\DB\ArtistDB;
use Antsstyle\ArtRetweeter\DB\UserDB;

class SubmitArtist {

    public static function processAjax() {
        Session::checkSession();
        $userTwitterID = filter_input(INPUT_POST, 'userid', FILTER_SANITIZE_NUMBER_INT);
        if ($userTwitterID !== $_SESSION['usertwitterid']) {
            return;
        }

        $userInfo = UserDB::getUserInfo($userTwitterID);
        if (is_null($userInfo) || $userInfo === false) {
            echo "A database error occurred. Try again later or contact "
            . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.";
            return;
        }

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

        $autoRetweetOnApproval = htmlspecialchars($_POST['autoretweetonapproval']);
        if ($autoRetweetOnApproval !== "Y" && $autoRetweetOnApproval !== "N" && $operation !== "cancel") {
            echo "Invalid auto retweet on approval setting.";
            return;
        }

        $operation = htmlspecialchars($_POST['operation']);

        if ($operation !== "cancel" && $operation !== "submit") {
            echo "Invalid operation";
            return;
        }


        if ($operation === "cancel") {
            $result = ArtistDB::cancelArtistSubmission($userInfo, $artistTwitterHandle);
        } else {
            $result = json_encode(ArtistDB::submitArtistForApproval($userInfo, $artistTwitterHandle, $autoRetweetOnApproval));
        }

        echo $result;
    }

}

SubmitArtist::processAjax();
