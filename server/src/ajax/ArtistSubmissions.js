function approveArtistSubmission(artistScreenName) {
    if (!artistScreenName.match(/^[A-Za-z0-9_]{1,15}$/)) {
        var resulttext = "Invalid twitter handle. Twitter usernames are 1-15 characters (with or without the @).<br/><br/>";
        document.getElementById("artistapprovalresultdiv").innerHTML = resulttext;
        return;
    }
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            document.getElementById("artistapprovalresultdiv").innerHTML = this.responseText;
        }
    };
    var params = 'artistscreenname='.concat(artistScreenName)
            .concat("&type=approval");
    //Send the proper header information along with the request
    xmlhttp.open("POST", "/artretweeter/src/ajax/ArtistSubmissions.php", true);
    xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xmlhttp.send(params);
}

function rejectArtistSubmission(artistScreenName) {
    if (!artistScreenName.match(/^[A-Za-z0-9_]{1,15}$/)) {
        var resulttext = "Invalid twitter handle. Twitter usernames are 1-15 characters (with or without the @).<br/><br/>";
        document.getElementById("artistapprovalresultdiv").innerHTML = resulttext;
        return;
    }
    var rejectionreason = window.prompt("Enter rejection reason: ");
    if (rejectionreason.trim() === "") {
        var resulttext = "Rejection reason cannot be empty.<br/><br/>";
        document.getElementById("artistapprovalresultdiv").innerHTML = resulttext;
    }
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            document.getElementById("artistapprovalresultdiv").innerHTML = this.responseText;
        }
    };
    var params = 'artistscreenname='.concat(artistScreenName)
            .concat("&type=rejection&reason=").concat(rejectionreason);
    //Send the proper header information along with the request
    xmlhttp.open("POST", "/artretweeter/src/ajax/ArtistSubmissions.php", true);
    xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xmlhttp.send(params);
}