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
public class CachedVariable {

    private String name;
    private String value;

    public String getName() {
        return name;
    }

    public CachedVariable setName(String name) {
        this.name = name;
        return this;
    }

    public String getValue() {
        return value;
    }

    public CachedVariable setValue(String value) {
        this.value = value;
        return this;
    }

}
