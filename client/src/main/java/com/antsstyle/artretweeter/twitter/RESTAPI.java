/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.twitter;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.CollectionOrdering;
import com.antsstyle.artretweeter.datastructures.OrderedByLikesStatusHolder;
import com.antsstyle.artretweeter.datastructures.OrderedByRetweetsStatusHolder;
import com.antsstyle.artretweeter.datastructures.OrderedStatusHolder;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.RequestToken;
import com.antsstyle.artretweeter.datastructures.StatusJSON;
import com.antsstyle.artretweeter.datastructures.TwitterCollectionHolder;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.main.ArtRetweeterMain;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class RESTAPI {

    private static final Logger LOGGER = LogManager.getLogger(RESTAPI.class);

    public static Boolean CURRENTLY_PROCESSING = false;

    /* Twitter API Status codes */
    public static final int PAGE_NOT_FOUND = 34;
    public static final int RATE_LIMIT_EXCEEDED = 88;
    public static final int INVALID_ACCESS_TOKEN = 89;
    public static final int CANNOT_VALIDATE_OAUTH_CREDENTIALS = 99;
    public static final int TWITTER_OVER_CAPACITY = 130;
    public static final int TWITTER_INTERNAL_ERROR = 131;
    public static final int TIMESTAMP_AUTH_ERROR = 135;
    public static final int BLOCKED_BY_TWEET_AUTHOR = 136;
    public static final int TWEET_NOT_FOUND = 144;
    public static final int TWEET_NOT_VIEWABLE = 179;
    public static final int TWEET_TEXT_TOO_LONG = 186;
    public static final int DUPLICATE_TWEET = 187;
    public static final int ALREADY_RETWEETED = 327;

    /**
     *
     * @param endpoint
     * @param userTwitterID
     * @param response
     * @return A null OperationResult on success, with the result JSON in the JsonObject. The OperationResult will be instantiated with a status code on failure.
     */
    public static Pair<OperationResult, JsonObject> processResponse(String endpoint,
            Long userTwitterID, CloseableHttpResponse response) {
        if (response.getStatusLine().getStatusCode() != 200) {
            OperationResult res = new OperationResult()
                    .setArtRetweeterStatusCode(OperationResult.ARTRETWEETER_SERVER_ERROR)
                    .setArtRetweeterStatusMessage("Failed to communicate with ArtRetweeter server!");
            return Pair.of(res, null);
        }
        HttpEntity entity = response.getEntity();
        Gson gson = new Gson();
        try ( InputStream is = entity.getContent();  InputStreamReader reader = new InputStreamReader(is, "UTF-8")) {
            String jsonString = IOUtils.toString(reader);
            PrintWriter pw = new PrintWriter("I:/zzztestoutput.txt");
            pw.println(jsonString);
            pw.close();
            JsonObject responseJSON = gson.fromJson(jsonString, JsonObject.class);
            if (responseJSON.keySet().contains("headers")) {
                try {
                    JsonObject headerJSON = responseJSON.get("headers").getAsJsonObject();
                    processHeaders(headerJSON, endpoint, userTwitterID);
                } catch (Exception e1) {
                    LOGGER.error("Unable to process headers - empty array returned.", e1);
                }
            }
            OperationResult result = processArtRetweeterServerErrorResponse(responseJSON);
            EntityUtils.consume(entity);
            if (result != null) {
                return Pair.of(result, responseJSON);
            }
            result = processTwitterAPIErrorResponse(responseJSON);
            if (result != null) {
                return Pair.of(result, responseJSON);
            }
            return Pair.of(null, responseJSON);
        } catch (Exception e) {
            LOGGER.error("Unable to read response entity!", e);
            OperationResult res = new OperationResult()
                    .setArtRetweeterStatusCode(OperationResult.JSON_PARSE_ERROR)
                    .setArtRetweeterStatusMessage("Unable to read response entity!");
            return Pair.of(res, null);
        }
    }

    public static OperationResult processTwitterAPIErrorResponse(JsonObject responseJSON) {
        Set<String> keys = responseJSON.keySet();
        if (!keys.contains("response")) {
            return null;
        }
        JsonElement responseElem = responseJSON.get("response");
        if (responseElem.isJsonArray()) {
            return null;
        }
        JsonObject responseObj = responseElem.getAsJsonObject();
        if (!responseObj.keySet().contains("errors")) {
            return null;
        }

        JsonArray errorObj = responseJSON.get("errors").getAsJsonArray();
        // Use first error only
        OperationResult result = new OperationResult()
                .setTwitterErrorCode(errorObj.get(0).getAsJsonObject().get("code").getAsInt())
                .setTwitterStatusMessage(errorObj.get(0).getAsJsonObject().get("message").getAsString())
                .setArtRetweeterStatusCode(OperationResult.TWITTER_API_ERROR)
                .setHttpStatusCode(responseJSON.get("httpcode").getAsInt());
        return result;
    }

    public static OperationResult processArtRetweeterServerErrorResponse(JsonObject responseJSON) {
        if (responseJSON.keySet().size() >= 1 && responseJSON.get("artretweetererrors") != null) {
            String message = responseJSON.get("artretweetererrors").getAsString();
            OperationResult result = new OperationResult();
            if (message.equals("Rate limit reached.")) {
                result.setArtRetweeterStatusCode(OperationResult.RATE_LIMIT_EXCEEDED_ERROR);
                int timeToResetSeconds = responseJSON.get("timetoresetseconds").getAsInt();
                int minutes = timeToResetSeconds / 60;
                int seconds = timeToResetSeconds % 60;
                String timeMessage;
                if (minutes == 0) {
                    timeMessage = String.valueOf(seconds).concat(" seconds.");
                } else if (minutes == 1) {
                    timeMessage = String.valueOf(minutes).concat(" minute, ").concat(String.valueOf(seconds)).concat(" seconds.");
                } else {
                    timeMessage = String.valueOf(minutes).concat(" minutes, ").concat(String.valueOf(seconds)).concat(" seconds.");
                }
                result.setArtRetweeterStatusMessage("You can try this request again in ".concat(timeMessage));
                result.setReturnedObject(responseJSON.get("timetoresetseconds").getAsInt());
            } else {
                result.setArtRetweeterStatusCode(OperationResult.ARTRETWEETER_SERVER_ERROR);
                result.setArtRetweeterStatusMessage(message);
            }
            return result;
        } else {
            return null;
        }
    }

    public static void processHeaders(JsonObject headerJSON, String endpoint, Long userTwitterID) {
        Set<String> keys = headerJSON.keySet();
        Timestamp rateLimitReset;
        Integer rateLimitTotal;
        Integer rateLimitRemaining;
        if (keys.contains("x_rate_limit_reset")) {
            rateLimitReset = new Timestamp(headerJSON.get("x_rate_limit_reset").getAsLong());
        } else {
            // Headers have no rate limit information; no need to process.
            return;
        }
        try {
            rateLimitTotal = headerJSON.get("x_rate_limit_limit").getAsInt();
            rateLimitRemaining = headerJSON.get("x_rate_limit_remaining").getAsInt();
        } catch (Exception e) {
            LOGGER.error("Unable to parse rate limit headers!", e);
            return;
        }
        if (userTwitterID == null) {
            CoreDB.mergeAppRateLimitInfo(endpoint, rateLimitTotal, rateLimitRemaining, rateLimitReset);
        } else {
            CoreDB.mergeUserRateLimitInfo(userTwitterID, endpoint, rateLimitTotal, rateLimitRemaining, rateLimitReset);
        }
    }

    public static OperationResult apiCall(List<NameValuePair> nameValuePairs, Endpoint endpoint,
            Account account) {
        OperationResult opResult = new OperationResult();
        if (account == null && endpoint.getRequiresUserAuth()) {
            opResult.setArtRetweeterStatusCode(OperationResult.MISSING_CREDENTIALS_ERROR);
            return opResult;
        }
        Long userTwitterID = null;
        if (account != null) {
            userTwitterID = account.getTwitterID();
            nameValuePairs.add(new BasicNameValuePair("access_token", account.getToken()));
            nameValuePairs.add(new BasicNameValuePair("access_token_secret", account.getTokenSecret()));
            nameValuePairs.add(new BasicNameValuePair("user_auth_twitter_id", String.valueOf(account.getTwitterID())));
        }
        String url = ArtRetweeterMain.prop.getProperty(endpoint.getPropertyName());
        try ( CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            int artRetweeterServerErrors = 0;
            int twitterAPIErrors = 0;
            while (artRetweeterServerErrors < 3 && twitterAPIErrors < 3) {
                try ( CloseableHttpResponse response = httpclient.execute(httpPost)) {
                    Pair<OperationResult, JsonObject> checkResult = processResponse(endpoint.getEndpointName(), userTwitterID, response);
                    int httpCode = response.getStatusLine().getStatusCode();
                    if (httpCode != 200 || checkResult.getLeft() != null) {
                        if (httpCode != 200) {
                            artRetweeterServerErrors++;
                        } else {
                            twitterAPIErrors++;
                        }
                        if (twitterAPIErrors == 3 || (checkResult.getLeft() != null
                                && (checkResult.getLeft().getTwitterErrorCode() < 500
                                || checkResult.getLeft().getTwitterErrorCode() > 599))) {
                            if (checkResult.getLeft() != null) {
                                return checkResult.getLeft();
                            } else {
                                opResult.setArtRetweeterStatusCode(OperationResult.TWITTER_API_ERROR);
                                return opResult;
                            }
                        }
                        if (artRetweeterServerErrors == 3) {
                            if (checkResult.getLeft() != null) {
                                return checkResult.getLeft();
                            } else {
                                opResult.setArtRetweeterStatusCode(OperationResult.ARTRETWEETER_SERVER_ERROR);
                                return opResult;
                            }
                        }
                        try {
                            Thread.sleep(artRetweeterServerErrors * 1000);
                        } catch (Exception e) {
                            LOGGER.error("Interrupted while waiting to retry API request", e);
                            opResult.setArtRetweeterStatusCode(OperationResult.INTERRUPTED_ERROR);
                            return opResult;
                        }
                        continue;
                    }
                    JsonObject responseJSON = checkResult.getRight();
                    opResult.setHeaderJSON(responseJSON.get("headers").getAsJsonObject());
                    if (responseJSON.get("response").isJsonObject()) {
                        opResult.setResponseJSONObject(responseJSON.get("response").getAsJsonObject());
                    } else if (responseJSON.get("response").isJsonArray()) {
                        opResult.setResponseJSONArray(responseJSON.get("response").getAsJsonArray());
                    }
                    opResult.setArtRetweeterStatusCode(OperationResult.QUERY_OK);
                    break;
                } catch (Exception e) {
                    LOGGER.error("Failed to get HTTP response!", e);
                    opResult.setArtRetweeterStatusCode(OperationResult.MISC_ERROR);
                    return opResult;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create HTTP client!", e);
            opResult.setArtRetweeterStatusCode(OperationResult.MISC_ERROR);
            return opResult;
        }

        return opResult;
    }

    public static OperationResult statusesRetweet(Long tweetID, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", String.valueOf(tweetID)));
        OperationResult apiCallResult = apiCall(nvps, Endpoint.STATUSES_RETWEET, account);
        if (!apiCallResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getResponseJSONObject();
        Gson gson = new Gson();
        StatusJSON retweetedStatus = gson.fromJson(responseJSON.get("retweeted_status").getAsJsonObject(), StatusJSON.class);
        apiCallResult.setReturnedObject(retweetedStatus);
        return apiCallResult;
    }

    public static OperationResult statusesUnretweet(Long retweetID, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", String.valueOf(retweetID)));
        OperationResult apiCallResult = apiCall(nvps, Endpoint.STATUSES_UNRETWEET, account);
        return apiCallResult;
    }

    public static OperationResult getTweetsByIDs(ArrayList<Long> tweetIDs, Account account, Path tweetFolderPath) {
        List<NameValuePair> nvps = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (Long tweetID : tweetIDs) {
            sb = sb.append(String.valueOf(tweetID)).append(",");
        }
        sb.setLength(sb.length() - 1);
        nvps.add(new BasicNameValuePair("id", sb.toString()));
        OperationResult apiCallResult = apiCall(nvps, Endpoint.STATUSES_LOOKUP, account);
        if (!apiCallResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            return apiCallResult;
        }
        OperationResult opResult = new OperationResult();
        Gson gson = new Gson();
        JsonArray responseJSON = apiCallResult.getResponseJSONArray();
        StatusJSON[] receivedStatuses;
        try {
            receivedStatuses = gson.fromJson(responseJSON, StatusJSON[].class);
        } catch (Exception e1) {
            LOGGER.error("Failed to parse returned statuses JSON!", e1);
            opResult.setArtRetweeterStatusCode(OperationResult.JSON_PARSE_ERROR);
            opResult.setArtRetweeterStatusMessage("Failed to parse returned statuses JSON!");
            return opResult;
        }
        ArrayList<Object[]> params = new ArrayList<>();
        ArrayList<StatusJSON> statuses = new ArrayList<>();
        for (StatusJSON status : receivedStatuses) {
            if (status.getIn_reply_to_screen_name() != null
                    && !status.getIn_reply_to_screen_name().equals(account.getScreenName())) {
                continue;
            }
            if (status.getExtended_entities() == null) {
                continue;
            }
            Integer extendedEntitiesLength = status.getExtended_entities().getMedia().length;
            if (extendedEntitiesLength == 0) {
                continue;
            } else if (!status.getExtended_entities().getMedia()[0].getType().equals("photo")) {
                continue;
            }
            if (status.getRetweeted_status() != null) {
                continue;
            }
            OperationResult tweetParamsResult = status.downloadAndGetDBParams(tweetFolderPath);
            if (tweetParamsResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
                params.add((Object[]) tweetParamsResult.getReturnedObject());
            } else {
                return tweetParamsResult;
            }

            statuses.add(status);
        }
        if (!params.isEmpty()) {
            if (!CoreDB.parameterisedTweetMergeBatch(params)) {
                opResult.setArtRetweeterStatusCode(OperationResult.DB_ERROR);
                opResult.setArtRetweeterStatusMessage("Failed to update database with tweet batch!");
                return opResult;
            }
        }
        opResult.setArtRetweeterStatusCode(OperationResult.QUERY_OK);
        opResult.setReturnedObject(statuses);
        return opResult;
    }

    public static OperationResult getTweetByID(Long tweetID, Account account, Path tweetFolderPath, boolean downloadAndStore) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", String.valueOf(tweetID)));
        OperationResult apiCallResult = apiCall(nvps, Endpoint.STATUSES_SHOW, account);
        if (!apiCallResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            return apiCallResult;
        }
        OperationResult opResult = new OperationResult();
        Gson gson = new Gson();
        JsonObject responseJSON = apiCallResult.getResponseJSONObject();
        StatusJSON status = gson.fromJson(responseJSON, StatusJSON.class);

        if (downloadAndStore) {
            OperationResult tweetParamsResult = status.downloadAndGetDBParams(tweetFolderPath);
            if (tweetParamsResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
                Object[] tweetParams = (Object[]) tweetParamsResult.getReturnedObject();
                CoreDB.insertTweet(tweetParams);
                opResult.setReturnedObject(status);
                opResult.setArtRetweeterStatusCode(OperationResult.QUERY_OK);
            }
            return opResult;
        } else {
            apiCallResult.setReturnedObject(status);
            return apiCallResult;
        }

    }

    public static OperationResult collectionDestroy(String collectionID, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", collectionID));
        OperationResult apiCallResult = apiCall(nvps, Endpoint.COLLECTIONS_DESTROY, account);
        if (!apiCallResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            return apiCallResult;
        }
        OperationResult opResult = new OperationResult();
        JsonObject responseJSON = apiCallResult.getResponseJSONObject();
        String destroyed = responseJSON.get("destroyed").getAsString();
        if (destroyed.toLowerCase().equals("true")) {
            opResult.setArtRetweeterStatusCode(OperationResult.QUERY_OK);
        } else {
            opResult.setArtRetweeterStatusCode(OperationResult.TWITTER_API_ERROR);
        }
        return opResult;

    }

    public static OperationResult collectionCreate(String name, String description, CollectionOrdering ordering, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("name", name));
        nvps.add(new BasicNameValuePair("description", description));
        nvps.add(new BasicNameValuePair("timeline_order", ordering.getParameterName()));
        OperationResult apiCallResult = apiCall(nvps, Endpoint.COLLECTIONS_CREATE, account);
        if (!apiCallResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            return apiCallResult;
        }
        OperationResult opResult;
        JsonObject responseJSON = apiCallResult.getResponseJSONObject();
        TwitterCollectionHolder holder;
        String id = responseJSON.get("response").getAsJsonObject().get("timeline_id").getAsString();
        opResult = collectionShow(id, account);
        if (!opResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            LOGGER.info("Failed to show collection - manually creating collection object.");
            String collectionURL = "https://twitter.com/".concat(account.getScreenName()).concat("/timelines/")
                    .concat(id.substring(id.indexOf("-") + 1));
            holder = new TwitterCollectionHolder()
                    .setTwitterID(id)
                    .setName(name)
                    .setCollectionURL(collectionURL)
                    .setDescription(description)
                    .setOrdering(CollectionOrdering.CURATION_REVERSE_CHRON);
            opResult.setReturnedObject(holder);
        }
        opResult.setArtRetweeterStatusCode(OperationResult.QUERY_OK);
        return opResult;
    }

    public static OperationResult collectionCurate(String jsonData, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("json_data", jsonData));
        OperationResult apiCallResult = apiCall(nvps, Endpoint.COLLECTIONS_CURATE, account);
        if (!apiCallResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            return apiCallResult;
        }
        OperationResult opResult = new OperationResult();
        JsonObject responseJSON = apiCallResult.getResponseJSONObject();
        JsonObject innerResponseJSON = responseJSON.get("response").getAsJsonObject();
        if (innerResponseJSON.keySet().contains("errors")) {
            JsonArray errors = innerResponseJSON.get("errors").getAsJsonArray();
            if (errors.size() == 0) {
                opResult.setArtRetweeterStatusCode(OperationResult.QUERY_OK);
            } else {
                opResult.setArtRetweeterStatusCode(OperationResult.TWITTER_API_ERROR);
            }
        }

        return opResult;

    }

    public static OperationResult requestRequestToken() {
        List<NameValuePair> nvps = new ArrayList<>();
        String requestParameters = "['oauth_callback' => 'oob']";
        nvps.add(new BasicNameValuePair("requestendpoint", "oauth/request_token"));
        nvps.add(new BasicNameValuePair("requestparameters", requestParameters));
        LOGGER.debug("API Call");
        OperationResult apiCallResult = apiCall(nvps, Endpoint.REQUEST_TOKEN, null);
        if (!apiCallResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            return apiCallResult;
        }
        LOGGER.debug("Finished API Call.");
        JsonObject responseJSON = apiCallResult.getResponseJSONObject();
        OperationResult result = new OperationResult();
        RequestToken requestToken;

        String token = responseJSON.get("oauth_token").getAsString();
        String tokenSecret = responseJSON.get("oauth_token_secret").getAsString();
        Boolean callbackConfirmed = responseJSON.get("oauth_callback_confirmed").getAsBoolean();
        requestToken = new RequestToken(token, tokenSecret, callbackConfirmed);
        result.setArtRetweeterStatusCode(OperationResult.QUERY_OK);
        result.setReturnedObject(requestToken);
        return result;

    }

    public static OperationResult requestAccessToken(String pin, RequestToken token) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("oauth_token", token.getToken()));
        nvps.add(new BasicNameValuePair("oauth_verifier", pin));
        OperationResult apiCallResult = apiCall(nvps, Endpoint.ACCESS_TOKEN, null);
        if (!apiCallResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getResponseJSONObject();
        OperationResult res = new OperationResult();
        Account account;

        String accessTokenString = responseJSON.get("oauth_token").getAsString();
        String accessTokenSecret = responseJSON.get("oauth_token_secret").getAsString();
        Long userTwitterID = responseJSON.get("user_id").getAsLong();
        String screenName = responseJSON.get("screen_name").getAsString();
        account = new Account()
                .setScreenName(screenName)
                .setTwitterID(userTwitterID)
                .setToken(accessTokenString)
                .setTokenSecret(accessTokenSecret);

        res.setArtRetweeterStatusCode(OperationResult.QUERY_OK);
        res.setReturnedObject(account);
        return res;
    }

    public static OperationResult getCollectionsByUserID(Long userID, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("user_id", String.valueOf(userID)));
        OperationResult apiCallResult = apiCall(nvps, Endpoint.COLLECTIONS_LIST, account);
        if (!apiCallResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getResponseJSONObject();
        OperationResult opResult = new OperationResult();
        ArrayList<TwitterCollectionHolder> collections = new ArrayList<>();
        Set<String> keys = responseJSON.keySet();
        if (keys.contains("objects")) {
            JsonObject objects = responseJSON.get("objects").getAsJsonObject();
            if (objects.keySet().contains("timelines")) {
                JsonObject timelines = responseJSON.get("objects").getAsJsonObject().get("timelines").getAsJsonObject();
                Set<String> tweetIDs = timelines.keySet();
                ArrayList<Object[]> params = new ArrayList<>();
                for (String s : tweetIDs) {
                    JsonObject o = timelines.get(s).getAsJsonObject();
                    TwitterCollectionHolder holder = new TwitterCollectionHolder()
                            .setTwitterID(s)
                            .setDescription(o.get("description").getAsString())
                            .setName(o.get("name").getAsString())
                            .setCollectionURL(o.get("collection_url").getAsString())
                            .setOrdering(CollectionOrdering.getOrdering(o.get("timeline_order").getAsString()));
                    collections.add(holder);
                    params.add(new Object[]{account.getTwitterID(), s, holder.getCollectionURL(), holder.getName(),
                        holder.getDescription(), holder.getOrdering().getParameterName()});
                }
                if (!params.isEmpty()) {
                    if (!CoreDB.parameterisedCollectionMergeBatch(params)) {

                    }
                }
            }
        } else {
            opResult.setArtRetweeterStatusCode(OperationResult.JSON_PARSE_ERROR);
            opResult.setArtRetweeterStatusMessage("Unable to parse JSON!");
            return opResult;
        }

        opResult.setArtRetweeterStatusCode(OperationResult.QUERY_OK);
        opResult.setReturnedObject(collections);
        return opResult;
    }

    public static OperationResult collectionShow(String collectionID, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", collectionID));
        OperationResult apiCallResult = apiCall(nvps, Endpoint.COLLECTIONS_SHOW, account);
        if (!apiCallResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            LOGGER.error("Failed to show collection with ID: " + collectionID);
            LOGGER.error("Operation result was: " + apiCallResult.getReadableStatusCode());
            return apiCallResult;
        }
        OperationResult opResult = new OperationResult();
        JsonObject responseJSON = apiCallResult.getResponseJSONObject();
        try {
            JsonObject obj = responseJSON.get("objects").getAsJsonObject().get("timelines").getAsJsonObject();
            String[] keys = obj.keySet().toArray(new String[obj.keySet().size()]);
            if (keys.length == 1 && keys[0].equals(collectionID)) {
                JsonObject collectionObject = obj.get(keys[0]).getAsJsonObject();
                String collectionURL = collectionObject.get("collection_url").getAsString();
                String name = collectionObject.get("name").getAsString();
                String description = collectionObject.get("description").getAsString();
                CollectionOrdering ordering = CollectionOrdering.getOrdering(collectionObject.get("timeline_order").getAsString());
                TwitterCollectionHolder holder = new TwitterCollectionHolder()
                        .setTwitterID(collectionID)
                        .setDescription(description)
                        .setName(name)
                        .setCollectionURL(collectionURL)
                        .setOrdering(ordering);
                opResult.setReturnedObject(holder);
            } else {
                LOGGER.error("Collection not found in response JSON!");
                opResult.setArtRetweeterStatusCode(OperationResult.MISC_ERROR);
                return opResult;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse JSON response", e);
            opResult.setArtRetweeterStatusCode(OperationResult.JSON_PARSE_ERROR);
            return opResult;
        }
        opResult.setArtRetweeterStatusCode(OperationResult.QUERY_OK);
        return opResult;

    }

    /**
     * Call this only when the collection is already present within the database.
     *
     * @param collectionID
     * @param account
     * @return
     */
    public static OperationResult getFullyHydratedCollectionByID(String collectionID, Account account) {
        OperationResult opResult = new OperationResult();
        Path tweetFolderPath = CoreDB.getTweetFolderPath(account);
        if (tweetFolderPath == null) {
            opResult.setArtRetweeterStatusCode(OperationResult.DB_ERROR);
            opResult.setArtRetweeterStatusMessage("Unable to retrieve tweet folder path from DB!");
            return opResult;
        }
        DBResponse collectionCheckResp = CoreDB.selectFromTable(DBTable.COLLECTIONS,
                new String[]{"collectionid"},
                new Object[]{collectionID});
        if (!collectionCheckResp.wasSuccessful() || collectionCheckResp.getReturnedRows().isEmpty()) {
            opResult.setArtRetweeterStatusCode(OperationResult.DB_ERROR);
            opResult.setArtRetweeterStatusMessage("Unable to retrieve collection information from DB!");
            return opResult;
        }

        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", collectionID));
        OperationResult apiCallResult = apiCall(nvps, Endpoint.COLLECTIONS_ENTRIES, account);
        if (!apiCallResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getResponseJSONObject();
        Set<String> keys = responseJSON.keySet();
        if (keys.contains("objects")) {
            JsonObject objects = responseJSON.get("objects").getAsJsonObject();
            if (objects.keySet().contains("tweets")) {
                JsonObject tweets = responseJSON.get("objects").getAsJsonObject().get("tweets").getAsJsonObject();
                Set<String> tweetIDs = tweets.keySet();
                ArrayList<Long> tweetIDsList = new ArrayList<>();
                for (String tweetID : tweetIDs) {
                    tweetIDsList.add(Long.valueOf(tweetID));
                }
                OperationResult statusesResult = RESTAPI.getTweetsByIDs(tweetIDsList, account, tweetFolderPath);
                if (!statusesResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
                    return statusesResult;
                }
                ArrayList<StatusJSON> statuses = (ArrayList<StatusJSON>) statusesResult.getReturnedObject();
                ArrayList<Object[]> tweetMergeParams = new ArrayList<>();
                ArrayList<Object[]> collectionTweetsMergeParams = new ArrayList<>();
                for (StatusJSON status : statuses) {
                    OperationResult res = status.downloadAndGetDBParams(tweetFolderPath);
                    if (res.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
                        Object[] params = (Object[]) res.getReturnedObject();
                        tweetMergeParams.add(params);
                        collectionTweetsMergeParams.add(new Object[]{status.getId(), collectionID});
                    }
                }
                CoreDB.deleteFromTable(DBTable.COLLECTIONTWEETS,
                        new String[]{"collectionid"},
                        new Object[]{collectionID});
                CoreDB.parameterisedTweetMergeBatch(tweetMergeParams);
                CoreDB.parameterisedCollectionTweetsMergeBatch(collectionTweetsMergeParams);
            }
        } else {
            opResult.setArtRetweeterStatusCode(OperationResult.JSON_PARSE_ERROR);
            opResult.setArtRetweeterStatusMessage("Unable to parse JSON!");
            return opResult;
        }

        opResult.setArtRetweeterStatusCode(OperationResult.QUERY_OK);
        return opResult;
    }

    public static Pair<OperationResult, ArrayList<StatusJSON>> getAllUnrecordedUserTweetsByDate(CloseableHttpClient httpclient,
            Account account, Long historicalMaxID, Long latestMaxID, Path tweetFolderPath, Long highestIDInDB) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("screen_name", account.getScreenName()));
        if (!historicalMaxID.equals(Long.MAX_VALUE) && !historicalMaxID.equals(0L)) {
            nvps.add(new BasicNameValuePair("max_id", String.valueOf(historicalMaxID)));
        } else if (latestMaxID != null && !latestMaxID.equals(Long.MAX_VALUE)) {
            nvps.add(new BasicNameValuePair("max_id", String.valueOf(latestMaxID)));
        }
        OperationResult apiCallResult = apiCall(nvps, Endpoint.USER_TIMELINE, account);
        if (!apiCallResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            return Pair.of(apiCallResult, null);
        }
        JsonArray responseJSON = apiCallResult.getResponseJSONArray();
        OperationResult result = new OperationResult();
        ArrayList<Object[]> params = new ArrayList<>();
        Gson gson = new Gson();
        ArrayList<StatusJSON> statuses = new ArrayList<>();
        Long highestID = 0L;

        StatusJSON[] receivedStatuses;
        try {
            receivedStatuses = gson.fromJson(responseJSON, StatusJSON[].class);
        } catch (Exception e1) {
            LOGGER.error("Failed to parse returned statuses JSON!", e1);
            result.setArtRetweeterStatusCode(OperationResult.JSON_PARSE_ERROR);
            result.setArtRetweeterStatusMessage("Failed to parse returned statuses JSON!");
            return Pair.of(result, null);
        }
        LOGGER.debug("Number of statuses received: " + receivedStatuses.length);
        for (StatusJSON status : receivedStatuses) {
            if (historicalMaxID > status.getId()) {
                historicalMaxID = status.getId();
            }
            if (highestID < status.getId()) {
                highestID = status.getId();
            }
            if (latestMaxID != null && status.getId() <= highestIDInDB) {
                break;
            }
            if (status.getIn_reply_to_screen_name() != null
                    && !status.getIn_reply_to_screen_name().equals(account.getScreenName())) {
                continue;
            }
            if (status.getExtended_entities() == null) {
                continue;
            }
            Integer extendedEntitiesLength = status.getExtended_entities().getMedia().length;
            if (extendedEntitiesLength == 0) {
                continue;
            } else if (!status.getExtended_entities().getMedia()[0].getType().equals("photo")) {
                continue;
            }
            if (status.getRetweeted_status() != null) {
                continue;
            }
            OperationResult tweetParamsResult = status.downloadAndGetDBParams(tweetFolderPath);
            if (tweetParamsResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
                params.add((Object[]) tweetParamsResult.getReturnedObject());
            } else {
                return Pair.of(tweetParamsResult, statuses);
            }

            statuses.add(status);
        }
        historicalMaxID--;
        highestID++;
        if (!params.isEmpty()) {
            if (!CoreDB.parameterisedTweetMergeBatch(params)) {
                result.setArtRetweeterStatusCode(OperationResult.DB_ERROR);
                result.setArtRetweeterStatusMessage("Failed to update database with tweet batch!");
                return Pair.of(result, statuses);
            }
        }
        result.setReturnedObject(Pair.of(historicalMaxID, highestID));
        result.setReceivedTweetCount(receivedStatuses.length);
        result.setStoredTweetCount(statuses.size());

        result.setArtRetweeterStatusCode(OperationResult.QUERY_OK);
        return Pair.of(result, statuses);
    }

    public static TreeSet<OrderedStatusHolder> sortStatusResultsByMetrics(ArrayList<StatusJSON> results, String metricToSortBy) {
        TreeSet<OrderedStatusHolder> set = new TreeSet<>();
        switch (metricToSortBy) {
            case "Likes":
                for (StatusJSON status : results) {
                    OrderedStatusHolder holder
                            = new OrderedByLikesStatusHolder(status.getFavorite_count(), status.getRetweet_count(), status);
                    set.add(holder);
                }
                return set;
            case "Retweets":
                for (StatusJSON status : results) {
                    OrderedStatusHolder holder
                            = new OrderedByRetweetsStatusHolder(status.getFavorite_count(), status.getRetweet_count(), status);
                    set.add(holder);
                }
                return set;
            default:
                return null;
        }
    }

}
