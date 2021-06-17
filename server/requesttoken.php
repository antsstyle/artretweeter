<?php

require "core.php";
require "twitteroauth/vendor/autoload.php";

use Abraham\TwitterOAuth\TwitterOAuth;

$request_method = filter_var(getenv('REQUEST_METHOD'), FILTER_SANITIZE_STRING);

if ($request_method == "POST") {
    $connection = new TwitterOAuth($consumerkey, $consumersecret);
    $response = $connection->oauth("oauth/request_token", ["oauth_callback" => "oob"]);
    echo encodeTwitterResponseInformation($connection, $response);
}