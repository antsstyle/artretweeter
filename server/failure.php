<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\Config;
?>
<html>
    <head>
        <link rel="stylesheet" href="main.css" type="text/css">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
    </head>
    <title>
        ArtRetweeter
    </title>
    <body>
        <div class="main">
            <?php Core::echoSidebar(); ?>
            <h1>ArtRetweeter</h1>
            Something went wrong authenticating your account via Twitter. <br/><br/>Please try again or contact 
            <a href="<?php echo Config::ADMIN_URL; ?>"><?php echo Config::ADMIN_NAME;?></a> if the problem persists.
        </div>
    </body>
    <script src="src/ajax/Collapsibles.js"></script>
</html>