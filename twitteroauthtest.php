<?php

// These two functions I got off the internet somewhere... I forget where, but they do the job correctly.
// I modified the openssl_random_pseudo_bytes call to use 128 bytes instead of 32 for better security.

function base64url_encode($plainText) {
    $base64 = base64_encode($plainText);
    $base64 = trim($base64, "=");
    $base64url = strtr($base64, '+/', '-_');
    return ($base64url);
}

function generatePKCEVerifierAndChallenge() {
    $random = bin2hex(openssl_random_pseudo_bytes(128));
    $verifier = OAuth::base64url_encode(pack('H*', $random));
    $challenge = OAuth::base64url_encode(pack('H*', hash('sha256', $verifier)));
    return [$verifier, $challenge];
}

$array = generatePKCEVerifierAndChallenge();
$code_verifier = $array[0];
$code_challenge = $array[1];

// Store the code verifier in the user session; we will need this in the callback
$_SESSION['code_verifier'] = $code_verifier;

$paramsArray['response_type'] = "code";
$paramsArray['client_id'] = "your Twitter v2 client ID";
$paramsArray['redirect_uri'] = "your callback URL";
// The code challenge, which will be used later as part of the verification process
$paramsArray['code_challenge'] = $code_challenge;
// It isn't clear what this "state" parameter is for, but the request will not work without it
$paramsArray['state'] = "state";
// There is also a "plain" method, but this is not advisable to use due to weaker security
$paramsArray['code_challenge_method'] = "s256";
// Modify scopes as needed; this will change the permissions the user is asked to give, and the resulting permissions for the access token
$paramsArray['scope'] = urlencode("tweet.read tweet.write offline.access users.read");

// Construct the request URL
$url = "https://twitter.com/i/oauth2/authorize?";
foreach ($paramsArray as $key => $value) {
    $url .= $key . "=" . $value . "&";
}
// Remove the last &; there will always be parameters, so we can do this safely
$url = substr($url, 0, -1);

// Send user to authorization URL

