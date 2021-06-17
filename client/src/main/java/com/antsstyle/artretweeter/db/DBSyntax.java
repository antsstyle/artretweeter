/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.db;

/**
 *
 * @author Ant
 */
public enum DBSyntax {

    /**
     *
     */
    IS_NOT_NULL(" IS NOT NULL "),

    /**
     *
     */
    IS_NULL(" IS NULL "),

    /**
     *
     */
    GREATER_THAN(" > "),

    /**
     *
     */
    GREATER_THAN_OR_EQUAL_TO(" >= "),

    /**
     *
     */
    LESS_THAN(" < "),

    /**
     *
     */
    LESS_THAN_OR_EQUAL_TO(" <= "),

    /**
     *
     */
    OR(" OR "),

    /**
     *
     */
    AND(" AND "),

    /**
     *
     */
    EQUALS(" = "),

    /**
     *
     */
    LIKE(" LIKE "),

    /**
     *
     */
    NOT_LIKE(" NOT LIKE "),

    /**
     *
     */
    ASC("ASC"),

    /**
     *
     */
    DESC("DESC");

    private String name;
    
    /**
     * 
     * @return The SQL string for this DBSyntax object.
     */
    public String getName() {
        return name;
    }

    private DBSyntax(String name) {
        this.name = name;
    }

}
