package io.github.ksh1024.tweet_monitor.mapper;

import io.github.ksh1024.tweet_monitor.dto.KeywordRecipientDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KeywordMapper {
    // 활성화 상태(IS_ACTIVE)의 키워드와 매핑된 수신자 조회
    List<KeywordRecipientDTO> selectActiveKeywordsWithRecipients();

    // 특정 트윗 ID와 키워드 ID 조합이 PROCESSED_TWEETS 테이블에 있는지 조회
    boolean existsProcessedTweet(@Param("tweetId") long tweetId, @Param("keywordId") int keywordId);

    // PROCESSED_TWEETS 테이블에 새로운 처리 기록을 삽입
    // Mybatis는 기본적으로 INSERT 후 성공한 행의 개수를 반환한다
    int insertProcessedTweet(@Param("tweetId") long tweetId, @Param("keywordId") int keywordId);

    // SERVICE_STATE 테이블에서 마지막 처리 트윗 ID (ID=1 레코드) 조회
    Long selectLastTweetId(); // 결과가 없을 수도 있으니 Long (null 허용)으로 반환받음

    // SERVICE_STATE 테이블에 마지막 처리 트윗 ID (ID=1 레코드) 업데이트
    void updateLastTweetId(@Param("lastTweetId") long lastTweetId);

    // SERVICE_STATE 테이블에 초기 레코드 (ID=1, LAST_TWEET_ID=0) 삽입
    // INSERT IGNORE 구문을 사용하므로 이미 존재해도 오류 없이 0 또는 1 반환
    int insertInitialServiceState(@Param("initialTweetId") long initialTweetId);
}