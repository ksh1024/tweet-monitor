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

    // DB에서 키워드와 수신자 정보 로딩하여 메모리 맵에 저장
    // @PostConstruct 시점과 주기적 새로고침 스케줄(@Scheduled)에서 호출된다
    private void loadKeywordsAndRecipients() {
        log.info("Loading active keywords and recipients from DB...");
        // DB에서 활성화된 키워드와 매핑된 수신자 정보 조회
        List<KeywordRecipientDTO> mappings = keywordMapper.selectActiveKeywordsWithRecipients();

        // 기존 메모리 맵을 비우고 새로 채움 (DB 설정 변경이 있다면 반영하기 위함)
        keywordToRecipientUserIds.clear();
        keywordTextToId.clear();

        mappings.forEach(mapping -> {
            // keywordToRecipientUserIds 맵 채우기:
            // 키워드 텍스트를 키로 하고, 해당 키워드에 연결된 수신자 User ID 리스트를 값으로 가진다
            keywordToRecipientUserIds
                    .computeIfAbsent(mapping.getKeywordText(), k -> new java.util.ArrayList<>()) // 키가 없으면 새 리스트 생성
                    .add(mapping.getRecipientTwitterUserId()); // 수신자 User ID 추가

            // keywordTextToId 맵 채우기:
            // 키워드 텍스트를 키로 하고, 해당 키워드의 DB ID를 값으로 가진다 (중복 확인 시 필요)
            keywordTextToId.put(mapping.getKeywordText(), mapping.getKeywordId());
        });

        log.info("Loaded {} active keywords. Total {} recipient mappings found.",
                keywordTextToId.size(), mappings.size());
        // 로딩된 키워드 목록을 로깅
        keywordToRecipientUserIds.forEach((keyword, recipients) ->
                log.debug("Keyword: '{}', Recipients Count: {}", keyword, recipients.size())
        );
    }

    // 5분마다 Twitter Search API를 사용하여 트윗 모니터링 및 처리
    // Twitter API 호출(DM 발송)은 트랜잭션의 영향을 받지 않는다
    @Scheduled(cron = "0 */5 * * * *") // 매 5분 정각에 실행
    @Transactional
    public void performScheduledSearch() {
        // 메모리에 로딩된 키워드 목록이 비어 있으면 모니터링 건너뛰기
        if (keywordToRecipientUserIds.isEmpty()) {
            log.warn("No active keywords configured. Skipping scheduled search.");
            return;
        }
        log.info("Performing scheduled Twitter search...");
        // DB에서 lastTweetId 읽어오기 (트랜잭션 범위 내에서 실행)
        // selectLastTweetId()는 결과가 없으면 null을 반환한다
        Long currentLastTweetId = keywordMapper.selectLastTweetId();
        // null이면 초기값 0L 사용 (SERVICE_STATE 초기 삽입 로직으로 보통 null이 되지 않음)
        if (currentLastTweetId == null) {
            currentLastTweetId = 0L;
            log.warn("SERVICE_STATE record not found on search execution, using default: {}", currentLastTweetId);
            // init()에서 insertInitialServiceState(0L)를 호출하므로 이 블록에 들어올 가능성은 낮다
        }
        log.debug("Loaded lastTweetId from DB: {}", currentLastTweetId);

        // 모든 활성 키워드를 OR 조건으로 묶어 Search 쿼리 문자열 생성
        String searchQueryString = keywordToRecipientUserIds.keySet().stream()
                // 각 키워드를 따옴표로 감싸 정확한 단어 검색 유도
                .map(keyword -> "\"" + keyword + "\"")
                .collect(Collectors.joining(" OR "));

        // 활성 키워드가 없는 경우에는 검색을 하지 않는다
        if (searchQueryString.isEmpty()) {
            log.warn("Generated search query is empty. Skipping search.");
            return;
        }

        Query query = new Query(searchQueryString);
        query.setCount(100); // 한 번에 가져올 트윗 최대 개수 (API 제한 확인)
        // DB에서 읽어온 lastTweetId 이후의 트윗만 가져오도록 설정 (중복 방지)
        if (currentLastTweetId > 0) {
            query.setSinceId(currentLastTweetId);
            log.debug("Setting Twitter Search API sinceId to {}", currentLastTweetId);
        } else {
            // lastTweetId가 0이면 sinceId 설정을 건너뛰고 최신 트윗부터 검색한다
            log.debug("lastTweetId is 0, searching from the beginning of available history.");
        }

        long newLastTweetId = currentLastTweetId; // 이번 검색 후 DB에 저장할 최신 트윗 ID

        try {
            // Twitter Search API 호출
            log.debug("Calling Twitter Search API with query: {}", query.getQuery());
            QueryResult result = twitter.search(query);
            List<Status> tweets = result.getTweets();

            log.info("Search for '{}' returned {} tweets.", searchQueryString, tweets.size());

            // 가져온 트윗 목록을 순회하며 처리 (가장 최신 트윗부터 처리하기 위해 트윗 ID 내림차순 정렬)
            tweets.sort((t1, t2) -> Long.compare(t2.getId(), t1.getId()));
            log.debug("Sorted tweets by ID descending. Processing...");

            // 가져온 트윗 목록 순회 및 개별 트윗 처리 호출
            for (Status status : tweets) {
                long tweetId = status.getId();

                // 검색 결과 중 가장 큰 트윗 ID를 newLastTweetId에 저장 (이번 스케줄 실행 후 DB에 저장할 값)
                // Search API의 특성상 중요한 로직 (가져온 트윗 중 가장 큰 ID를 다음 검색 기준으로 삼기 위함)
                if (tweetId > newLastTweetId) {
                    newLastTweetId = tweetId;
                }

                // 단일 트윗 처리 메서드 호출
                processSingleTweet(status, tweetId);
            }

            // DB에 업데이트된 lastTweetId 저장 (현재 트랜잭션 범위 내)
            // 이번 검색에서 발견된 가장 큰 트윗 ID가 기존 lastTweetId보다 크다면 업데이트
            if (newLastTweetId > currentLastTweetId) {
                log.info("New latest tweet ID found: {}. Updating DB.", newLastTweetId);
                keywordMapper.updateLastTweetId(newLastTweetId);
                log.info("Successfully updated lastTweetId in DB to {}.", newLastTweetId);
            } else {
                log.debug("No new tweets found or processed that are newer than {}. lastTweetId remains {}.", currentLastTweetId, currentLastTweetId);
            }
        } catch (TwitterException e) {
            log.error("Error during Twitter search: {}", e.getMessage(), e);
            // Twitter API 호출 중 발생한 예외 처리
            // @Transactional은 기본적으로 RuntimeException에 대해서만 롤백하므로,
            // TwitterException 발생 시 DB 트랜잭션은 커밋될 수 있다
            // 필요시 @Transactional(rollbackFor = TwitterException.class) 추가 고려
            // TODO: Twitter API rate limit 초과 등 특정 예외에 대한 상세 처리 로직 추가
        } catch (RuntimeException e) {
            // DB 작업 등에서 발생한 RuntimeException은 @Transactional에 의해 롤백된다
            log.error("RuntimeException occurred during scheduled search processing: {}", e.getMessage(), e);
            // 예외를 다시 던져서 Spring의 @Transactional이 롤백을 수행하도록 한다
            throw new RuntimeException("DB Transaction failed during scheduled search execution", e);
        }
    }

    // ================================================================
    // 단일 트윗 처리 메서드 (키워드 매칭, 중복 확인, DB 기록, DM 발송)
    // performScheduledSearch 메서드 내에서 각 트윗별로 호출됨
    // @Transactional 메서드 내에서 호출되므로 동일 트랜잭션 범위에 포함됨
    // ================================================================

    private void processSingleTweet(Status status, long tweetId) {
        String tweetText = status.getText();
        String screenName = status.getUser().getScreenName();
//        long userId = status.getUser().getId(); // 트윗 작성자 ID

        log.debug("Processing single tweet [{}]: '{}' by @{}", tweetId, tweetText, screenName);

        // 트윗 텍스트를 소문자로 변환하여 비교 (대소문자 무시)
        String lowerCaseTweetText = tweetText.toLowerCase();

        // 이 트윗이 어떤 키워드에 매칭되는지 확인 (다중 키워드 매칭 가능)
        keywordToRecipientUserIds.forEach((keyword, recipientUserIds) -> {
            // 트윗 내용이 현재 순회 중인 키워드를 포함하는지 확인
            if (lowerCaseTweetText.contains(keyword.toLowerCase())) {
                // 이 트윗이 이 키워드와 매칭되었음을 확인
                log.debug("Tweet [{}] matched keyword: '{}'", tweetId, keyword);

                // 매칭된 키워드 텍스트에 해당하는 키워드 ID 가져오기 (메모리 맵에서 조회)
                Integer matchedKeywordId = keywordTextToId.get(keyword);

                if (matchedKeywordId == null) {
                    // 이런 경우는 발생하면 안 되지만, 데이터 불일치 시 방어 로직
                    log.error("Could not find ID for matched keyword text: '{}'. Skipping processing for this keyword on tweet [{}].", keyword, tweetId);
                    return; // 다음 키워드로 넘어감 (forEach 람다에서는 continue 역할)
                }

                // DB에서 이미 처리된 트윗인지 확인 (트윗 ID와 매칭된 키워드 ID 조합)
                // 현재 @Transactional 메서드 내에서 실행되므로, 이전 DB 작업 결과가 반영된 상태에서 조회됨
                boolean alreadyProcessed = keywordMapper.existsProcessedTweet(tweetId, matchedKeywordId);

                if (!alreadyProcessed) {
                    // 이 트윗-키워드 조합은 처음 발견된 경우
                    log.info("Tweet [{}] is new for keyword '{}' (ID: {}). Recording and sending DMs.", tweetId, keyword, matchedKeywordId);

                    // DB에 처리 기록 삽입 및 DM 발송 시도
                    try {
                        // DB에 처리 기록 삽입 (Mybatis 매퍼 호출)
                        int insertedRows = keywordMapper.insertProcessedTweet(tweetId, matchedKeywordId);

                        if (insertedRows > 0) { // 삽입 성공 (UNIQUE 제약 등에 의해 실패할 수 있음)
                            log.info("Processed tweet [{}] recorded in DB for keyword '{}'.", tweetId, keyword);

                            // 해당 키워드의 수신자 목록 가져오기 (메모리 맵에서 조회)
                            List<Long> recipientUserIdsList = keywordToRecipientUserIds.get(keyword);

                            if (recipientUserIdsList != null && !recipientUserIdsList.isEmpty()) {
                                String tweetUrl = "https://x.com/" + status.getUser().getScreenName() + "/status/" + status.getId(); // 트윗 링크 생성
                                String dmText = String.format(
                                        "키워드 '%s' 포함 트윗 발견!\n작성자: @%s\n내용: %s\n링크: %s",
                                        keyword, status.getUser().getScreenName(), status.getText(), tweetUrl
                                );

                                // Twitter API Plan에 따른 조건부 DM 발송/로깅 로직
                                if ("free".equalsIgnoreCase(twitterApiPlan)) {
                                    // Plan이 'free'인 경우 실제 DM 발송 대신 로깅만 함
                                    log.info("[Simulated DM] Would send DM for tweet [{}] to recipients: {}. DM Content: {}",
                                            tweetId, recipientUserIdsList, dmText);
                                } else if ("basic".equalsIgnoreCase(twitterApiPlan) || "pro".equalsIgnoreCase(twitterApiPlan)) {
                                    // Plan이 유료인 경우 실제 DM 발송 시도
                                    log.info("[Actual DM] Attempting to send DM for tweet [{}] to recipients: {} (Plan: {})...",
                                            tweetId, recipientUserIdsList, twitterApiPlan);
                                    recipientUserIdsList.forEach(recipientId -> {
                                        try {
                                            // Twitter API를 사용하여 DM 발송 (Twitter4J twitter 객체 사용)
                                            twitter.sendDirectMessage(recipientId, dmText);
                                            log.info("DM sent for tweet [{}] to recipient User ID {}.", tweetId, recipientId);
                                        } catch (TwitterException e) {
                                            log.error("Failed to send DM for tweet [{}] to recipient User ID {}: {}", tweetId, recipientId, e.getMessage());
                                            // DM 발송 실패 시 로깅만 하고 계속 진행 (이 예외는 @Transactional 롤백을 유발하지 않음)
                                        }
                                    });
                                } else {
                                    // 알 수 없는 Plan 값인 경우 경고 로깅
                                    log.warn("Unknown twitter.api.plan '{}'. Skipping DM sending for tweet [{}].", twitterApiPlan, tweetId);
                                }
                            } else {
                                log.warn("No active recipients found for keyword '{}'. Skipping DM sending for tweet [{}].", keyword, tweetId);
                            }
                        } else {
                            log.error("Failed to record processed tweet [{}] in DB for keyword '{}'. insertProcessedTweet returned 0.", tweetId, keyword);
                            // DB 삽입 실패 시 (UNIQUE 제약 충돌 등으로 인해) -> RuntimeException을 발생시켜 트랜잭션 롤백 유도
                            throw new RuntimeException("Failed to insert processed tweet record for tweet " + tweetId + " and keyword " + matchedKeywordId);
                        }
                    } catch (Exception e) { // insertProcessedTweet 중 DB 오류 등
                        log.error("Error during DB record or DM sending attempt for tweet [{}] and keyword '{}': {}", tweetId, keyword, e.getMessage());
                        // RuntimeException을 다시 던져서 @Transactional에 의해 롤백되도록 한다
                        throw new RuntimeException("Error processing tweet " + tweetId + " for keyword " + matchedKeywordId, e);
                    }
                } else {
                    // 이 트윗-키워드 조합은 이미 처리된 경우
                    log.debug("Tweet [{}] already processed for keyword '{}'. Skipping.", tweetId, keyword);
                }
            }
        });
    }

}