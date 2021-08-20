/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

/**
 *
 * @author antss
 */
public class TableFilterEntry {
    
    private String databaseFieldName;

    public String getDatabaseFieldName() {
        return databaseFieldName;
    }

    public void setDatabaseFieldName(String databaseFieldName) {
        this.databaseFieldName = databaseFieldName;
    }
    private Boolean filterEnabled;
    private String operator;
    private String fieldValue;

    public TableFilterEntry(String fieldName, Boolean filterEnabled, String operator, String fieldValue) {
        this.databaseFieldName = fieldName;
        this.filterEnabled = filterEnabled;
        this.operator = operator;
        this.fieldValue = fieldValue;
    }

    public Boolean getFilterEnabled() {
        return filterEnabled;
    }

    public void setFilterEnabled(Boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }
    
}
