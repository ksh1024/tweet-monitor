package io.github.ksh1024.tweet_monitor.dto;

import lombok.*;

@Data
public class KeywordRecipientDTO {
    private int keywordId;
    private String keywordText;
    private int recipientId;
    private long recipientTwitterUserId;
    private String recipientTwitterScreenName; // @사용자명
}