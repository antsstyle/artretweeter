<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\CoreDB;
use Antsstyle\ArtRetweeter\Core\Core;

Session::checkSession();

if (!$_SESSION['oauth_token']) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
}

if (!$_SESSION['usertwitterid']) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
}

$userTwitterID = $_SESSION['usertwitterid'];

$userInfo = Core::getUserInfo($userTwitterID);
if ($userInfo === false) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
} else if ($userInfo === null) {
    $errorURL = Config::HOMEPAGE_URL . "error";
    header("Location: $errorURL", true, 302);
    exit();
}

$userArtistRetweetSettings = CoreDB::getUserArtistRetweetSettings($userTwitterID);
?>


<html>
    <script src="src/ajax/Tables.js"></script>
    <script src="src/ajax/SearchArtists.js"></script>
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
    <body onload="storeSearchResults()">
        <div class="main">
            <?php Core::echoSidebar(); ?>
            <h1>ArtRetweeter</h1>
            <p>
                This page shows the artists you are automatically retweeting.
            </p>
            <div id="userartistsresultsdiv">

            </div>
            <div id="userartistsdiv">
                <table id="userartiststable" class="dblisttable">
                    <tr>
                        <th onclick="sortTable(0, 'userartiststable')">Twitter Handle</th>
                        <th onclick="sortTable(1, 'userartiststable')">Follower Count</th>
                        <th>Remove</th>
                    </tr>
                    <?php
                    if (is_null($userArtistRetweetSettings)) {
                        echo "An error occurred retrieving your artist records, check back later.";
                    } else if (count($userArtistRetweetSettings) === 0) {
                        echo "You are not retweeting any artists via this app yet.";
                    } else {
                        $i = 0;
                        foreach ($userArtistRetweetSettings as $userArtistRetweetSettings) {
                            $screenName = $userArtistRetweetSettings['screenname'];
                            $artistID = $userArtistRetweetSettings['artisttwitterid'];
                            $twitterLink = "<a href=\"https://twitter.com/" . $screenName . "\" target=\"_blank\"> "
                                    . "@" . $screenName . "</a>";
                            echo "<tr>";
                            echo "<td>" . $twitterLink . "</td>";
                            echo "<td>" . $userArtistRetweetSettings['followercount'] . "</td>";
                            $removeButton = "<button id=\"removebutton$i\" type=\"button\" onclick=\"addArtistForUser('$userTwitterID', '$artistID'"
                                    . ", 'removebutton$i', 'Disable', 'Remove')\">Remove</button>";
                            echo "<td id=\"userartiststablerow$i\">$removeButton</td>";
                            echo "</tr>";
                            $i++;
                        }
                    }
                    ?>
                </table>
            </div>

            <br/><br/>
            <p>
                You can add more artists to retweet using the search input below.
            </p>
            <p>
                If an artist does not appear in the search results, it means they have not yet been added to the app's retweeting database. You can 
                submit an artist to be added on the <a href="<?php echo Config::HOMEPAGE_URL . "submitartist"; ?>">Artist Submissions</a> page.
            </p>
            <input type="text" id="dbsearch" placeholder="Search by twitter handle...">
            <button type="button" id="searchbutton" 
                    onclick="artistsSearch('dbsearch', '<?php echo $_SESSION['usertwitterid']; ?>')">Search</button>
            <br/><br/>
            <div id="searchresultstextdiv">

            </div>
            <div id="searchresultsdiv">
                <table id="maintable" class="dblisttable">
                    <tr>
                        <th onclick="sortTable(0, 'maintable')">Twitter Handle</th>
                        <th onclick="sortTable(1, 'maintable')">Follower Count</th>
                        <th>Options</th>
                    </tr>
                </table>
            </div>

            <div id="tablecachediv" class="hiddendiv" hidden>
                "So going back to the original occasion, Chris Rea had already run you a bath..."
            </div>

        </div>
    </body>
    <script src="src/ajax/Collapsibles.js"></script>
</html>
