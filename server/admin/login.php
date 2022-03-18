<?php
require dirname(__DIR__) . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\Session;

Session::checkSession();

$error = htmlspecialchars($_GET['error']);

// Authenticate that user is on admin list before allowing access to this page - use PHP session
// + some kind of password login perhaps? Or just use twitter ID?
?>

<html>
    <head>
        <link rel="stylesheet" href="../src/css/artretweeter.css" type="text/css">
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
                <h2>Admin Login</h2>
            </div>
            <div class="loginerror">
                <?php
                switch ($error) {
                    case "":
                        break;
                    case "userlocked":
                        echo "Your admin account is temporarily locked due to too many login attempts.<br/><br/>";
                        break;
                    case "usernotfound":
                        echo "Login failed; admin account not found.<br/><br/>";
                        break;
                    case "dberror":
                        echo "Login failed due to a database error.<br/><br/>";
                        break;
                    case "invalidpassword":
                        echo "Login failed due to invalid password - passwords cannot contain restricted characters.<br/><br/>";
                        break;
                    case "incorrectpassword":
                        echo "Your password was incorrect, try again.<br/><br/>";
                        break;
                    default:
                        break;
                }
                ?>
            </div>
            <form action="processlogin.php" method="post">
                <label for="username"><b>Username:</b></label>
                <input type="text" placeholder="Username" name="username" required>
                <br/>
                <label for="password"><b>Password:</b></label>
                <input type="password" id="password" name="password">
                <button type="submit">Login</button>
            </form>
        </div>
    </body>
    <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "collapsibles.js"; ?>></script>
</html>
