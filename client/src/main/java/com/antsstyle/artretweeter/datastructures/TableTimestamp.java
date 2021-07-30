/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import com.antsstyle.artretweeter.tools.FormatTools;
import java.sql.Timestamp;
import java.util.Date;

/**
 *
 * @author antss
 */
public class TableTimestamp implements Comparable<TableTimestamp> {

    private String timestampString;

    public String getTimestampString() {
        return timestampString;
    }
    private Timestamp timestamp;

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
        this.timestampString = FormatTools.DATETIME_FORMAT.format(new Date(timestamp.getTime()));
    }

    public TableTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
        this.timestampString = FormatTools.DATETIME_FORMAT.format(new Date(timestamp.getTime()));
    }

    @Override
    public int compareTo(TableTimestamp o) {
        return this.timestamp.compareTo(o.timestamp);
    }
    
    @Override
    public String toString() {
        return timestampString;
    }

}
