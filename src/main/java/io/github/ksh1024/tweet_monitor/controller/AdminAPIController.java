package io.github.ksh1024.tweet_monitor.controller;

import io.github.ksh1024.tweet_monitor.dto.KeywordRequest;
import io.github.ksh1024.tweet_monitor.dto.RecipientRequest;
import io.github.ksh1024.tweet_monitor.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // 키워드 수정 API (PUT /admin/keywords/{id})
    @PutMapping("/keywords/{id}")
    public ResponseEntity<Map<String, String>> updateKeyword(@PathVariable int id, @RequestBody KeywordRequest request) {
        log.info("[AdminRestController] 키워드 ID {} 수정 요청. 새 값: {}", id, request.getKeywordText());
        // 요청 DTO의 ID가 PathVariable과 일치하도록 설정 (안전성)
        request.setId(id);
        try {
            int result = adminService.updateKeyword(request);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "키워드 수정 성공", "status", "success"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "키워드 수정 실패 (ID 없음 또는 DB 오류)", "status", "fail"));
            }
        } catch (Exception e) {
            log.error("[AdminRestController] 키워드 ID {} 수정 중 오류 발생: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("message", "키워드 수정 중 서버 오류 발생: " + e.getMessage(), "status", "error"));
        }
    }

    // 수신자 수정 API (PUT /admin/recipients/{id})
    @PutMapping("/recipients/{id}")
    public ResponseEntity<Map<String, String>> updateRecipient(@PathVariable int id, @RequestBody RecipientRequest request) {
        log.info("[AdminRestController] 수신자 ID {} 수정 요청. 새 값: {}", id, request.getTwitterScreenName());
        request.setId(id);
        try {
            int result = adminService.updateRecipient(request);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "수신자 수정 성공", "status", "success"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "수신자 수정 실패 (ID 없음 또는 DB 오류)", "status", "fail"));
            }
        } catch (Exception e) {
            log.error("[AdminRestController] 수신자 ID {} 수정 중 오류 발생: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("message", "수신자 수정 중 서버 오류 발생: " + e.getMessage(), "status", "error"));
        }
    }

    // 키워드 삭제 API (DELETE /admin/keywords/{id})
    @DeleteMapping("/keywords/{id}")
    public ResponseEntity<Map<String, String>> deleteKeyword(@PathVariable int id) {
        log.info("[AdminRestController] 키워드 ID {} 삭제 요청.", id);
        try {
            int result = adminService.deleteKeyword(id);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "키워드 삭제 성공", "status", "success"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "키워드 삭제 실패 (ID 없음 또는 DB 오류)", "status", "fail"));
            }
        } catch (Exception e) {
            log.error("[AdminRestController] 키워드 ID {} 삭제 중 오류 발생: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("message", "키워드 삭제 중 서버 오류 발생: " + e.getMessage(), "status", "error"));
        }
    }

    // 수신자 삭제 API (DELETE /admin/recipients/{id})
    @DeleteMapping("/recipients/{id}")
    public ResponseEntity<Map<String, String>> deleteRecipient(@PathVariable int id) {
        log.info("[AdminRestController] 수신자 ID {} 삭제 요청.", id);
        try {
            int result = adminService.deleteRecipient(id);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "수신자 삭제 성공", "status", "success"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "수신자 삭제 실패 (ID 없음 또는 DB 오류)", "status", "fail"));
            }
        } catch (Exception e) {
            log.error("[AdminRestController] 수신자 ID {} 삭제 중 오류 발생: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("message", "수신자 삭제 중 서버 오류 발생: " + e.getMessage(), "status", "error"));
        }
    }

    // 키워드-수신자 매핑 삭제 API (DELETE /admin/mappings/{keywordId}/{recipientId})
    @DeleteMapping("/mappings/{keywordId}/{recipientId}")
    public ResponseEntity<Map<String, String>> deleteMapping(@PathVariable int keywordId, @PathVariable int recipientId) {
        log.info("[AdminRestController] 키워드 ID {}와 수신자 ID {} 매핑 삭제 요청.", keywordId, recipientId);
        try {
            int result = adminService.deleteKeywordRecipientMapping(keywordId, recipientId);
            if (result > 0) {
                return ResponseEntity.ok(Map.of("message", "매핑 삭제 성공", "status", "success"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "매핑 삭제 실패 (매핑 없음 또는 DB 오류)", "status", "fail"));
            }
        } catch (Exception e) {
            log.error("[AdminRestController] 매핑 ID {}와 수신자 ID {} 삭제 중 오류 발생: {}", keywordId, recipientId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("message", "매핑 삭제 중 서버 오류 발생: " + e.getMessage(), "status", "error"));
        }
    }

}
