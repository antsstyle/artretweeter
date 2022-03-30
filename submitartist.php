<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\CachedVariables;
use Antsstyle\ArtRetweeter\DB\CoreDB;
use Antsstyle\ArtRetweeter\DB\UserDB;

Session::checkSession();
Session::validateUserLoggedIn();

$userTwitterID = $_SESSION['usertwitterid'];

$maxPendingSubmissions = CoreDB::getCachedVariable(CachedVariables::MAX_PENDING_SUBMISSIONS_FREE_USER);
if ($userInfo['paiduser'] === "Y") {
    $maxPendingSubmissions = CoreDB::getCachedVariable(CachedVariables::MAX_PENDING_SUBMISSIONS_PAID_USER);
}
$pendingArtistSubmissions = UserDB::getPendingArtistSubmissionsForUser($userTwitterID);
?>

<html>
    <script src="src/ajax/SubmitArtist.js"></script>
    <script src="src/ajax/Tables.js"></script>
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
                <h2>Artist Submissions</h2>
            </div>
            <p>
                You can submit a user to be added to ArtRetweeter here; this will allow you, or other app users, to retweet that user automatically.
            </p>
            <p>
                <b>Approval is not automatic;</b> I will review submissions manually, but it will usually just be a formality.
                Still, submitted users must meet the rules below for approval.
            </p>
            <div class="center">
                <h2>Rules</h2>
            </div>
            <ol>
                <li>User must be an artist, not e.g. a celebrity/meme account. Pretty much all kinds of art are 
                    welcome; cosplayers, 3D artists, 2D artists, NSFW artists, etc.</li>
                <br/>
                <li>User must not sell or advocate for NFTs or cryptocurrency.</li>
                <br/>
                <li>User must have a certain level of engagement on their art posts (typically >5000 followers or so), 
                    or only post art and not other kinds of images. <b>This requirement is not for restricting 
                        smaller artists from being retweeted by the app;</b> it is explained below.</li>
            </ol>
            The third condition exists purely for accuracy reasons - ArtRetweeter will struggle to tell the difference between art tweets and non-art 
            tweets for artists with very low follower counts, unless they only post art and not any other kinds of images.
            <div class="center">
                <h2>Submit Artist</h2>
            </div>

            To submit a user to be added to ArtRetweeter, use the simple form below. You can have up to <?php echo $maxPendingSubmissions; ?> 
            pending submissions at any one time.
            <br/><br/>
            <label for="submitartistenablertonapproval"> Enable retweets for this artist automatically when they're approved </label>
            <input type="checkbox" id="submitartistenablertonapproval" name="submitartistenablertonapproval" 
                   value="submitartistenablertonapproval">
            <br/><br/>
            <input type="text" style="width:250px" id="submitartistinput" placeholder="Enter artist's twitter handle here.">
            <button type="button" id="submitartistbutton" 
                    onclick="submitArtist('<?php echo $userTwitterID; ?>', 'submitartistenablertonapproval')">Submit</button>
            <br/><br/>
            <div id="submitartisttextdiv">

            </div>
            <p>
                Below is a list of your artist submissions from the last 30 days.
            </p>
            <div id="usersubmissionsresultsdiv">

            </div>
            <div id="usersubmissionsdiv">
                <table id="usersubmissionstable" class="dblisttable">
                    <tr>
                        <th>Twitter Handle</th>
                        <th>Date Submitted</th>
                        <th>Status</th>
                        <th>Date Approved/Rejected</th>
                        <th>Rejection Reason</th>
                        <th>Cancel Submission</th>
                    </tr>
                    <?php
                    if (!is_null($pendingArtistSubmissions) && count($pendingArtistSubmissions) > 0) {
                        $i = 0;
                        foreach ($pendingArtistSubmissions as $pendingSubmission) {
                            $dateSubmitted = substr($pendingSubmission['datesubmitted'], 0, 10);
                            $screenName = $pendingSubmission['screenname'];
                            $status = $pendingSubmission['status'];
                            if ($status === "N") {
                                $status = "Rejected";
                            } else if ($status === "Y") {
                                $status = "Approved";
                            } else {
                                $status = "Pending";
                            }
                            $dateDecided = $pendingSubmission['datedecided'];
                            if (is_null($dateDecided)) {
                                $dateDecided = "N/A";
                            } else {
                                $dateDecided = substr($dateDecided, 0, 10);
                            }
                            $rejectionReason = $pendingSubmission['rejectionreason'];
                            if (is_null($rejectionReason)) {
                                $rejectionReason = "N/A";
                            }
                            $twitterLink = "<a href=\"https://twitter.com/" . $screenName . "\" target=\"_blank\"> "
                                    . "@" . $screenName . "</a>";
                            echo "<tr>";
                            echo "<td>" . $twitterLink . "</td>";
                            echo "<td>" . $dateSubmitted . "</td>";
                            echo "<td>" . $status . "</td>";
                            echo "<td>" . $dateDecided . "</td>";
                            echo "<td>" . $rejectionReason . "</td>";
                            if ($dateDecided === "N/A") {
                                $cancelButton = "<button type=\"button\" "
                                        . "onclick=\"cancelArtistSubmissionForUser('$userTwitterID', '$screenName')\">Cancel</button>";
                            } else {
                                $cancelButton = "";
                            }
                            echo "<td id=\"usersubmissionstablerow$i\">$cancelButton</td>";
                            echo "</tr>";
                            $i++;
                        }
                    }
                    ?>
                </table>
            </div>
        </div>
    </body>
    <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "collapsibles.js"; ?>></script>
</html>
