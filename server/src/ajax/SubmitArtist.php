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

        $artistTwitterHandle = filter_input(INPUT_POST, 'artisttwitterhandle', FILTER_SANITIZE_STRING);
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

        if (strpos($artistTwitterHandle, "@") === 0) {
            $searchString = substr($searchString, 1);
        }
        $result = CoreDB::submitArtistForApproval($userAuth, $artistTwitterHandle);
        echo $result;
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
                $artistID = $resultRow['twitterid'];
                $addButton = "<button id=\"followbutton$i\" type=\"button\" onclick=\"addArtistForUser('$userTwitterID', '$artistID'"
                        . ", 'followbutton$i', 'Enable', 'Update', '$i')\">Enable automated retweeting</button>";
                $tableString .= "<tr>";
                $tableString .= "<td>@" . $screenName . "</td>";
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

SubmitArtist::processAjax();
