<?php

namespace ArtRetweeter;

require_once "core.php";

function removeAccount($userauth) {
    if (!$userauth['access_token'] || !$userauth['access_token_secret'] || !$userauth['twitter_id']) {
        echo encodeErrorInformation("Parameters are not set correctly.");
        exit;
    }

    validateUserAuth($userauth);

    removeAccountFromDB($userauth['twitter_id']);
}
