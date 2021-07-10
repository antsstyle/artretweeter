/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.db;

import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class CollectionsDB {

    private static final Logger LOGGER = LogManager.getLogger(CollectionsDB.class);

    private static final String COLLECTION_TWEET_MERGE_QUERY = "MERGE INTO collectiontweets USING (VALUES (?,?))" 
            + " AS vals(tweetid,collectionid) ON (collectiontweets.tweetid = vals.tweetid AND collectiontweets.collectionid = vals.collectionid)" 
            + " WHEN MATCHED THEN UPDATE SET collectiontweets.tweetid=vals.tweetid,collectiontweets.collectionid=vals.collectionid" 
            + " WHEN NOT MATCHED THEN INSERT (tweetid,collectionid) VALUES (vals.tweetid, vals.collectionid)";

    private static final String COLLECTION_MERGE_QUERY = "MERGE INTO collections USING (VALUES(?,?,?,?,?,?))"
            + " AS vals(usertwitterid,collectionid,collectionurl,name,description,ordering) ON "
            + "(collections.collectionid = vals.collectionid) "
            + "WHEN MATCHED THEN UPDATE SET collections.usertwitterid=vals.usertwitterid,collections.collectionid=vals.collectionid,"
            + "collections.collectionurl=vals.collectionurl,collections.name=vals.name,"
            + "collections.description=vals.description,collections.ordering=vals.ordering "
            + "WHEN NOT MATCHED THEN INSERT (usertwitterid,collectionid,collectionurl,name,description,ordering) VALUES "
            + "(vals.usertwitterid,vals.collectionid,vals.collectionurl,vals.name,vals.description,vals.ordering) ";

    public static boolean insertCollection(Object[] params) {
        return CoreDB.runCustomUpdate(COLLECTION_MERGE_QUERY, params);
    }

    public static boolean parameterisedCollectionMergeBatch(ArrayList<Object[]> params) {
        return CoreDB.runParameterisedUpdateBatch(CollectionsDB.COLLECTION_MERGE_QUERY, params);
    }

    public static boolean parameterisedCollectionTweetsMergeBatch(ArrayList<Object[]> params) {
        return CoreDB.runParameterisedUpdateBatch(CollectionsDB.COLLECTION_TWEET_MERGE_QUERY, params);
    }

}
