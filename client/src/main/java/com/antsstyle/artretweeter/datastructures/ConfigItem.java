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
public class ConfigItem {
    
    private String name;
    private String value;

    public String getName() {
        return name;
    }

    public ConfigItem setName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public ConfigItem setValue(String value) {
        this.value = value;
        return this;
    }
    
    public ConfigItem() {
        
    }
    
    public ConfigItem(String name, String value) {
        this.name = name;
        this.value = value;
    }
    
}
