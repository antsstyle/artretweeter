<?php

namespace Antsstyle\ArtRetweeter\Ajax;

chdir(dirname(__DIR__, 2));

$dir = getcwd();

require $dir . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\CoreDB;

class ArtistSubmissions {

    public static function processAjax() {
        Session::checkSession();
        if ($_SESSION['adminlogin']) {
            return "Error - you are not logged in as an administrator.";
        }
        $artistScreenName = htmlspecialchars($_POST['artistscreenname']);
        $type = htmlspecialchars($_POST['type']);
        $reason = htmlspecialchars($_POST['reason']);
        if ($artistScreenName === "" || $type === "") {
            return;
        }
        if ($type === "rejection" && $reason === "") {
            return;
        }

        $result = CoreDB::processArtistSubmission($artistScreenName, $type, $reason);
        echo $result;
    }

}

ArtistSubmissions::processAjax();
