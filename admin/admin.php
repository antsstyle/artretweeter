<?php
require dirname(__DIR__) . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\DB\ArtistDB;
use Antsstyle\ArtRetweeter\Core\Session;

Session::checkSession();
?>

<html>
    <script src="../src/ajax/ArtistSubmissions.js"></script>
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
                <h2>Administration</h2>
            </div>

            <?php
            if (!$_SESSION['adminlogin']) {
                echo "You are not logged in as an administrator. Go to the admin login page.";
            } else {
                $artistSubmissions = ArtistDB::getAllPendingArtistSubmissions();
                if (is_null($artistSubmissions) || $artistSubmissions === false) {
                    echo "Error - could not get artist submissions from DB.";
                } else if (count($artistSubmissions) === 0) {
                    echo "There are no artist submissions waiting for approval.";
                } else {
                    echo "<table><th>Twitter Handle</th><th>Date Submitted</th><th>Num Submissions</th><th></th><th></th>";
                    $i = 0;
                    foreach ($artistSubmissions as $submission) {
                        $artistScreenName = $submission['screenname'];
                        $twitterLink = "https://twitter.com/" . $submission['screenname'];
                        $twitterHandle = "<a href=\"$twitterLink\" target=\"_blank\">@" . $submission['screenname'] . "</a>";
                        $dateSubmitted = substr($submission['datesubmitted'], 0, 10);
                        $submissionCount = $submission['submissioncount'];
                        $approveButton = "<button id=\"approvebutton$i\" type=\"button\" onclick=\"approveArtistSubmission("
                                . "'$artistScreenName')\">Approve</button>";
                        $rejectButton = "<button id=\"rejectbutton$i\" type=\"button\" onclick=\"rejectArtistSubmission("
                                . "'$artistScreenName')\">Reject</button>";
                        echo "<tr>";
                        echo "<td>$twitterHandle</td>";
                        echo "<td>$dateSubmitted</td>";
                        echo "<td>$submissionCount</td>";
                        echo "<td>$approveButton</td>";
                        echo "<td>$rejectButton</td>";
                        echo "</tr>";
                        $i++;
                    }
                    echo "</table><br/><br/>";
                    echo "<div id=\"artistapprovalresultdiv\"></div>";
                }
            }
            ?>
        </div>
    </body>
    <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "collapsibles.js"; ?>></script>
</html>
