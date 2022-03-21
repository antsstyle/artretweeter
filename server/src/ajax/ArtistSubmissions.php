<?php

namespace Antsstyle\ArtRetweeter\Ajax;

chdir(dirname(__DIR__, 2));

$dir = getcwd();

require $dir . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\DB\ArtistDB;

class ArtistSubmissions {

    public static function processAjax() {
        Session::checkSession();
        if (!$_SESSION['adminlogin']) {
            echo "Error - you are not logged in as an administrator.";
            return;
        }
        $artistScreenName = htmlspecialchars($_POST['artistscreenname']);
        $type = htmlspecialchars($_POST['type']);
        $reason = htmlspecialchars($_POST['reason']);
        if ($artistScreenName === "" || $type === "") {
            echo "Artist screen name or type was empty.";
            return;
        }
        if ($type === "rejection" && $reason === "") {
            echo "Reason cannot be empty when rejecting an artist.";
            return;
        }

        $result = ArtistDB::processArtistSubmission($artistScreenName, $type, $reason);
        echo $result;
    }

}

ArtistSubmissions::processAjax();
