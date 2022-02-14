<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\Core;

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
?>

<html>
    <script src="src/ajax/SubmitArtist.js"></script>
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
                You can submit a user to be added to ArtRetweeter here; this will allow you, or other app users, to retweet that user automatically.
            </p>
            <p>
                <b>Approval is not automatic;</b> I will review submissions manually, but it will usually just be a formality.
                Still, submitted users must meet these conditions for approval:
            </p>
            <ol>
                <li>User must be an artist, i.e. ArtRetweeter is not for auto-retweeting celebrities or meme accounts. Pretty much all kinds of art are 
                    welcome; cosplayers, 3D artists, 2D artists, NSFW artists, etc.</li>
                <br/>
                <li>User must not sell or advocate for NFTs or cryptocurrency.</li>
                <br/>
                <li>User must either have at least 1,000 followers, or only tweet their art with specific text. <b>This requirement is not for restricting 
                        smaller artists from being retweeted by the app;</b> it is explained below.</li>
            </ol>
            The reason for the third condition is because at low follower counts, 
            it becomes nearly impossible for the app to determine which tweets are art and which are not. If however, an artist with 50 followers 
            always tweets their art with a specific phrase or hashtag that is not used for non-art posts (e.g. "#Art" - but it can be any phrase), then 
            this rule doesn't apply, as then the app will be able to retweet their art correctly.
            <br/><br/>
            To submit a user to be added to ArtRetweeter, use the simple form below. To avoid spam, you can make a maximum of 10 
            unique submissions per week.
            </br><br/>
            <input type="text" style="width:250px" id="submitartistinput" placeholder="Enter artist's twitter handle here.">
            <button type="button" id="submitartistbutton" 
                    onclick="submitArtist('<?php echo $_SESSION['usertwitterid']; ?>')">Submit</button>
            <br/><br/>
            <div id="submitartisttextdiv">

            </div>
            
        </div>
    </body>
    <script src="src/ajax/Collapsibles.js"></script>
</html>
