<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\StatusCode;

Session::checkSession();

$userTwitterID = $_SESSION['usertwitterid'];

if (!$_SESSION['usertwitterid']) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
}

$invalidInputs = [];

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

function saveAutomationSettings($userTwitterID) {
    $enableAutomation = filter_input(INPUT_POST, "enableautomatedretweeting", FILTER_SANITIZE_STRING);

    if (is_null($enableAutomation)) {
        $enableAutomation = "N";
    } else if ($enableAutomation === "enable_automated_retweeting") {
        $enableAutomation = "Y";
    } else {
        error_log("Invalid automation enabled setting.");
        return "Invalid automation enabled setting.";
    }

    $ignoreOldTweets = filter_input(INPUT_POST, "ignoreoldtweets", FILTER_SANITIZE_STRING);

    if (is_null($ignoreOldTweets)) {
        $ignoreOldTweets = "N";
    } else if ($ignoreOldTweets === "ignore_old_tweets") {
        $ignoreOldTweets = "Y";
    } else {
        error_log("Invalid ignore old tweets setting.");
        return "Invalid ignore old tweets setting.";
    }

    $ignoreOldTweetsDate = filter_input(INPUT_POST, "ignoreoldtweetsdate", FILTER_SANITIZE_STRING);
    if (is_null($ignoreOldTweetsDate) && $ignoreOldTweets == "Y") {
        error_log("Invalid ignore old tweets date setting.");
        return "Invalid ignore old tweets date setting.";
    } else if ($ignoreOldTweetsDate === false) {
        error_log("Invalid ignore old tweets date setting.");
        return "Invalid ignore old tweets date setting.";
    }

    $includeTextEnabled = filter_input(INPUT_POST, "includetextenabled", FILTER_SANITIZE_STRING);

    if (is_null($includeTextEnabled)) {
        $includeTextEnabled = "N";
    } else if ($includeTextEnabled === "include_text_enabled") {
        $includeTextEnabled = "Y";
    } else {
        error_log("Invalid include text enabled setting.");
        return "Invalid include text enabled setting.";
    }

    $includeTextOperation = filter_input(INPUT_POST, "includetextoperation", FILTER_SANITIZE_STRING);

    if (is_null($includeTextOperation)) {
        $includeTextOperation = "N";
    } else if ($includeTextOperation === "all") {
        $includeTextOperation = "All of these words";
    } else if ($includeTextOperation === "any") {
        $includeTextOperation = "Any of these words";
    } else if ($includeTextOperation === "exact") {
        $includeTextOperation = "This exact phrase";
    } else {
        error_log("Invalid include text operation setting.");
        return "Invalid include text operation setting.";
    }

    $includeText = filter_input(INPUT_POST, "includetext", FILTER_SANITIZE_STRING);

    if (is_null($includeText)) {
        $includeText = "";
    } else if (strlen($includeText) > 50) {
        error_log("Invalid include text setting.");
        return "Invalid include text setting.";
    }

    $excludeTextEnabled = filter_input(INPUT_POST, "excludetextenabled", FILTER_SANITIZE_STRING);

    if (is_null($excludeTextEnabled)) {
        $excludeTextEnabled = "N";
    } else if ($excludeTextEnabled === "exclude_text_enabled") {
        $excludeTextEnabled = "Y";
    } else {
        error_log("Invalid exclude text enabled setting.");
        return "Invalid exclude text enabled setting";
    }

    $excludeTextOperation = filter_input(INPUT_POST, "excludetextoperation", FILTER_SANITIZE_STRING);

    if (is_null($excludeTextOperation)) {
        error_log("Invalid exclude text operation setting.");
    } else if ($excludeTextOperation === "all") {
        $excludeTextOperation = "All of these words";
    } else if ($excludeTextOperation === "any") {
        $excludeTextOperation = "Any of these words";
    } else if ($excludeTextOperation === "exact") {
        $excludeTextOperation = "This exact phrase";
    } else {
        error_log("Invalid exclude text operation setting.");
        return "Invalid exclude text operation setting";
    }

    $excludeText = filter_input(INPUT_POST, "excludetext", FILTER_SANITIZE_STRING);

    if (is_null($excludeText)) {
        $excludeText = "";
    } else if (strlen($excludeText) > 50) {
        error_log("Invalid exclude text setting.");
        return "Invalid exclude text setting";
    }

    $excludeTweetsMetrics = filter_input(INPUT_POST, "excludetweetsmetrics", FILTER_SANITIZE_STRING);
    if (is_null($excludeTweetsMetrics)) {
        $excludeTweetsMetrics = "N";
    } else if ($excludeTweetsMetrics === "exclude_tweets_metrics") {
        $excludeTweetsMetrics = "Y";
    } else {
        error_log("Invalid exclude tweet metrics setting.");
        return "Invalid exclude tweet metrics setting.";
    }

    $metricsPercent = filter_input(INPUT_POST, "metricspercent", FILTER_SANITIZE_NUMBER_INT);
    if (is_null($metricsPercent)) {
        error_log("Invalid metrics percent setting.");
        return "Invalid metrics percent setting.";
    } else if ($metricsPercent < 20 || $metricsPercent > 75) {
        error_log("Invalid metrics percent setting.");
        return "Invalid metrics percent setting.";
    }

    $metricsMethod = filter_input(INPUT_POST, "metricsmethod", FILTER_SANITIZE_STRING);

    if (is_null($metricsMethod)) {
        error_log("Invalid metrics method setting.");
        return "Invalid metrics method setting.";
    } else if ($metricsMethod === "mean_average") {
        $metricsMethod = "Mean Average";
    } else if ($metricsMethod === "adaptive") {
        $metricsMethod = "Adaptive";
    } else {
        error_log("Invalid metrics method setting.");
        return "Invalid metrics method setting.";
    }

    $dayFlags = "";

    $mondayEnabled = filter_input(INPUT_POST, "mondayenabled", FILTER_SANITIZE_STRING);
    if (is_null($mondayEnabled)) {
        $dayFlags .= "N";
    } else if ($mondayEnabled === "monday_enabled") {
        $dayFlags .= "Y";
    } else {
        error_log("Invalid monday enabled setting.");
        return "Invalid monday enabled setting.";
    }

    $tuesdayEnabled = filter_input(INPUT_POST, "tuesdayenabled", FILTER_SANITIZE_STRING);
    if (is_null($tuesdayEnabled)) {
        $dayFlags .= "N";
    } else if ($tuesdayEnabled === "tuesday_enabled") {
        $dayFlags .= "Y";
    } else {
        error_log("Invalid tuesday enabled setting.");
        return "Invalid tuesday enabled setting.";
    }

    $wednesdayEnabled = filter_input(INPUT_POST, "wednesdayenabled", FILTER_SANITIZE_STRING);
    if (is_null($wednesdayEnabled)) {
        $dayFlags .= "N";
    } else if ($wednesdayEnabled === "wednesday_enabled") {
        $dayFlags .= "Y";
    } else {
        error_log("Invalid wednesday enabled setting.");
        return "Invalid wednesday enabled setting.";
    }

    $thursdayEnabled = filter_input(INPUT_POST, "thursdayenabled", FILTER_SANITIZE_STRING);
    if (is_null($thursdayEnabled)) {
        $dayFlags .= "N";
    } else if ($thursdayEnabled === "thursday_enabled") {
        $dayFlags .= "Y";
    } else {
        error_log("Invalid thursday enabled setting.");
        return "Invalid thursday enabled setting.";
    }

    $fridayEnabled = filter_input(INPUT_POST, "fridayenabled", FILTER_SANITIZE_STRING);
    if (is_null($fridayEnabled)) {
        $dayFlags .= "N";
    } else if ($fridayEnabled === "friday_enabled") {
        $dayFlags .= "Y";
    } else {
        error_log("Invalid friday enabled setting.");
        return "Invalid friday enabled setting.";
    }

    $saturdayEnabled = filter_input(INPUT_POST, "saturdayenabled", FILTER_SANITIZE_STRING);
    if (is_null($saturdayEnabled)) {
        $dayFlags .= "N";
    } else if ($saturdayEnabled === "saturday_enabled") {
        $dayFlags .= "Y";
    } else {
        error_log("Invalid saturday enabled setting.");
        return "Invalid saturday enabled setting.";
    }

    $sundayEnabled = filter_input(INPUT_POST, "sundayenabled", FILTER_SANITIZE_STRING);
    if (is_null($sundayEnabled)) {
        $dayFlags .= "N";
    } else if ($sundayEnabled === "sunday_enabled") {
        $dayFlags .= "Y";
    } else {
        error_log("Invalid sunday enabled setting.");
        return "Invalid sunday enabled setting.";
    }

    if (strpos($dayFlags, "Y") === false) {
        error_log("No days selected.");
        return "No days selected.";
    }

    $hourFlags = "";
    $minuteFlags = "";

    for ($i = 0; $i < 24; $i++) {
        $j = $i + 1;
        if ($i < 10) {
            $iString = "0" . strval($i);
        } else {
            $iString = strval($i);
        }
        if ($j < 10) {
            $jString = "0" . strval($j);
        } else if ($j == 24) {
            $jString = "00";
        } else {
            $jString = strval($j);
        }
        $concatString = "h" . $iString . $jString;
        error_log("Concat string: $concatString");
        $timePeriod = filter_input(INPUT_POST, $concatString, FILTER_SANITIZE_STRING);
        error_log("Timeperiod $concatString: $timePeriod");
        if (is_null($timePeriod)) {
            $hourFlags .= "N";
        } else if ($timePeriod === $concatString) {
            $hourFlags .= "Y";
        } else {
            error_log("Invalid hour flag setting: $timePeriod");
            return "Invalid hour flag setting: $timePeriod";
        }
    }

    error_log("Hour flags: $hourFlags");

    if (strpos($hourFlags, "Y") === false) {
        error_log("No hours selected.");
        return "No hours selected.";
    }

    $minute0 = filter_input(INPUT_POST, "minute0", FILTER_SANITIZE_STRING);
    if (is_null($minute0)) {
        $minuteFlags .= "N";
    } else if ($minute0 === "minute_0") {
        $minuteFlags .= "Y";
    } else {
        error_log("Invalid minutes setting.");
        return "Invalid minutes setting.";
    }

    $minute15 = filter_input(INPUT_POST, "minute15", FILTER_SANITIZE_STRING);
    if (is_null($minute15)) {
        $minuteFlags .= "N";
    } else if ($minute15 === "minute_15") {
        $minuteFlags .= "Y";
    } else {
        error_log("Invalid minutes setting.");
        return "Invalid minutes setting.";
    }

    $minute30 = filter_input(INPUT_POST, "minute30", FILTER_SANITIZE_STRING);
    if (is_null($minute30)) {
        $minuteFlags .= "N";
    } else if ($minute30 === "minute_30") {
        $minuteFlags .= "Y";
    } else {
        error_log("Invalid minutes setting.");
        return "Invalid minutes setting.";
    }

    $minute45 = filter_input(INPUT_POST, "minute45", FILTER_SANITIZE_STRING);
    if (is_null($minute45)) {
        $minuteFlags .= "N";
    } else if ($minute45 === "minute_45") {
        $minuteFlags .= "Y";
    } else {
        error_log("Invalid minutes setting.");
        return "Invalid minutes setting.";
    }

    if (strpos($minuteFlags, "Y") === false) {
        error_log("No minutes selected.");
        return "No minutes selected.";
    }

    $timezone = filter_input(INPUT_POST, "timezone", FILTER_SANITIZE_STRING);
    if (is_null($timezone)) {
        error_log("No timezone detected.");
        return "No timezone selected.";
    }

    $timezoneStrings = ["t-1200", "t-1100", "t-1000", "t-0900", "t-0800", "t-0700", "t-0600", "t-0500", "t-0400", "t-0330",
        "t-0300", "t-0200", "t-0100", "t0000", "t0100", "t0200", "t0300", "t0330", "t0400", "t0430", "t0500", "t0530",
        "t0545", "t0600", "t0630", "t0700", "t0800", "t0900", "t0930", "t1000", "t1100", "t1200", "t1300"];

    if (!in_array($timezone, $timezoneStrings)) {
        error_log("Timezone string is invalid or not supported: $timezone");
        return "Timezone string is invalid or not supported: $timezone";
    } else {
        if (strpos($timezone, "-") !== false) {
            $hourOffset = -intval(substr($timezone, 2, 2));
            $minuteOffset = intval(substr($timezone, 4, 2));
        } else {
            $hourOffset = intval(substr($timezone, 1, 2));
            $minuteOffset = intval(substr($timezone, 3, 2));
        }
    }


    $aS['hourflags'] = $hourFlags;
    $aS['minuteflags'] = $minuteFlags;
    $aS['dayflags'] = $dayFlags;
    $aS['automationenabled'] = $enableAutomation;
    $aS['usertwitterid'] = $userTwitterID;
    $aS['includedtextenabled'] = $includeTextEnabled;
    $aS['excludedtextenabled'] = $excludeTextEnabled;
    $aS['includetextcondition'] = $includeTextOperation;
    $aS['excludetextcondition'] = $excludeTextOperation;
    $aS['includedtext'] = $includeText;
    $aS['excludedtext'] = $excludeText;
    $aS['oldtweetcutoffdateenabled'] = $ignoreOldTweets;
    $aS['oldtweetcutoffdate'] = $ignoreOldTweetsDate;
    $aS['timezonehouroffset'] = $hourOffset;
    $aS['timezoneminuteoffset'] = $minuteOffset;
    $aS['metricsmeasurementtype'] = $metricsMethod;
    $aS['retweetpercent'] = $metricsPercent;

    $automationSavedSuccess = Core::commitAutomationSettingsInDB($aS, false);
    return $automationSavedSuccess;
}

$result = saveAutomationSettings($userTwitterID);
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
            $homepage = Config::HOMEPAGE_URL;
            $settingspage = Config::SETTINGSPAGE_URL;
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
</html>
