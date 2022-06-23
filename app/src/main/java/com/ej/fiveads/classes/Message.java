package com.ej.fiveads.classes;

import androidx.annotation.NonNull;

import java.util.Map;

public class Message {

    private final String username;
    private final String content;
    private final String uid;
    private final Map<String, String> timestamp;
    private final Long timestampLong;

    public Message() {
        this.username = "";
        this.content = "";
        this.uid = "";
        this.timestamp = null;
        this.timestampLong = null;
    }

    public Message(String username, String content, String uid, Map<String, String> timestamp) {
        this.content = content;
        this.username = username;
        this.uid = uid;
        this.timestamp = timestamp;
        this.timestampLong = null;
    }

    public Message(String username, String content, String userId, Long timestamp) {
        this.username = username;
        this.content = content;
        this.uid = userId;
        this.timestamp = null;
        this.timestampLong = timestamp;
        if (username == null && content == null && userId == null && timestampLong == null) {
            throw new IllegalStateException("Not a valid message");
        }
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }

    public String getUid() {
        return uid;
    }

    public Map<String, String> getTimestamp() {
        return timestamp;
    }

    public Long getTimestampLong() {
        return timestampLong;
    }

    @NonNull
    @Override
    public String toString() {
        return "Message{" +
                "username='" + username + '\'' +
                ", content='" + content + '\'' +
                ", uid='" + uid + '\'' +
                ", timestamp=" + timestamp + timestampLong +
                '}';
    }
}
