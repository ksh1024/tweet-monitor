package io.github.ksh1024.tweet_monitor.mapper;

import io.github.ksh1024.tweet_monitor.dto.KeywordRecipientDTO;
import io.github.ksh1024.tweet_monitor.dto.KeywordRequest;
import io.github.ksh1024.tweet_monitor.dto.RecipientRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMapper {
    // 모든 키워드-수신자 매핑 정보를 조회 (비활성화 포함)
    List<KeywordRecipientDTO> selectAllKeywordRecipientMappings();

    // 모든 키워드만 조회 (비활성화 포함)
    List<KeywordRecipientDTO> selectAllKeywords();

    // 모든 수신자만 조회 (비활성화 포함)
    List<KeywordRecipientDTO> selectAllRecipients();

    // 새로운 키워드 추가
    int insertKeyword(KeywordRequest keywordRequest);

    // 새로운 수신자 추가
    int insertRecipient(RecipientRequest recipientRequest);

    // 키워드-수신자 매핑 추가
    int insertKeywordRecipientMapping(@Param("keywordId") int keywordId, @Param("recipientId") int recipientId);

    // 키워드 수정
    int updateKeyword(KeywordRequest keywordRequest);

    // 수신자 수정
    int updateRecipient(RecipientRequest recipientRequest);

    // 키워드 삭제
    int deleteKeyword(@Param("id") int id);

    // 수신자 삭제
    int deleteRecipient(@Param("id") int id);

    // 키워드-수신자 매핑 삭제
    int deleteKeywordRecipientMapping(@Param("keywordId") int keywordId, @Param("recipientId") int recipientId);
}