<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\Core;

class Accounts {

    public static function removeAccount($userAuth) {
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id']) {
            echo Core::encodeStatusInformation(StatusCodes::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }

        Core::validateUserAuth($userAuth);

        Core::removeAccountFromDB($userAuth['twitter_id']);
    }

    public static function commitAutomationSettings($userAuth) {
        $automationSettings = json_decode(str_replace("%20", " ", filter_input(INPUT_POST, 'automation_settings', FILTER_SANITIZE_URL)), true);
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id'] || !$automationSettings) {
            echo Core::encodeStatusInformation(StatusCodes::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }

        Core::validateUserAuth($userAuth);

        Core::commitAutomationSettingsInDB($automationSettings);
    }

    public static function getAutomationSettings($userAuth) {
        if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id']) {
            echo Core::encodeStatusInformation(StatusCodes::INVALID_INPUT, "Parameters are not set correctly.");
            exit;
        }

        Core::validateUserAuth($userAuth);

        Core::getAutomationSettingsInDB($userAuth);
    }

}
