function initialiseQueueStatus() {
    let elements = document.getElementsByClassName("rescheduledate");
    let date = new Date();
    let stringDate = date.toISOString();
    let day = stringDate.substring(0, stringDate.indexOf("T"));
    let hour = stringDate.substring(stringDate.indexOf("T") + 1, stringDate.indexOf("T") + 3);
    let hourModifier = 1;
    let minute = stringDate.substring(stringDate.indexOf("T") + 4, stringDate.indexOf("T") + 6);
    let minuteNumber = parseInt(minute);
    if (minuteNumber < 15) {
        minute = "15";
    } else if (minuteNumber < 30) {
        minute = "30";
    } else if (minuteNumber < 45) {
        minute = "45";
    } else {
        minute = "00";
        hourModifier++;
    }

    let hourNumber = parseInt(hour) + hourModifier;
    if (hourNumber < 10) {
        hour = "0".concat(hourNumber.toString());
    } else if (hourNumber === 24) {
        hour = "00";
    } else if (hourNumber === 25) {
        hour = "01";
    } else {
        hour = hourNumber.toString();
    }
    for (i = 0; i < elements.length; i++) {
        elements[i].value = day;
    }
    elements = document.getElementsByClassName("rescheduletimehour");
    for (i = 0; i < elements.length; i++) {
        elements[i].value = "hour_".concat(hour);
    }
    elements = document.getElementsByClassName("rescheduletimeminute");
    for (i = 0; i < elements.length; i++) {
        elements[i].value = "minute_".concat(minute);
    }
    twemoji.parse(document.body, {
        size: "14x14"
    });
}

function hideQueueEntry(id) {
    document.getElementById(id).innerHTML = "Entry deleted.";
}

function toggleRescheduleVisibility(id) {
    let element = document.getElementById("rdiv_".concat(id.toString()));
    let element2 = document.getElementById("ddiv_".concat(id.toString()));
    element.classList.toggle('transition');
    let button = document.getElementById("showrescheduleoptions_".concat(id.toString()));
    if (button.innerHTML === "Show reschedule options") {
        button.innerHTML = "Hide reschedule options";
        if (element2.classList.contains('transition')) {
            element2.classList.toggle('transition');
        }
    } else {
        button.innerHTML = "Show reschedule options";
    }
    if (element.classList.contains('transition')) {
        let queueDiv = document.getElementById("rresultdiv_".concat(id.toString()));
        queueDiv.innerHTML = "";
    }
}

function toggleDeleteVisibility(id) {
    let element = document.getElementById("ddiv_".concat(id.toString()));
    element.classList.toggle('transition');
    if (element.classList.contains('transition')) {
        let queueDiv = document.getElementById("dresultdiv_".concat(id.toString()));
        queueDiv.innerHTML = "";
    }
    let element2 = document.getElementById("rdiv_".concat(id.toString()));
    if (element2.classList.contains('transition')) {
        element2.classList.toggle('transition');
        let button = document.getElementById("showrescheduleoptions_".concat(id.toString()));
        button.innerHTML = "Show reschedule options";
    }
}

function deleteQueueEntry(id, userid) {
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            var json = this.responseText;
            if (json === "") {
                alert("Invalid request ID. Try again.");
            } else if (json === "false") {
                alert("A database error occurred, try refreshing the page and trying again.");
            } else if (json === "0") {
                alert("The entry was not found - nothing was deleted.");
            } else {
                let queueDiv = document.getElementById(id);
                queueDiv.innerHTML = "Queued entry deleted successfully.";
            }
        }
    };
    var params = 'request=deletequeueentry&id='.concat(id).concat('&userid=').concat(userid);
    //Send the proper header information along with the request
    xmlhttp.open("POST", "src/ajax/Ajax.php", true);
    xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xmlhttp.send(params);
}

function rescheduleQueueEntry(id, userid) {
    let rescheduledate = document.getElementById("rescheduledate_".concat(id.toString())).value;
    let year = parseInt(rescheduledate.substring(0, 4));
    let month = parseInt(rescheduledate.substring(5, 7));
    let day = parseInt(rescheduledate.substring(8, 10));
    let hour = document.getElementById("rescheduletimehour_".concat(id.toString())).value;
    let minute = document.getElementById("rescheduletimeminute_".concat(id.toString())).value;
    hour = hour.substring(hour.indexOf("_") + 1);
    minute = minute.substring(minute.indexOf("_") + 1);
    let date1h1mFromNow = new Date();
    date1h1mFromNow.setTime(date1h1mFromNow.getTime() + (1 * 60 * 60 * 1000) + (1 * 60 * 1000));
    let scheduleDate = new Date(year, month - 1, day, hour, minute, 0, 0);
    if (scheduleDate < date1h1mFromNow) {
        let queueDiv = document.getElementById("rresultdiv_".concat(id.toString()));
        queueDiv.innerHTML = "Invalid date and/or time. Make sure to set a date at least 1 hour from now.";
        if (!queueDiv.classList.contains('transition')) {
            queueDiv.classList.add('transition');
        }
        return;
    }
    let time = rescheduledate.concat(" ").concat(hour).concat(":").concat(minute).concat(":00");
    var xmlhttp = new XMLHttpRequest();
    xmlhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            var json = this.responseText;
            let queueDiv = document.getElementById("rresultdiv_".concat(id.toString()));
            if (json === "Success") {
                queueDiv.innerHTML = "Queued entry rescheduled successfully. Refresh the page if you want to see the updated queue order.";
                let rTimeDiv = document.getElementById("rtimediv_".concat(id.toString()));
                rTimeDiv.innerHTML = time;
            } else if (json === "Database error") {
                queueDiv.innerHTML = "A database error occurred, try refreshing the page and trying again.";
            } else if (json === "Record does not exist") {
                queueDiv.innerHTML = "The entry was not found - could not reschedule.";
            } else {
                queueDiv.innerHTML = "Invalid request ID. Try again.";
            }
            if (!queueDiv.classList.contains('transition')) {
                queueDiv.classList.add('transition');
            }
        }
    };
    var params = 'request=reschedulequeueentry&id='.concat(id).concat('&userid=').concat(userid)
            .concat('&newtime=').concat(time);
    //Send the proper header information along with the request
    xmlhttp.open("POST", "src/ajax/Ajax.php", true);
    xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
    xmlhttp.send(params);
}