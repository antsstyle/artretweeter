<?php

namespace Antsstyle\ArtRetweeter;

require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\DB\AutomationDB;

Session::checkSession();
Session::validateUserLoggedIn();

$userAutomationSettings = AutomationDB::getAutomationSettings($_SESSION['usertwitterid']);
$showWarning = "N";
if (!is_null($userAutomationSettings) && $userAutomationSettings !== false) {
    if ($userAutomationSettings['automationenabled'] === "Y") {
        $showWarning = "Y";
    }
}
?>


<html>
    <script src="src/ajax/Settings.js"></script>
    <head>
        
        <link rel="stylesheet" href="src/css/artretweeter.css" type="text/css">
        <link rel="stylesheet" href=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "main.css"; ?> type="text/css">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
    </head>
    <title>
        ArtRetweeter
    </title>
    <body onload="getUserSimpleAutomationSettings('<?php echo $_SESSION['usertwitterid']; ?>')">
        <div class="main">
            <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "main.js"; ?>></script>
            <h1>ArtRetweeter</h1>
            <div class="subtitle">
                <h2>Artist Settings</h2> 
            </div>

            <div class="start">
                <b>This page shows your settings for retweeting yourself. If you want to change your settings for retweeting other artists, go to the 
                    <a href=<?php echo Config::NONARTISTSETTINGSPAGE_URL ?>>Non-Artist Settings</a> page.</b>
                <br/><br/>
            </div>
            <div class="center">
                <button class="settingsswitchbutton" type="button" disabled>Simple Settings</button>
                <button class="settingsswitchbutton" type="button" 
                        onclick="window.location.href = '<?php echo Config::HOMEPAGE_URL . "artistadvancedsettings"; ?>';">Advanced Settings</button>
            </div>
            <form action="saveartistsettings" method="post">
                <h2>Automated Retweeting</h2>
                The simple automated retweet settings will schedule retweets for:
                <ul>
                    <li>Any day of the week, any hour, any minute</li>
                    <li>Tweets with images, but not video or GIF tweets</li>
                </ul>
                If you want to customise your retweeting options, go to the Advanced Settings using the button above.
                <br/><br/>
                <div class="formsection" style="max-width:600px;">
                    <input type="hidden" id="simplesettingsform" name="simplesettingsform" value="simplesettingsform">
                    <input type="hidden" id="simplesettingsform_timezone" name="simplesettingsform_timezone" value="notset">
                    <input type="checkbox" id="enableautomatedretweeting_simplesettings" name="enableautomatedretweeting_simplesettings" 
                           value="enable_automated_retweeting_simplesettings" onclick="showDisableAutomationWarning('<?php echo $showWarning; ?>')">
                    <label for="enableautomatedretweeting_simplesettings"> Enable automated retweeting </label><br/>
                    <div class="disableautomationwarningdiv" id="autowarningdiv">
                        Note: disabling automated retweeting will remove all currently queued retweets.</div>
                </div>
                <br/><br/>
                <div class="center">
                    <input type="submit" id="savesettingsbutton" value="Save Settings">
                </div>
            </form>
        </div>
        <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "collapsibles.js"; ?>></script>
    </body>
</html>