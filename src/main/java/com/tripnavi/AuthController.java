package com.tripnavi;

import java.util.Map;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 카카오 OAuth 인가 코드를 서버에서 교환하고 TripNavi 세션을 생성합니다.
    @PostMapping("/kakao")
    public ResponseEntity<?> kakaoLogin(@RequestBody Map<String, String> input, HttpSession session) {
        String restKey = System.getenv("KAKAO_REST_API_KEY");
        if (restKey == null || restKey.isBlank()) {
            return ResponseEntity.status(503).body(Map.of("message", "서버에 KAKAO_REST_API_KEY 환경변수를 설정해 주세요."));
        }
        String code = input.getOrDefault("code", "");
        String redirectUri = input.getOrDefault("redirectUri", "");
        // 허용된 리다이렉트 주소만 사용 (클라이언트가 임의의 주소를 지정하지 못하게 함)
        String allowedRedirect = System.getenv("KAKAO_REDIRECT_URI");
        if (allowedRedirect == null || allowedRedirect.isBlank()) {
            return ResponseEntity.status(503).body(Map.of("message", "서버에 KAKAO_REDIRECT_URI 환경변수를 설정해 주세요."));
        }
        if (code.isBlank() || !allowedRedirect.equals(redirectUri)) {
            return ResponseEntity.badRequest().body(Map.of("message", "카카오 인증 요청 주소가 올바르지 않습니다."));
        }
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            String form = "grant_type=authorization_code&client_id=" + URLEncoder.encode(restKey, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);
            HttpRequest tokenRequest = HttpRequest.newBuilder(URI.create("https://kauth.kakao.com/oauth/token"))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
            HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            if (tokenResponse.statusCode() != 200) {
                return ResponseEntity.status(401).body(Map.of("message", "카카오 토큰 발급 실패. REST API 키와 Redirect URI를 확인해 주세요."));
            }
            String accessToken = JsonParser.parseString(tokenResponse.body()).getAsJsonObject().get("access_token").getAsString();
            HttpRequest profileRequest = HttpRequest.newBuilder(URI.create("https://kapi.kakao.com/v2/user/me"))
                .header("Authorization", "Bearer " + accessToken).timeout(Duration.ofSeconds(20)).GET().build();
            HttpResponse<String> profileResponse = client.send(profileRequest, HttpResponse.BodyHandlers.ofString());
            if (profileResponse.statusCode() != 200) {
                return ResponseEntity.status(401).body(Map.of("message", "카카오 사용자 정보 확인 실패"));
            }
            JsonObject profile = JsonParser.parseString(profileResponse.body()).getAsJsonObject();
            String kakaoId = profile.get("id").getAsString();
            String email = "kakao_" + kakaoId + "@kakao.tripnavi.local";
            String name = "카카오 사용자";
            if (profile.has("properties") && profile.get("properties").isJsonObject()) {
                JsonObject props = profile.getAsJsonObject("properties");
                if (props.has("nickname") && !props.get("nickname").isJsonNull()) name = props.get("nickname").getAsString();
            }
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
                user = userRepository.save(user);
            }
            session.setAttribute("tripnaviUserId", user.getId());
            return ResponseEntity.ok(Map.of("message", "카카오 로그인 성공", "name", user.getName()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("message", "카카오 인증 서버 연결 실패: " + e.getClass().getSimpleName()));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestBody Map<String, String> request) {

        String name = request.get("name");
        String email = request.get("email");
        String password = request.get("password");

        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "모든 항목을 입력해주세요."));
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "이미 가입된 이메일입니다."));
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);

        return ResponseEntity.ok(
                Map.of("message", "회원가입 성공!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> request, HttpSession session) {

        String email = request.get("email");
        String password = request.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "이메일과 비밀번호를 입력해주세요."));
        }

        Optional<User> optionalUser =
                userRepository.findByEmail(email);

        if (optionalUser.isEmpty() ||
                !passwordEncoder.matches(
                    password, optionalUser.get().getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "이메일 또는 비밀번호가 틀렸습니다."));
        }

        User user = optionalUser.get();
        session.setAttribute("tripnaviUserId", user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("message", "로그인 성공!");
        result.put("name", user.getName());

        return ResponseEntity.ok(result);
    }

    private Optional<User> sessionUser(HttpSession session) {
        Object id = session.getAttribute("tripnaviUserId");
        if (!(id instanceof Number)) return Optional.empty();
        return userRepository.findById(((Number) id).longValue());
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Optional<User> user = sessionUser(session);
        if (user.isEmpty()) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        return ResponseEntity.ok(userInfo(user.get()));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> profile(@RequestBody Map<String, String> request, HttpSession session) {
        Optional<User> found = sessionUser(session);
        if (found.isEmpty()) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        String name = request.getOrDefault("name", "").trim();
        if (name.isBlank() || name.length() > 40) return ResponseEntity.badRequest().body(Map.of("message", "닉네임은 1~40자로 입력해 주세요."));
        User user = found.get();
        if (request.containsKey("profileImage")) {
            String photo = request.get("profileImage");
            if (photo == null) photo = "";
            if (!photo.isEmpty() && (!photo.matches("(?s)^data:image/jpeg;base64,[A-Za-z0-9+/=]+$") || photo.length() > 300000))
                return ResponseEntity.badRequest().body(Map.of("message", "프로필 사진 형식 또는 크기가 올바르지 않습니다."));
            user.setProfileImage(photo.isEmpty() ? null : photo);
        }
        user.setName(name); userRepository.save(user);
        return ResponseEntity.ok(userInfo(user));
    }

    private Map<String, String> userInfo(User user) {
        Map<String, String> info = new HashMap<>();
        info.put("name", user.getName());
        info.put("email", user.getEmail());
        info.put("profileImage", user.getProfileImage() == null ? "" : user.getProfileImage());
        return info;
    }

    @PutMapping("/password")
    public ResponseEntity<?> password(@RequestBody Map<String, String> request, HttpSession session) {
        Optional<User> found = sessionUser(session);
        if (found.isEmpty()) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        User user = found.get();
        if (user.getEmail().startsWith("kakao_") && user.getEmail().endsWith("@kakao.tripnavi.local"))
            return ResponseEntity.badRequest().body(Map.of("message", "카카오 계정은 비밀번호를 변경할 수 없습니다."));
        String current = request.getOrDefault("currentPassword", "");
        String next = request.getOrDefault("newPassword", "");
        if (!passwordEncoder.matches(current, user.getPassword()))
            return ResponseEntity.badRequest().body(Map.of("message", "현재 비밀번호가 일치하지 않습니다."));
        if (next.length() < 8) return ResponseEntity.badRequest().body(Map.of("message", "새 비밀번호는 8자 이상이어야 합니다."));
        user.setPassword(passwordEncoder.encode(next)); userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "비밀번호 변경 완료"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "로그아웃 완료"));
    }
}