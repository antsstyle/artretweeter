/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.db;

import java.sql.Timestamp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class RateLimitsDB {
    
    private static final Logger LOGGER = LogManager.getLogger(RateLimitsDB.class);
    private static final String APP_RATELIMITS_MERGE_QUERY = "MERGE INTO appratelimits USING (VALUES(?,?,?,?))" + " AS vals(endpoint,maxlimit,remaininglimit,resettime) ON " + "(appratelimits.endpoint=vals.endpoint) " + "WHEN MATCHED THEN UPDATE SET appratelimits.endpoint=vals.endpoint," + "appratelimits.maxlimit=vals.maxlimit,appratelimits.remaininglimit=vals.remaininglimit," + "appratelimits.resettime=vals.resettime " + "WHEN NOT MATCHED THEN INSERT (endpoint,maxlimit,remaininglimit,resettime) VALUES " + "(vals.endpoint,vals.maxlimit,vals.remaininglimit,vals.resettime) ";
    private static final String USER_RATELIMITS_MERGE_QUERY = "MERGE INTO userratelimits USING (VALUES(?,?,?,?,?))" + " AS vals(usertwitterid,endpoint,maxlimit,remaininglimit,resettime) ON " + "(userratelimits.usertwitterid = vals.usertwitterid AND userratelimits.endpoint=vals.endpoint) " + "WHEN MATCHED THEN UPDATE SET userratelimits.usertwitterid=vals.usertwitterid,userratelimits.endpoint=vals.endpoint," + "userratelimits.maxlimit=vals.maxlimit,userratelimits.remaininglimit=vals.remaininglimit," + "userratelimits.resettime=vals.resettime " + "WHEN NOT MATCHED THEN INSERT (usertwitterid,endpoint,maxlimit,remaininglimit,resettime) VALUES " + "(vals.usertwitterid,vals.endpoint,vals.maxlimit,vals.remaininglimit,vals.resettime) ";

    public static boolean mergeAppRateLimitInfo(String endpoint, Integer maxLimit, Integer remainingLimit, Timestamp resetTimestamp) {
        Object[] params = new Object[]{endpoint, maxLimit, remainingLimit, resetTimestamp};
        return CoreDB.runCustomUpdate(APP_RATELIMITS_MERGE_QUERY, params);
    }

    public static boolean mergeUserRateLimitInfo(Long userTwitterID, String endpoint, Integer maxLimit, Integer remainingLimit, Timestamp resetTimestamp) {
        Object[] params = new Object[]{userTwitterID, endpoint, maxLimit, remainingLimit, resetTimestamp};
        return CoreDB.runCustomUpdate(USER_RATELIMITS_MERGE_QUERY, params);
    }
    
}
