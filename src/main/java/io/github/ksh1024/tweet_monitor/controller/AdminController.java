package io.github.ksh1024.tweet_monitor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    /**
     * 관리자 페이지
     */
    @GetMapping("/")
    public String adminIndex() {
        log.info("[AdminController] 관리자 메인 페이지 접근 시도");
        return "admin/dashboard";
    }
}