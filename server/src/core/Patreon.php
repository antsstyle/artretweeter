<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Credentials\AdminUserAuth;
use Antsstyle\ArtRetweeter\Core\LogManager;
use Patreon\API;

class Patreon {
    
    public static $logger;
    
    public static function getPatrons() {
        $api_client = new API(AdminUserAuth::patreon_creator_access_token);
        $campaigns = $api_client->fetch_campaigns();
        $firstCampaign = $campaigns['data'][0]['id'];
        $campaignInfo = $api_client->fetch_page_of_members_from_campaign($firstCampaign, 100, null);

    }
    
    
}

Patreon::$logger = LogManager::getLogger("Patreon");