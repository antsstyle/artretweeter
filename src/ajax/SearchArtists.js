function artistsSearch(searchid, userid, viewmode, maxcolumns) {
    var input = document.getElementById(searchid);
    if (input === null) {
        return;
    } else {
        var searchstring = input.value;
        if (!searchstring.match(/^@?[A-Za-z0-9_]{1,15}$/)) {
            var resulttext = "Invalid search text. Twitter usernames are 1-15 characters (with or without the @).<br/><br/>";
            document.getElementById("searchresultstextdiv").innerHTML = resulttext;
            return;
        }
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            if (this.readyState === 4 && this.status === 200) {
                if (this.responseText === "") {
                    console.log("No response");
                } else if (this.responseText === "Invalid username") {
                    var resulttext = "Invalid search text. Twitter usernames are 1-15 characters (with or without the @).<br/><br/>";
                    document.getElementById("searchresultstextdiv").innerHTML = resulttext;
                } else {
                    console.log(this.responseText);
                    var resultsJSON = JSON.parse(this.responseText);
                    var tablestring = processArtistResults(resultsJSON, viewmode, maxcolumns);
                    let resultcount = resultsJSON[1].resultcount;
                    var resulttext = resultcount.toString();
                    resulttext = "Search finished. " + resulttext + " results found.<br/><br/>";
                    document.getElementById("searchresultstextdiv").innerHTML = resulttext;
                    document.getElementById("searchresultsdiv").innerHTML = tablestring;
                }
            }
        };
        var params = 'searchstring='.concat(searchstring).concat('&userid=').concat(userid);
        //Send the proper header information along with the request
        xmlhttp.open("POST", "src/ajax/SearchArtists.php", true);
        xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
        xmlhttp.send(params);
    }
}

function storeSearchResults() {
    document.getElementById("tablecachediv").innerHTML = document.getElementById("searchresultsdiv").innerHTML;
}

function resetSearchTable() {
    document.getElementById("searchresultsdiv").innerHTML = document.getElementById("tablecachediv").innerHTML;
    document.getElementById("searchresultstextdiv").innerHTML = "";
}

function addArtistForUser(userid, artistid, twitterhandle, buttonid, operation, type, viewmode, maxcolumns) {
    var userartiststable = document.getElementById("userartiststable");
    var actualRow = -1;
    var tablelength = userartiststable.rows.length;
    if (viewmode === "Normal") {
        for (let i = 1; i < tablelength; i++) {
            let string1 = "'";
            let tablecellhtml = userartiststable.rows[i].cells[1].innerHTML;
            let index1 = 0;
            for (let j = 0; j < 3; j++) {
                index1 = tablecellhtml.indexOf(string1, index1) + 1;
            }
            let index2 = tablecellhtml.indexOf(string1, index1);
            let artistIDInTable = tablecellhtml.substring(index1, index2).trim();
            if (artistIDInTable.includes(artistid)) {
                actualRow = i;
                break;
            }
        }
    }
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            if (this.responseText === "") {
                console.log("No response");
            } else {
                var json;
                try {
                    json = JSON.parse(this.responseText);
                } catch (err) {
                    resulttext = this.responseText.concat("<br/><br/>");
                    if (type === "Update") {
                        document.getElementById("searchresultstextdiv").innerHTML = resulttext;
                    } else if (type === "Remove") {
                        document.getElementById("userartistsresultsdiv").innerHTML = resulttext;
                    }
                }
                var artisttwitterid = json.artisttwitterid;
                var screenname = json.screenname;
                var affectedrows = json.affectedrows;
                var resulttext;
                if (parseInt(affectedrows) === 0) {
                    resulttext = "An error occurred - try again later or contact @antsstyle if it persists.<br/><br/>";
                    if (type === "Update") {
                        document.getElementById("searchresultstextdiv").innerHTML = resulttext;
                    } else if (type === "Remove") {
                        document.getElementById("userartistsresultsdiv").innerHTML = resulttext;
                    }
                } else if (operation === "Enable") {
                    resulttext = "Retweets enabled for artist @" + screenname + " successfully.<br/><br/>";
                    if (type === "Update") {
                        document.getElementById("searchresultstextdiv").innerHTML = resulttext;
                        document.getElementById(buttonid).innerHTML = "Disable automated retweeting";
                        var lowerCaseScreenName = "@" + screenname.toLowerCase();
                        var lastIndex = -1;
                        for (let i = 1; i < tablelength; i++) {
                            let tablecellhtml = userartiststable.rows[i].cells[0].innerHTML;
                            let index1 = tablecellhtml.indexOf(">") + 1;
                            let index2 = tablecellhtml.indexOf("<", index1);
                            let artistHandle = tablecellhtml.substring(index1, index2).trim().toLowerCase();
                            if (artistHandle > lowerCaseScreenName) {
                                lastIndex = i;
                                break;
                            }
                        }
                        console.log("View mode: " + viewmode);
                        if (viewmode === "Normal") {
                            var newRow = userartiststable.insertRow(lastIndex);
                            var cell1 = newRow.insertCell(0);
                            var cell2 = newRow.insertCell(1);
                            var tableCount = userartiststable.rows.length;
                            var buttonidcount = buttonid.substring(12);
                            var newOnclick = "addArtistForUser('" + userid + "','" + artistid + "','" + buttonid + "','" + "Disable"
                                    + "','Update','" + buttonidcount + "')";
                            document.getElementById(buttonid).setAttribute('onclick', newOnclick);
                            cell1.innerHTML = "<a href=\"https://twitter.com/" + screenname + "\" target=\"_blank\"> "
                                    + "@" + screenname + "</a>";
                            cell2.innerHTML = "<button id=\"followbutton" + tableCount + "\" type=\"button\" "
                                    + "onclick=\"addArtistForUser('" + userid + "', '" + artisttwitterid + "'"
                                    + ", 'removebutton" + tableCount + "', 'Disable', 'Remove', '" + tableCount + "', "
                                    + "'" + viewmode + "', " + maxcolumns + "')\">Remove</button>";
                        } else {
                            let newElement = "<a href=\"https://twitter.com/" + twitterhandle.substring(1) + "\">" + twitterhandle + "</a>";
                            let newIndices = findRowAndColumnForTwitterHandle("userartiststable", newElement, maxcolumns);
                            console.log("New indices: " + newIndices);
                            insertIntoTable(newIndices[0], newIndices[1], newElement, "userartiststable", maxcolumns);
                        }
                    } else if (type === "Remove") {
                        document.getElementById("userartistsresultsdiv").innerHTML = resulttext;
                        userartiststable.deleteRow(actualRow);
                    }
                } else {
                    resulttext = "Retweets disabled for artist @" + screenname + " successfully.<br/><br/>";
                    if (type === "Update") {
                        document.getElementById("searchresultstextdiv").innerHTML = resulttext;
                        document.getElementById(buttonid).innerHTML = "Enable automated retweeting";
                        var buttonidcount = buttonid.substring(12);
                        var newOnclick = "addArtistForUser('" + userid + "','" + artistid + "','" + buttonid + "','" + "Enable"
                                + "','Update','" + buttonidcount + "')";
                        document.getElementById(buttonid).setAttribute('onclick', newOnclick);
                        userartiststable.deleteRow(actualRow);
                    } else if (type === "Remove") {
                        document.getElementById("userartistsresultsdiv").innerHTML = resulttext;
                        userartiststable.deleteRow(actualRow);
                        changeButtonInSecondTable(artistid);
                    }
                }
            }
        }
    };
    var params = 'userid='.concat(userid).concat('&artistid=').concat(artistid).concat('&operation=').concat(operation);
    //Send the proper header information along with the request
    xmlhttp.open("POST", "src/ajax/AddArtistForUser.php", true);
    xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xmlhttp.send(params);
}

