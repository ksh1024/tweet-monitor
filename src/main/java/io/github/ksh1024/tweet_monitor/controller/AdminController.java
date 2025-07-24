package io.github.ksh1024.tweet_monitor.controller;

import io.github.ksh1024.tweet_monitor.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    /**
     * 관리자 페이지
     */
    @GetMapping("/")
    public String adminDashboard(Model model) {
        log.info("[AdminController] 관리자 대시보드 페이지 접근 시도");
        model.addAttribute("groupedMappings", adminService.getGroupedKeywordRecipientMappings());
        model.addAttribute("keywords", adminService.getAllKeywords());
        model.addAttribute("recipients", adminService.getAllRecipients());

        return "admin/dashboard";
    }
}