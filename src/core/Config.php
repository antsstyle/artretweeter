<?php

namespace Antsstyle\ArtRetweeter\Core;

class Config {

    const HOMEPAGE_URL = "https://antsstyle.com/artretweeter/";
    const ARTISTSETTINGSPAGE_URL = Config::HOMEPAGE_URL . "artistsettings";
    const NONARTISTSETTINGSPAGE_URL = Config::HOMEPAGE_URL . "nonartistsettings";
    const ARTISTQUEUESTATUS_URL = Config::HOMEPAGE_URL . "artistqueuestatus";
    const NONARTISTQUEUESTATUS_URL = Config::HOMEPAGE_URL . "nonartistqueuestatus";
    const ADMIN_URL = "https://twitter.com/antsstyle";
    const ADMIN_NAME = "@antsstyle";
    const CARD_IMAGE_URL = Config::HOMEPAGE_URL . "twittercard.jpg";
    const OAUTH_CALLBACK = Config::HOMEPAGE_URL . "results";
    const WEBSITE_STYLE_DIRECTORY = "/website/";
    const ADMIN_EMAIL_TEXT = "ant[at]antsstyle[dot]com";
    
}
