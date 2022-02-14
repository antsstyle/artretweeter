function submitArtist(usertwitterid) {
    let twitterhandle = document.getElementById("submitartistinput").value;
    if (!twitterhandle.match(/^@?[A-Za-z0-9_]{1,15}$/)) {
        var resulttext = "Invalid twitter handle. Twitter usernames are 1-15 characters (with or without the @).<br/><br/>";
        document.getElementById("submitartisttextdiv").innerHTML = resulttext;
        return;
    }
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            if (this.responseText === "Invalid username") {
                var resulttext = "Invalid search text. Twitter usernames are 1-15 characters (with or without the @).<br/><br/>";
                document.getElementById("submitartisttextdiv").innerHTML = resulttext;
            } else {
                document.getElementById("submitartisttextdiv").innerHTML = this.responseText;
;            }
        }
    };
    var params = 'artisttwitterhandle='.concat(twitterhandle).concat('&userid=').concat(usertwitterid);
    //Send the proper header information along with the request
    xmlhttp.open("POST", "src/ajax/SubmitArtist.php", true);
    xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xmlhttp.send(params);
}

