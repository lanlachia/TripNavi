package com.tripnavi;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/inquiries")
public class InquiryController {
    private final InquiryRepository inquiries;
    private final UserRepository users;

    public InquiryController(InquiryRepository inquiries, UserRepository users) {
        this.inquiries = inquiries;
        this.users = users;
    }

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody Map<String, String> body, HttpSession session) {
        Object userId = session.getAttribute("tripnaviUserId");
        if (!(userId instanceof Long)) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        String title = Objects.toString(body.get("title"), "").trim();
        String message = Objects.toString(body.get("message"), "").trim();
        if (title.isEmpty() || message.isEmpty() || title.length() > 100 || message.length() > 2000)
            return ResponseEntity.badRequest().body(Map.of("message", "제목(100자 이하)과 문의 내용(2000자 이하)을 입력해 주세요."));
        Inquiry inquiry = new Inquiry();
        inquiry.setUserId((Long) userId);
        inquiry.setTitle(title);
        inquiry.setMessage(message);
        inquiries.save(inquiry);
        return ResponseEntity.ok(Map.of("message", "문의가 접수되었습니다.", "id", inquiry.getId()));
    }

    @GetMapping("/admin")
    public ResponseEntity<?> adminList(HttpSession session) {
        Object userId = session.getAttribute("tripnaviUserId");
        if (!(userId instanceof Long)) return ResponseEntity.status(401).body(Map.of("message", "관리자 로그인이 필요합니다."));
        String adminEmail = System.getenv("TRIPNAVI_ADMIN_EMAIL");
        if (adminEmail == null || adminEmail.isBlank())
            return ResponseEntity.status(403).body(Map.of("message", "서버에 관리자 계정이 설정되지 않았습니다."));
        Optional<User> user = users.findById((Long) userId);
        if (user.isEmpty() || !adminEmail.equalsIgnoreCase(user.get().getEmail()))
            return ResponseEntity.status(403).body(Map.of("message", "관리자만 조회할 수 있습니다."));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Inquiry inquiry : inquiries.findAllByOrderByCreatedAtDesc()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", inquiry.getId());
            item.put("title", inquiry.getTitle());
            item.put("message", inquiry.getMessage());
            item.put("createdAt", inquiry.getCreatedAt().toString());
            Optional<User> author = users.findById(inquiry.getUserId());
            item.put("author", author.map(User::getName).orElse("탈퇴한 사용자"));
            item.put("email", author.map(User::getEmail).orElse("-"));
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }
}
