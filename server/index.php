<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Abraham\TwitterOAuth\TwitterOAuth;

Session::checkSession();

if (!$_SESSION['usertwitterid']) {
    $connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret);
    try {
        $response = $connection->oauth("oauth/request_token", ["oauth_callback" => Config::OAUTH_CALLBACK]);
        $httpcode = $connection->getLastHttpCode();
        if ($httpcode != 200) {
            error_log("Failed to get request token!");
            // Show error page
        }
    } catch (\Exception $e) {
        error_log("Failed to get request token: " . print_r($e, true));
    }
    $oauth_token = $response['oauth_token'];
    $oauth_token_secret = $response['oauth_token_secret'];
    $oauth_callback_confirmed = $response['oauth_callback_confirmed'];
    $_SESSION['oauth_token'] = $oauth_token;
    $_SESSION['oauth_token_secret'] = $oauth_token_secret;
    $oauth_token_array['oauth_token'] = $oauth_token;
    try {
        $url = $connection->url('oauth/authenticate', array('oauth_token' => $oauth_token));
    } catch (\Exception $e) {
        error_log("Failed to authenticate request token: " . print_r($e, true));
    }
}
?>

<html>
    <head>
        <link rel="stylesheet" href="main.css" type="text/css">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="twitter:card" content="summary" />
        <meta name="twitter:site" content="@antsstyle" />
        <meta name="twitter:title" content="ArtRetweeter, an auto-retweeting app for artists" />
        <meta name="twitter:description" content="ArtRetweeter can automatically retweet your art for you, without the need to schedule retweets manually." />
        <meta name="twitter:image" content="<?php echo Config::CARD_IMAGE_URL; ?>" />
    </head>
    <title>
        ArtRetweeter
    </title>
    <body>
        <div class="main">
            <?php Core::echoSidebar(); ?>
            <h1>ArtRetweeter</h1>
            <p>
                This app can automatically retweet your art posts, based on specific criteria or filters. You don't have to schedule retweets manually - 
                given some parameters to decide how it should retweet, it will do that for you.
            </p>
            <p>
                You can also use the app to automatically retweet other artists' work. It works in a similar way; you give it settings, and tell it which artists 
                you want to automatically retweet, and the app will do the rest.
            </p>
            <p>
                Once you sign in, you can configure your retweet settings for yourself and/or other artists in the settings pages. 
                The app won't retweet anything until you have saved your settings.
            </p>
            <p>
                If you want more information about how the app works or want to see the FAQ, you can go to the 
                <a href=<?php echo Config::HOMEPAGE_URL . "info" ?>>Info</a> page.
            </p>

            <br/>
            <?php
            if ($_SESSION['usertwitterid']) {
                echo "<hr><p>You are already logged in. You can change your settings or manage your queued retweets using the menu options on the left.</p><hr>";
            } else {
                echo "<hr><p>To use this app or change your settings, you must first sign in with Twitter. Use the button below to proceed.</p>";
                echo "<a href=\"$url\">
                <img alt=\"Sign in with Twitter\" src=\"src/images/signinwithtwitter.png\"
                     width=158\" height=\"28\"></a><hr>";
            }
            ?>
            <p>
                Hosting this app costs money, and developing this app is taking a lot of my time at the moment. If you'd like to support me, 
                <a href="https://patreon.com/antsstyle" target="_blank">I have a patreon here.</a> I'd be very grateful for your support! It will 
                allow me to pay for the hosting costs and spend more time on developing this app and others.
                <br/><br/>
                <a href="https://www.patreon.com/bePatron?u=406925" data-patreon-widget-type="become-patron-button">Become a Patron!</a>
                <script async src="https://c6.patreon.com/becomePatronButton.bundle.js"></script>
            </p>
        </div>
    </body>
    <script src="src/ajax/Collapsibles.js"></script>
</html>
