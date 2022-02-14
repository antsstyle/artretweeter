function disableSaveButton() {
    document.getElementById("savesettingsbutton").disabled = true;
}

function enableSaveButton() {
    document.getElementById("savesettingsbutton").disabled = false;
}

function showDisableAutomationWarning(showWarning) {
    let warningDiv = document.getElementById("autowarningdiv");
    if (showWarning === "Y") {
        warningDiv.classList.toggle('transition');
    }
}

function selectAllHours(selectAll) {
    let checkboxes = document.getElementsByClassName("hourcheckbox");
    for (i = 0; i < checkboxes.length; i++) {
        checkboxes[i].checked = selectAll;
    }
    if (selectAll === false) {
        disableSaveButton();
        document.getElementById("hourintervalserrormsg").innerHTML = "You must select at least one hour interval.";
    } else {
        enableSaveButton();
        document.getElementById("hourintervalserrormsg").innerHTML = "&nbsp;";
    }
}

function checkValidMediaSettings() {
    let checkboxes = document.getElementsByClassName("mediacheckbox");
    let noneChecked = true;
    for (i = 0; i < checkboxes.length; i++) {
        if (checkboxes[i].checked === true) {
            noneChecked = false;
        }
    }
    if (noneChecked) {
        disableSaveButton();
        document.getElementById("mediasettingserrormsg").innerHTML = "You must select at least one media type.";
    } else {
        enableSaveButton();
        document.getElementById("mediasettingserrormsg").innerHTML = "&nbsp;";
    }
}

function checkValidDays() {
    let checkboxes = document.getElementsByClassName("daycheckbox");
    let noneChecked = true;
    for (i = 0; i < checkboxes.length; i++) {
        if (checkboxes[i].checked === true) {
            noneChecked = false;
        }
    }
    if (noneChecked) {
        disableSaveButton();
        document.getElementById("dayintervalserrormsg").innerHTML = "You must select at least one day.";
    } else {
        enableSaveButton();
        document.getElementById("dayintervalserrormsg").innerHTML = "";
    }
}

function checkValidHours() {
    let checkboxes = document.getElementsByClassName("hourcheckbox");
    let noneChecked = true;
    for (i = 0; i < checkboxes.length; i++) {
        if (checkboxes[i].checked === true) {
            noneChecked = false;
        }
    }
    if (noneChecked) {
        disableSaveButton();
        document.getElementById("hourintervalserrormsg").innerHTML = "You must select at least one hour interval.";
    } else {
        enableSaveButton();
        document.getElementById("hourintervalserrormsg").innerHTML = "&nbsp;";
    }
}

function checkValidMinutes() {
    let checkboxes = document.getElementsByClassName("minutecheckbox");
    let noneChecked = true;
    for (i = 0; i < checkboxes.length; i++) {
        if (checkboxes[i].checked === true) {
            noneChecked = false;
        }
    }
    if (noneChecked) {
        disableSaveButton();
        document.getElementById("minuteintervalserrormsg").innerHTML = "You must select at least one minute interval.";
    } else {
        enableSaveButton();
        document.getElementById("minuteintervalserrormsg").innerHTML = "&nbsp;";
    }
}

function checkValidIncludeText(elementID, checkboxID, errorElementID) {
    let enabled = document.getElementById(checkboxID).checked;
    let text = document.getElementById(elementID).value;
    let string = text.toString();
    let strlen = string.length;
    if (strlen === 0 && enabled) {
        disableSaveButton();
        document.getElementById(errorElementID).innerHTML = "This field can't be empty when enabling this feature.";
        return;
    }
    if (strlen > 50) {
        disableSaveButton();
        document.getElementById(errorElementID).innerHTML = "The text must be less than 50 characters.";
        return;
    }
    let words = string.split(' ');
    for (i = 0; i < words.length; i++) {
        if (words[i].startsWith("#") && words[i].length < 4) {
            disableSaveButton();
            document.getElementById(errorElementID).innerHTML = "Words must be 3 or more characters each (excluding hashtag symbol).";
            return;
        } else if (!words[i].startsWith("#") && words[i].length < 3) {
            disableSaveButton();
            document.getElementById(errorElementID).innerHTML = "Words must be 3 or more characters each (excluding hashtag symbol).";
            return;
        }
    }
    enableSaveButton();
    document.getElementById(errorElementID).innerHTML = "&nbsp;";
}

