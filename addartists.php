<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\DB\UserDB;

Session::checkSession();
Session::validateUserLoggedIn();

$userTwitterID = $_SESSION['usertwitterid'];

$pageNum = filter_input(INPUT_GET, "page", FILTER_VALIDATE_INT);
if (is_null($pageNum) || $pageNum === false) {
    $pageNum = 1;
} else if (!is_numeric($pageNum)) {
    $pageNum = 1;
}

$artistCount = UserDB::getUserArtistRetweetSettingsCount($userTwitterID);

$pageCount = ceil($artistCount / 10);
if ($pageNum > $pageCount) {
    $pageNum = $pageCount;
}
if ($pageNum < 1) {
    $pageNum = 1;
}

$userArtistRetweetSettings = UserDB::getUserArtistRetweetSettings($userTwitterID, $pageNum);

$nextPage = $pageNum + 1;
$prevPage = $pageNum - 1;
?>


<html>
    <script src="src/ajax/Tables.js"></script>
    <script src="src/ajax/SearchArtists.js"></script>
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
    <body onload="storeSearchResults()">
        <div class="main">
            <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "main.js"; ?>></script>
            <h1>ArtRetweeter</h1>
            <?php
            if (is_null($userArtistRetweetSettings)) {
                echo "An error occurred retrieving your artist records, check back later.";
            } else if (count($userArtistRetweetSettings) === 0) {
                echo "You are not retweeting any artists via this app yet.";
            } else {
                echo "This page shows the artists you are automatically retweeting, in alphabetical order. "
                . "<br/><br/>This is page $pageNum of $pageCount.<br/><br/>";
            }

            if ($prevPage >= 1) {
                echo "<button onclick=\"window.location.href = '" . Config::HOMEPAGE_URL . "addartists?page=" . $prevPage . "';\">"
                . "Previous page"
                . "</button>";
            } else {
                echo "<button onclick=\"window.location.href = '" . Config::HOMEPAGE_URL . "addartists?page=" . $prevPage . "';\" disabled>"
                . "Previous page"
                . "</button>";
            }
            echo "&nbsp;";
            if ($nextPage <= $pageCount) {
                echo "<button onclick=\"window.location.href = '" . Config::HOMEPAGE_URL . "addartists?page=" . $nextPage . "';\">"
                . "Next page"
                . "</button>";
            } else {
                echo "<button onclick=\"window.location.href = '" . Config::HOMEPAGE_URL . "addartists?page=" . $nextPage . "';\" disabled>"
                . "Next page"
                . "</button>";
            }
            ?>
            <div id="userartistsresultsdiv">

            </div>
            <div id="userartistsdiv">
                <table id="userartiststable" class="dblisttable">
                    <tr>
                        <th onclick="sortTable(0, 'userartiststable')">Twitter Handle</th>
                        <th>Remove</th>
                    </tr>
                    <?php
                    if (!is_null($userArtistRetweetSettings) && count($userArtistRetweetSettings) > 0) {
                        $i = 0;
                        foreach ($userArtistRetweetSettings as $userArtistRetweetSettings) {
                            $screenName = $userArtistRetweetSettings['screenname'];
                            $artistID = $userArtistRetweetSettings['artisttwitterid'];
                            $twitterLink = "<a href=\"https://twitter.com/" . $screenName . "\" target=\"_blank\"> "
                                    . "@" . $screenName . "</a>";
                            echo "<tr>";
                            echo "<td>" . $twitterLink . "</td>";
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
                        <th>Options</th>
                    </tr>
                </table>
            </div>

            <div id="tablecachediv" class="hiddendiv" hidden>
                "So going back to the original occasion, Chris Rea had already run you a bath..."
            </div>

        </div>
    </body>
    <script src=<?php echo Config::WEBSITE_STYLE_DIRECTORY . "collapsibles.js"; ?>></script>
</html>
