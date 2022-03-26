<?php

namespace Antsstyle\ArtRetweeter\Cronjobs;

set_time_limit(0);

chdir(dirname(__DIR__, 2));

$dir = getcwd();

require $dir . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\TweetManager;

TweetManager::getAllNewTweetsForAllUsers();