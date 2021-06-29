<?php

namespace ArtRetweeter;

require_once "core.php";

function removeAccount($userAuth) {
    if (!$userAuth['access_token'] || !$userAuth['access_token_secret'] || !$userAuth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    validateUserAuth($userAuth);

    removeAccountFromDB($userAuth['twitter_id']);
}
