<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Core\CoreDB;

class UserSettings {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function saveSimpleAutomationSettings($userTwitterID) {
        $enableAutomation = htmlspecialchars($_POST['enableautomatedretweeting_simplesettings']);
        if ($enableAutomation === "") {
            $enableAutomation = "N";
        } else if ($enableAutomation === "enable_automated_retweeting_simplesettings") {
            $enableAutomation = "Y";
        } else {
            self::$logger->error("Invalid automation enabled setting.");
            return "Invalid automation enabled setting.";
        }

        $offset = filter_input(INPUT_POST, "simplesettingsform_timezone", FILTER_SANITIZE_NUMBER_INT);
        if (is_null($offset) || $offset === false) {
            self::$logger->error("Invalid timezone offset.");
            return "Invalid timezone offset.";
        }
        if ($offset == 0) {
            $hourOffset = 0;
            $minuteOffset = 0;
        } else {
            $hourOffset = floor($offset / 60);
            $minuteOffset = abs($offset % 60);
        }

        $aS['hourflags'] = "YYYYYYYYYYYYYYYYYYYYYYYY";
        $aS['dayflags'] = "YYYYYYY";
        $aS['minuteflags'] = "YYYY";
        $aS['automationenabled'] = $enableAutomation;
        $aS['usertwitterid'] = $userTwitterID;
        $aS['includetextenabled'] = "N";
        $aS['excludetextenabled'] = "N";
        $aS['includetextcondition'] = "Any of these words";
        $aS['excludetextcondition'] = "Any of these words";
        $aS['includetext'] = null;
        $aS['excludetext'] = null;
        $aS['oldtweetcutoffdateenabled'] = "N";
        $aS['oldtweetcutoffdate'] = null;
        $aS['timezonehouroffset'] = $hourOffset;
        $aS['timezoneminuteoffset'] = $minuteOffset;
        $aS['metricsmeasurementtype'] = "Adaptive";
        $aS['retweetpercent'] = 20;
        $aS['imagesenabled'] = "Y";
        $aS['gifsenabled'] = "N";
        $aS['videosenabled'] = "N";
        $aS['settingstype'] = "Simple";

        $automationSavedSuccess = CoreDB::commitAutomationSettingsInDB($aS);
        return $automationSavedSuccess;
    }

