<?php

namespace Antsstyle\ArtRetweeter\Ajax;

chdir(dirname(__DIR__, 2));

$dir = getcwd();

require $dir . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\CoreDB;

class SearchArtists {

    public static function processAjax() {
        Session::checkSession();
        $userTwitterID = filter_input(INPUT_POST, 'userid', FILTER_SANITIZE_NUMBER_INT);
        if ($userTwitterID !== $_SESSION['usertwitterid']) {
            return;
        }

        $searchString = filter_input(INPUT_POST, 'searchstring', FILTER_SANITIZE_STRING);
        if ($searchString === false) {
            echo "Invalid username";
            return;
        } else if (is_null($searchString)) {
            echo "Invalid username";
            return;
        } else if (!preg_match("/^@?[A-Za-z0-9_]{1,15}$/", $searchString)) {
            echo "Invalid username";
            return;
        }

        if (strpos($searchString, "@") === 0) {
            $searchString = substr($searchString, 1);
        }
        $searchString = "%" . $searchString . "%";
        $artistResults = CoreDB::searchArtistsForUser($searchString, $userTwitterID);
        if (is_null($artistResults)) {
            echo "";
        } else {
            SearchArtists::echoTable($userTwitterID, $artistResults);
        }
    }

    public static function echoTable($userTwitterID, $artistResults) {
        $tableString = "<table id=\"maintable\" class=\"dblisttable\"><tr>
                        <th onclick=\"sortTable(0, 'maintable')\">Twitter Handle</th>
                        <th onclick=\"sortTable(1, 'maintable')\">Follower Count</th>
                        <th>Options</th>

            </tr>";
        $resultCount = $artistResults['resultcount'];
        $rows = $artistResults['rows'];
        $i = 0;
        if ($rows !== false) {
            foreach ($rows as $resultRow) {
                $screenName = $resultRow['screenname'];
                $hrefScreenName = "<a href=\"https://twitter.com/" . $screenName . "\" target=_\"blank\">"
                        . "@" . $screenName . "</a>";
                $artistID = $resultRow['twitterid'];
                $addButton = "<button id=\"followbutton$i\" type=\"button\" onclick=\"addArtistForUser('$userTwitterID', '$artistID'"
                        . ", 'followbutton$i', 'Enable', 'Update', '$i')\">Enable automated retweeting</button>";
                $tableString .= "<tr>";
                $tableString .= "<td>$hrefScreenName</td>";
                $tableString .= "<td>" . $resultRow['followercount'] . "</td>";
                $tableString .= "<td>" . $addButton . "</td>";
                $tableString .= "</tr>";
                $i++;
            }
        }
        $tableString .= "</table>";
        $returnArray['resultcount'] = $resultCount;
        $returnArray['tablestring'] = $tableString;
        echo json_encode($returnArray);
    }

}

SearchArtists::processAjax();
