package com.twitter.sdk.android.core.internal;

import com.google.gson.annotations.SerializedName;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.models.User;

import java.util.List;
import java.util.Map;

/**
 * TwitterCollection is a new type of timeline you control: you create the collection, give it a
 * name, and select which Tweets to add, either by hand or programmatically using the REST API.
 */
public class TwitterCollection {

    public TwitterCollection(Content contents, Metadata metadata) {
        this.contents = contents;
        this.metadata = metadata;
    }

    @SerializedName("objects")
    public final Content contents;

    @SerializedName("response")
    public final Metadata metadata;

    /**
     * Contents represent the grouped, decomposed collection objects (tweets, users).
     */
    public static final class Content {
        /**
         * Represents the mapping from string Tweet ids to user-trimmed Tweets.
         */
        @SerializedName("tweets")
        public final Map<Long, Tweet> tweetMap;

        /**
         * Represents the mapping from string user ids to Users who authored Tweets or Timelines.
         */
        @SerializedName("users")
        public final Map<Long, User> userMap;

        public Content(Map<Long, Tweet> tweetMap, Map<Long, User> userMap) {
            this.tweetMap = tweetMap;
            this.userMap = userMap;
        }
    }

    /**
     * Metadata lists references to decomposed objects and contextual information (such as cursors)
     * needed to navigate the boundaries of the collection in subsequent requests.
     */
    public static final class Metadata {

        public Metadata(String timelineId, Position position, List<TimelineItem> timelines) {
            this.timelineId = timelineId;
            this.position = position;
            this.timelineItems = timelines;
        }

        /**
         * The collection object identifier (e.g. "custom-393773270547177472")
         */
        @SerializedName("timeline_id")
        public final String timelineId;

        @SerializedName("position")
        public final Position position;

        /**
         * The ordered set of Collection items.
         */
        @SerializedName("timeline")
        public final List<TimelineItem> timelineItems;

        /**
         * Position information for navigation.
         */
        public static final class Position {

            /**
             * The exclusive minimum position value of the results (positions will be greater than
             * this value).
             */
            @SerializedName("min_position")
            public final Long minPosition;

            /**
             * The inclusive maximum position value of the results (positions will be less than or
             * equal to this value).
             */
            @SerializedName("max_position")
            public final Long maxPosition;

            public Position(Long maxPosition, Long minPosition) {
                this.maxPosition = maxPosition;
                this.minPosition = minPosition;
            }
        }
    }

    /**
     * Represents an item in a Timeline with a object references.
     */
    public static class TimelineItem {

        public TimelineItem(TweetItem tweetItem) {
            this.tweetItem = tweetItem;
        }

        /**
         * Represents a reference to a Tweet.
         */
        @SerializedName("tweet")
        public final TweetItem tweetItem;

        public static final class TweetItem {

            public TweetItem(Long id) {
                this.id = id;
            }

            /**
             * A Tweet id.
             */
            @SerializedName("id")
            public final Long id;
        }
    }
}
