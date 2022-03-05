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
                <h2>Intro</h2>
            </div>
            <h3>What is ArtRetweeter?</h3>

            ArtRetweeter is an automated app that you can connect to your Twitter account. It has two main uses:
            <ul>
                <li>If you're an artist, ArtRetweeter allows you to retweet your own art automatically.</li>
                <li>If you're not an artist, ArtRetweeter allows you to retweet the art of your favourite artists automatically.</li>
            </ul>
            You can, of course, use both options if you want to both retweet your work and other artists' work.
            
            To find out more, go to the ArtRetweeter section you want in the menu, and read the Info & FAQs page. 
        </div>
    </body>
    <script src="src/ajax/Collapsibles.js"></script>
</html>
