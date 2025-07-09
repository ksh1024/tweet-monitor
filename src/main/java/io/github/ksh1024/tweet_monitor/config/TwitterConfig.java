package io.github.ksh1024.tweet_monitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

@Configuration
public class TwitterConfig {
    @Value("${twitter.api.consumerKey}")
    private String consumerKey;

    @Value("${twitter.api.consumerSecret}")
    private String consumerSecret;

    @Value("${twitter.api.accessToken}")
    private String accessToken;

    @Value("${twitter.api.accessTokenSecret}")
    private String accessTokenSecret;

    @Bean
    public Twitter twitter() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(consumerKey)
                .setOAuthConsumerSecret(consumerSecret)
                .setOAuthAccessToken(accessToken)
                .setOAuthAccessTokenSecret(accessTokenSecret);

        return new TwitterFactory(cb.build()).getInstance();
    }

}