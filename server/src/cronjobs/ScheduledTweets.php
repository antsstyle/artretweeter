<?php

namespace Antsstyle\ArtRetweeter\Cronjobs;

chdir(dirname(__DIR__, 2));

$dir = getcwd();

require $dir . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Core;

Core::postScheduledRetweets();

Core::removeExpiredRetweets();