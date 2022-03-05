<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\Session;

Session::checkSession();

if (!$_SESSION['usertwitterid']) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
}

$userInfo = Core::getUserInfo($_SESSION['usertwitterid']);
if ($userInfo === false) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
} else if ($userInfo === null) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
}

$paidUser = $userInfo['paiduser'];
?>

<html>
    <head>
        <link rel="stylesheet" href="main.css" type="text/css">
        <link rel="stylesheet" href=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "sidebar.css"; ?> type="text/css">
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
            <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "sidebar.js"; ?>></script>
            <h1>ArtRetweeter</h1>
            <p>
                ArtRetweeter is free to use, but if you want to be able to add more artists or have a higher retweet limit per day, you can subscribe! 
                This money helps me to pay for the servers, and to spend additional time developing this app and others.
            </p>
            <p>
                Subscription is $3/month, and is done via pledging on Patreon. Once you're a patron, make sure to have your Twitter in your Patreon 
                profile settings, so the app knows you are a subscriber.
            </p>
            <?php
            if ($paidUser === "Y") {
                echo "You are already subscribed! Thank you for your support :)";
            } else {
                echo "<p>You can subscribe using the button below!</p>
                    <p>
                    <a href=\"https://www.patreon.com/bePatron?u=406925\" data-patreon-widget-type=\"become-patron-button\">Become a Patron!</a>
                    <script async src=\"https://c6.patreon.com/becomePatronButton.bundle.js\"></script>
                    </p>";
            }
            ?>
        </div>
    </body>
    <script src="src/ajax/Collapsibles.js"></script>
</html>
