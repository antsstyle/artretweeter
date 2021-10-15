/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.db;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.AutomationSettingsHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class AutomationDB {

    private static final Logger LOGGER = LogManager.getLogger(AutomationDB.class);

    private static final String AUTOMATION_SETTINGS_MERGE_QUERY = "MERGE INTO userautomationsettings USING (VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?))"
            + " AS vals(usertwitterid,automationenabled,dayflags,hourflags,minuteflags,includedtext,excludedtext,retweetpercent,oldtweetcutoffdate,"
            + "oldtweetcutoffdateenabled,includedtextenabled,excludedtextenabled,timezonehouroffset,timezoneminuteoffset,includetextcondition,"
            + "excludetextcondition) ON "
            + "(userautomationsettings.usertwitterid = vals.usertwitterid)"
            + " WHEN MATCHED THEN UPDATE SET userautomationsettings.automationenabled=vals.automationenabled, "
            + "userautomationsettings.dayflags=vals.dayflags,userautomationsettings.hourflags=vals.hourflags,"
            + "userautomationsettings.minuteflags=vals.minuteflags,"
            + "userautomationsettings.includedtext=vals.includedtext,userautomationsettings.excludedtext=vals.excludedtext,"
            + "userautomationsettings.retweetpercent=vals.retweetpercent,userautomationsettings.oldtweetcutoffdate=vals.oldtweetcutoffdate,"
            + "userautomationsettings.oldtweetcutoffdateenabled=vals.oldtweetcutoffdateenabled,"
            + "userautomationsettings.includedtextenabled=vals.includedtextenabled, userautomationsettings.excludedtextenabled=vals.excludedtextenabled,"
            + "userautomationsettings.timezonehouroffset=vals.timezonehouroffset, userautomationsettings.timezoneminuteoffset=vals.timezoneminuteoffset,"
            + "userautomationsettings.includetextcondition=vals.includetextcondition, userautomationsettings.excludetextcondition=vals.excludetextcondition"
            + " WHEN NOT MATCHED THEN INSERT (usertwitterid,automationenabled,dayflags,hourflags,minuteflags,includedtext,excludedtext,retweetpercent,"
            + "oldtweetcutoffdate,oldtweetcutoffdateenabled,includedtextenabled,excludedtextenabled, timezonehouroffset, timezoneminuteoffset, "
            + "includetextcondition, excludetextcondition) VALUES "
            + "(vals.usertwitterid, vals.automationenabled, vals.dayflags, vals.hourflags, vals.minuteflags, "
            + "vals.includedtext, vals.excludedtext, vals.retweetpercent, "
            + "vals.oldtweetcutoffdate, vals.oldtweetcutoffdateenabled, vals.includedtextenabled, vals.excludedtextenabled, vals.timezonehouroffset, "
            + "vals.timezoneminuteoffset, vals.includetextcondition, vals.excludetextcondition)";

    public static boolean updateAutomationSettings(Object[] params) {
        return CoreDB.runCustomUpdate(AUTOMATION_SETTINGS_MERGE_QUERY, params);
    }
    
    public static boolean updateAutomationSettings(AutomationSettingsHolder holder) {
        Object[] params = new Object[]{holder.getUserTwitterID(), holder.getAutomationEnabled(), holder.getDayFlags(),
        holder.getHourFlags(), holder.getMinuteFlags(), holder.getIncludedText(), holder.getExcludedText(), holder.getRetweetPercent(), 
        holder.getOldTweetCutoffDate(), holder.getOldTweetCutoffDateEnabled(), holder.getIncludedTextEnabled(), 
        holder.getExcludedTextEnabled(), holder.getTimeZoneHourOffset(), holder.getTimeZoneMinuteOffset(), holder.getIncludeTextCondition(),
        holder.getExcludeTextCondition()};
        return updateAutomationSettings(params);
    }

}
