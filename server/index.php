<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Abraham\TwitterOAuth\TwitterOAuth;

Session::checkSession();

$connection = new TwitterOAuth(APIKeys::consumer_key, APIKeys::consumer_secret);
try {
    $response = $connection->oauth("oauth/request_token", ["oauth_callback" => "https://antsstyle.com/artretweeter/results"]);
    $httpcode = $connection->getLastHttpCode();
    if ($httpcode != 200) {
        error_log("Failed to get request token!");
        // Show error page
    }
} catch (\Exception $e) {
    
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
                Once you sign in, you will be taken to the settings page where you can decide what conditions to set. 
                The app won't retweet anything until you have saved your settings.
            </p>
            <p>
                To use this app or change your settings, you must first sign in with Twitter. Use the button below to proceed.
            </p>
            <br/>
            <a href=<?php echo "$url" ?>>
                <img alt="Sign in with Twitter" src="src/images/signinwithtwitter.png"
                     width=158" height="28">
            </a>
            <br/><br/>
            <button class="collapsible">FAQs</button>
            <div class="content">
                <h3>How does this app know which of my posts are art and which aren't?</h3>
                <p>
                    It can't know with 100% certainty - but it gets it right the vast majority of the time. It automatically filters out tweets
                    with the following criteria:<br/>
                <ul>
                    <li>
                        Tweets which are replies to other tweets (unless the tweet is you replying to yourself), e.g. reaction GIFs posted to friends
                    </li>
                    <br/>
                    <li>
                        Tweets that don't contain images, or that don't contain media types you want to retweet (this is customisable in the options)
                    </li>
                    <br/>
                    <li>
                        Tweets that are below a certain threshold of retweets (this is calculated dynamically according to your average retweets, and is 
                        scaled down - as such, it won't miss low-engagement art posts, but it will miss e.g. food pics or other image-containing tweets 
                        that your followers will rarely retweet).
                    </li>
                </ul>
                </p>
                <h3>Is the source code for this app available?</h3>
                <p>
                    Yes. You can find it here: <a href="https://github.com/antsstyle/artretweeter">https://github.com/antsstyle/artretweeter</a>
                </p>
            </div>
        </div>
    </body>
    <script src="src/ajax/Collapsibles.js"></script>
</html>
