<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\Config;

$defaultMessage = "You are not logged in. Go back to the homepage to sign in.";
$errorMessage = htmlspecialchars($_POST['errormsg']);
?>

<html>
    <head>
        <link rel="stylesheet" href="main.css" type="text/css">
        <link rel="stylesheet" href=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "sidebar.css"; ?> type="text/css">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
    </head>
    <title>
        ArtRetweeter
    </title>
    <body>
        <div class="main">
            <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "sidebar.js"; ?>></script>
            <h1>ArtRetweeter</h1>
            <?php
            $hp = Config::HOMEPAGE_URL;
            if ($errorMessage !== "") {
                echo $errorMessage;
            } else {
                echo $defaultMessage;
            }
            ?>
        </div>
    </body>
    <script src="src/ajax/Collapsibles.js"></script>
</html>