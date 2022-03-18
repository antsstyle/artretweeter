<?php
require __DIR__ . '/vendor/autoload.php';

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
                <h2>Information & FAQs (artists)</h2>
            </div>

            <h3>How do I use this app?</h3>
            This app is split into two settings areas:
            <ul>
                <li>
                    Artist settings, where you can configure the app to retweet your own tweets
                </li>
                <li>
                    Non-artist settings, where you can configure the app to retweet other artists for you
                </li>
            </ul>
            In both cases, it retweets from your own account. All you have to do is sign in with Twitter on the homepage, then configure your settings as you like.
            <br/><br/>
            <button class="collapsible">How does this app know which of my posts are art?</button>
            <div class="content">
                <p>
                    It can't know with 100% certainty - but it gets it right most of the time. It automatically filters out tweets
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
            </div>
            <button class="collapsible">Is the source code for this app available?</button>
            <div class="content">
                <p>
                    Yes. You can find it here: <a href="https://github.com/antsstyle/artretweeter">https://github.com/antsstyle/artretweeter</a>
                </p>
            </div>
        </div>
    </body>
    <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "collapsibles.js"; ?>></script>
</html>