function checkValidRetweetPercent(elementID, errorElementID) {
    let text = document.getElementById(elementID).value;
    let percentage = parseInt(text);
    if (isNaN(percentage)) {
        disableSaveButton();
        document.getElementById(errorElementID).innerHTML = "Enter only numbers, no text.";
        return;
    }
    if (percentage < 20 || percentage > 75) {
        disableSaveButton();
        document.getElementById(errorElementID).innerHTML = "You must enter a number between 20 and 75.";
        return;
    }
    enableSaveButton();
    document.getElementById(errorElementID).innerHTML = "&nbsp;";
}

function getUserNonArtistAutomationSettings(id) {
    if (id === null) {
        return;
    } else {
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            if (this.readyState === 4 && this.status === 200) {
                var offset = 0;
                var date = null;
                if (this.responseText === "") {
                    offset = new Date().getTimezoneOffset();
                } else {
                    var json = JSON.parse(this.responseText);
                    let hourflags = json.hourflags;
                    for (let i = 0; i < 24; i++) {
                        let flag = hourflags.charAt(i);
                        if (flag === "Y") {
                            let j = i + 1;
                            let iString = "";
                            let jString = "";
                            if (i < 10) {
                                iString = "0".concat(i.toString());
                            } else {
                                iString = i.toString();
                            }
                            if (j < 10) {
                                jString = "0".concat(j.toString());
                            } else if (j === 24) {
                                jString = "00";
                            } else {
                                jString = j.toString();
                            }
                            let concatString = "h".concat(iString).concat(jString);
                            document.getElementById(concatString).checked = true;
                        }
                    }

                    let enableautomatedretweeting = json.automationenabled;
                    if (enableautomatedretweeting === "Y") {
                        document.getElementById("enableautomatedretweeting").checked = true;
                    } else {
                        document.getElementById("enableautomatedretweeting").checked = false;
                    }

                    let oldtweetcutoffdateenabled = json.oldtweetcutoffdateenabled;
                    if (oldtweetcutoffdateenabled === "Y") {
                        document.getElementById("ignoreoldtweets").checked = true;
                    } else {
                        document.getElementById("ignoreoldtweets").checked = false;
                    }

                    let includetextenabled = json.includetextenabled;
                    if (includetextenabled === "Y") {
                        document.getElementById("includetextenabled").checked = true;
                    } else {
                        document.getElementById("includetextenabled").checked = false;
                    }

                    let excludetextenabled = json.excludetextenabled;
                    if (excludetextenabled === "Y") {
                        document.getElementById("excludetextenabled").checked = true;
                    } else {
                        document.getElementById("excludetextenabled").checked = false;
                    }

                    let includetextcondition = json.includetextcondition;
                    if (includetextcondition === "Y") {
                        document.getElementById("includetextoperation").checked = true;
                    } else {
                        document.getElementById("includetextoperation").checked = false;
                    }

                    let excludetextcondition = json.excludetextcondition;
                    if (excludetextcondition === "Y") {
                        document.getElementById("excludetextoperation").checked = true;
                    } else {
                        document.getElementById("excludetextoperation").checked = false;
                    }

                    let includetext = json.includetext;
                    if (includetext !== null) {
                        document.getElementById("includetext").value = includetext;
                    }

                    let excludetext = json.excludetext;
                    if (excludetext !== null) {
                        document.getElementById("excludetext").value = excludetext;
                    }

                    let imagesenabled = json.imagesenabled;
                    if (imagesenabled === "Y") {
                        document.getElementById("imagesenabled").checked = true;
                    } else {
                        document.getElementById("imagesenabled").checked = false;
                    }

                    let gifsenabled = json.gifsenabled;
                    if (gifsenabled === "Y") {
                        document.getElementById("gifsenabled").checked = true;
                    } else {
                        document.getElementById("gifsenabled").checked = false;
                    }

                    let videosenabled = json.videosenabled;
                    if (videosenabled === "Y") {
                        document.getElementById("videosenabled").checked = true;
                    } else {
                        document.getElementById("videosenabled").checked = false;
                    }

                    date = json.oldtweetcutoffdate;
                    let dbHourOffset = parseInt(json.timezonehouroffset);
                    let dbMinuteOffset = parseInt(json.timezoneminuteoffset);
                    offset = (dbHourOffset * 60) + (dbMinuteOffset);
                }
                let hourOffset = Math.abs(Math.floor(offset / 60));
                let minuteOffset = offset % 60;
                let fullString = "";
                let hourString = "";
                let minuteString = "";
                if (hourOffset < 10) {
                    hourString = "0".concat(hourOffset);
                } else {
                    hourString = hourOffset;
                }
                if (minuteOffset < 10) {
                    minuteString = "0".concat(minuteOffset);
                } else {
                    minuteString = minuteOffset;
                }
                if (offset < 0) {
                    fullString = "t-".concat(hourString).concat(minuteString);
                } else {
                    fullString = "t".concat(hourString).concat(minuteString);
                }
                let element = document.getElementById("timezone");
                element.value = fullString;
                if (date !== null) {
                    date = date.substring(0, date.indexOf(" "));
                    document.getElementById("ignoreoldtweetsdate").value = date;
                }
            }
        };
        var params = 'request=usernonartistautomationsettings&userid='.concat(id);
        //Send the proper header information along with the request
        xmlhttp.open("POST", "src/ajax/Ajax.php", true);
        xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
        xmlhttp.send(params);
    }
}

