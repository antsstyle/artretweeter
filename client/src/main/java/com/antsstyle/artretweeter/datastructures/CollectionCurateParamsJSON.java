/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author antss
 */
public class CollectionCurateParamsJSON {

    private String id;

    public String getId() {
        return id;
    }

    public CollectionCurateParamsJSON setId(String id) {
        this.id = id;
        return this;
    }

    public Change[] getChanges() {
        return changes;
    }

    public CollectionCurateParamsJSON setChanges(Change[] changes) {
        this.changes = changes;
        return this;
    }
    private Change[] changes;

    public class Change {

        private String op;

        public String getOp() {
            return op;
        }

        public Change setOp(String op) {
            this.op = op;
            return this;
        }

        public String getTweet_id() {
            return tweet_id;
        }

        public Change setTweet_id(String tweet_id) {
            this.tweet_id = tweet_id;
            return this;
        }
        private String tweet_id;
    }

    public void setChanges(HashMap<Long, CollectionOperation> tweetsToCurate) {
        changes = new Change[tweetsToCurate.size()];
        ArrayList<Long> keys = new ArrayList<>(tweetsToCurate.keySet());
        for (int i = 0; i < keys.size(); i++) {
            Long tweetID = keys.get(i);
            CollectionOperation op = tweetsToCurate.get(tweetID);
            Change change = new Change()
                    .setOp(op.getParameterName())
                    .setTweet_id(String.valueOf(tweetID));
            changes[i] = change;
        }
    }
}