function changeButtonInSecondTable(artistid) {
    var maintable = document.getElementById("maintable");
    var tablelength = maintable.rows.length;
    for (let i = 1; i < tablelength; i++) {
        var tdhtml = maintable.rows[i].cells[2].innerHTML;
        var index1 = 0;
        var string1 = "'";
        for (let j = 0; j < 3; j++) {
            index1 = tdhtml.indexOf(string1, index1) + 1;
        }
        var index2 = tdhtml.indexOf(string1, index1);
        var artistIDInTable = tdhtml.substring(index1, index2).trim();
        if (artistIDInTable.includes(artistid)) {
            var oldbutton = maintable.rows[i].cells[2].innerHTML;
            oldbutton = oldbutton.replace("'Disable'", "'Enable'");
            oldbutton = oldbutton.replace("Disable automated retweeting", "Enable automated retweeting");
            maintable.rows[i].cells[2].innerHTML = oldbutton;
            break;
        }
    }
}

function processArtistResults(resultsJSON, viewMode, maxColumns) {
    let tableString = "<table id=\"maintable\" class=\"dblisttable\"><tr>"
            + "<th onclick=\"sortTable(0, 'maintable')\">Twitter Handle</th>"
            + "<th>Options</th>"
            + "</tr>";
    let userTwitterID = resultsJSON[0];
    let rows = resultsJSON[1].rows;
    if (rows) {
        for (let i = 0; i < rows.length; i++) {
            let iString = i.toString();
            let row = rows[i];
            let screenName = row.screenname;
            let artistID = row.twitterid;
            let hrefScreenName = "<a href=\"https://twitter.com/" + screenName + "\" target=_\"blank\">"
                    + "@" + screenName + "</a>";
            let addButton = "<button id=\"followbutton" + iString + "\" type=\"button\" onclick=\"addArtistForUser('"
                    + userTwitterID + "', '" + artistID + "'" + ", '@" + screenName + "'"
                    + ", 'followbutton" + iString + "','Enable', 'Update', '"
                    + viewMode + "'," + maxColumns + ")\">Enable automated retweeting</button>";
            tableString += "<tr>";
            tableString += "<td>" + hrefScreenName + "</td>";
            tableString += "<td>" + addButton + "</td>";
            tableString += "</tr>";
        }
    }
    tableString += "</table>";
    return tableString;
}

function switchViewModes(viewMode, userTwitterID, href) {
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            if (this.responseText === "") {
                console.log("No response");
            } else {
                window.location.href = href;
            }
        }
    };
    var params = 'request=switchviewmodes'.concat('&userid=').concat(userTwitterID).concat("&viewmode=").concat(viewMode);
    //Send the proper header information along with the request
    xmlhttp.open("POST", "src/ajax/Ajax.php", true);
    xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xmlhttp.send(params);

}