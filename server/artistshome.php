<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\OAuth;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Abraham\TwitterOAuth\TwitterOAuth;

Session::checkSession();

$_SESSION['artretweeterpage'] = "artists";

if (!$_SESSION['artretweeterlogin']) {
    $connection = new TwitterOAuth(APIKeys::twitter_consumer_key, APIKeys::twitter_consumer_secret);
    $array = OAuth::generatePKCEVerifierAndChallenge();
    $code_verifier = $array[0];
    $code_challenge = $array[1];
    $_SESSION['code_verifier'] = $code_verifier;
    $url = "https://twitter.com/i/oauth2/authorize?";
    $paramString = "response_type=code&client_id=" . APIKeys::twitter_oauth2_client_id . "&redirect_uri=" . Config::OAUTH_CALLBACK
            . "&scope=" . "tweet.read%20tweet.write%20offline.access%20users.read"
            . "&state=state&code_challenge=$code_challenge&code_challenge_method=s256";
    $url .= $paramString;
}
?>

<html>
    <head>

        <link rel="stylesheet" href="src/css/artretweeter.css" type="text/css">
        <link rel="stylesheet" href=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "main.css"; ?> type="text/css">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="twitter:card" content="summary" />
        <meta name="twitter:site" content="@antsstyle" />
        <meta name="twitter:title" content="ArtRetweeter, an auto-retweeting app for artists and users" />
        <meta name="twitter:description" content="ArtRetweeter can automatically retweet your art for you, without the need to schedule retweets manually." />
        <meta name="twitter:image" content="<?php echo Config::CARD_IMAGE_URL; ?>" />
    </head>
    <title>
        ArtRetweeter
    </title>
    <body>
        <div class="main">
            <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "main.js"; ?>></script>
            <h1>ArtRetweeter</h1>

            <p>
                This app can automatically retweet your art posts, based on specific criteria or filters. You don't have to schedule retweets manually - 
                given some parameters to decide how it should retweet, it will do that for you.
            </p>
            <p>
                Once you sign in, you can configure your retweet settings. 
                The app won't retweet anything until you have saved your settings.
            </p>
            <p>
                If you want more information about how the app works or want to see the FAQ, you can go to the 
                <a href=<?php echo Config::HOMEPAGE_URL . "artistsinfo" ?>>Info</a> page.
            </p>

            <br/>
            <?php
            if ($_SESSION['usertwitterid'] && $_SESSION['artretweeterlogin']) {
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
    <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "collapsibles.js"; ?>></script>
</html>
