package io.github.ksh1024.tweet_monitor.service;

import io.github.ksh1024.tweet_monitor.dto.KeywordRecipientDTO;
import io.github.ksh1024.tweet_monitor.mapper.KeywordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwitterMonitorService {
    private final KeywordMapper keywordMapper; // 데이터베이스 접근을 위한 Mybatis 매퍼
    private final Twitter twitter; // Twitter API 통신을 위한 Twitter4J 클라이언트

    // application.properties에서 twitter.api.plan 값을 주입받는다
    @Value("${twitter.api.plan}")
    private String twitterApiPlan;

    private Map<String, List<Long>> keywordToRecipientUserIds = new HashMap<>();
    private Map<String, Integer> keywordTextToId = new HashMap<>();


    // ================================================================
    // 애플리케이션 초기화 및 종료 관련 메서드
    // ================================================================

    // 애플리케이션(Spring Context)이 완전히 초기화된 후 자동으로 실행될 메서드
    @PostConstruct
    public void init() {
        log.info("Initializing Twitter Monitor Service...");

        // SERVICE_STATE 테이블에 초기 레코드 (ID=1, LAST_TWEET_ID=0) 삽입 시도
        // Mybatis Mapper를 통해 DB에 접근하며, INSERT IGNORE 구문을 사용하므로
        // 테이블이 비어있을 때만 이 레코드가 삽입되고, 이미 존재하면 무시된다
        keywordMapper.insertInitialServiceState(0L);
        log.info("Ensured initial SERVICE_STATE record exists.");

        // DB에서 키워드 및 수신자 정보를 로딩하는 메서드를 호출
        loadKeywordsAndRecipients();

        log.info("Twitter Monitor Service initialized. Keywords and recipients loaded.");
    }

    // 애플리케이션(Spring Context)이 종료되기 직전에 실행될 메서드
    // 애플리케이션 종료 시 필요한 정리 작업(예: 스트림 연결 해제)을 수행한다
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Twitter Monitor Service...");
        // 현재 Search API 방식을 사용하므로 Twitter 객체나 관련 리소스에 특별한 정리 작업이 필요없을 수 있다
        // 만약 Twitter Streaming API를 사용한다면 여기서 연결을 끊는 코드가 필요하다
        log.info("Twitter Monitor Service shut down.");
    }

    // DB에서 키워드와 수신자 정보 로딩하여 메모리 맵에 저장할 메서드 시그니처
    // @PostConstruct 시점과 주기적 새로고침 스케줄(@Scheduled)에서 호출된다
    private void loadKeywordsAndRecipients() {
        // TODO: DB에서 키워드 및 수신자 정보를 로딩하는 로직 구현
    }

    // 5분마다 Twitter Search API를 사용하여 트윗을 모니터링하고 처리할 메서드 시그니처
    @Scheduled(cron = "0 */5 * * * *") // 매 5분 정각에 실행될 설정
    @Transactional
    public void performScheduledSearch() {
        // TODO: 트윗 검색, 필터링, DB 기록, DM 발송 로직 구현
    }

    // 단일 트윗을 처리할 보조 메서드 시그니처
    private void processSingleTweet(Status status, long tweetId) {
        // TODO: 키워드 매칭, 중복 확인, DB 기록, DM 발송 로직 구현
    }
}