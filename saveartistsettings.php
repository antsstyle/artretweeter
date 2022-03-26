<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\UserSettings;
use Antsstyle\ArtRetweeter\Core\TwitterResponseStatus;

Session::checkSession();
Session::validateUserLoggedIn();

$result = UserSettings::saveAutomationSettings($_SESSION['usertwitterid']);
?>

<html>
    <head>
        
        <link rel="stylesheet" href="src/css/artretweeter.css" type="text/css">
        <link rel="stylesheet" href=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "main.css"; ?> type="text/css">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="twitter:card" content="summary" />
        <meta name="twitter:site" content="@antsstyle" />
        <meta name="twitter:title" content="ArtRetweeter, an auto-retweeting app for artists" />
        <meta name="twitter:description" content="ArtRetweeter can automatically retweet your art for you, without the need to schedule retweets manually." />
        <meta name="twitter:image" content="<?php echo Config::CARD_IMAGE_URL; ?>" />
    </head>
    <title>
        ArtRetweeter
    </title>
    <body>
        <div class="main">
            <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "main.js"; ?>></script>
            <h1>ArtRetweeter</h1>
            <?php
            $adminURL = Config::ADMIN_URL;
            $adminName = Config::ADMIN_NAME;
            if ($result === TwitterResponseStatus::ARTRETWEETER_QUERY_OK) {
                echo "ArtRetweeter settings saved successfully. If you want to change your settings, you can go back to the settings page.";
            } else if (is_string($result)) {
                error_log("Failed to save settings correctly. User twitter ID: " . $_SESSION['usertwitterid'] . " Error string was: $result");
                echo "ArtRetweeter settings were not saved successfully ($result).<br/><br/>"
                . "Go back to the homepage to try"
                . " again, or contact <a href=$adminURL>$adminName</a> on Twitter if the problem persists.";
            }
            ?>
        </div>
    </body>
    <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "collapsibles.js"; ?>></script>
</html>
