<?php

// These two parameters are returned by Twitter in the URL: we'll need them later
$state = htmlspecialchars($_GET['state']);
$code = htmlspecialchars($_GET['code']);

// This identifies our application
$appAuth = base64_encode(APIKeys::twitter_oauth2_client_id . ":" . APIKeys::twitter_oauth2_client_secret);

$curl = curl_init("https://api.twitter.com/2/oauth2/token");
curl_setopt($curl, CURLOPT_RETURNTRANSFER, 1);
curl_setopt($curl, CURLOPT_HTTPHEADER, array(
    'Content-Type: application/x-www-form-urlencoded',
    "Authorization: Basic $appAuth"
));

// Use the code verifier from the user session
$postFields = http_build_query(array(
    "code" => $code, "grant_type" => "authorization_code", "client_id" => "your Twitter v2 client ID",
    "redirect_uri" => "your callback URL", "code_verifier" => $_SESSION['code_verifier']
        ));
curl_setopt($curl, CURLOPT_POST, 1);
curl_setopt($curl, CURLOPT_POSTFIELDS, $postFields);
$content = curl_exec($curl);

// Access token is in this form:
/* stdClass Object
(
    [token_type] => bearer
    [expires_in] => 7200
    [access_token] => somestringofcharacters
    [scope] => tweet.write users.read tweet.read offline.access
    [refresh_token] => someotherstringofcharacters
)
 */
$accessTokenObject = json_decode($content);

// Making a request. Note that OAuth 2.0 user access tokens are bearer tokens!

$connection = new TwitterOAuth("your Twitter v2 client ID", "your Twitter v2 client secret", null, $accessTokenObject->access_token);
$connection->setApiVersion('2');
$params = ["text" => "This is a test tweet."];
$connection->setBearer($accessTokenObject);
// This endpoint requires JSON data, so set the last parameter to true
$response = $connection->post("tweets", $params, true);