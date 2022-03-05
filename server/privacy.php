<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\Config;
?>

<html>
    <head>
        <link rel="stylesheet" href="main.css" type="text/css">
        <link rel="stylesheet" href=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "sidebar.css"; ?> type="text/css">
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
            <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "sidebar.js"; ?>></script>
            <h1>ArtRetweeter</h1>
            <div class="subtitle">
                <h2>Privacy Policy</h2>
            </div>
            ArtRetweeter doesn't sell or share any of your data whatsoever with anyone.
            <br/><br/>
            The only information it collects is that required to do its job, namely:
            <ul>
                <li>
                    Your twitter ID and username
                </li>
                <li>
                    If you are retweeting yourself, the app keeps a record of your tweets so it can analyse their engagement metrics. It needs to do this in 
                    order to distinguish between your art tweets and non-art tweets.
                </li>
            </ul>

            As part of making the app as transparent as possible, the source code is openly available. 
            <a href="https://github.com/antsstyle/artretweeter" target="_blank">You can find it on GitHub here.</a> 
        </div>
    </body>
    <script src="src/ajax/Collapsibles.js"></script>
</html>
