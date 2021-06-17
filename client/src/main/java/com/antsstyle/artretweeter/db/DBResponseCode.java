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
public enum DBResponseCode {

    /**
     * 
     */
    SUCCESS(1),
    
    /**
     * 
     */
    DB_ERROR(-1),
    
    /**
     * 
     */
    DUPLICATE_ERROR(-2),
    
    /**
     * 
     */
    INPUT_ERROR(-3),
    
    /**
     * 
     */
    FOREIGN_KEY_CONSTRAINT_FAILURE_ERROR(-4),
    
    /**
     * 
     */
    CONNECTION_FAILURE_ERROR(-5);
    
    private final Integer statusCode;
    
    public Integer getStatusCode() {
        return statusCode;
    }
    
    private DBResponseCode(Integer statusCode){ 
        this.statusCode = statusCode;
    }
    
}
