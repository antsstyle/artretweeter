/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import java.sql.Timestamp;

/**
 *
 * @author antss
 */
public class AutomationSettingsHolder {

    private Integer id;
    private Long usertwitterid;
    private String dayflags;
    private String hourflags;
    private String minuteflags;

    private String includedtext;
    private String excludedtext;
    private String includedtextenabled;
    private String excludedtextenabled;
    private String automationenabled;
    private Integer retweetpercent;
    private Timestamp oldtweetcutoffdate;
    private String oldtweetcutoffdateenabled;
    private Integer timezonehouroffset;
    private Integer timezoneminuteoffset;
    private String includetextcondition;
    private String excludetextcondition;
    private String metricsmeasurementtype;

    public String getMetricsMeasurementType() {
        return metricsmeasurementtype;
    }

    public AutomationSettingsHolder setMetricsMeasurementType(String metricsMeasurementType) {
        this.metricsmeasurementtype = metricsMeasurementType;
        return this;
    }

    public String getIncludeTextCondition() {
        return includetextcondition;
    }

    public AutomationSettingsHolder setIncludeTextCondition(String includeTextCondition) {
        this.includetextcondition = includeTextCondition;
        return this;
    }

    public String getExcludeTextCondition() {
        return excludetextcondition;
    }

    public AutomationSettingsHolder setExcludeTextCondition(String excludeTextCondition) {
        this.excludetextcondition = excludeTextCondition;
        return this;
    }

    public Integer getTimeZoneHourOffset() {
        return timezonehouroffset;
    }

    public AutomationSettingsHolder setTimeZoneHourOffset(Integer timeZoneHourOffset) {
        this.timezonehouroffset = timeZoneHourOffset;
        return this;
    }

    public Integer getTimeZoneMinuteOffset() {
        return timezoneminuteoffset;
    }

    public AutomationSettingsHolder setTimeZoneMinuteOffset(Integer timeZoneMinuteOffset) {
        this.timezoneminuteoffset = timeZoneMinuteOffset;
        return this;
    }

    public String getMinuteFlags() {
        return minuteflags;
    }

    public AutomationSettingsHolder setMinuteFlags(String minuteflags) {
        this.minuteflags = minuteflags;
        return this;
    }

    public String getOldTweetCutoffDateEnabled() {
        return oldtweetcutoffdateenabled;
    }

    public AutomationSettingsHolder setOldTweetCutoffDateEnabled(String oldTweetCutoffDateEnabled) {
        this.oldtweetcutoffdateenabled = oldTweetCutoffDateEnabled;
        return this;
    }

    public Timestamp getOldTweetCutoffDate() {
        return oldtweetcutoffdate;
    }

    public AutomationSettingsHolder setOldTweetCutoffDate(Timestamp oldTweetCutoffDate) {
        this.oldtweetcutoffdate = oldTweetCutoffDate;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public AutomationSettingsHolder setId(Integer id) {
        this.id = id;
        return this;
    }

    public Long getUserTwitterID() {
        return usertwitterid;
    }

    public AutomationSettingsHolder setUserTwitterID(Long userTwitterID) {
        this.usertwitterid = userTwitterID;
        return this;
    }

    public String getDayFlags() {
        return dayflags;
    }

    public AutomationSettingsHolder setDayFlags(String dayFlags) {
        this.dayflags = dayFlags;
        return this;
    }

    public String getHourFlags() {
        return hourflags;
    }

    public AutomationSettingsHolder setHourFlags(String hourFlags) {
        this.hourflags = hourFlags;
        return this;
    }

    public String getIncludedText() {
        return includedtext;
    }

    public AutomationSettingsHolder setIncludedText(String includedText) {
        this.includedtext = includedText;
        return this;
    }

    public String getExcludedText() {
        return excludedtext;
    }

    public AutomationSettingsHolder setExcludedText(String excludedText) {
        this.excludedtext = excludedText;
        return this;
    }

    public String getIncludedTextEnabled() {
        return includedtextenabled;
    }

    public AutomationSettingsHolder setIncludedTextEnabled(String includedTextEnabled) {
        this.includedtextenabled = includedTextEnabled;
        return this;
    }

    public String getExcludedTextEnabled() {
        return excludedtextenabled;
    }

    public AutomationSettingsHolder setExcludedTextEnabled(String excludedTextEnabled) {
        this.excludedtextenabled = excludedTextEnabled;
        return this;
    }

    public String getAutomationEnabled() {
        return automationenabled;
    }

    public AutomationSettingsHolder setAutomationEnabled(String automationEnabled) {
        this.automationenabled = automationEnabled;
        return this;
    }

    public Integer getRetweetPercent() {
        return retweetpercent;
    }

    public AutomationSettingsHolder setRetweetPercent(Integer retweetPercent) {
        this.retweetpercent = retweetPercent;
        return this;
    }

}
