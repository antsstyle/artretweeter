/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.configuration;

import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.gui.GUI;
import java.awt.Color;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public final class Config {

    private static final Logger LOGGER = LogManager.getLogger(Config.class);

    public static final MiscConfig miscConfig = new MiscConfig();
    public static final GUIConfig guiConfig = new GUIConfig();
    public static final TwitterConfig twitterConfig = new TwitterConfig();

    private static final ArrayList<ConfigurationModule> configModules = new ArrayList<>();

    /**
     *
     * @param configName The name of the configuration item (as defined in the database, e.g. artposter.something.something) to find the value field for.
     * @return The Field object containing the value field associated with the given configuration item.
     */
    public static Field findValueField(String configName) {
        for (ConfigurationModule module : configModules) {
            Field[] fields = module.getClass().getDeclaredFields();
            for (Field f : fields) {
                if (!(f.getName().startsWith("P_") || f.getName().startsWith("T_"))) {
                    continue;
                }
                try {
                    String value = (String) f.get(null);
                    if (value.equals(configName)) {
                        String fieldName = f.getName();
                        fieldName = fieldName.substring(2, fieldName.length());
                        Field valueField = module.getClass().getDeclaredField(fieldName);
                        return valueField;
                    }
                } catch (Exception e) {

                }
            }
        }
        return null;
    }

    public static boolean validateAndSaveAll(HashMap<String, Object> changeMappings, ConfigurationModule module) {
        try {
            boolean successful = true;
            ArrayList<String> failedModuleCommits = new ArrayList<>();
            String failedItem = module.validateAndSaveInputs(changeMappings);
            if (failedItem != null) {
                successful = false;
                failedModuleCommits.add(module.getClass().getName());
            }
            if (!successful) {
                LOGGER.error("Failed to commit for module: \n");
                for (String s : failedModuleCommits) {
                    LOGGER.error(s);
                }
            }
            if (changeMappings.containsKey(TwitterConfig.P_DO_NOT_SHOW_METRICS_ANYWHERE)) {
                GUI.getMainManagementPanel().getMainTweetsPanel().setMetricsGUISettings();
                GUI.getAutomationPanel().setMetricsOptionsPanel(TwitterConfig.DO_NOT_SHOW_METRICS_ANYWHERE);
            }
            return successful;
        } catch (Exception e) {
            LOGGER.error("Failed to validate and save change mappings!", e);
        }
        return false;
    }

    /**
     * Initialises the Config class and loads the properties map.
     *
     */
    public static void initialise() {
        configModules.clear();
        configModules.add(miscConfig);
        configModules.add(guiConfig);
        configModules.add(twitterConfig);
        for (ConfigurationModule module : configModules) {
            module.initialisePropertiesMap();
        }
        refreshConfigurations();
    }

    public static void refreshConfigurations() {
        HashMap<String, String> configValues = getConfigurationFromDatabase();
        Set<String> configKeys = configValues.keySet();
        HashMap<String, String> missingKeys = new HashMap<>();
        for (ConfigurationModule module : configModules) {
            HashMap<String, String> moduleKeys = initialiseConfigModule(module, configValues, configKeys);
            for (String k : moduleKeys.keySet()) {
                if (missingKeys.get(k) == null) {
                    missingKeys.put(k, moduleKeys.get(k));
                } else if (missingKeys.get(k).equals("N") && moduleKeys.get(k).equals("Y")) {
                    missingKeys.put(k, "Y");
                }
            }
        }
        for (String k : missingKeys.keySet()) {
            if (missingKeys.get(k).equals("N")) {
                LOGGER.debug("Config key in database has no variable: " + k);
            }
        }
    }

    /**
     *
     * @return A HashMap with all of the name/value pairs in the configuration database table.
     */
    private static HashMap<String, String> getConfigurationFromDatabase() {
        DBTable configTable = DBTable.CONFIGURATION;
        ArrayList<HashMap<String, Object>> rows = CoreDB.selectFromTable(configTable).getReturnedRows();
        HashMap<String, String> configMap = new HashMap<>();
        for (HashMap<String, Object> row : rows) {
            String name = (String) row.get("NAME");
            String value = (String) row.get("VALUE");
            configMap.put(name, value);
        }
        return configMap;
    }

    private static HashMap<String, String> initialiseConfigModule(ConfigurationModule module, HashMap<String, String> configValues,
            Set<String> configKeys) {
        HashMap<String, String> missingKeys = new HashMap<>();
        for (String key : configKeys) {
            String configValue = configValues.get(key);
            if (configValue == null || configValue.trim().equals("")) {
                continue;
            }
            Field field = module.getConfigFields().get(key);
            if (field == null) {
                missingKeys.put(key, "N");
                continue;
            } else {
                missingKeys.put(key, "Y");
            }
            Object valueToSet = null;
            Class clazz = field.getType();
            if (clazz == Boolean.class) {
                valueToSet = Boolean.valueOf(configValue);
            } else if (clazz == Integer.class) {
                valueToSet = Integer.valueOf(configValue);
            } else if (clazz == Double.class) {
                valueToSet = Double.valueOf(configValue);
            } else if (clazz == Long.class) {
                valueToSet = Long.valueOf(configValue);
            } else if (clazz == String.class) {
                valueToSet = configValue;
            } else if (clazz == Path.class) {
                valueToSet = Paths.get(configValue);
            } else if (clazz == Color.class) {
                String[] s = StringUtils.split(configValue, ",");
                valueToSet = new Color(Integer.valueOf(s[0]), Integer.valueOf(s[1]), Integer.valueOf(s[2]));
            }
            try {
                if (!valueToSet.equals("")) {
                    field.set(module, valueToSet);
                }
            } catch (Exception e) {
                if (e instanceof NullPointerException) {
                    //LOGGER.debug("Missing configuration value in properties file: " + key + "     " + configValue, e);
                } else {
                    LOGGER.error("Unknown error when setting default config values!", e);
                }
            }
        }
        return missingKeys;
    }

}
