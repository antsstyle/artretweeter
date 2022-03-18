<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Core\CoreDB;
use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Users;

Session::checkSession();
Session::validateUserLoggedIn();

$userTwitterID = $_SESSION['usertwitterid'];

$queuedRetweets = CoreDB::getUserRetweetQueue($userTwitterID);
$userInfo = CoreDB::getUserInfo($userTwitterID);
if ($userInfo !== false && $userInfo !== null) {
    if (!is_null($userInfo['screenname'])) {
        $twitterHandle = $userInfo['screenname'];
    } else {
        $twitterHandle = Users::retrieveUserTwitterHandle($userInfo);
    }
}
?>

<html>
    <script src="https://twemoji.maxcdn.com/v/latest/twemoji.min.js" crossorigin="anonymous"></script>
    <script src="src/ajax/Queue.js"></script>
    <head>

        <link rel="stylesheet" href="src/css/artretweeter.css" type="text/css">
        <link rel="stylesheet" href=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "main.css"; ?> type="text/css">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
    </head>
    <title>
        ArtRetweeter
    </title>
    <body onload="initialiseQueueStatus()">
        <div class="main">
            <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "main.js"; ?>></script>
            <h1>ArtRetweeter</h1>
            <h2>Artist Retweet Queue</h2>
            <b>This page shows your queued retweets for your own account. If you want to see your queued retweets for other accounts, 
                go to the <a href=<?php echo Config::NONARTISTQUEUESTATUS_URL ?>>Non-Artists Queue Status</a> page.</b><br/><br/>
            <?php
            if ($userInfo === false) {
                echo "A database error occurred whilst attempting to load your queued retweets.<br/><br/>"
                . "Go back to the homepage to try"
                . " again, or contact <a href=$adminURL>$adminName</a> on Twitter if the problem persists.";
            } else if ($userInfo === null) {
                echo "Your user information could not be found; your session may be invalid.<br/><br/>"
                . "Go back to the homepage to try"
                . " again, or contact <a href=$adminURL>$adminName</a> on Twitter if the problem persists.";
            } else if ($queuedRetweets === null) {
                echo "You do not currently have any tweets scheduled for retweeting."
                . "<br/><br/>If you have just enabled automated retweeting for the first time, it usually takes 15-30 minutes for ArtRetweeter to"
                . " queue retweets for your account; check this page again later.";
            } else if ($queuedRetweets === false || count($queuedRetweets) === 0) {
                echo "A database error occurred whilst attempting to load your queued retweets.<br/><br/>"
                . "Go back to the homepage to try"
                . " again, or contact <a href=$adminURL>$adminName</a> on Twitter if the problem persists.";
            } else {
                echo "These are the tweets currently scheduled for retweeting on your account, soonest first.<br/><br/>";
                foreach ($queuedRetweets as $queuedRetweet) {
                    $scheduledID = $queuedRetweet['schid'];
                    $retweetTime = $queuedRetweet['retweettime'];
                    $tweetText = $queuedRetweet['fulltweettext'];
                    $tweetText = nl2br($tweetText);
                    $retweetTime = Core::convertServerTimeStringToUserTime($retweetTime,
                                    $userInfo['timezonehouroffset'], $userInfo['timezoneminuteoffset']);
                    $tweetID = $queuedRetweet['tweetid'];
                    $tweetURL = "https://twitter.com/" . $twitterHandle . "/status/" . $tweetID;
                    $echoStr = "<div class=\"queuedtweet\" id=\"$scheduledID\">Tweet ID: <a href=\"$tweetURL\" target=\"_blank\">$tweetID</a><br/>"
                            . "Scheduled retweet time: <div class=\"retweettime\" id=\"rtimediv_$scheduledID\">$retweetTime</div><br/><br/>"
                            . "Tweet text: \"$tweetText\""
                            . "<br/><br/><button type=\"button\" class=\"deletequeueentrybutton\" id=\"showdeleteconfirmation_$scheduledID\" "
                            . "onclick=\"toggleDeleteVisibility('$scheduledID')\">Delete</button>"
                            . "<button type=\"button\" class=\"reschedulequeueentrybutton\" id=\"showrescheduleoptions_$scheduledID\" "
                            . "onclick=\"toggleRescheduleVisibility('$scheduledID')\">Show reschedule options</button>"
                            . "<div class=\"reschedulequeuedentrydiv\" id=\"rdiv_$scheduledID\">"
                            . "Choose a new date and time: <input type=\"date\" id=\"rescheduledate_$scheduledID\" name=\"rescheduledate_$scheduledID\" "
                            . "class=\"rescheduledate\">"
                            . "<select name=\"rescheduletimehour_$scheduledID\" id=\"rescheduletimehour_$scheduledID\" class=\"rescheduletimehour\">";
                    for ($i = 0; $i < 24; $i++) {
                        if ($i < 10) {
                            $iString = "0" . strval($i);
                        } else {
                            $iString = strval($i);
                        }
                        $optionString = "<option value=\"hour_$iString\">$iString</option>";
                        $echoStr .= $optionString;
                    }
                    $echoStr .= "</select><select name=\"rescheduletimeminute_$scheduledID\" id=\"rescheduletimeminute_$scheduledID\" "
                            . "class=\"rescheduletimeminute\">";
                    for ($i = 0; $i < 60; $i += 15) {
                        if ($i < 10) {
                            $iString = "0" . strval($i);
                        } else {
                            $iString = strval($i);
                        }
                        $optionString = "<option value=\"minute_$iString\">$iString</option>";
                        $echoStr .= $optionString;
                    }
                    $echoStr .= "</select><button type=\"button\" class=\"reschedulebutton\" "
                            . "onclick=\"rescheduleQueueEntry('$scheduledID', '$userTwitterID')\" "
                            . "id=\"reschedulebutton_$scheduledID\">Reschedule</button>"
                            . "<div class=\"rescheduleresultdiv\" id=\"rresultdiv_$scheduledID\"></div>"
                            . "</div>";
                    $echoStr .= "<div class=\"deletequeueentrydiv\" id=\"ddiv_$scheduledID\">"
                            . "Are you sure you want to delete this queued entry? This action cannot be undone."
                            . "<button type=\"button\" class=\"deletequeueentrybutton_confirm\" id=\"deleteconfirm_$scheduledID\" "
                            . "onclick=\"deleteQueueEntry('$scheduledID', '$userTwitterID')\">Yes</button>"
                            . "<button type=\"button\" class=\"deletequeueentrybutton_cancel\" id=\"deletecancel_$scheduledID\" "
                            . "onclick=\"toggleDeleteVisibility('$scheduledID')\">No</button>"
                            . "<div class=\"deleteresultdiv\" id=\"dresultdiv_$scheduledID\"></div>";
                    $echoStr .= "</div></div><br/>";
                    echo $echoStr;
                }
            }
            ?>
        </div>
    </body>
    <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "collapsibles.js"; ?>></script>
</html>