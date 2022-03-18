<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\CoreDB;

Session::checkSession();

if (!$_SESSION['oauth_token']) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
}

if (!$_SESSION['usertwitterid']) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
}

$userInfo = CoreDB::getUserInfo($_SESSION['usertwitterid']);
if ($userInfo === false) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
} else if ($userInfo === null) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
}
?>

<html>
    <head>
        
        <link rel="stylesheet" href="src/css/artretweeter.css" type="text/css">
        <link rel="stylesheet" href=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "main.css"; ?> type="text/css">
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
            <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "main.js"; ?>></script>
            <h1>ArtRetweeter</h1>
            <div class="subtitle">
                <h2>Artist Submission Rules</h2>
            </div>
            <p>
                First of all, <b>approval is not automatic;</b> I will review submissions manually, but it will usually just be a formality.
                Still, submitted users must meet these conditions for approval:
            </p>
            <ol>
                <li>User must be an artist, i.e. ArtRetweeter is not for auto-retweeting celebrities or meme accounts. Pretty much all kinds of art are 
                    welcome; cosplayers, 3D artists, 2D artists, NSFW artists, etc.</li>
                <br/>
                <li>User must not sell or advocate for NFTs or cryptocurrency.</li>
                <br/>
                <li>User must have a certain level of engagement on their art posts (typically if they have >5,000 followers this will work), 
                    in order that their art tweets are sufficiently easy to distinguish from their non-art tweets. <b>This requirement is not for restricting 
                        smaller artists from being retweeted by the app;</b> it is explained below.</li>
            </ol>
            The third condition exists because if an artist's art tweets and non-art tweets have similar retweet counts, 
            it becomes nearly impossible for the app to determine which tweets are art and which are not. If however, an artist 
            always tweets their art with a specific phrase or hashtag that is not used for non-art posts (e.g. "#Art" - but it can be any phrase), then 
            this rule doesn't apply, as then the app will be able to retweet their art correctly. It also won't apply if e.g. said artist *only* tweets art 
            with their account and not any other kind of images, as then there isn't going to be any confusion for the app as to which images are which.
        </div>
    </body>
    <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "collapsibles.js"; ?>></script>
</html>
