<?php

namespace Antsstyle\ArtRetweeter;

require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\Config;

Session::checkSession();

$artistSettingsPageURL = Config::ARTISTSETTINGSPAGE_URL;
$nonArtistSettingsPageURL = Config::NONARTISTSETTINGSPAGE_URL;
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
            <h3>Authentication successful</h3>
            Choose which settings you want to modify. You can also use the menu options on the left to change them at any time.
            <br/><br/>
            <button onclick="window.location = '<?php echo $artistSettingsPageURL ?>';">Artist settings page (retweeting yourself)</button>
            <br/><br/>
            <button onclick="window.location = '<?php echo $nonArtistSettingsPageURL ?>';">Non-artist settings page (retweeting other artists)</button>
        </div>
    </body>
    <script src="src/ajax/Collapsibles.js"></script>
</html>

