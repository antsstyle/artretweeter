<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\Config;

$defaultMessage = "You are not logged in. Go back to the homepage to sign in.";
$errorMessage = filter_input(INPUT_POST, "errormsg", FILTER_SANITIZE_STRING);
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
            <?php
            $hp = Config::HOMEPAGE_URL;
            if ($errorMessage !== false && !is_null($errorMessage)) {
                echo $errorMessage;
            } else {
                echo $defaultMessage;
            }
            ?>
        </div>
    </body>
</html>