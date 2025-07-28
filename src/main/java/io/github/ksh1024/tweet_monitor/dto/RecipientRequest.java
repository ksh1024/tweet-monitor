package io.github.ksh1024.tweet_monitor.dto;

import lombok.Data;

@Data
public class RecipientRequest {
    private Integer id;
    private long twitterUserId;
    private String twitterScreenName;
    private String description;
    private boolean isActive;
}
