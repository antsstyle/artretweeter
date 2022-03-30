<?php
require __DIR__ . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Session;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\DB\UserDB;
use Antsstyle\ArtRetweeter\Core\LogManager;

$logger = LogManager::getLogger("addartists");

Session::checkSession();
Session::validateUserLoggedIn();

$userTwitterID = $_SESSION['usertwitterid'];

$pageNum = filter_input(INPUT_GET, "page", FILTER_VALIDATE_INT);
if (is_null($pageNum) || $pageNum === false) {
    $pageNum = 1;
} else if (!is_numeric($pageNum)) {
    $pageNum = 1;
}

$viewMode = "Normal";

$userArtistAutomationSettings = UserDB::getUserArtistAutomationSettings($userTwitterID);
if (is_null($userArtistAutomationSettings)) {
    $logger->critical("Failed to retrieve user artist automation settings from DB for user twitter ID $userTwitterID");
} else if ($userArtistAutomationSettings !== false) {
    $viewMode = $userArtistAutomationSettings['addartistsview'];
}

$pageURL = Config::HOMEPAGE_URL . "addartists";

$artistCount = UserDB::getUserArtistRetweetSettingsCount($userTwitterID);

$compactViewModeMaxColumns = 5;

if ($viewMode === "Normal") {
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
} else {
    $pageCount = ceil($artistCount / 75);
    if ($pageNum > $pageCount) {
        $pageNum = $pageCount;
    }
    if ($pageNum < 1) {
        $pageNum = 1;
    }

    $userArtistRetweetSettings = UserDB::getUserArtistRetweetSettings($userTwitterID, $pageNum, 75);

    $nextPage = $pageNum + 1;
    $prevPage = $pageNum - 1;
}
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
                echo "This page shows the artists you are automatically retweeting, in alphabetical order. ";
                if ($userArtistAutomationSettings === false) {
                    echo "<br/><br/><div class=\"error\">Note that you have not yet enabled automated retweeting in your "
                    . "<a href=\"" . Config::NONARTISTSETTINGSPAGE_URL . "\">non-artist settings</a>. "
                    . "You can add artists here, but ArtRetweeter will not retweet them for you until you enable it.</div>";
                }
                echo "<br/><br/><div class=\"center\">";
                if ($viewMode === "Normal") {
                    echo "<button class=\"settingsswitchbutton\" type=\"button\" 
                        onclick=\"switchViewModes('Normal', $userTwitterID, '$pageURL');\" disabled>Normal View</button>";
                    echo "<button class=\"settingsswitchbutton\" type=\"button\" 
                        onclick=\"switchViewModes('Compact', $userTwitterID, '$pageURL');\">Compact View</button>";
                } else {
                    echo "<button class=\"settingsswitchbutton\" type=\"button\" 
                        onclick=\"switchViewModes('Normal', $userTwitterID, '$pageURL');\">Normal View</button>";
                    echo "<button class=\"settingsswitchbutton\" type=\"button\" 
                        onclick=\"switchViewModes('Compact', $userTwitterID, '$pageURL');\" disabled>Compact View</button>";
                }
                echo "</div>";
                echo "<br/><br/>This is page $pageNum of $pageCount.<br/><br/>";
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
                <?php
                if (!is_null($userArtistRetweetSettings) && count($userArtistRetweetSettings) > 0) {
                    if ($viewMode === "Normal") {
                        echo "<table id=\"userartiststable\" class=\"dblisttable\">"
                        . "<tr>"
                        . "<th onclick=\"sortTable(0, 'userartiststable')\">Twitter Handle</th>"
                        . "<th>Remove</th>"
                        . "</tr>";
                        $i = 0;
                        foreach ($userArtistRetweetSettings as $userArtistRetweetSettings) {
                            $screenName = $userArtistRetweetSettings['screenname'];
                            $twitterHandle = "@" . $screenName;
                            $artistID = $userArtistRetweetSettings['artisttwitterid'];
                            $twitterLink = "<a href=\"https://twitter.com/" . $screenName . "\" target=\"_blank\"> "
                                    . $twitterHandle . "</a>";
                            echo "<tr>";
                            echo "<td>" . $twitterLink . "</td>";
                            $removeButton = "<button id=\"removebutton$i\" type=\"button\" onclick=\"addArtistForUser('$userTwitterID', '$artistID'"
                                    . ", '$twitterHandle', 'removebutton$i', 'Disable', 'Remove', '$viewMode', "
                                    . "$compactViewModeMaxColumns)\">Remove</button>";
                            echo "<td id=\"userartiststablerow$i\">$removeButton</td>";
                            echo "</tr>";
                            $i++;
                        }
                    } else {
                        echo "<table id=\"userartiststable\" class=\"dblisttable\">";
                        $i = 0;
                        foreach ($userArtistRetweetSettings as $userArtistRetweetSettings) {
                            $screenName = $userArtistRetweetSettings['screenname'];
                            $twitterLink = "<a href=\"https://twitter.com/" . $screenName . "\" target=\"_blank\"> "
                                    . "@" . $screenName . "</a>";
                            if ($i % $compactViewModeMaxColumns === 0) {
                                echo "<tr>";
                            }
                            echo "<td>" . $twitterLink . "</td>";
                            $i++;
                            if ($i % $compactViewModeMaxColumns === 0) {
                                echo "</tr>";
                            }
                        }
                    }
                    echo "</table>";
                }
                ?>
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
                    onclick="artistsSearch('dbsearch', '<?php echo $_SESSION['usertwitterid']; ?>',
                                    '<?php echo $viewMode ?>', <?php echo $compactViewModeMaxColumns ?>)">Search</button>
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
