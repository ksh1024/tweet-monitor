package io.github.ksh1024.tweet_monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class TweetMonitorApplication {

	public static void main(String[] args) {
		SpringApplication.run(TweetMonitorApplication.class, args);
	}

}
