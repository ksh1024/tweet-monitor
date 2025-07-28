package io.github.ksh1024.tweet_monitor.dto;

import lombok.*;

@Data
public class KeywordRequest {
    private Integer id;
    private String keywordText;
    private boolean isActive;
}