<?php

require_once "../core.php";

ArtRetweeter\postScheduledRetweets();

ArtRetweeter\removeExpiredRetweets();