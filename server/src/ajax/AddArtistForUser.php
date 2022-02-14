<?php

namespace Antsstyle\ArtRetweeter\Ajax;

chdir(dirname(__DIR__, 2));

$dir = getcwd();

require $dir . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\CoreDB;
use Antsstyle\ArtRetweeter\Core\Config;

class AddArtistForUser {

    public static function processAjax() {
        Session::checkSession();
        $userTwitterID = filter_input(INPUT_POST, 'userid', FILTER_SANITIZE_NUMBER_INT);
        if ($userTwitterID !== $_SESSION['usertwitterid']) {
            return;
        }

        $artistTwitterID = filter_input(INPUT_POST, 'artistid', FILTER_SANITIZE_NUMBER_INT);
        $operation = filter_input(INPUT_POST, 'operation', FILTER_SANITIZE_STRING);
        if ($operation !== "Disable" && $operation !== "Enable") {
            echo "Invalid input";
            return;
        }


        $returnValue = CoreDB::updateArtistForUser($userTwitterID, $artistTwitterID, $operation);
        if (is_null($returnValue)) {
            echo "A database error occurred.<br/><br/>Try again later or contact "
            . "<a href=\"" . Config::ADMIN_URL . "\" target=\"_blank\">" . Config::ADMIN_NAME . "</a> if it persists.";
        } else if (gettype($returnValue) === "string") {
            echo $returnValue;
        } else {
            echo json_encode($returnValue);
        }
    }

}

AddArtistForUser::processAjax();
