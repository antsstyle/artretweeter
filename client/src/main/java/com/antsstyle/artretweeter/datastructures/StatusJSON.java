/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import com.antsstyle.artretweeter.enumerations.StatusCode;
import com.antsstyle.artretweeter.tools.FormatTools;
import com.antsstyle.artretweeter.tools.ImageTools;
import com.antsstyle.artretweeter.tools.PathTools;
import java.nio.file.Path;
import java.sql.Timestamp;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class StatusJSON {

    private static final Logger LOGGER = LogManager.getLogger(StatusJSON.class);

    private Long id;
    private String created_at;
    private String text;
    private String full_text;
    private Entities entities;
    private ExtendedEntities extended_entities;
    private String source;
    private Long in_reply_to_status_id;
    private Long in_reply_to_user_id;
    private String in_reply_to_screen_name;
    private Integer retweet_count;
    private Integer favorite_count;
    private Boolean favorited;

    public String getFull_text() {
        return full_text;
    }

    public void setFull_text(String full_text) {
        this.full_text = full_text;
    }

    public Boolean getFavorited() {
        return favorited;
    }

    public void setFavorited(Boolean favorited) {
        this.favorited = favorited;
    }

    public Boolean getRetweeted() {
        return retweeted;
    }

    public void setRetweeted(Boolean retweeted) {
        this.retweeted = retweeted;
    }
    private Boolean retweeted;
    private StatusJSON retweeted_status;
    private User user;
    private Integer internalDatabaseID;

    public OperationResult downloadAndGetDBParams(Path tweetFolderPath) {
        OperationResult result = new OperationResult();
        Path filepath1, filepath2 = null, filepath3 = null, filepath4 = null;
        String url1, url2 = null, url3 = null, url4 = null;
        if (extended_entities == null && (entities.getMedia() == null || entities.getMedia().length == 0)) {
            result.setTwitterResponse(new TwitterResponse(StatusCode.TWEET_HAS_NO_IMAGES));
            return result;
        }
        if (extended_entities == null) {
            url1 = extended_entities.getMedia()[0].getMedia_url();
        } else {
            url1 = entities.getMedia()[0].getMedia_url();
        }
        filepath1 = tweetFolderPath.resolve(url1.substring(url1.lastIndexOf("/") + 1));
        OperationResult res1 = ImageTools.downloadImageFromSiteWithRetry(url1, filepath1, false);
        if (!res1.wasSuccessful()) {
            return res1;
        }

        if (!res1.getClientResponse().getStatusCode().equals(StatusCode.FILE_ALREADY_DOWNLOADED)) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                LOGGER.error("Interrupted - aborting tweet download.", e);
                result.setClientResponse(new ClientResponse(StatusCode.INTERRUPTED_ERROR));
                return result;
            }
        }

        if (extended_entities.getMedia() != null && extended_entities.getMedia().length > 1) {
            url2 = extended_entities.getMedia()[1].getMedia_url();
            filepath2 = tweetFolderPath.resolve(url2.substring(url2.lastIndexOf("/") + 1));
            OperationResult res2 = ImageTools.downloadImageFromSiteWithRetry(url2, filepath2, false);
            if (!res2.wasSuccessful()) {
                return res2;
            }

            if (!res2.getClientResponse().getStatusCode().equals(StatusCode.FILE_ALREADY_DOWNLOADED)) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    LOGGER.error("Interrupted - aborting tweet download.", e);
                    result.setClientResponse(new ClientResponse(StatusCode.INTERRUPTED_ERROR));
                    return result;
                }
            }

        }
        if (extended_entities.getMedia() != null && extended_entities.getMedia().length > 2) {
            url3 = extended_entities.getMedia()[2].getMedia_url();
            filepath3 = tweetFolderPath.resolve(url3.substring(url3.lastIndexOf("/") + 1));
            OperationResult res3 = ImageTools.downloadImageFromSiteWithRetry(url3, filepath3, false);
            if (!res3.wasSuccessful()) {
                return res3;
            }
            if (!res3.getClientResponse().getStatusCode().equals(StatusCode.FILE_ALREADY_DOWNLOADED)) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    LOGGER.error("Interrupted - aborting tweet download.", e);
                    result.setClientResponse(new ClientResponse(StatusCode.INTERRUPTED_ERROR));
                    return result;
                }
            }
        }
        if (extended_entities.getMedia() != null && extended_entities.getMedia().length > 3) {
            url4 = extended_entities.getMedia()[3].getMedia_url();
            filepath4 = tweetFolderPath.resolve(url4.substring(url4.lastIndexOf("/") + 1));
            OperationResult res4 = ImageTools.downloadImageFromSiteWithRetry(url4, filepath4, false);
            if (!res4.wasSuccessful()) {
                return res4;
            }
            if (!res4.getClientResponse().getStatusCode().equals(StatusCode.FILE_ALREADY_DOWNLOADED)) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    LOGGER.error("Interrupted - aborting tweet download.", e);
                    result.setClientResponse(new ClientResponse(StatusCode.INTERRUPTED_ERROR));
                    return result;
                }
            }
        }
        Timestamp createdAtTimestamp;
        try {
            createdAtTimestamp = new Timestamp(FormatTools.TWITTER_DATE_FORMAT.parse(created_at).getTime());
        } catch (Exception e) {
            LOGGER.error("Failed to parse Twitter timestamp!", e);
            return null;
        }
        String textParam;
        if (full_text != null) {
            textParam = StringUtils.replace(full_text, "&amp;", "&");
        } else {
            textParam = StringUtils.replace(text, "&amp;", "&");
        }
        Object[] params = new Object[]{id, user.getId(), user.getScreen_name(),
            PathTools.convertPathToString(filepath1), PathTools.convertPathToString(filepath2),
            PathTools.convertPathToString(filepath3), PathTools.convertPathToString(filepath4), url1, url2, url3, url4,
            createdAtTimestamp, favorite_count, retweet_count,
            textParam, source};
        result.setClientResponse(new ClientResponse(StatusCode.SUCCESS));
        result.getClientResponse().setReturnedObject(params);
        return result;
    }

    public Integer getInternalDatabaseID() {
        return internalDatabaseID;
    }

    public StatusJSON setInternalDatabaseID(Integer internalDatabaseID) {
        this.internalDatabaseID = internalDatabaseID;
        return this;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public StatusJSON getRetweeted_status() {
        return retweeted_status;
    }

    public void setRetweeted_status(StatusJSON retweeted_status) {
        this.retweeted_status = retweeted_status;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Entities getEntities() {
        return entities;
    }

    public void setEntities(Entities entities) {
        this.entities = entities;
    }

    public ExtendedEntities getExtended_entities() {
        return extended_entities;
    }

    public void setExtended_entities(ExtendedEntities extended_entities) {
        this.extended_entities = extended_entities;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getIn_reply_to_status_id() {
        return in_reply_to_status_id;
    }

    public void setIn_reply_to_status_id(Long in_reply_to_status_id) {
        this.in_reply_to_status_id = in_reply_to_status_id;
    }

    public Long getIn_reply_to_user_id() {
        return in_reply_to_user_id;
    }

    public void setIn_reply_to_user_id(Long in_reply_to_user_id) {
        this.in_reply_to_user_id = in_reply_to_user_id;
    }

    public String getIn_reply_to_screen_name() {
        return in_reply_to_screen_name;
    }

    public void setIn_reply_to_screen_name(String in_reply_to_screen_name) {
        this.in_reply_to_screen_name = in_reply_to_screen_name;
    }

    public Integer getRetweet_count() {
        return retweet_count;
    }

    public void setRetweet_count(Integer retweet_count) {
        this.retweet_count = retweet_count;
    }

    public Integer getFavorite_count() {
        return favorite_count;
    }

    public void setFavorite_count(Integer favorite_count) {
        this.favorite_count = favorite_count;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public class Entities {

        private Media[] media;

        public Media[] getMedia() {
            return media;
        }

        public void setMedia(Media[] media) {
            this.media = media;
        }

        public TwitterURL[] getUrls() {
            return urls;
        }

        public void setUrls(TwitterURL[] urls) {
            this.urls = urls;
        }
        private TwitterURL[] urls;

        public class TwitterURL {

            private String url;
            private String expanded_url;

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getExpanded_url() {
                return expanded_url;
            }

            public void setExpanded_url(String expanded_url) {
                this.expanded_url = expanded_url;
            }
        }
    }

    public class ExtendedEntities {

        private Media[] media;

        public Media[] getMedia() {
            return media;
        }

        public void setMedia(Media[] media) {
            this.media = media;
        }

    }

    public class Media {

        private Long id;
        private String media_url;
        private String type;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getMedia_url() {
            return media_url;
        }

        public void setMedia_url(String media_url) {
            this.media_url = media_url;
        }

        public String getMedia_url_https() {
            return media_url_https;
        }

        public void setMedia_url_https(String media_url_https) {
            this.media_url_https = media_url_https;
        }
        private String media_url_https;
    }

    public class User {

        private Long id;
        private String name;
        private String screen_name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getScreen_name() {
            return screen_name;
        }

        public void setScreen_name(String screen_name) {
            this.screen_name = screen_name;
        }
    }

}
