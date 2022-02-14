<?php

namespace Antsstyle\ArtRetweeter\Ajax;

chdir(dirname(__DIR__, 2));

$dir = getcwd();

require $dir . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\CoreDB;

class Ajax {

    public static function processAjax() {
        Session::checkSession();
        $userTwitterID = filter_input(INPUT_POST, 'userid', FILTER_SANITIZE_NUMBER_INT);
        if ($userTwitterID !== $_SESSION['usertwitterid']) {
            exit();
        }

        $request = filter_input(INPUT_POST, 'request', FILTER_SANITIZE_STRING);
        if ($request === false) {
            exit();
        } else if (is_null($request)) {
            exit();
        }


        switch ($request) {
            case "usernonartistautomationsettings":
                $automationSettings = Core::getNonArtistAutomationSettings($userTwitterID);
                if (!$automationSettings) {
                    echo "";
                } else {
                    echo json_encode($automationSettings);
                }
                break;
            case "userautomationsettings":
                $automationSettings = Core::getAutomationSettings($userTwitterID);
                if (!$automationSettings) {
                    echo "";
                } else {
                    echo json_encode($automationSettings);
                }
                break;
            case "reschedulequeueentry":
                $idToReschedule = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_NUMBER_INT);
                if ($idToReschedule === false || $idToReschedule === null) {
                    error_log("Invalid request ID, cannot reschedule queue entry.");
                    echo "";
                    break;
                }
                $newTime = filter_input(INPUT_POST, 'newtime', FILTER_SANITIZE_STRING);
                if ($newTime === false || $newTime === null) {
                    error_log("Invalid schedule time, cannot reschedule queue entry.");
                    echo "";
                    break;
                }
                $rescheduleResult = CoreDB::rescheduleQueuedTweet($idToReschedule, $userTwitterID, $newTime);
                if (is_string($rescheduleResult)) {
                    echo $rescheduleResult;
                } else if ($rescheduleResult === false) {
                    echo "Database error";
                } else if ($rescheduleResult === 0) {
                    echo "Record does not exist";
                } else {
                    echo "Success";
                }
                break;
            case "deletequeueentry":
                $idToDelete = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_NUMBER_INT);
                if ($idToDelete === false || $idToDelete === null) {
                    error_log("Invalid request ID, cannot delete queue entry.");
                    echo "";
                } else {
                    $result = CoreDB::deleteQueuedRetweet($idToDelete, $userTwitterID);
                    echo json_encode($result);
                }
                break;
            default:
                break;
        }
    }

}

Ajax::processAjax();
