package io.github.ksh1024.tweet_monitor.service;

import io.github.ksh1024.tweet_monitor.dto.KeywordRecipientDTO;
import io.github.ksh1024.tweet_monitor.dto.KeywordRequest;
import io.github.ksh1024.tweet_monitor.dto.RecipientRequest;
import io.github.ksh1024.tweet_monitor.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminMapper adminMapper;
    private final TwitterMonitorService twitterMonitorService;

    // 모든 키워드 정보 조회 (비활성화 포함)
    public List<KeywordRecipientDTO> getAllKeywords() {
        List<KeywordRecipientDTO> keywords = adminMapper.selectAllKeywords();
        log.info("[AdminService] 총 {}개의 키워드 정보 조회 완료", keywords.size());
        return keywords;
    }

    // 모든 수신자 정보 조회 (비활성화 포함)
    public List<KeywordRecipientDTO> getAllRecipients() {
        List<KeywordRecipientDTO> recipients = adminMapper.selectAllRecipients();
        log.info("[AdminService] 총 {}개의 수신자 정보 조회 완료", recipients.size());
        return recipients;
    }

    // 키워드별로 수신자 목록을 그룹화하여 반환하는 메서드
    // Map<키워드 텍스트, 해당 키워드를 받는 수신자 DTO 리스트>
    public Map<String, List<KeywordRecipientDTO>> getGroupedKeywordRecipientMappings() {
        List<KeywordRecipientDTO> allMappings = adminMapper.selectAllKeywordRecipientMappings();

        // Java Stream API를 사용하여 keywordText를 기준으로 그룹화
        Map<String, List<KeywordRecipientDTO>> groupedMap = allMappings.stream()
                .collect(Collectors.groupingBy(KeywordRecipientDTO::getKeywordText));

        log.info("[AdminService] 총 {}개의 키워드 그룹으로 매핑 정보 그룹화 완료", groupedMap.size());
        return groupedMap;
    }

    // 새로운 키워드 추가
    @Transactional
    public int addKeyword(KeywordRequest request) {
        log.info("[AdminService] 새 키워드 추가 시도: {}", request.getKeywordText());
        int result = adminMapper.insertKeyword(request);
        if (result > 0) {
            log.info("[AdminService] 키워드 '{}' 추가 성공.", request.getKeywordText());
            // 키워드 추가 후 TwitterMonitorService의 키워드 목록 새로고침
            twitterMonitorService.loadKeywordsAndRecipients();
        } else {
            log.warn("[AdminService] 키워드 '{}' 추가 실패.", request.getKeywordText());
        }
        return result;
    }

    // 새로운 수신자 추가
    @Transactional
    public int addRecipient(RecipientRequest request) {
        log.info("[AdminService] 새 수신자 추가 시도: {} (@{})", request.getTwitterUserId(), request.getTwitterScreenName());
        int result = adminMapper.insertRecipient(request);
        if (result > 0) {
            log.info("[AdminService] 수신자 '{}' 추가 성공.", request.getTwitterScreenName());
            // 수신자 추가 후 TwitterMonitorService의 수신자 목록 새로고침
            twitterMonitorService.loadKeywordsAndRecipients();
        } else {
            log.warn("[AdminService] 수신자 '{}' 추가 실패.", request.getTwitterScreenName());
        }
        return result;
    }

    // 키워드-수신자 매핑 추가
    @Transactional
    public int addKeywordRecipientMapping(int keywordId, int recipientId) {
        log.info("[AdminService] 키워드 ID {}와 수신자 ID {} 매핑 추가 시도.", keywordId, recipientId);
        int result = adminMapper.insertKeywordRecipientMapping(keywordId, recipientId);
        if (result > 0) {
            log.info("[AdminService] 키워드 ID {}와 수신자 ID {} 매핑 성공.", keywordId, recipientId);
            // 매핑 추가 후 TwitterMonitorService의 목록 새로고침
            twitterMonitorService.loadKeywordsAndRecipients();
        } else {
            log.warn("[AdminService] 키워드 ID {}와 수신자 ID {} 매핑 실패.", keywordId, recipientId);
        }
        return result;
    }

}