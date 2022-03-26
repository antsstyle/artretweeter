<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\Config;
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
                <h2>Information & FAQs (non-artists)</h2>
            </div>
            <h3>How do I use this app?</h3>
            It's very simple:
            <ol>
                <li>Sign in with Twitter on the homepage</li>
                <li>Save your settings for when the app should retweet artists for you</li>
                <li>Use the Add Artists page to tell the app which artists you want to retweet</li>
            </ol>
            Everything else is done for you. The app automatically figures out which tweets by an artist are art posts, and does the retweeting and scheduling 
            for you.
            <br/><br/>
            <button class="collapsible">Does the app distinguish between SFW and NSFW tweets from an artist?</button>
            <div class="content">
                <p>
                    No. There isn't a way of doing this; as such, if you are looking to specifically retweet only SFW or NSFW posts, you might want 
                    to consider limiting the artists you add for retweeting to those who only tweet one or the other.
                </p>
            </div>
            <button class="collapsible">Why is there an artist whitelist instead of just being able to retweet anyone I want?</button>
            <div class="content">
                <p>
                    This is to prevent abuse. I do not want the app used to promote crypto or NFTs, or completely non-art-related accounts, and as such 
                    the whitelist is in place for that reason (in addition, plenty of dodgy accounts selling goodness knows what would jump on auto-retweeting 
                    functionality like this, and I have no desire to give it to them).
                    <br/><br/>
                    As described on the submissions page, approval is largely a formality provided an artist is, in fact, an artist and not doing NFTs.
                </p>
            </div>
            <button class="collapsible">Is the source code for this app available?</button>
            <div class="content">
                <p>
                    Yes. You can find it here: <a href="https://github.com/antsstyle/artretweeter">https://github.com/antsstyle/artretweeter</a>.
                </p>
            </div>
        </div>
    </body>
    <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "collapsibles.js"; ?>></script>
</html>
