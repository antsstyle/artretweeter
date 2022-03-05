let loc = window.location.href;

let activemenuitem = "activemenuitem";
let activecontentstyle = "style=\"max-height: 100%\"";

let website = "https://antsstyle.com/";

let index = loc.indexOf(".com/") + 5;
let path = loc.substring(index);

var mmactive = "";
var artrt0active = "";
var artrt1active = "";
var artrt2active = "";
var nftcbactive = "";

var mmstyle = "";
var artrt0style = "";
var artrt1style = "";
var artrt2style = "";
var nftcbstyle = "";

if (path.includes("artretweeter")) {
    if (path.includes("nonartist") || path.includes("submitartist") || path.includes("subscribe") || path.includes("index")
             || path.includes("addartists")) {
        artrt2active = activemenuitem;
        artrt2style = activecontentstyle;
    } else if (path.includes("artist")) {
        artrt1active = activemenuitem;
        artrt1style = activecontentstyle;
    } else {
        artrt0active = activemenuitem;
        artrt0style = activecontentstyle;
    }
} else if (path.includes("nftcryptoblocker")) {
    nftcbactive = activemenuitem;
    nftcbstyle = activecontentstyle;
} else {
    mmactive = activemenuitem;
    mmstyle = activecontentstyle;
}



document.write('<div class=\"sidenav\">\
                <button class=\"collapsiblemenuitem ' + mmactive + '\" id=\"mainmenu\"><b>Home</b></button>\
                <div class=\"content\" ' + mmstyle + '>\
                    <a href=\"' + website + '\">About</a>\
                    <a href=\"' + website + 'apps\">Apps</a>\
                </div>\
                <br/>\
                <button class=\"collapsiblemenuitem ' + artrt0active + '\" id=\"artretweetermenu0\"><b>ArtRetweeter - Information</b></button>\
                <div class=\"content\" ' + artrt0style + '>\
                    <a href=\"' + website + 'artretweeter/intro\">Introduction</a>\
                    <a href=\"' + website + 'artretweeter/privacy\">Privacy</a> \
                    <a href=\"' + website + 'artretweeter/tos\">Terms of Service</a> \
                </div>\
                <br/>\
                <button class=\"collapsiblemenuitem ' + artrt1active + '\" id=\"artretweetermenu1\"><b>ArtRetweeter - For Artists</b></button>\
                <div class=\"content\" ' + artrt1style + '>\
                    <a href=\"' + website + 'artretweeter/artistshome\">Home</a>\
                    <a href=\"' + website + 'artretweeter/artistsinfo\">Info & FAQ</a> \
                    <a href=\"' + website + 'artretweeter/artistsettings\">Settings</a> \
                    <a href=\"' + website + 'artretweeter/artistqueuestatus\">Queue Status</a>\
                </div>\
                <br/>\
                <button class=\"collapsiblemenuitem ' + artrt2active + '\" id=\"artretweetermenu2\"><b>ArtRetweeter - For Non-Artists</b></button>\
                <div class=\"content\" ' + artrt2style + '>\
                    <a href=\"' + website + 'artretweeter/nonartistshome\">Home</a>\
                    <a href=\"' + website + 'artretweeter/nonartistsinfo\">Info & FAQ</a> \
                    <a href=\"' + website + 'artretweeter/nonartistsettings\">Settings</a> \
                    <a href=\"' + website + 'artretweeter/nonartistqueuestatus\">Queue Status</a>\
                    <a href=\"' + website + 'artretweeter/addartists\">Add Artists</a>\
                    <a href=\"' + website + 'artretweeter/subscribe\">Subscribe</a>\
                </div>\
                <br/>\
                <button class=\"collapsiblemenuitem ' + nftcbactive + '\" id=\"nftcryptoblockermenu\"><b>NFT Artist & Cryptobro Blocker</b></button>\
                <div class=\"content\" ' + nftcbstyle + '>\
                    <a href=\"' + website + 'nftcryptoblocker/\">Home</a>\
                    <a href=\"' + website + 'nftcryptoblocker/settings\">Settings</a>\
                    <a href=\"' + website + 'nftcryptoblocker/statistics\">Your Stats</a>\
                    <a href=\"' + website + 'nftcryptoblocker/centraldb\">Central DB</a>\
                    <a href=\"' + website + 'nftcryptoblocker/info\">Info</a>\
                    <a href=\"' + website + 'nftcryptoblocker/comparisons\">Comparison to browser apps</a>\
                    <a href=\"' + website + 'nftcryptoblocker/privacy\">Privacy</a>\
                </div>\
            </div>');
