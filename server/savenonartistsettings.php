<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\UserSettings;
use Antsstyle\ArtRetweeter\Core\StatusCode;

Session::checkSession();

$userTwitterID = $_SESSION['usertwitterid'];

if (!$_SESSION['usertwitterid']) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
}

$userInfo = Core::getUserInfo($_SESSION['usertwitterid']);
if ($userInfo === false) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
} else if ($userInfo === null) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
}


$result = UserSettings::saveNonArtistAutomationSettings($userTwitterID);
?>

<html>
    <head>
        <link rel="stylesheet" href="main.css" type="text/css">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
    </head>
    <title>
        ArtRetweeter
    </title>
    <body>
        <div class="main">
            <?php Core::echoSidebar(); ?>
            <h1>ArtRetweeter</h1>
            <?php
            $adminURL = Config::ADMIN_URL;
            $adminName = Config::ADMIN_NAME;
            if ($result === StatusCode::ARTRETWEETER_QUERY_OK) {
                echo "ArtRetweeter settings saved successfully. If you want to change your settings, you can go back to the settings page.";
            } else if (is_string($result)) {
                echo "ArtRetweeter settings were not saved successfully ($result).<br/><br/>"
                . "Go back to the homepage to try"
                . " again, or contact <a href=$adminURL>$adminName</a> on Twitter if the problem persists.";
            }
            ?>
        </div>
    </body>
    <script src="src/ajax/Collapsibles.js"></script>
</html>
