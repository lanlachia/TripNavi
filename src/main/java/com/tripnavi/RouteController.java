package com.tripnavi;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/routes")
public class RouteController {
 private final SavedRouteRepository repo;
 public RouteController(SavedRouteRepository repo){this.repo=repo;}
 private Long uid(HttpSession session){return (Long)session.getAttribute("tripnaviUserId");}
 @GetMapping public ResponseEntity<?> list(HttpSession s){if(uid(s)==null)return ResponseEntity.status(401).body(Map.of("message","로그인이 필요합니다."));return ResponseEntity.ok(repo.findByUserIdOrderByCreatedAtDesc(uid(s)));}
 @PostMapping public ResponseEntity<?> save(@RequestBody Map<String,String> input,HttpSession s){if(uid(s)==null)return ResponseEntity.status(401).body(Map.of("message","로그인이 필요합니다."));String title=input.getOrDefault("title","").trim(),places=input.getOrDefault("placesJson","");if(title.isEmpty()||places.isEmpty()||places.length()>60000)return ResponseEntity.badRequest().body(Map.of("message","저장할 경로가 올바르지 않습니다."));SavedRoute r=new SavedRoute();r.userId=uid(s);r.title=title;r.placesJson=places;return ResponseEntity.ok(repo.save(r));}
 @DeleteMapping("/{id}") public ResponseEntity<?> delete(@PathVariable Long id,HttpSession s){if(uid(s)==null)return ResponseEntity.status(401).body(Map.of("message","로그인이 필요합니다."));Optional<SavedRoute> r=repo.findById(id);if(r.isEmpty()||!r.get().userId.equals(uid(s)))return ResponseEntity.status(404).body(Map.of("message","경로를 찾을 수 없습니다."));repo.delete(r.get());return ResponseEntity.ok(Map.of("message","삭제 완료"));}
}
