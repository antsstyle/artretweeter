<?php

namespace Antsstyle\ArtRetweeter;

require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\DB\AutomationDB;

Session::checkSession();
Session::validateUserLoggedIn();

$userAutomationSettings = AutomationDB::getNonArtistAutomationSettings($_SESSION['usertwitterid']);
if (is_null($userAutomationSettings) || $userAutomationSettings === false || $userAutomationSettings['settingstype'] === "Simple") {
    $settingsURL = Config::HOMEPAGE_URL . "nonartistsimplesettings";
    header("Location: $settingsURL", true, 302);
    exit();
} else {
    $settingsURL = Config::HOMEPAGE_URL . "nonartistadvancedsettings";
    header("Location: $settingsURL", true, 302);
    exit();
}