function getUserAutomationSettings(id) {
    if (id === null) {
        return;
    } else {
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.onreadystatechange = function () {
            if (this.readyState === 4 && this.status === 200) {
                var offset = 0;
                var date = null;
                if (this.responseText === "") {
                    offset = new Date().getTimezoneOffset();
                } else {
                    var json = JSON.parse(this.responseText);
                    let hourflags = json.hourflags;
                    for (let i = 0; i < 24; i++) {
                        let flag = hourflags.charAt(i);
                        if (flag === "Y") {
                            let j = i + 1;
                            let iString = "";
                            let jString = "";
                            if (i < 10) {
                                iString = "0".concat(i.toString());
                            } else {
                                iString = i.toString();
                            }
                            if (j < 10) {
                                jString = "0".concat(j.toString());
                            } else if (j === 24) {
                                jString = "00";
                            } else {
                                jString = j.toString();
                            }
                            let concatString = "h".concat(iString).concat(jString);
                            document.getElementById(concatString).checked = true;
                        }
                    }
                    var minuteflags = json.minuteflags;
                    for (let i = 0; i < 4; i++) {
                        let flag = minuteflags.charAt(i);
                        if (flag === "Y") {
                            let minString = "minute".concat((i * 15).toString());
                            document.getElementById(minString).checked = true;
                        }
                    }

                    let enableautomatedretweeting = json.automationenabled;
                    if (enableautomatedretweeting === "Y") {
                        document.getElementById("enableautomatedretweeting").checked = true;
                    } else {
                        document.getElementById("enableautomatedretweeting").checked = false;
                    }

                    let oldtweetcutoffdateenabled = json.oldtweetcutoffdateenabled;
                    if (oldtweetcutoffdateenabled === "Y") {
                        document.getElementById("ignoreoldtweets").checked = true;
                    } else {
                        document.getElementById("ignoreoldtweets").checked = false;
                    }

                    let includetextenabled = json.includetextenabled;
                    if (includetextenabled === "Y") {
                        document.getElementById("includetextenabled").checked = true;
                    } else {
                        document.getElementById("includetextenabled").checked = false;
                    }

                    let excludetextenabled = json.excludetextenabled;
                    if (excludetextenabled === "Y") {
                        document.getElementById("excludetextenabled").checked = true;
                    } else {
                        document.getElementById("excludetextenabled").checked = false;
                    }

                    let includetextcondition = json.includetextcondition;
                    if (includetextcondition === "Y") {
                        document.getElementById("includetextoperation").checked = true;
                    } else {
                        document.getElementById("includetextoperation").checked = false;
                    }

                    let excludetextcondition = json.excludetextcondition;
                    if (excludetextcondition === "Y") {
                        document.getElementById("excludetextoperation").checked = true;
                    } else {
                        document.getElementById("excludetextoperation").checked = false;
                    }

                    let includetext = json.includetext;
                    if (includetext !== null) {
                        document.getElementById("includetext").value = includetext;
                    }

                    let excludetext = json.excludetext;
                    if (excludetext !== null) {
                        document.getElementById("excludetext").value = excludetext;
                    }

                    let retweetpercent = json.retweetpercent;
                    if (retweetpercent !== null) {
                        document.getElementById("metricspercent").value = retweetpercent;
                    }


                    let metricsmeasurementtype = json.metricsmeasurementtype;
                    if (metricsmeasurementtype === "Mean Average") {
                        document.getElementById("metricsmethod").value = "mean_average";
                    } else if (metricsmeasurementtype === "Adaptive") {
                        document.getElementById("metricsmethod").value = "adaptive";
                    }

                    let imagesenabled = json.imagesenabled;
                    if (imagesenabled === "Y") {
                        document.getElementById("imagesenabled").checked = true;
                    } else {
                        document.getElementById("imagesenabled").checked = false;
                    }

                    let gifsenabled = json.gifsenabled;
                    if (gifsenabled === "Y") {
                        document.getElementById("gifsenabled").checked = true;
                    } else {
                        document.getElementById("gifsenabled").checked = false;
                    }

                    let videosenabled = json.videosenabled;
                    if (videosenabled === "Y") {
                        document.getElementById("videosenabled").checked = true;
                    } else {
                        document.getElementById("videosenabled").checked = false;
                    }

                    date = json.oldtweetcutoffdate;
                    let dbHourOffset = parseInt(json.timezonehouroffset);
                    let dbMinuteOffset = parseInt(json.timezoneminuteoffset);
                    offset = (dbHourOffset * 60) + (dbMinuteOffset);
                }
                let hourOffset = Math.abs(Math.floor(offset / 60));
                let minuteOffset = offset % 60;
                let fullString = "";
                let hourString = "";
                let minuteString = "";
                if (hourOffset < 10) {
                    hourString = "0".concat(hourOffset);
                } else {
                    hourString = hourOffset;
                }
                if (minuteOffset < 10) {
                    minuteString = "0".concat(minuteOffset);
                } else {
                    minuteString = minuteOffset;
                }
                if (offset < 0) {
                    fullString = "t-".concat(hourString).concat(minuteString);
                } else {
                    fullString = "t".concat(hourString).concat(minuteString);
                }
                let element = document.getElementById("timezone");
                element.value = fullString;
                if (date !== null) {
                    date = date.substring(0, date.indexOf(" "));
                    document.getElementById("ignoreoldtweetsdate").value = date;
                }
            }
        };
        var params = 'request=userautomationsettings&userid='.concat(id);
        //Send the proper header information along with the request
        xmlhttp.open("POST", "src/ajax/Ajax.php", true);
        xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
        xmlhttp.send(params);
    }
}
