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
public class CollectionCurateRespJSON {
    
    private CollectionCurateResponse response;

    public CollectionCurateResponse getResponse() {
        return response;
    }

    public void setResponse(CollectionCurateResponse response) {
        this.response = response;
    }

    public class CollectionCurateResponse {
        private CollectionCurateEntry[] entries;

        public CollectionCurateEntry[] getEntries() {
            return entries;
        }

        public void setEntries(CollectionCurateEntry[] entries) {
            this.entries = entries;
        }
        
        public class CollectionCurateEntry {
            private String reason;

            public String getReason() {
                return reason;
            }

            public void setReason(String reason) {
                this.reason = reason;
            }

            public ChangeEntry getEntry() {
                return entry;
            }

            public void setEntry(ChangeEntry entry) {
                this.entry = entry;
            }
            private ChangeEntry entry;
            
            public class ChangeEntry {
                private String op;
                private Long tweetID;

                public String getOp() {
                    return op;
                }

                public void setOp(String op) {
                    this.op = op;
                }

                public Long getTweetID() {
                    return tweetID;
                }

                public void setTweetID(Long tweetID) {
                    this.tweetID = tweetID;
                }
            }
        }
    }

}
