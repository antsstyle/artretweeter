/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.configuration;

import com.antsstyle.artretweeter.db.ConfigDB;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.tools.PathTools;
import java.awt.Color;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Antsstyle
 */
public abstract class ConfigurationModule {

    private static final Logger LOGGER = LogManager.getLogger(ConfigurationModule.class);

    public static String convertConfigItemToString(Object object) {
        if (object instanceof Color) {
            Color c = (Color) object;
            String s = String.valueOf(c.getRed()).concat(",").concat(String.valueOf(c.getGreen())).concat(",").concat(String.valueOf(c.getBlue()));
            return s;
        }
        if (object instanceof Path) {
            String s = PathTools.convertPathToString((Path) object);
            return s;
        }
        return String.valueOf(object);
    }

    /**
     * Map of property name strings in the artposter.properties file to fields in this class.
     */
    protected final HashMap<String, Field> configFields = new HashMap<>();
    // Map of property name strings to the default values listed here

    /**
     * Map storing the different properties by name, along with their default values.
     */
    protected final HashMap<String, Object> defaultValues = new HashMap<>();
    
    public HashMap<String, Field> getConfigFields() {
        return configFields;
    }
    
    public HashMap<String, Object> getDefaultValues() {
        return defaultValues;
    }

    /**
     * Validates, and commits to the database, user changes to the configuration from the Configuration GUI panel.
     *
     * @param changeMappings The changed configuration values to commit changes for.
     * @return Null on successful commit; the name of the failing configuration item otherwise.
     */
    public String validateAndSaveInputs(HashMap<String, Object> changeMappings) {
        ConfigValidator.validateInputMappings(changeMappings);
        Set<String> configItemsToChange = changeMappings.keySet();
        LOGGER.debug("Number of config changes to save: " + configItemsToChange.size());
        for (String item : configItemsToChange) {
            Field f = findNameFieldByConfigName(item);
            if (f == null) {
                continue;
            }
            Object value = changeMappings.get(item);
            ConfigDB.updateConfigItem(item, convertConfigItemToString(value));
            updateConfigFieldByName(item, value);
        }
        return null;
    }

    /**
     *
     * @param configName The name of the configuration item (as defined in the database, e.g. artposter.something.something) to find the name field for.
     * @return The Field object containing the name field associated with the given configuration item (must begin with P_ or T_).
     */
    public Field findNameFieldByConfigName(String configName) {
        Field[] fields = getClass().getDeclaredFields();
        for (Field f : fields) {
            if (!(f.getName().startsWith("P_") || f.getName().startsWith("T_"))) {
                continue;
            }
            try {
                String value = (String) f.get(this);
                if (value.equals(configName)) {
                    return f;
                }
            } catch (Exception e) {

            }
        }
        return null;
    }

    /**
     * Initialises the defaultValues and configFields maps, using reflection to automatically populate value fields and associate name and value fields with each other.
     */
    public void initialisePropertiesMap() {
        defaultValues.clear();
        Field[] fields = getClass().getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            if (!fieldName.startsWith("S_") && !fieldName.startsWith("P_") && !fieldName.startsWith("T_")) {
                try {
                    Field nameField = findNameField(fieldName);
                    if (nameField != null) {
                        String nameFieldValue = (String) nameField.get(this);
                        Object fieldValue = field.get(this);
                        defaultValues.put(nameFieldValue, fieldValue);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error finding name-value field pairings!", e);
                }
            } else {
                try {
                    Field valueField = getClass().getDeclaredField(fieldName.substring(2, fieldName.length()));
                    configFields.put((String) field.get(this), valueField);
                } catch (Exception e) {
                    LOGGER.error("Error assigning config field values!", e);
                }
            }
        }
    }

    /**
     *
     * @param configName The name of the configuration item (as defined in the database, e.g. artposter.something.something) to update the value field for.
     * @param newValue The new value to set.
     */
    public void updateConfigFieldByName(String configName, Object newValue) {
        Field[] fields = getClass().getDeclaredFields();
        for (Field f : fields) {
            if (!(f.getName().startsWith("P_") || f.getName().startsWith("T_"))) {
                continue;
            }
            try {
                String value = (String) f.get(this);
                if (value.equals(configName)) {
                    String fieldName = f.getName();
                    fieldName = fieldName.substring(2, fieldName.length());
                    Field valueField = getClass().getDeclaredField(fieldName);
                    valueField.set(this, newValue);
                }
            } catch (Exception e) {

            }
        }

    }

    /**
     *
     * @param configName The name of the configuration item (as defined in the database, e.g. artposter.something.something) to find the value field for.
     * @return The Field object containing the value field associated with the given configuration item.
     */
    public Field findValueFieldByConfigName(String configName) {
        Field[] fields = getClass().getDeclaredFields();
        for (Field f : fields) {
            if (!(f.getName().startsWith("P_") || f.getName().startsWith("T_") || f.getName().startsWith("S_"))) {
                continue;
            }
            try {
                String value = (String) f.get(this);
                if (value.equals(configName)) {
                    String fieldName = f.getName();
                    fieldName = fieldName.substring(2, fieldName.length());
                    Field valueField = getClass().getDeclaredField(fieldName);
                    return valueField;
                }
            } catch (Exception e) {

            }
        }
        return null;
    }

    /**
     * Given a value field's name, finds the corresponding name field.
     *
     * @param fieldName The name of the name field to find the value field for.
     * @return The value field, if found; otherwise, null.
     */
    public Field findNameField(String fieldName) {
        Field field = null;
        try {
            field = getClass().getDeclaredField("S_".concat(fieldName));
            return field;
        } catch (NoSuchFieldException e) {

        }
        try {
            field = getClass().getDeclaredField("P_".concat(fieldName));
            return field;
        } catch (NoSuchFieldException e) {

        }
        try {
            field = getClass().getDeclaredField("T_".concat(fieldName));
            return field;
        } catch (NoSuchFieldException e) {

        }
        return field;
    }

}