    public static function saveAutomationSettings($userTwitterID) {
        $simpleSettingsForm = htmlspecialchars($_POST['simplesettingsform']);
        if ($simpleSettingsForm !== "") {
            return UserSettings::saveSimpleAutomationSettings($userTwitterID);
        }


        $enableAutomation = htmlspecialchars($_POST['enableautomatedretweeting']);

        if ($enableAutomation === "") {
            $enableAutomation = "N";
        } else if ($enableAutomation === "enable_automated_retweeting") {
            $enableAutomation = "Y";
        } else {
            self::$logger->error("Invalid automation enabled setting.");
            return "Invalid automation enabled setting.";
        }

        $ignoreOldTweets = htmlspecialchars($_POST["ignoreoldtweets"]);

        if ($ignoreOldTweets === "") {
            $ignoreOldTweets = "N";
        } else if ($ignoreOldTweets === "ignore_old_tweets") {
            $ignoreOldTweets = "Y";
        } else {
            self::$logger->error("Invalid ignore old tweets setting.");
            return "Invalid ignore old tweets setting.";
        }

        $ignoreOldTweetsDate = htmlspecialchars($_POST["ignoreoldtweetsdate"]);
        if ($ignoreOldTweetsDate === "" && $ignoreOldTweets == "Y") {
            self::$logger->error("Invalid ignore old tweets date setting.");
            return "Invalid ignore old tweets date setting.";
        } else if ($ignoreOldTweetsDate === "") {
            $ignoreOldTweetsDate = null;
        }

        $includeTextEnabled = htmlspecialchars($_POST["includetextenabled"]);

        if ($includeTextEnabled === "") {
            $includeTextEnabled = "N";
        } else if ($includeTextEnabled === "include_text_enabled") {
            $includeTextEnabled = "Y";
        } else {
            self::$logger->error("Invalid include text enabled setting.");
            return "Invalid include text enabled setting.";
        }

        $includeTextOperation = htmlspecialchars($_POST["includetextoperation"]);

        if ($includeTextOperation === "all") {
            $includeTextOperation = "All of these words";
        } else if ($includeTextOperation === "any") {
            $includeTextOperation = "Any of these words";
        } else if ($includeTextOperation === "exact") {
            $includeTextOperation = "This exact phrase";
        } else {
            self::$logger->error("Invalid include text operation setting.");
            return "Invalid include text operation setting.";
        }

        $includeText = htmlspecialchars($_POST["includetext"]);

        if ($includeText === "") {
            $includeText = null;
        } else if (strlen($includeText) > 50) {
            self::$logger->error("Invalid include text setting.");
            return "Invalid include text setting.";
        }

        $excludeTextEnabled = htmlspecialchars($_POST["excludetextenabled"]);

        if ($excludeTextEnabled === "") {
            $excludeTextEnabled = "N";
        } else if ($excludeTextEnabled === "exclude_text_enabled") {
            $excludeTextEnabled = "Y";
        } else {
            self::$logger->error("Invalid exclude text enabled setting.");
            return "Invalid exclude text enabled setting";
        }

        $excludeTextOperation = htmlspecialchars($_POST["excludetextoperation"]);

        if ($excludeTextOperation === "all") {
            $excludeTextOperation = "All of these words";
        } else if ($excludeTextOperation === "any") {
            $excludeTextOperation = "Any of these words";
        } else if ($excludeTextOperation === "exact") {
            $excludeTextOperation = "This exact phrase";
        } else {
            self::$logger->error("Invalid exclude text operation setting.");
            return "Invalid exclude text operation setting";
        }

        $excludeText = htmlspecialchars($_POST["excludetext"]);

        if ($excludeText === "") {
            $excludeText = null;
        } else if (strlen($excludeText) > 50) {
            self::$logger->error("Invalid exclude text setting.");
            return "Invalid exclude text setting";
        }

        $metricsPercent = filter_input(INPUT_POST, "metricspercent", FILTER_SANITIZE_NUMBER_INT);
        if (is_null($metricsPercent)) {
            self::$logger->error("Invalid metrics percent setting.");
            return "Invalid metrics percent setting.";
        } else if ($metricsPercent < 20 || $metricsPercent > 75) {
            self::$logger->error("Invalid metrics percent setting.");
            return "Invalid metrics percent setting.";
        }

        $metricsMethod = htmlspecialchars($_POST["metricsmethod"]);

        if ($metricsMethod === "mean_average") {
            $metricsMethod = "Mean Average";
        } else if ($metricsMethod === "adaptive") {
            $metricsMethod = "Adaptive";
        } else {
            self::$logger->error("Invalid metrics method setting.");
            return "Invalid metrics method setting.";
        }

        $imagesEnabled = htmlspecialchars($_POST["imagesenabled"]);
        if ($imagesEnabled === "") {
            $imagesEnabled = "N";
        } else if ($imagesEnabled === "images_enabled") {
            $imagesEnabled = "Y";
        } else {
            self::$logger->error("Invalid images enabled setting.");
            return "Invalid images enabled setting.";
        }

        $gifsEnabled = htmlspecialchars($_POST["gifsenabled"]);
        if ($gifsEnabled === "") {
            $gifsEnabled = "N";
        } else if ($gifsEnabled === "gifs_enabled") {
            $gifsEnabled = "Y";
        } else {
            self::$logger->error("Invalid gifs enabled setting.");
            return "Invalid gifs enabled setting.";
        }

        $videosEnabled = htmlspecialchars($_POST["videosenabled"]);
        if ($videosEnabled === "") {
            $videosEnabled = "N";
        } else if ($videosEnabled === "videos_enabled") {
            $videosEnabled = "Y";
        } else {
            self::$logger->error("Invalid videos enabled setting.");
            return "Invalid videos enabled setting.";
        }


        $dayFlags = "";

        $mondayEnabled = htmlspecialchars($_POST["mondayenabled"]);
        if ($mondayEnabled === "") {
            $dayFlags .= "N";
        } else if ($mondayEnabled === "monday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid monday enabled setting.");
            return "Invalid monday enabled setting.";
        }

