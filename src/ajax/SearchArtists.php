<?php

namespace Antsstyle\ArtRetweeter\Ajax;

chdir(dirname(__DIR__, 2));

$dir = getcwd();

require $dir . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\DB\UserDB;

class SearchArtists {

    public static function processAjax() {
        Session::checkSession();
        $userTwitterID = filter_input(INPUT_POST, 'userid', FILTER_SANITIZE_NUMBER_INT);
        if ($userTwitterID !== $_SESSION['usertwitterid']) {
            return;
        }

        $searchString = htmlspecialchars($_POST['searchstring']);
        if ($searchString === "") {
            echo "Invalid username";
            return;
        } else if (!preg_match("/^@?[A-Za-z0-9_]{1,15}$/", $searchString)) {
            echo "Invalid username";
            return;
        }

        if (strpos($searchString, "@") === 0) {
            $searchString = substr($searchString, 1);
        }
        if (strlen($searchString) < 3) {
            echo "Your search string must be at least 3 characters long.";
            return;
        }
        $searchString = "%" . $searchString . "%";
        $artistResults = UserDB::searchArtistsForUser($searchString, $userTwitterID);
        if (is_null($artistResults)) {
            echo "";
        } else {
            echo json_encode([$userTwitterID, $artistResults]);
        }
    }
}

SearchArtists::processAjax();
