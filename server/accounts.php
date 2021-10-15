<?php

namespace ArtRetweeter;

require_once "core.php";

function removeAccount($userAuth) {
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id']) {
        echo encodeStatusInformation(StatusCodes::INVALID_INPUT, "Parameters are not set correctly.");
        exit;
    }

    validateUserAuth($userAuth);

    removeAccountFromDB($userAuth['twitter_id']);
}

function commitAutomationSettings($userAuth) {
    $automationSettings = json_decode(str_replace("%20", " ", filter_input(INPUT_POST, 'automation_settings', FILTER_SANITIZE_URL)), true);
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id'] || !$automationSettings) {
        echo encodeStatusInformation(StatusCodes::INVALID_INPUT, "Parameters are not set correctly.");
        exit;
    }

    validateUserAuth($userAuth);

    commitAutomationSettingsInDB($automationSettings);
}

function getAutomationSettings($userAuth) {
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id']) {
        echo encodeStatusInformation(StatusCodes::INVALID_INPUT, "Parameters are not set correctly.");
        exit;
    }

    validateUserAuth($userAuth);

    getAutomationSettingsInDB($userAuth);
}
