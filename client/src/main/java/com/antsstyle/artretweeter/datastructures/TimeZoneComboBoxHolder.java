/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import java.util.TimeZone;

/**
 *
 * @author antss
 */
public class TimeZoneComboBoxHolder implements Comparable<TimeZoneComboBoxHolder> {

    private TimeZone timeZone;

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public TimeZoneComboBoxHolder setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public String getFormattedString() {
        return formattedString;
    }

    public TimeZoneComboBoxHolder setFormattedString(String formattedString) {
        this.formattedString = formattedString;
        return this;
    }
    private String formattedString;
    private Integer hourOffset;
    private Integer minuteOffset;

    public Integer getHourOffset() {
        return hourOffset;
    }

    public TimeZoneComboBoxHolder setHourOffset(Integer hourOffset) {
        this.hourOffset = hourOffset;
        return this;
    }

    public Integer getMinuteOffset() {
        return minuteOffset;
    }

    public TimeZoneComboBoxHolder setMinuteOffset(Integer minuteOffset) {
        this.minuteOffset = minuteOffset;
        return this;
    }

    @Override
    public String toString() {
        return formattedString;
    }

    @Override
    public int compareTo(TimeZoneComboBoxHolder o) {
        Double offset;
        if (this.hourOffset >= 0) {
            offset = this.hourOffset + (Math.abs(this.minuteOffset) / 60.0);
        } else {
            offset = this.hourOffset - (Math.abs(this.minuteOffset) / 60.0);
        }
        Double comparisonOffset;
        if (o.hourOffset >= 0) {
            comparisonOffset = o.hourOffset + (Math.abs(o.minuteOffset) / 60.0);
        } else {
            comparisonOffset = o.hourOffset - (Math.abs(o.minuteOffset) / 60.0);
        }
        return offset.compareTo(comparisonOffset);
    }

}
