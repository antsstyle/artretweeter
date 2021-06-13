<?php

require "core.php";

$database_connection->query("CREATE TABLE IF NOT EXISTS ratelimitrecords (
		id INT AUTO_INCREMENT PRIMARY KEY,
		userid BIGINT NOT NULL,
		endpoint VARCHAR(255) NOT NULL,
		maxlimit INT NOT NULL,
		remaininglimit INT NOT NULL,
		resettime DATETIME NOT NULL, 
                timetoresetseconds INT NOT NULL)");

$database_connection->query("ALTER TABLE ratelimitrecords ADD CONSTRAINT 
        userauthunique UNIQUE KEY(userid,endpoint)");
