<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\Core;

Session::checkSession();

if (!$_SESSION['oauth_token']) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
}

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

$nonArtistUserAutomationSettings = Core::getNonArtistAutomationSettings($_SESSION['usertwitterid']);
$showWarning = "N";
if (!is_null($nonArtistUserAutomationSettings) && $nonArtistUserAutomationSettings !== false) {
    if ($nonArtistUserAutomationSettings['automationenabled'] === "Y") {
        $showWarning = "Y";
    }
}
?>



<html>
    <script src="src/ajax/Settings.js"></script>
    <head>
        <link rel="stylesheet" href="main.css" type="text/css">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
    </head>
    <title>
        ArtRetweeter
    </title>
    <body onload="getUserNonArtistAutomationSettings('<?php echo $_SESSION['usertwitterid']; ?>')">
        <div class="main">
            <?php Core::echoSidebar(); ?>
            <h1>ArtRetweeter</h1>
            <div class="subtitle">
                <h2>Non-Artist Settings</h2> 
            </div>

            <div class="start">
                <b>This page shows your settings for retweeting other people. If you want to change your settings for retweeting your own tweets, go to the 
                    <a href=<?php echo Config::ARTISTSETTINGSPAGE_URL ?>>Artist Settings</a> page.</b>
                <br/><br/>
                To manage your list of artists to retweet, go here.
            </div>
            <p>
            <form action="savenonartistsettings" method="post">
                <h2>Automated Retweeting</h2>
                <div class="formsection" style="max-width:600px;">
                    <input type="checkbox" id="enableautomatedretweeting" name="enableautomatedretweeting" value="enable_automated_retweeting"
                           onclick="showDisableAutomationWarning('<?php echo $showWarning; ?>')">
                    <label for="enableautomatedretweeting"> Enable automated retweeting </label><br/>
                    <div class="disableautomationwarningdiv" id="autowarningdiv">Note: disabling automated retweeting will remove all currently queued retweets.</div>
                </div>
                <h3>Search Settings</h3>
                <div class="formsection" style="max-width:600px;">
                    <input type="checkbox" id="ignoreoldtweets" name="ignoreoldtweets" value="ignore_old_tweets">
                    <label for="ignoreoldtweets"> Ignore tweets posted before this date: </label><br/><br/>
                    <input type="date" id="ignoreoldtweetsdate" name="ignoreoldtweetsdate" style="width:60%;">
                    <br/><br/>
                    <input type="checkbox" id="excludetextenabled" name="excludetextenabled" value="exclude_text_enabled"
                           onclick="checkValidIncludeText('excludetext', 'excludetextenabled', 'excludetexterrormsg')">
                    <label for="excludetextenabled"> Exclude all tweets that contain this text: (max 50 characters) </label><br/>
                    <div id="excludetexterrormsg" class="errormsg">
                        &nbsp;
                    </div>
                    <input type="text" id="excludetext" name="excludetext" 
                           onkeyup="checkValidIncludeText('excludetext', 'excludetextenabled', 'excludetexterrormsg')"  style="width:60%;">
                    <select name="excludetextoperation" id="excludetextoperation">
                        <option value="any">Any of these words</option>
                        <option value="all">All of these words</option>
                        <option value="exact">This exact phrase</option>
                    </select>
                    <br/><br/>
                    <input type="checkbox" id="includetextenabled" name="includetextenabled" value="include_text_enabled"
                           onclick="checkValidIncludeText('includetext', 'includetextenabled', 'includetexterrormsg')">
                    <label for="includetextenabled"> Include <b>only</b> tweets that contain this text: (max 50 characters) </label><br/>
                    <div id="includetexterrormsg" class="errormsg">
                        &nbsp;
                    </div>
                    <input type="text" id="includetext" name="includetext" 
                           onkeyup="checkValidIncludeText('includetext', 'includetextenabled', 'includetexterrormsg')" style="width:60%;">
                    <select name="includetextoperation" id="includetextoperation">
                        <option value="any">Any of these words</option>
                        <option value="all">All of these words</option>
                        <option value="exact">This exact phrase</option>
                    </select>
                    <br/><br/>
                </div>
                <h3>Media Settings</h3>
                By default, ArtRetweeter will only consider tweets which have images in them for retweeting. If you want it to consider tweets 
                which contain videos or GIFs (and/or exclude tweets with images), you can change that here.
                <div id="mediasettingserrormsg" class="errormsg">&nbsp;</div>
                <div class="formsection" style="max-width:600px;">
                    <input type="checkbox" id="imagesenabled" name="imagesenabled" value="images_enabled" checked="checked" 
                           class="mediacheckbox" onclick="checkValidMediaSettings()">   
                    <label for="imagesenabled"> Image tweets </label>
                    <input type="checkbox" id="gifsenabled" name="gifsenabled" value="gifs_enabled" 
                           class="mediacheckbox" onclick="checkValidMediaSettings()">   
                    <label for="gifsenabled"> GIF tweets </label>
                    <input type="checkbox" id="videosenabled" name="videosenabled" value="videos_enabled" 
                           class="mediacheckbox" onclick="checkValidMediaSettings()">   
                    <label for="videosenabled"> Video tweets </label>
                </div>
                <h3>Day and Time Settings</h3>
                <h4>Day Settings</h4>
                <div id="dayintervalserrormsg" class="errormsg"></div>
                <div class="formsection" style="max-width:600px;">
                    <input type="checkbox" id="mondayenabled" name="mondayenabled" value="monday_enabled" checked="checked" 
                           class="daycheckbox" onclick="checkValidDays()">
                    <label for="mondayenabled"> Monday </label>
                    <input type="checkbox" id="tuesdayenabled" name="tuesdayenabled" value="tuesday_enabled" checked="checked"
                           class="daycheckbox" onclick="checkValidDays()">
                    <label for="tuesdayenabled"> Tuesday </label>
                    <input type="checkbox" id="wednesdayenabled" name="wednesdayenabled" value="wednesday_enabled" checked="checked"
                           class="daycheckbox" onclick="checkValidDays()">
                    <label for="wednesdayenabled"> Wednesday </label>
                    <input type="checkbox" id="thursdayenabled" name="thursdayenabled" value="thursday_enabled" checked="checked"
                           class="daycheckbox" onclick="checkValidDays()">
                    <label for="thursdayenabled"> Thursday </label><br/>
                    <input type="checkbox" id="fridayenabled" name="fridayenabled" value="friday_enabled" checked="checked"
                           class="daycheckbox" onclick="checkValidDays()">
                    <label for="fridayenabled"> Friday </label>
                    <input type="checkbox" id="saturdayenabled" name="saturdayenabled" value="saturday_enabled" checked="checked"
                           class="daycheckbox" onclick="checkValidDays()">
                    <label for="saturdayenabled"> Saturday </label>
                    <input type="checkbox" id="sundayenabled" name="sundayenabled" value="sunday_enabled" checked="checked"
                           class="daycheckbox" onclick="checkValidDays()">
                    <label for="sundayenabled"> Sunday </label>
                </div>
                <h4>Hour Interval Settings</h4>
                These correspond to which hours ArtRetweeter will schedule retweets for. It will only ever post one retweet in a given 
                hour interval for a given day.
                <div id="hourintervalserrormsg" class="errormsg">
                    &nbsp;
                </div>
                <div class="formsection" style="max-width:600px;">
                    <table>
                        <?php
                        for ($i = 0; $i < 24; $i++) {
                            if ($i % 3 == 0 && $i > 0) {
                                echo "</tr>";
                            }
                            if ($i % 3 == 0) {
                                echo "<tr>";
                            }
                            $modifier = (($i % 3) * 8) + floor($i / 3);
                            if ($modifier < 10) {
                                $iString = "0" . $modifier;
                            } else {
                                $iString = $modifier;
                            }
                            $j = $modifier + 1;
                            if ($j < 10) {
                                $jString = "0" . $j;
                            } else if ($j == 24) {
                                $jString = "00";
                            } else {
                                $jString = $j;
                            }
                            $concatString = "h" . $iString . $jString;
                            $input = "<td><input type=\"checkbox\" id=\"" . $concatString . "\" name=\"" . $concatString
                                    . "\" onclick=\"checkValidHours()\" class=\"hourcheckbox\" value=\""
                                    . $concatString . "\"><label for=\"" . $concatString . "\"> " . $iString . ":00 - " . $jString . ":00 </label>"
                                    . "&nbsp;&nbsp;</td>";
                            echo $input;
                        }
                        ?>
                    </table>
                    <br/>

                    <button type="button" onclick="selectAllHours(true)">Select all</button>
                    <button type="button" onclick="selectAllHours(false)">Select none</button>

                </div>
                <h4>Time Zone Settings</h4>
                <div class="formsection" style="max-width:600px;">
                    <label for="timezone"></label>
                    <select name="timezone" id="timezone">
                        <option value="t-1200" id="t-1200">(UTC-12:00) International Date Line West</option>
                        <option value="t-1100" id="t-1100">(UTC-11:00) Midway Island, Samoa</option>
                        <option value="t-1000" id="t-1000">(UTC-10:00) Hawaii</option>
                        <option value="t-0900" id="t-0900">(UTC-09:00) Alaska</option>
                        <option value="t-0800" id="t-0800">(UTC-08:00) Pacific Time (US & Canada)</option>
                        <option value="t-0700" id="t-0700">(UTC-07:00) Mountain Time (US & Canada)</option>
                        <option value="t-0600" id="t-0600">(UTC-06:00) Central Time (US & Canada)</option>
                        <option value="t-0500" id="t-0500">(UTC-05:00) Eastern Time (US & Canada)</option>
                        <option value="t-0400" id="t-0400">(UTC-04:00) Atlantic Time (Canada)</option>
                        <option value="t-0330" id="t-0330">(UTC-03:30) Newfoundland</option>
                        <option value="t-0300" id="t-0300">(UTC-03:00) Greenland</option>
                        <option value="t-0200" id="t-0200">(UTC-02:00) Mid-Atlantic</option>
                        <option value="t-0100" id="t-0100">(UTC-01:00) Cape Verde Is.</option>
                        <option value="t0000" id="t0000">(UTC+00:00) UTC : Dublin, Edinburgh, Lisbon, London</option>
                        <option value="t0100" id="t0100">(UTC+01:00) Brussels, Copenhagen, Madrid, Paris</option>
                        <option value="t0200" id="t0200">(UTC+02:00) Athens, Bucharest, Istanbul</option>
                        <option value="t0300" id="t0300">(UTC+03:00) Moscow, St. Petersburg, Volgograd</option>
                        <option value="t0330" id="t0330">(UTC+03:30) Tehran</option>
                        <option value="t0400" id="t0400">(UTC+04:00) Abu Dhabi, Muscat</option>
                        <option value="t0430" id="t0430">(UTC+04:30) Kabul</option>
                        <option value="t0500" id="t0500">(UTC+05:00) Islamabad, Karachi, Tashkent</option>
                        <option value="t0530" id="t0530">(UTC+05:30) Chennai, Kolkata, Mumbai, New Delhi</option>
                        <option value="t0545" id="t0545">(UTC+05:45) Kathmandu</option>
                        <option value="t0600" id="t0600">(UTC+06:00) Almaty, Novosibirsk</option>
                        <option value="t0630" id="t0630">(UTC+06:30) Yangon (Rangoon)</option>
                        <option value="t0700" id="t0700">(UTC+07:00) Bangkok, Hanoi, Jakarta</option>
                        <option value="t0800" id="t0800">(UTC+08:00) Kuala Lumpur, Singapore</option>
                        <option value="t0900" id="t0900">(UTC+09:00) Osaka, Sapporo, Tokyo</option>
                        <option value="t0930" id="t0930">(UTC+09:30) Adelaide</option>
                        <option value="t1000" id="t1000">(UTC+10:00) Canberra, Melbourne, Sydney</option>
                        <option value="t1100" id="t1100">(UTC+11:00) Magadan, Solomon Is., New Caledonia</option>
                        <option value="t1200" id="t1200">(UTC+12:00) Auckland, Wellington</option>
                        <option value="t1300" id="t1300">(UTC+13:00) Nuku'alofa</option>
                    </select>
                </div>
                <br/><br/>
                <div class="container">
                    <div class="center">
                        <input type="submit" id="savesettingsbutton" value="Save Settings">
                    </div>
                </div>

            </form>
        </p>
    </div>
    <script src="src/ajax/Collapsibles.js"></script>
</body>
</html>