/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.db;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.ConfigItem;
import com.antsstyle.artretweeter.tools.PathTools;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class ConfigDB {

    private static final Logger LOGGER = LogManager.getLogger(ConfigDB.class);

    private static final String CONFIG_MERGE_QUERY = "MERGE INTO configuration USING (VALUES (?,?))"
            + " AS vals(name,value) ON (configuration.name = vals.name)"
            + " WHEN MATCHED THEN UPDATE SET configuration.value=vals.value"
            + " WHEN NOT MATCHED THEN INSERT (name,value) VALUES (vals.name, vals.value)";

    public static ConfigItem getConfigItemByName(String configName) {
        DBResponse resp = CoreDB.selectFromTable(DBTable.CONFIGURATION,
                new String[]{"name"},
                new Object[]{configName});
        if (!resp.wasSuccessful() || resp.getReturnedRows().isEmpty()) {
            return null;
        }
        return ResultSetConversion.getConfigItem(resp.getReturnedRows().get(0));
    }

    public static boolean updateConfigItem(String configName, String configValue) {
        return CoreDB.runCustomUpdate(CONFIG_MERGE_QUERY, new Object[]{configName, configValue});
    }

    public static boolean updateConfigItem(ConfigItem item) {
        return CoreDB.runCustomUpdate(CONFIG_MERGE_QUERY, new Object[]{item.getName(), item.getValue()});
    }

    public static Path getTweetFolderPath(Account account) {
        Path tweetFolderPath;
        DBResponse resp = CoreDB.selectFromTable(DBTable.CONFIGURATION, new String[]{"name"}, new Object[]{"tweetsavedirectory"});
        if (!resp.wasSuccessful()) {
            return null;
        } else if (resp.getReturnedRows().isEmpty()) {
            tweetFolderPath = Paths.get(System.getProperty("user.dir")).resolve("tweetimages/").resolve(account.getScreenName().concat("/"));
            CoreDB.insertIntoTable(DBTable.CONFIGURATION, new String[]{"name", "value"}, new Object[]{"tweetsavedirectory", PathTools.convertPathToString(tweetFolderPath)});
        } else {
            tweetFolderPath = Paths.get((String) resp.getReturnedRows().get(0).get("VALUE"));
        }
        return tweetFolderPath;
    }

}