        $tuesdayEnabled = htmlspecialchars($_POST["tuesdayenabled"]);
        if ($tuesdayEnabled === "") {
            $dayFlags .= "N";
        } else if ($tuesdayEnabled === "tuesday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid tuesday enabled setting.");
            return "Invalid tuesday enabled setting.";
        }

        $wednesdayEnabled = htmlspecialchars($_POST["wednesdayenabled"]);
        if ($wednesdayEnabled === "") {
            $dayFlags .= "N";
        } else if ($wednesdayEnabled === "wednesday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid wednesday enabled setting.");
            return "Invalid wednesday enabled setting.";
        }

        $thursdayEnabled = htmlspecialchars($_POST["thursdayenabled"]);
        if ($thursdayEnabled === "") {
            $dayFlags .= "N";
        } else if ($thursdayEnabled === "thursday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid thursday enabled setting.");
            return "Invalid thursday enabled setting.";
        }

        $fridayEnabled = htmlspecialchars($_POST["fridayenabled"]);
        if ($fridayEnabled === "") {
            $dayFlags .= "N";
        } else if ($fridayEnabled === "friday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid friday enabled setting.");
            return "Invalid friday enabled setting.";
        }

        $saturdayEnabled = htmlspecialchars($_POST["saturdayenabled"]);
        if ($saturdayEnabled === "") {
            $dayFlags .= "N";
        } else if ($saturdayEnabled === "saturday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid saturday enabled setting.");
            return "Invalid saturday enabled setting.";
        }

        $sundayEnabled = htmlspecialchars($_POST["sundayenabled"]);
        if ($sundayEnabled === "") {
            $dayFlags .= "N";
        } else if ($sundayEnabled === "sunday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid sunday enabled setting.");
            return "Invalid sunday enabled setting.";
        }

        if (strpos($dayFlags, "Y") === false) {
            self::$logger->error("No days selected.");
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

            $timePeriod = htmlspecialchars($_POST[$concatString]);
            if ($timePeriod === "") {
                $hourFlags .= "N";
            } else if ($timePeriod === $concatString) {
                $hourFlags .= "Y";
            } else {
                self::$logger->error("Invalid hour flag setting: $timePeriod");
                return "Invalid hour flag setting: $timePeriod";
            }
        }

        self::$logger->debug("Hour flags: $hourFlags");

        if (strpos($hourFlags, "Y") === false) {
            self::$logger->error("No hours selected. User twitter ID: $userTwitterID");
            return "No hours selected.";
        }

        $minute0 = htmlspecialchars($_POST["minute0"]);
        if ($minute0 === "") {
            $minuteFlags .= "N";
        } else if ($minute0 === "minute_0") {
            $minuteFlags .= "Y";
        } else {
            self::$logger->error("Invalid minutes setting.");
            return "Invalid minutes setting.";
        }

        $minute15 = htmlspecialchars($_POST["minute15"]);
        if ($minute15 === "") {
            $minuteFlags .= "N";
        } else if ($minute15 === "minute_15") {
            $minuteFlags .= "Y";
        } else {
            self::$logger->error("Invalid minutes setting.");
            return "Invalid minutes setting.";
        }

        $minute30 = htmlspecialchars($_POST["minute30"]);
        if ($minute30 === "") {
            $minuteFlags .= "N";
        } else if ($minute30 === "minute_30") {
            $minuteFlags .= "Y";
        } else {
            self::$logger->error("Invalid minutes setting.");
            return "Invalid minutes setting.";
        }

        $minute45 = htmlspecialchars($_POST["minute45"]);
        if ($minute45 === "") {
            $minuteFlags .= "N";
        } else if ($minute45 === "minute_45") {
            $minuteFlags .= "Y";
        } else {
            self::$logger->error("Invalid minutes setting.");
            return "Invalid minutes setting.";
        }

        if (strpos($minuteFlags, "Y") === false) {
            self::$logger->error("No minutes selected.");
            return "No minutes selected.";
        }

        $timezone = htmlspecialchars($_POST["timezone"]);
        if ($timezone === "") {
            self::$logger->error("No timezone detected.");
            return "No timezone selected.";
        }

        $timezoneStrings = ["t-1200", "t-1100", "t-1000", "t-0900", "t-0800", "t-0700", "t-0600", "t-0500", "t-0400", "t-0330",
            "t-0300", "t-0200", "t-0100", "t0000", "t0100", "t0200", "t0300", "t0330", "t0400", "t0430", "t0500", "t0530",
            "t0545", "t0600", "t0630", "t0700", "t0800", "t0900", "t0930", "t1000", "t1100", "t1200", "t1300"];

        if (!in_array($timezone, $timezoneStrings)) {
            self::$logger->error("Timezone string is invalid or not supported: $timezone");
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
        $aS['includetextenabled'] = $includeTextEnabled;
        $aS['excludetextenabled'] = $excludeTextEnabled;
        $aS['includetextcondition'] = $includeTextOperation;
        $aS['excludetextcondition'] = $excludeTextOperation;
        $aS['includetext'] = $includeText;
        $aS['excludetext'] = $excludeText;
        $aS['oldtweetcutoffdateenabled'] = $ignoreOldTweets;
        $aS['oldtweetcutoffdate'] = $ignoreOldTweetsDate;
        $aS['timezonehouroffset'] = $hourOffset;
        $aS['timezoneminuteoffset'] = $minuteOffset;
        $aS['metricsmeasurementtype'] = $metricsMethod;
        $aS['retweetpercent'] = $metricsPercent;
        $aS['imagesenabled'] = $imagesEnabled;
        $aS['gifsenabled'] = $gifsEnabled;
        $aS['videosenabled'] = $videosEnabled;
        $aS['settingstype'] = "Advanced";

        $automationSavedSuccess = CoreDB::commitAutomationSettingsInDB($aS);
        return $automationSavedSuccess;
    }

    public static function saveSimpleNonArtistAutomationSettings($userTwitterID) {
        $enableAutomation = htmlspecialchars($_POST['enableautomatedretweeting_simplesettings']);
        if ($enableAutomation === "") {
            $enableAutomation = "N";
        } else if ($enableAutomation === "enable_automated_retweeting_simplesettings") {
            $enableAutomation = "Y";
        } else {
            self::$logger->error("Invalid automation enabled setting.");
            return "Invalid automation enabled setting.";
        }

        $offset = filter_input(INPUT_POST, "simplesettingsform_timezone", FILTER_SANITIZE_NUMBER_INT);
        if (is_null($offset) || $offset === false) {
            self::$logger->error("Invalid timezone offset.");
            return "Invalid timezone offset.";
        }
        if ($offset == 0) {
            $hourOffset = 0;
            $minuteOffset = 0;
        } else {
            $hourOffset = floor($offset / 60);
            $minuteOffset = abs($offset % 60);
        }

        $aS['hourflags'] = "YYYYYYYYYYYYYYYYYYYYYYYY";
        $aS['dayflags'] = "YYYYYYY";
        $aS['automationenabled'] = $enableAutomation;
        $aS['usertwitterid'] = $userTwitterID;
        $aS['includetextenabled'] = "N";
        $aS['excludetextenabled'] = "N";
        $aS['includetextcondition'] = "Any of these words";
        $aS['excludetextcondition'] = "Any of these words";
        $aS['includetext'] = null;
        $aS['excludetext'] = null;
        $aS['oldtweetcutoffdateenabled'] = "N";
        $aS['oldtweetcutoffdate'] = null;
        $aS['timezonehouroffset'] = $hourOffset;
        $aS['timezoneminuteoffset'] = $minuteOffset;
        $aS['imagesenabled'] = "Y";
        $aS['gifsenabled'] = "N";
        $aS['videosenabled'] = "N";
        $aS['settingstype'] = "Simple";

        $automationSavedSuccess = CoreDB::commitNonArtistAutomationSettingsInDB($aS);
        return $automationSavedSuccess;
    }

    public static function saveNonArtistAutomationSettings($userTwitterID) {
        $simpleSettingsForm = htmlspecialchars($_POST['simplesettingsform']);
        if ($simpleSettingsForm !== "") {
            return UserSettings::saveSimpleNonArtistAutomationSettings($userTwitterID);
        }


        $enableAutomation = htmlspecialchars($_POST["enableautomatedretweeting"]);

        if ($enableAutomation === "") {
            $enableAutomation = "N";
        } else if ($enableAutomation === "enable_automated_retweeting") {
            $enableAutomation = "Y";
        } else {
            self::$logger->error("Invalid automation enabled setting.");
            return "Invalid automation enabled setting.";
        }

        $ignoreOldTweets = htmlspecialchars($_POST["ignoreoldtweets"]);

        if ($ignoreOldTweets === "") {
            $ignoreOldTweets = "N";
        } else if ($ignoreOldTweets === "ignore_old_tweets") {
            $ignoreOldTweets = "Y";
        } else {
            self::$logger->error("Invalid ignore old tweets setting.");
            return "Invalid ignore old tweets setting.";
        }

        $ignoreOldTweetsDate = htmlspecialchars($_POST["ignoreoldtweetsdate"]);
        if ($ignoreOldTweetsDate === "" && $ignoreOldTweets == "Y") {
            self::$logger->error("Invalid ignore old tweets date setting.");
            return "Invalid ignore old tweets date setting.";
        } else if ($ignoreOldTweetsDate === "") {
            $ignoreOldTweetsDate = null;
        }

        $includeTextEnabled = htmlspecialchars($_POST["includetextenabled"]);

        if ($includeTextEnabled === "") {
            $includeTextEnabled = "N";
        } else if ($includeTextEnabled === "include_text_enabled") {
            $includeTextEnabled = "Y";
        } else {
            self::$logger->error("Invalid include text enabled setting.");
            return "Invalid include text enabled setting.";
        }

        $includeTextOperation = htmlspecialchars($_POST["includetextoperation"]);

        if ($includeTextOperation === "all") {
            $includeTextOperation = "All of these words";
        } else if ($includeTextOperation === "any") {
            $includeTextOperation = "Any of these words";
        } else if ($includeTextOperation === "exact") {
            $includeTextOperation = "This exact phrase";
        } else {
            self::$logger->error("Invalid include text operation setting.");
            return "Invalid include text operation setting.";
        }

        $includeText = htmlspecialchars($_POST["includetext"]);

        if ($includeText === "") {
            $includeText = null;
        } else if (strlen($includeText) > 50) {
            self::$logger->error("Invalid include text setting.");
            return "Invalid include text setting.";
        }

        $excludeTextEnabled = htmlspecialchars($_POST["excludetextenabled"]);

        if ($excludeTextEnabled === "") {
            $excludeTextEnabled = "N";
        } else if ($excludeTextEnabled === "exclude_text_enabled") {
            $excludeTextEnabled = "Y";
        } else {
            self::$logger->error("Invalid exclude text enabled setting.");
            return "Invalid exclude text enabled setting";
        }

        $excludeTextOperation = htmlspecialchars($_POST["excludetextoperation"]);

        if ($excludeTextOperation === "all") {
            $excludeTextOperation = "All of these words";
        } else if ($excludeTextOperation === "any") {
            $excludeTextOperation = "Any of these words";
        } else if ($excludeTextOperation === "exact") {
            $excludeTextOperation = "This exact phrase";
        } else {
            self::$logger->error("Invalid exclude text operation setting.");
            return "Invalid exclude text operation setting";
        }

        $excludeText = htmlspecialchars($_POST["excludetext"]);

        if ($excludeText === "") {
            $excludeText = null;
        } else if (strlen($excludeText) > 50) {
            self::$logger->error("Invalid exclude text setting.");
            return "Invalid exclude text setting";
        }

        $imagesEnabled = htmlspecialchars($_POST["imagesenabled"]);
        if ($imagesEnabled === "") {
            $imagesEnabled = "N";
        } else if ($imagesEnabled === "images_enabled") {
            $imagesEnabled = "Y";
        } else {
            self::$logger->error("Invalid images enabled setting.");
            return "Invalid images enabled setting.";
        }

        $gifsEnabled = htmlspecialchars($_POST["gifsenabled"]);
        if ($gifsEnabled === "") {
            $gifsEnabled = "N";
        } else if ($gifsEnabled === "gifs_enabled") {
            $gifsEnabled = "Y";
        } else {
            self::$logger->error("Invalid gifs enabled setting.");
            return "Invalid gifs enabled setting.";
        }

        $videosEnabled = htmlspecialchars($_POST["videosenabled"]);
        if ($videosEnabled === "") {
            $videosEnabled = "N";
        } else if ($videosEnabled === "videos_enabled") {
            $videosEnabled = "Y";
        } else {
            self::$logger->error("Invalid videos enabled setting.");
            return "Invalid videos enabled setting.";
        }


        $dayFlags = "";

        $mondayEnabled = htmlspecialchars($_POST["mondayenabled"]);
        if ($mondayEnabled === "") {
            $dayFlags .= "N";
        } else if ($mondayEnabled === "monday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid monday enabled setting.");
            return "Invalid monday enabled setting.";
        }

        $tuesdayEnabled = htmlspecialchars($_POST["tuesdayenabled"]);
        if ($tuesdayEnabled === "") {
            $dayFlags .= "N";
        } else if ($tuesdayEnabled === "tuesday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid tuesday enabled setting.");
            return "Invalid tuesday enabled setting.";
        }

        $wednesdayEnabled = htmlspecialchars($_POST["wednesdayenabled"]);
        if ($wednesdayEnabled === "") {
            $dayFlags .= "N";
        } else if ($wednesdayEnabled === "wednesday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid wednesday enabled setting.");
            return "Invalid wednesday enabled setting.";
        }

        $thursdayEnabled = htmlspecialchars($_POST["thursdayenabled"]);
        if ($thursdayEnabled === "") {
            $dayFlags .= "N";
        } else if ($thursdayEnabled === "thursday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid thursday enabled setting.");
            return "Invalid thursday enabled setting.";
        }

        $fridayEnabled = htmlspecialchars($_POST["fridayenabled"]);
        if ($fridayEnabled === "") {
            $dayFlags .= "N";
        } else if ($fridayEnabled === "friday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid friday enabled setting.");
            return "Invalid friday enabled setting.";
        }

        $saturdayEnabled = htmlspecialchars($_POST["saturdayenabled"]);
        if ($saturdayEnabled === "") {
            $dayFlags .= "N";
        } else if ($saturdayEnabled === "saturday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid saturday enabled setting.");
            return "Invalid saturday enabled setting.";
        }

        $sundayEnabled = htmlspecialchars($_POST["sundayenabled"]);
        if ($sundayEnabled === "") {
            $dayFlags .= "N";
        } else if ($sundayEnabled === "sunday_enabled") {
            $dayFlags .= "Y";
        } else {
            self::$logger->error("Invalid sunday enabled setting.");
            return "Invalid sunday enabled setting.";
        }

        if (strpos($dayFlags, "Y") === false) {
            self::$logger->error("No days selected.");
            return "No days selected.";
        }

        $hourFlags = "";

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
            $timePeriod = htmlspecialchars($_POST[$concatString]);
            if ($timePeriod === "") {
                $hourFlags .= "N";
            } else if ($timePeriod === $concatString) {
                $hourFlags .= "Y";
            } else {
                self::$logger->error("Invalid hour flag setting: $timePeriod");
                return "Invalid hour flag setting: $timePeriod";
            }
        }

        self::$logger->debug("Hour flags: $hourFlags");

        if (strpos($hourFlags, "Y") === false) {
            self::$logger->error("No hours selected.");
            return "No hours selected.";
        }

        $timezone = htmlspecialchars($_POST["timezone"]);
        if ($timezone === "") {
            self::$logger->error("No timezone detected.");
            return "No timezone selected.";
        }

        $timezoneStrings = ["t-1200", "t-1100", "t-1000", "t-0900", "t-0800", "t-0700", "t-0600", "t-0500", "t-0400", "t-0330",
            "t-0300", "t-0200", "t-0100", "t0000", "t0100", "t0200", "t0300", "t0330", "t0400", "t0430", "t0500", "t0530",
            "t0545", "t0600", "t0630", "t0700", "t0800", "t0900", "t0930", "t1000", "t1100", "t1200", "t1300"];

        if (!in_array($timezone, $timezoneStrings)) {
            self::$logger->error("Timezone string is invalid or not supported: $timezone");
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
        $aS['dayflags'] = $dayFlags;
        $aS['automationenabled'] = $enableAutomation;
        $aS['usertwitterid'] = $userTwitterID;
        $aS['includetextenabled'] = $includeTextEnabled;
        $aS['excludetextenabled'] = $excludeTextEnabled;
        $aS['includetextcondition'] = $includeTextOperation;
        $aS['excludetextcondition'] = $excludeTextOperation;
        $aS['includetext'] = $includeText;
        $aS['excludetext'] = $excludeText;
        $aS['oldtweetcutoffdateenabled'] = $ignoreOldTweets;
        $aS['oldtweetcutoffdate'] = $ignoreOldTweetsDate;
        $aS['timezonehouroffset'] = $hourOffset;
        $aS['timezoneminuteoffset'] = $minuteOffset;
        $aS['imagesenabled'] = $imagesEnabled;
        $aS['gifsenabled'] = $gifsEnabled;
        $aS['videosenabled'] = $videosEnabled;
        $aS['settingstype'] = "Advanced";

        $automationSavedSuccess = CoreDB::commitNonArtistAutomationSettingsInDB($aS);
        return $automationSavedSuccess;
    }

}

UserSettings::initialiseLogger();
