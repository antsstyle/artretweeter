/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.db;

import com.antsstyle.artretweeter.datastructures.CachedVariable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class CachedVariableDB {

    private static final Logger LOGGER = LogManager.getLogger(CachedVariableDB.class);

    private static final String CACHEDVAR_MERGE_QUERY = "MERGE INTO cachedvariables USING (VALUES (?,?))"
            + " AS vals(name,value) ON (cachedvariables.name = vals.name)"
            + " WHEN MATCHED THEN UPDATE SET cachedvariables.value=vals.value"
            + " WHEN NOT MATCHED THEN INSERT (name,value) VALUES (vals.name, vals.value)";

    public static CachedVariable getCachedVariableByName(String configName) {
        DBResponse resp = CoreDB.selectFromTable(DBTable.CACHEDVARIABLES,
                new String[]{"name"},
                new Object[]{configName});
        if (!resp.wasSuccessful() || resp.getReturnedRows().isEmpty()) {
            return null;
        }
        return ResultSetConversion.getCachedVariable(resp.getReturnedRows().get(0));
    }

    public static boolean updateConfigItem(String configName, String configValue) {
        return CoreDB.runCustomUpdate(CACHEDVAR_MERGE_QUERY, new Object[]{configName, configValue});
    }

    public static boolean updateConfigItem(CachedVariable var) {
        return CoreDB.runCustomUpdate(CACHEDVAR_MERGE_QUERY, new Object[]{var.getName(), var.getValue()});
    }

}
