package io.github.ksh1024.tweet_monitor.controller;

import io.github.ksh1024.tweet_monitor.dto.KeywordRequest;
import io.github.ksh1024.tweet_monitor.dto.RecipientRequest;
import io.github.ksh1024.tweet_monitor.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class AdminAPIController {
    private final AdminService adminService;

    @PostMapping("/keywords")
    public ResponseEntity<Map<String, String>> addKeyword(@RequestBody KeywordRequest request) {
        log.info("[AdminRestController] 새 키워드 추가 요청: {}", request.getKeywordText());
        try {
            int result = adminService.addKeyword(request);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "키워드 추가 성공", "status", "success"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "키워드 추가 실패 (DB 오류)", "status", "fail"));
            }
        } catch (Exception e) {
            log.error("[AdminRestController] 키워드 추가 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("message", "키워드 추가 중 서버 오류 발생: " + e.getMessage(), "status", "error"));
        }
    }

    @PostMapping("/recipients")
    public ResponseEntity<Map<String, String>> addRecipient(@RequestBody RecipientRequest request) {
        log.info("[AdminRestController] 새 수신자 추가 요청: {} (@{})", request.getTwitterUserId(), request.getTwitterScreenName());
        try {
            int result = adminService.addRecipient(request);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "수신자 추가 성공", "status", "success"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "수신자 추가 실패 (DB 오류)", "status", "fail"));
            }
        } catch (Exception e) {
            log.error("[AdminRestController] 수신자 추가 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("message", "수신자 추가 중 서버 오류 발생: " + e.getMessage(), "status", "error"));
        }
    }

    @PostMapping("/mappings")
    public ResponseEntity<Map<String, String>> addMapping(@RequestBody Map<String, Integer> requestBody) {
        int keywordId = requestBody.get("keywordId");
        int recipientId = requestBody.get("recipientId");
        log.info("[AdminRestController] 키워드 ID {}와 수신자 ID {} 매핑 추가 요청.", keywordId, recipientId);
        try {
            int result = adminService.addKeywordRecipientMapping(keywordId, recipientId);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "매핑 추가 성공", "status", "success"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "매핑 추가 실패 (이미 존재하는 매핑 또는 DB 오류)", "status", "fail"));
            }
        } catch (Exception e) {
            log.error("[AdminRestController] 매핑 추가 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("message", "매핑 추가 중 서버 오류 발생: " + e.getMessage(), "status", "error"));
        }
    }

}
