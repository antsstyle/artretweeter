<?php

namespace Antsstyle\ArtRetweeter\Ajax;

chdir(dirname(__DIR__, 2));

$dir = getcwd();

require $dir . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\DB\AutomationDB;
use Antsstyle\ArtRetweeter\DB\UserDB;

class Ajax {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function processAjax() {
        Session::checkSession();
        $userTwitterID = filter_input(INPUT_POST, 'userid', FILTER_SANITIZE_NUMBER_INT);
        if ($userTwitterID !== $_SESSION['usertwitterid']) {
            exit();
        }

        $request = htmlspecialchars($_POST['request']);
        if ($request === "") {
            exit();
        }


        switch ($request) {
            case "switchviewmodes":
                $viewMode = htmlspecialchars($_POST['viewmode']);
                if ($viewMode === "") {
                    break;
                }
                UserDB::updateUserAddArtistsViewMode($userTwitterID, $viewMode);
                echo "Success";
                break;
            case "usernonartistautomationsettings":
                $automationSettings = AutomationDB::getNonArtistAutomationSettings($userTwitterID);
                if (!$automationSettings) {
                    echo "";
                } else {
                    echo json_encode($automationSettings);
                }
                break;
            case "userautomationsettings":
                $automationSettings = AutomationDB::getAutomationSettings($userTwitterID);
                if (!$automationSettings) {
                    echo "";
                } else {
                    echo json_encode($automationSettings);
                }
                break;
            case "reschedulequeueentry":
                $idToReschedule = filter_input(INPUT_POST, 'id', FILTER_SANITIZE_NUMBER_INT);
                if ($idToReschedule === false || $idToReschedule === null) {
                    self::$logger->error("Invalid request ID, cannot reschedule queue entry.");
                    echo "";
                    break;
                }
                $newTime = htmlspecialchars($_POST['newtime']);
                if ($newTime === "") {
                    self::$logger->error("Invalid schedule time, cannot reschedule queue entry.");
                    echo "";
                    break;
                }
                $rescheduleResult = AutomationDB::rescheduleQueuedTweet($idToReschedule, $userTwitterID, $newTime);
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
                    self::$logger->error("Invalid request ID, cannot delete queue entry.");
                    echo "";
                } else {
                    $result = AutomationDB::deleteQueuedRetweet($idToDelete, $userTwitterID);
                    echo json_encode($result);
                }
                break;
            default:
                break;
        }
    }

}

Ajax::initialiseLogger();
Ajax::processAjax();
