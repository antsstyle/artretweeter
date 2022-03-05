function submitArtist(usertwitterid) {
    let twitterhandle = document.getElementById("submitartistinput").value;
    if (!twitterhandle.match(/^@?[A-Za-z0-9_]{1,15}$/)) {
        var resulttext = "Invalid twitter handle. Twitter usernames are 1-15 characters (with or without the @).<br/><br/>";
        document.getElementById("submitartisttextdiv").innerHTML = resulttext;
        return;
    }
    if (twitterhandle.startsWith("@")) {
        twitterhandle = twitterhandle.substring(1);
    }
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            if (this.responseText === "Invalid username") {
                var resulttext = "Invalid search text. Twitter usernames are 1-15 characters (with or without the @).<br/><br/>";
                document.getElementById("submitartisttextdiv").innerHTML = resulttext;
            } else {
                var response = JSON.parse(this.responseText);
                document.getElementById("submitartisttextdiv").innerHTML = response[0];
                if (response[1] !== null) {
                    var usersubmissionstable = document.getElementById("usersubmissionstable");
                    var newRow = usersubmissionstable.insertRow(1);
                    var cell1 = newRow.insertCell(0);
                    var cell2 = newRow.insertCell(1);
                    var cell3 = newRow.insertCell(2);
                    var cell4 = newRow.insertCell(3);
                    var cell5 = newRow.insertCell(4);
                    var cell6 = newRow.insertCell(5);
                    var twitterLink = "<a href=\"https://twitter.com/" + twitterhandle + "\" target=\"_blank\"> "
                            + "@" + twitterhandle + "</a>";
                    let date = new Date();
                    var dateString = new Date(date.getTime() - (date.getTimezoneOffset() * 60000 )).toISOString().split("T")[0];
                    var cancelButton = "<button type=\"button\" onclick=\"cancelArtistSubmissionForUser('" 
                            + usertwitterid + "','" + twitterhandle + "')\">Cancel</button>";
                    cell1.innerHTML = twitterLink;
                    cell2.innerHTML = dateString;
                    cell3.innerHTML = "Pending";
                    cell4.innerHTML = "N/A";
                    cell5.innerHTML = "N/A";
                    cell6.innerHTML = cancelButton;
                }
            }
        }
    };
    var params = 'artisttwitterhandle='.concat(twitterhandle).concat('&userid=').concat(usertwitterid).concat("&operation=submit");
    xmlhttp.open("POST", "src/ajax/SubmitArtist.php", true);
    xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xmlhttp.send(params);
}

function cancelArtistSubmissionForUser(usertwitterid, artistscreenname) {
    var usersubmissionstable = document.getElementById("usersubmissionstable");
    var actualRow = -1;
    var tablelength = usersubmissionstable.rows.length;
    for (let i = 1; i < tablelength; i++) {
        let tablecellhtml = usersubmissionstable.rows[i].cells[0].innerHTML;
        let index1 = tablecellhtml.indexOf(">") + 1;
        let index2 = tablecellhtml.indexOf("<", index1);
        let artistScreenNameInTable = tablecellhtml.substring(index1, index2).trim();
        if (artistScreenNameInTable.includes(artistscreenname)) {
            actualRow = i;
            break;
        }
    }
    if (!artistscreenname.match(/^@?[A-Za-z0-9_]{1,15}$/)) {
        var resulttext = "Invalid twitter handle. Twitter usernames are 1-15 characters (with or without the @).<br/><br/>";
        document.getElementById("usersubmissionsresultsdiv").innerHTML = resulttext;
        return;
    }
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            document.getElementById("usersubmissionsresultsdiv").innerHTML = this.responseText;
            usersubmissionstable.deleteRow(actualRow);
        }
    };
    var params = 'artisttwitterhandle='.concat(artistscreenname).concat('&userid=').concat(usertwitterid).concat("&operation=cancel");
    xmlhttp.open("POST", "src/ajax/SubmitArtist.php", true);
    xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xmlhttp.send(params);
}