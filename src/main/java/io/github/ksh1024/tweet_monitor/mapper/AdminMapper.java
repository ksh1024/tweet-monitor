package io.github.ksh1024.tweet_monitor.mapper;

import io.github.ksh1024.tweet_monitor.dto.KeywordRecipientDTO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface AdminMapper {
    // 모든 키워드-수신자 매핑 정보를 조회 (비활성화 포함)
    List<KeywordRecipientDTO> selectAllKeywordRecipientMappings();

    // 모든 키워드만 조회 (비활성화 포함)
    List<KeywordRecipientDTO> selectAllKeywords();

    // 모든 수신자만 조회 (비활성화 포함)
    List<KeywordRecipientDTO> selectAllRecipients();
}