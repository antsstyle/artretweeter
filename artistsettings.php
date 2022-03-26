<?php

namespace Antsstyle\ArtRetweeter;

require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\DB\AutomationDB;

Session::checkSession();
Session::validateUserLoggedIn();

$userAutomationSettings = AutomationDB::getAutomationSettings($_SESSION['usertwitterid']);
if (is_null($userAutomationSettings) || $userAutomationSettings === false || $userAutomationSettings['settingstype'] === "Simple") {
    $settingsURL = Config::HOMEPAGE_URL . "artistsimplesettings";
    header("Location: $settingsURL", true, 302);
    exit();
} else {
    $settingsURL = Config::HOMEPAGE_URL . "artistadvancedsettings";
    header("Location: $settingsURL", true, 302);
    exit();
}