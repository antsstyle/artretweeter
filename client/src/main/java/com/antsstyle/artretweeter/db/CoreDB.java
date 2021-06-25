/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.db;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.TwitterCollectionHolder;
import com.antsstyle.artretweeter.gui.GUI;
import com.antsstyle.artretweeter.tools.PathTools;
import com.antsstyle.artretweeter.twitter.RESTAPI;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import javax.swing.JOptionPane;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class CoreDB {

    private static final Logger LOGGER = LogManager.getLogger(CoreDB.class);

    private static BasicDataSource connectionPool = new BasicDataSource();

    /**
     * General error code.
     */
    public static final int DB_ERROR = -1;
    /**
     * Error code, when an insert or update fails due to trying to add a duplicate value to a UNIQUE column.
     */
    public static final int DUPLICATE_ERROR = -2;
    /**
     * Error code, for when ArtPoster wasn't given the right parameters and didn't execute the DB query as a result.
     */
    public static final int INPUT_ERROR = -3;

    private static final String CREATE_CONFIG_TABLE = "CREATE TABLE configuration ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "name VARCHAR(255) NOT NULL, "
            + "value VARCHAR(255) NOT NULL)";

    private static final String CREATE_ACCOUNTS_TABLE = "CREATE TABLE accounts ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "twitterid BIGINT NOT NULL, "
            + "token VARCHAR(256) NOT NULL, "
            + "tokensecret VARCHAR(256) NOT NULL, "
            + "screen_name VARCHAR(256) NOT NULL, "
            + "historicalmaxid BIGINT, "
            + "latestmaxid BIGINT, "
            + "retrievedoldtweetslimit VARCHAR(1) DEFAULT 'N' NOT NULL)";

    private static final String CREATE_TWEETS_TABLE = "CREATE TABLE tweets ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "tweetid BIGINT NOT NULL, "
            + "usertwitterid BIGINT NOT NULL, "
            + "screen_name VARCHAR(255) NOT NULL, "
            + "fulltweettext VARCHAR(500) NOT NULL, "
            + "filepath1 VARCHAR(255) NOT NULL,"
            + "filepath2 VARCHAR(255), "
            + "filepath3 VARCHAR(255), "
            + "filepath4 VARCHAR(255), "
            + "url1 VARCHAR(255) NOT NULL, "
            + "url2 VARCHAR(255), "
            + "url3 VARCHAR(255), "
            + "url4 VARCHAR(255), "
            + "created_at TIMESTAMP NOT NULL, "
            + "likecount INTEGER NOT NULL, "
            + "retweetcount INTEGER NOT NULL, "
            + "source VARCHAR(500) NOT NULL)";

    private static final String CREATE_COLLECTIONS_TABLE = "CREATE TABLE collections ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "usertwitterid BIGINT NOT NULL, "
            + "collectionid VARCHAR(255) NOT NULL,"
            + "collectionurl VARCHAR(255) NOT NULL, "
            + "name VARCHAR(256) NOT NULL, "
            + "description VARCHAR(256), "
            + "ordering VARCHAR(50) NOT NULL)";

    private static final String CREATE_COLLECTION_TWEETS_TABLE = "CREATE TABLE collectiontweets ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "tweetid BIGINT NOT NULL, "
            + "collectionid VARCHAR(255) NOT NULL, "
            + "ordernumber INTEGER)";

    private static final String CREATE_USER_RATELIMITS_TABLE = "CREATE TABLE userratelimits ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "usertwitterid BIGINT NOT NULL, "
            + "endpoint VARCHAR(255) NOT NULL, "
            + "maxlimit INTEGER NOT NULL, "
            + "remaininglimit INTEGER NOT NULL, "
            + "resettime TIMESTAMP NOT NULL)";

    private static final String CREATE_APP_RATELIMITS_TABLE = "CREATE TABLE appratelimits ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "endpoint VARCHAR(255) NOT NULL, "
            + "maxlimit INTEGER NOT NULL, "
            + "remaininglimit INTEGER NOT NULL, "
            + "resettime TIMESTAMP NOT NULL)";

    private static final String CREATE_RETWEET_QUEUE_TABLE = "CREATE TABLE retweetqueue ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "tweetid BIGINT NOT NULL, "
            + "retweetingusertwitterid BIGINT NOT NULL, "
            + "retweettime TIMESTAMP NOT NULL)";

    private static final String CREATE_FAILED_RETWEETS_TABLE = "CREATE TABLE failedretweets ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "tweetid BIGINT NOT NULL, "
            + "retweetingusertwitterid BIGINT NOT NULL, "
            + "retweettime TIMESTAMP NOT NULL, "
            + "errorcode INTEGER NOT NULL, "
            + "failreason VARCHAR(255) NOT NULL)";

    private static final String CREATE_RETWEETS_TABLE = "CREATE TABLE retweets ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "retweetingusertwitterid BIGINT NOT NULL, "
            + "originaltweetid BIGINT NOT NULL, "
            + "retweetid BIGINT NOT NULL, "
            + "originaltweettime TIMESTAMP NOT NULL, "
            + "retweettime TIMESTAMP NOT NULL)";

    private static final String TWEETS_TABLE_ADD_UNIQUE
            = "ALTER TABLE tweets ADD UNIQUE (tweetid)";

    private static final String ACCOUNTS_TABLE_ADD_UNIQUE
            = "ALTER TABLE accounts ADD UNIQUE (twitterid)";

    private static final String CONFIG_TABLE_ADD_UNIQUE
            = "ALTER TABLE configuration ADD UNIQUE (name)";

    private static final String COLLECTIONS_TABLE_ADD_UNIQUE
            = "ALTER TABLE collections ADD UNIQUE (collectionid)";

    private static final String COLLECTION_TWEETS_TABLE_ADD_UNIQUE
            = "ALTER TABLE collectiontweets ADD UNIQUE (tweetid,collectionid)";

    private static final String USER_RATELIMITS_TABLE_ADD_UNIQUE
            = " ALTER TABLE userratelimits ADD UNIQUE (usertwitterid,endpoint)";

    private static final String APP_RATELIMITS_TABLE_ADD_UNIQUE
            = " ALTER TABLE appratelimits ADD UNIQUE (endpoint)";

    private static final String RETWEET_QUEUE_TABLE_ADD_UNIQUE
            = "ALTER TABLE retweetqueue ADD UNIQUE (tweetid,internalaccountid)";

    private static final String USER_RATELIMITS_MERGE_QUERY = "MERGE INTO userratelimits USING (VALUES(?,?,?,?,?))"
            + " AS vals(usertwitterid,endpoint,maxlimit,remaininglimit,resettime) ON "
            + "(userratelimits.usertwitterid = vals.usertwitterid AND userratelimits.endpoint=vals.endpoint) "
            + "WHEN MATCHED THEN UPDATE SET userratelimits.usertwitterid=vals.usertwitterid,userratelimits.endpoint=vals.endpoint,"
            + "userratelimits.maxlimit=vals.maxlimit,userratelimits.remaininglimit=vals.remaininglimit,"
            + "userratelimits.resettime=vals.resettime "
            + "WHEN NOT MATCHED THEN INSERT (usertwitterid,endpoint,maxlimit,remaininglimit,resettime) VALUES "
            + "(vals.usertwitterid,vals.endpoint,vals.maxlimit,vals.remaininglimit,vals.resettime) ";

    private static final String APP_RATELIMITS_MERGE_QUERY = "MERGE INTO appratelimits USING (VALUES(?,?,?,?))"
            + " AS vals(endpoint,maxlimit,remaininglimit,resettime) ON "
            + "(appratelimits.endpoint=vals.endpoint) "
            + "WHEN MATCHED THEN UPDATE SET appratelimits.endpoint=vals.endpoint,"
            + "appratelimits.maxlimit=vals.maxlimit,appratelimits.remaininglimit=vals.remaininglimit,"
            + "appratelimits.resettime=vals.resettime "
            + "WHEN NOT MATCHED THEN INSERT (endpoint,maxlimit,remaininglimit,resettime) VALUES "
            + "(vals.endpoint,vals.maxlimit,vals.remaininglimit,vals.resettime) ";

    private static final String COLLECTION_MERGE_QUERY = "MERGE INTO collections USING (VALUES(?,?,?,?,?,?))"
            + " AS vals(usertwitterid,collectionid,collectionurl,name,description,ordering) ON "
            + "(collections.collectionid = vals.collectionid) "
            + "WHEN MATCHED THEN UPDATE SET collections.usertwitterid=vals.usertwitterid,collections.collectionid=vals.collectionid,"
            + "collections.collectionurl=vals.collectionurl,collections.name=vals.name,"
            + "collections.description=vals.description,collections.ordering=vals.ordering "
            + "WHEN NOT MATCHED THEN INSERT (usertwitterid,collectionid,collectionurl,name,description,ordering) VALUES "
            + "(vals.usertwitterid,vals.collectionid,vals.collectionurl,vals.name,vals.description,vals.ordering) ";

    private static final String TWEET_MERGE_QUERY = "MERGE INTO tweets USING (VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?))"
            + "    AS vals(tweetid,usertwitterid,screen_name,filepath1,filepath2,filepath3,filepath4,url1,"
            + "url2,url3,url4,created_at,likecount,retweetcount,fulltweettext,source) ON (tweets.tweetid = vals.tweetid) "
            + "WHEN MATCHED THEN UPDATE SET tweets.tweetid=vals.tweetid,tweets.usertwitterid=vals.usertwitterid,"
            + "tweets.screen_name=vals.screen_name,tweets.filepath1=vals.filepath1,"
            + "tweets.filepath2=vals.filepath2,tweets.filepath3=vals.filepath3,tweets.filepath4=vals.filepath4,"
            + "tweets.url1=vals.url1,tweets.url2=vals.url2,tweets.url3=vals.url3,tweets.url4=vals.url4,"
            + "tweets.created_at=vals.created_at,tweets.likecount=vals.likecount,tweets.retweetcount=vals.retweetcount,"
            + "tweets.fulltweettext=vals.fulltweettext,tweets.source=vals.source "
            + "WHEN NOT MATCHED THEN INSERT (tweetid,usertwitterid,screen_name,filepath1,filepath2,filepath3,filepath4,url1,"
            + "url2,url3,url4,created_at,likecount,retweetcount,fulltweettext,source) VALUES "
            + "(vals.tweetid,vals.usertwitterid,vals.screen_name,vals.filepath1,"
            + "vals.filepath2,vals.filepath3,vals.filepath4,vals.url1,vals.url2,vals.url3,vals.url4,vals.created_at,"
            + "vals.likecount,vals.retweetcount,vals.fulltweettext,vals.source) ";

    private static final String COLLECTION_TWEET_MERGE_QUERY = "MERGE INTO collectiontweets USING (VALUES (?,?))"
            + " AS vals(tweetid,collectionid) ON (collectiontweets.tweetid = vals.tweetid AND collectiontweets.collectionid = vals.collectionid)"
            + " WHEN MATCHED THEN UPDATE SET collectiontweets.tweetid=vals.tweetid,collectiontweets.collectionid=vals.collectionid"
            + " WHEN NOT MATCHED THEN INSERT (tweetid,collectionid) VALUES (vals.tweetid, vals.collectionid)";

    private static final String RETWEET_QUEUE_MERGE_QUERY = "MERGE INTO retweetqueue USING (VALUES (?,?,?))"
            + " AS vals(tweetid,internalaccountid,retweettime) ON (retweetqueue.tweetid = vals.tweetid"
            + " AND retweetqueue.internalaccountid = vals.internalaccountid)"
            + " WHEN MATCHED THEN UPDATE SET retweetqueue.tweetid=vals.tweetid,retweetqueue.internalaccountid=vals.internalaccountid,"
            + " retweetqueue.retweettime=vals.retweettime"
            + " WHEN NOT MATCHED THEN INSERT (tweetid,internalaccountid,retweettime) VALUES (vals.tweetid, vals.internalaccountid, vals.retweettime)";

    private static final String RETWEETS_INSERT_QUERY = "INSERT INTO retweets (retweetingusertwitterid,originaltweetid,retweetid,originaltweettime,retweettime)"
            + " VALUES(?,?,?,?,?)";

    /**
     *
     * @return @throws SQLException
     */
    protected static synchronized Connection getPoolConnection() throws SQLException {
        return connectionPool.getConnection();
    }

    public static Path getTweetFolderPath(Account account) {
        Path tweetFolderPath;
        DBResponse resp = selectFromTable(DBTable.CONFIGURATION,
                new String[]{"name"},
                new Object[]{"tweetsavedirectory"});
        if (!resp.wasSuccessful()) {
            return null;
        } else if (resp.getReturnedRows().isEmpty()) {
            tweetFolderPath = Paths.get(System.getProperty("user.dir")).resolve("tweetimages/")
                    .resolve(account.getScreenName().concat("/"));
            CoreDB.insertIntoTable(DBTable.CONFIGURATION,
                    new String[]{"name", "value"},
                    new Object[]{"tweetsavedirectory", PathTools.convertPathToString(tweetFolderPath)});
        } else {
            tweetFolderPath = Paths.get((String) resp.getReturnedRows().get(0).get("VALUE"));
        }
        return tweetFolderPath;
    }

    /**
     * Query the database using a custom select query.
     *
     * @param query The query to run.
     * @param parameters A collection of parameter objects, if any are needed.
     * @return A DBResponse object containing the returned rows, and a status code.
     */
    public static DBResponse customQuerySelect(String query, Object... parameters) {
        DBResponse response = new DBResponse();
        Statement stmt = null;
        PreparedStatement preparedStmt = null;
        ResultSet rs = null;
        Connection connection = null;
        try {
            connection = getPoolConnection();
            if (parameters == null || parameters.length == 0) {
                preparedStmt = connection.prepareStatement(query);
                rs = preparedStmt.executeQuery();
            } else {
                preparedStmt = connection.prepareStatement(query);
                int paramCounter = 1;
                for (Object param : parameters) {
                    if (param.getClass().isArray()) {
                        for (int j = 0; j < Array.getLength(param); j++) {
                            preparedStmt.setObject(paramCounter, Array.get(param, j));
                            paramCounter++;
                        }
                    } else if (param instanceof Collection) {
                        Collection coll = (Collection) param;
                        for (Object o : coll) {
                            preparedStmt.setObject(paramCounter, o);
                            paramCounter++;
                        }
                    } else {
                        preparedStmt.setObject(paramCounter, param);
                        paramCounter++;
                    }
                }
                rs = preparedStmt.executeQuery();
            }
            ArrayList<HashMap<String, Object>> results = new ArrayList<>();
            ResultSetMetaData metadata = rs.getMetaData();
            int count = metadata.getColumnCount();
            while (rs.next()) {
                HashMap<String, Object> map = new HashMap<>();
                for (int i = 1; i <= count; i++) {
                    map.put(metadata.getColumnLabel(i), rs.getObject(i));
                }
                results.add(map);
            }
            response.setReturnedRows(results);
            response.setStatusCode(DBResponseCode.SUCCESS);
            return response;
        } catch (Exception e) {
            LOGGER.error("Failed to select data from table!", e);
            LOGGER.error("Query was: " + query);
            response.setStatusCode(DBResponseCode.DB_ERROR);
            response.setMessage(e.getMessage());
            return response;
        } finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(preparedStmt);
            DbUtils.closeQuietly(connection);
        }
    }

    public static boolean insertCollection(Object[] params) {
        return runCustomUpdate(COLLECTION_MERGE_QUERY, params);
    }

    public static boolean insertTweet(Object[] params) {
        return runCustomUpdate(TWEET_MERGE_QUERY, params);
    }

    public static boolean insertRetweetQueueEntry(Object[] params) {
        return runCustomUpdate(RETWEET_QUEUE_MERGE_QUERY, params);
    }

    public static boolean insertRetweetEntry(Object[] params) {
        return runCustomUpdate(RETWEETS_INSERT_QUERY, params);
    }

    public static boolean runCustomUpdate(String query, Object[] params) {
        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            connection = getPoolConnection();
            stmt = connection.prepareStatement(query);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            LOGGER.error("Error running custom update query!", e);
            return false;
        } finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(connection);
        }
    }

    public static boolean mergeUserRateLimitInfo(Long userTwitterID, String endpoint, Integer maxLimit, Integer remainingLimit,
            Timestamp resetTimestamp) {
        Object[] params = new Object[]{userTwitterID, endpoint, maxLimit, remainingLimit, resetTimestamp};
        return runCustomUpdate(USER_RATELIMITS_MERGE_QUERY, params);
    }

    public static boolean mergeAppRateLimitInfo(String endpoint, Integer maxLimit, Integer remainingLimit,
            Timestamp resetTimestamp) {
        Object[] params = new Object[]{endpoint, maxLimit, remainingLimit, resetTimestamp};
        return runCustomUpdate(APP_RATELIMITS_MERGE_QUERY, params);
    }

    public static boolean parameterisedCollectionMergeBatch(ArrayList<Object[]> params) {
        return runParameterisedUpdateBatch(COLLECTION_MERGE_QUERY, params);
    }

    public static boolean parameterisedTweetMergeBatch(ArrayList<Object[]> params) {
        return runParameterisedUpdateBatch(TWEET_MERGE_QUERY, params);
    }

    public static boolean parameterisedCollectionTweetsMergeBatch(ArrayList<Object[]> params) {
        return runParameterisedUpdateBatch(COLLECTION_TWEET_MERGE_QUERY, params);
    }

    public static boolean runParameterisedUpdateBatch(String query, ArrayList<Object[]> params) {
        Connection connection = null;
        PreparedStatement stmt = null;
        try {
            connection = getPoolConnection();
            stmt = connection.prepareStatement(query);
            for (Object[] paramList : params) {
                for (int i = 0; i < paramList.length; i++) {
                    stmt.setObject(i + 1, paramList[i]);
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
            return true;
        } catch (Exception e) {
            LOGGER.error("Error running batch update query!", e);
            if (stmt != null) {
                try {
                    LOGGER.error("Update batch size: " + params.size() + "     Update count: " + stmt.getUpdateCount());
                } catch (Exception e1) {
                    LOGGER.error("Error printing batch query info!", e1);
                }
            }
            return false;
        } finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(connection);
        }
    }

    /**
     * Runs a set of update queries against the database.
     *
     * @param queries An arraylist of update queries.
     * @return True if the queries all successfully ran; false otherwise.
     */
    public static boolean runUpdateBatch(ArrayList<String> queries) {
        Statement stmt = null;
        Connection connection = null;
        try {
            connection = getPoolConnection();
            stmt = connection.createStatement();
            for (String q : queries) {
                stmt.addBatch(q);
            }
            stmt.executeBatch();
            return true;
        } catch (Exception e) {
            LOGGER.error("Error running batch update query!", e);
            if (stmt != null) {
                try {
                    LOGGER.error("Number of queries: " + queries.size() + "     Update count: " + stmt.getUpdateCount());
                } catch (Exception e1) {
                    LOGGER.error("Error printing batch query info!", e1);
                }
            }
            return false;
        } finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(connection);
        }
    }

    public static boolean doesDBExist() {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = connectionPool.getConnection();
            stmt = conn.prepareStatement("SELECT * FROM ACCOUNTS LIMIT 1");
            stmt.executeQuery();
            return true;
        } catch (Exception e) {
            if (!e.getMessage()
                    .contains("object not found")) {
                LOGGER.error(e);
            }
            return false;
        } finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(conn);
        }
    }

    public static void initialise() {
        connectionPool = new BasicDataSource();
        connectionPool.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
        connectionPool.setUsername("SA");
        connectionPool.setPassword("");
        String url = "jdbc:hsqldb:file:db/awcdb";
        connectionPool.setUrl(url);
        connectionPool.setInitialSize(10);
        connectionPool.setMaxOpenPreparedStatements(10);
        connectionPool.setMaxConnLifetimeMillis(1000 * 60 * 5);
        connectionPool.setMaxTotal(10);
    }

    public static void shutDown() {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = connectionPool.getConnection();
            stmt = conn.prepareStatement("SHUTDOWN");
            stmt.executeUpdate();
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(conn);
        }
    }

    public static void initialiseTables() {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = connectionPool.getConnection();
            stmt = conn.prepareStatement(CREATE_ACCOUNTS_TABLE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(ACCOUNTS_TABLE_ADD_UNIQUE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(CREATE_TWEETS_TABLE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(TWEETS_TABLE_ADD_UNIQUE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(CREATE_CONFIG_TABLE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(CONFIG_TABLE_ADD_UNIQUE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(CREATE_COLLECTIONS_TABLE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(COLLECTIONS_TABLE_ADD_UNIQUE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(CREATE_USER_RATELIMITS_TABLE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(USER_RATELIMITS_TABLE_ADD_UNIQUE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(CREATE_APP_RATELIMITS_TABLE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(APP_RATELIMITS_TABLE_ADD_UNIQUE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(CREATE_COLLECTION_TWEETS_TABLE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(COLLECTION_TWEETS_TABLE_ADD_UNIQUE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(CREATE_RETWEET_QUEUE_TABLE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(RETWEET_QUEUE_TABLE_ADD_UNIQUE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(CREATE_RETWEETS_TABLE);
            stmt.executeUpdate();
            stmt = conn.prepareStatement(CREATE_FAILED_RETWEETS_TABLE);
            stmt.executeUpdate();
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(conn);
        }
    }

    /**
     *
     * @param table
     * @param insertFields
     * @param insertValues
     * @return
     */
    public static DBResponse insertIntoTable(DBTable table, String[] insertFields, Object[] insertValues) {
        DBResponse response = new DBResponse();
        if (insertFields.length == 0 || insertValues.length == 0) {
            LOGGER.error("You must provide at least one field to insert a value into.");
            response.setStatusCode(DBResponseCode.INPUT_ERROR);
            return response;
        }
        if (insertFields.length != insertValues.length) {
            LOGGER.error("Number of fields and values do not match.");
            response.setStatusCode(DBResponseCode.INPUT_ERROR);
            return response;
        }
        String query = "INSERT INTO ".concat(table.name())
                .concat(" (");
        for (String field : insertFields) {
            query = query.concat(field)
                    .concat(",");
        }
        query = query.substring(0, query.length() - 1)
                .concat(") VALUES (");
        for (String insertField : insertFields) {
            query = query.concat("?,");
        }
        query = query.substring(0, query.length() - 1)
                .concat(")");
        PreparedStatement stmt = null;
        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            stmt = connection.prepareStatement(query);
            int i = 1;
            for (Object value : insertValues) {
                stmt.setObject(i, value);
                i++;
            }
            stmt.executeUpdate();
            response.setStatusCode(DBResponseCode.SUCCESS);
            return response;
        } catch (Exception e) {
            if (e.getMessage()
                    .contains("unique constraint or index violation")) {
                response.setStatusCode(DBResponseCode.DUPLICATE_ERROR);
                return response;
            }
            LOGGER.error("Error inserting data into DB!", e);
            response.setStatusCode(DBResponseCode.DB_ERROR);
            return response;
        } finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(connection);
        }
    }

    /**
     *
     * @param table
     * @return
     */
    public static DBResponse selectFromTable(DBTable table) {
        return selectFromTable(table, new String[]{"*"}, new String[]{}, new Object[]{});
    }

    /**
     *
     * @param table
     * @param orderByField
     * @param orderByOperand
     * @return
     */
    public static DBResponse selectFromTable(DBTable table, String orderByField, String orderByOperand) {
        return selectFromTable(table, new String[]{"*"}, new String[]{}, new Object[]{}, "AND", orderByField, orderByOperand);
    }

    /**
     *
     * @param table
     * @param whereFields
     * @param whereValues
     * @return
     */
    public static DBResponse selectFromTable(DBTable table, String[] whereFields, Object[] whereValues) {
        return selectFromTable(table, new String[]{"*"}, whereFields, whereValues);
    }

    /**
     *
     * @param table
     * @param whereFields
     * @param whereValues
     * @param orderByField
     * @param orderByOperand
     * @return
     */
    public static DBResponse selectFromTable(DBTable table, String[] whereFields, Object[] whereValues, String orderByField, String orderByOperand) {
        return selectFromTable(table, new String[]{"*"}, whereFields, whereValues, "AND", orderByField, orderByOperand);
    }

    /**
     *
     * @param table
     * @param returnFields
     * @return
     */
    public static DBResponse selectFromTable(DBTable table, String[] returnFields) {
        return selectFromTable(table, returnFields, new String[]{}, new Object[]{});
    }

    /**
     *
     * @param table
     * @param returnFields
     * @param whereFields
     * @param whereValues
     * @return
     */
    public static DBResponse selectFromTable(DBTable table, String[] returnFields, String[] whereFields, Object[] whereValues) {
        return selectFromTable(table, returnFields, whereFields, whereValues, "AND");
    }

    /**
     *
     * @param table
     * @param returnFields
     * @param whereFields
     * @param whereValues
     * @param operand
     * @return
     */
    public static DBResponse selectFromTable(DBTable table, String[] returnFields, String[] whereFields, Object[] whereValues, String operand) {
        return selectFromTable(table, returnFields, whereFields, whereValues, operand, null, null);
    }

    // Pass a single "*" to this function in returnFields to select all fields.
    // Returns null if arguments are incorrect, DBResponse object otherwise.
    /**
     *
     * @param table
     * @param returnFields
     * @param whereFields
     * @param whereValues
     * @param operand
     * @param orderByField
     * @param orderByOperand
     * @return
     */
    public static DBResponse selectFromTable(DBTable table, String[] returnFields, String[] whereFields, Object[] whereValues, String operand,
            String orderByField, String orderByOperand) {
        if (table == null || returnFields == null || whereFields == null || whereValues == null) {
            LOGGER.error("No null arguments are accepted for this function.");
            return null;
        }
        if (returnFields.length == 0) {
            LOGGER.error("No return fields specified. If you want all fields, pass a 1-length array with \"*\" as the field.");
            return null;
        }
        if (whereFields.length != whereValues.length) {
            LOGGER.error("Length of where fields and values arrays do not match.");
            return null;
        }
        DBResponse response = new DBResponse();
        PreparedStatement preparedStmt = null;
        ResultSet rs = null;
        Connection connection = null;
        String query = "SELECT ";
        if (returnFields.length == 1 && returnFields[0].equals("*")) {
            query = query.concat("* FROM ");
        } else {
            for (String field : returnFields) {
                query = query.concat(field)
                        .concat(", ");
            }
            query = query.substring(0, query.length() - 2);
            query = query.concat(" FROM ");
        }
        query = query.concat(table.name());
        if (whereFields.length > 0) {
            query = query.concat(" WHERE ");
            for (int i = 0; i < whereFields.length; i++) {
                String field = whereFields[i];
                Object value = whereValues[i];
                if (value instanceof DBSyntax) {
                    if (value.equals(DBSyntax.IS_NULL)) {
                        query = query.concat(field)
                                .concat(" IS NULL ")
                                .concat(operand)
                                .concat(" ");
                    } else if (value.equals(DBSyntax.IS_NOT_NULL)) {
                        query = query.concat(field)
                                .concat(" IS NOT NULL ")
                                .concat(operand)
                                .concat(" ");
                    } else {
                        query = query.concat(field)
                                .concat(" = ? ")
                                .concat(operand)
                                .concat(" ");
                    }
                } else {
                    query = query.concat(field)
                            .concat(" = ? ")
                            .concat(operand)
                            .concat(" ");
                }
            }
            query = query.substring(0, query.length() - 2 - operand.length());
        }
        if (orderByField != null && orderByOperand != null) {
            query = query.concat(" ORDER BY ")
                    .concat(orderByField)
                    .concat(" ")
                    .concat(orderByOperand);
        }
        try {
            connection = connectionPool.getConnection();
            preparedStmt = connection.prepareStatement(query);
            int j = 1;
            for (Object value : whereValues) {
                if (value instanceof DBSyntax) {
                    if (!(value.equals(DBSyntax.IS_NULL) || value.equals(DBSyntax.IS_NOT_NULL))) {
                        preparedStmt.setObject(j, value);
                        j++;
                    }
                } else {
                    preparedStmt.setObject(j, value);
                    j++;
                }
            }
            rs = preparedStmt.executeQuery();
            ArrayList<HashMap<String, Object>> results = new ArrayList<>();
            ResultSetMetaData metadata = rs.getMetaData();
            int count = metadata.getColumnCount();
            while (rs.next()) {
                HashMap<String, Object> map = new HashMap<>();
                for (int i = 1; i <= count; i++) {
                    map.put(metadata.getColumnName(i), rs.getObject(i));
                }
                results.add(map);
            }
            response.setReturnedRows(results);
            response.setStatusCode(DBResponseCode.SUCCESS);
            return response;
        } catch (Exception e) {
            LOGGER.error("Failed to select data from table!", e);
            response.setStatusCode(DBResponseCode.DB_ERROR);
            response.setMessage(e.getMessage());
            return response;
        } finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(preparedStmt);
            DbUtils.closeQuietly(connection);
        }
    }

    /**
     * Deletes from the given table, where the given fields in whereFields are equal to the given values in whereValues. Example usage:
     * <p>
     * deleteFromTable(DBTables.IMAGETWEETS,
     * <p>
     * new String[]{"id"},
     * <p>
     * new Object[]{1234});
     * <p>
     * The above code would execute "DELETE FROM imagetweets WHERE id=1234".
     *
     * @param table The table to delete from. Enumerated to prevent an invalid table name being submitted to this method.
     * @param whereFields The fields to check against when deleting. You cannot provide an empty array.
     * @param whereValues The value conditions for the fields. This cannot be empty, and must be of equal length to the whereFields array.
     * @return The JDBC result of executing the delete query; DB_ERROR if an error occurs.
     */
    public static DBResponse deleteFromTable(DBTable table, String[] whereFields, Object[] whereValues) {
        DBResponse response = new DBResponse();
        String tableName = table.name();
        if (whereFields.length == 0 || whereValues.length == 0) {
            LOGGER.error("This function requires all arguments - you cannot call it with an empty array.");
            response.setStatusCode(DBResponseCode.INPUT_ERROR);
            return response;
        }
        if (whereFields.length != whereValues.length) {
            LOGGER.error("Update and where arrays passed to this function must be of equal length.");
            response.setStatusCode(DBResponseCode.INPUT_ERROR);
            return response;
        }
        String query = "DELETE FROM ".concat(tableName)
                .concat(" WHERE ");
        for (String field : whereFields) {
            query = query.concat(field)
                    .concat("=? AND ");
        }
        query = query.substring(0, query.length() - 4);
        PreparedStatement stmt = null;
        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            stmt = connection.prepareStatement(query);
            int i = 1;
            for (Object where : whereValues) {
                stmt.setObject(i, where);
                i++;
            }
            stmt.executeUpdate();
            response.setStatusCode(DBResponseCode.SUCCESS);
            return response;
        } catch (Exception e) {
            LOGGER.error("Error deleting from DB!", e);
            response.setStatusCode(DBResponseCode.DB_ERROR);
            return response;
        } finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(connection);
        }
    }

    /**
     *
     * @param tableName
     * @param updateFields
     * @param updateValues
     * @param whereFields
     * @param whereValues
     * @return
     */
    public static int updateTable(DBTable tableName, String[] updateFields, Object[] updateValues, String[] whereFields, Object[] whereValues) {
        return updateTable(tableName, updateFields, updateValues, whereFields, whereValues, new String[]{}, new int[]{});
    }

    /**
     *
     * @param table
     * @param updateFields
     * @param updateValues
     * @param whereFields
     * @param whereValues
     * @param incrementFields
     * @param incrementValues
     * @return
     */
    public static int updateTable(DBTable table, String[] updateFields, Object[] updateValues, String[] whereFields, Object[] whereValues,
            String[] incrementFields, int[] incrementValues) {
        String tableName = table.name();
        if (whereFields.length == 0 || whereValues.length == 0) {
            LOGGER.error("This function requires WHERE arguments - you cannot call it with empty arrays for those.");
            return INPUT_ERROR;
        }
        if ((updateFields.length == 0 || updateValues.length == 0) && (incrementFields.length == 0 || incrementValues.length == 0)) {
            LOGGER.error("You must pass at least one field to update.");
            return INPUT_ERROR;
        }
        if ((updateFields.length != updateValues.length) || (whereFields.length != whereValues.length)
                || (incrementFields.length != incrementValues.length)) {
            LOGGER.error("Update and where arrays passed to this function must be of equal length.");
            return INPUT_ERROR;
        }
        String query = "UPDATE ".concat(tableName)
                .concat(" SET ");
        for (String f : updateFields) {
            query = query.concat(f)
                    .concat("=?, ");
        }

        for (int i = 0; i < incrementFields.length; i++) {
            String f = incrementFields[i];
            if (incrementValues[i] < 0) {
                query = query.concat(f)
                        .concat("=")
                        .concat(f)
                        .concat(String.valueOf(incrementValues[i]))
                        .concat(", ");
            } else {
                query = query.concat(f)
                        .concat("=")
                        .concat(f)
                        .concat("+")
                        .concat(String.valueOf(incrementValues[i])
                                .concat(", "));
            }
        }
        query = query.substring(0, query.length() - 2);
        query = query.concat(" WHERE ");
        for (String f : whereFields) {
            query = query.concat(f)
                    .concat("=? AND ");
        }

        query = query.substring(0, query.length() - 4);
        PreparedStatement stmt = null;
        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            stmt = connection.prepareStatement(query);
            int i = 1;
            for (Object value : updateValues) {
                stmt.setObject(i, value);
                i++;
            }
            for (Object where : whereValues) {
                stmt.setObject(i, where);
                i++;
            }
            return stmt.executeUpdate();
        } catch (Exception e) {
            LOGGER.error("Failed to update values in DB!", e);
            return DB_ERROR;
        } finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(connection);
        }
    }

    public static boolean addAccountToDB(Account account) {
        DBResponse resp = CoreDB.selectFromTable(DBTable.ACCOUNTS, new String[]{"twitterid"}, new Object[]{account.getTwitterID()});
        if (!resp.wasSuccessful()) {
            LOGGER.error("Failed to check if expired access token exists");
            return false;
        }
        if (!resp.getReturnedRows().isEmpty()) {
            CoreDB.deleteFromTable(DBTable.ACCOUNTS, new String[]{"twitterid"}, new Object[]{account.getTwitterID()});
        }
        DBResponse insertResp = CoreDB.insertIntoTable(DBTable.ACCOUNTS, new String[]{"twitterid", "token", "tokensecret", "screen_name"}, new Object[]{account.getTwitterID(), account.getToken(), account.getTokenSecret(), account.getScreenName()});
        return insertResp.wasSuccessful();
    }

}
