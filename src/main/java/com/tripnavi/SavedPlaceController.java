package com.tripnavi;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/places")
public class SavedPlaceController {
 private final SavedPlaceRepository repo;
 public SavedPlaceController(SavedPlaceRepository repo){this.repo=repo;}
 private String normalize(String v){return v==null?"":v.replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT);}
 private Long uid(HttpSession s){return (Long)s.getAttribute("tripnaviUserId");}
 @GetMapping public ResponseEntity<?> list(HttpSession s){if(uid(s)==null)return ResponseEntity.status(401).body(Map.of("message","로그인이 필요합니다."));return ResponseEntity.ok(repo.findByUserIdOrderByCreatedAtDesc(uid(s)));}
 @PostMapping public ResponseEntity<?> save(@RequestBody SavedPlace input,HttpSession s){if(uid(s)==null)return ResponseEntity.status(401).body(Map.of("message","로그인이 필요합니다."));if(input.name==null||input.name.isBlank()||input.name.length()>200)return ResponseEntity.badRequest().body(Map.of("message","장소 이름이 올바르지 않습니다."));String normalizedName=normalize(input.name);
  String normalizedAddress=normalize(input.address);
  boolean duplicate=repo.findByUserIdOrderByCreatedAtDesc(uid(s)).stream().anyMatch(existing ->
    normalize(existing.name).equals(normalizedName) &&
    (normalizedAddress.isEmpty() || normalize(existing.address).isEmpty() || normalize(existing.address).equals(normalizedAddress)));
  if(duplicate)return ResponseEntity.status(409).body(Map.of("message","이미 저장한 장소입니다."));
  SavedPlace p=new SavedPlace();p.userId=uid(s);p.name=input.name.trim();p.address=input.address;p.lat=input.lat;p.lon=input.lon;p.reason=input.reason;return ResponseEntity.ok(repo.save(p));}
 @DeleteMapping("/{id}") public ResponseEntity<?> delete(@PathVariable Long id,HttpSession s){if(uid(s)==null)return ResponseEntity.status(401).body(Map.of("message","로그인이 필요합니다."));Optional<SavedPlace> p=repo.findById(id);if(p.isEmpty()||!p.get().userId.equals(uid(s)))return ResponseEntity.status(404).body(Map.of("message","장소를 찾을 수 없습니다."));repo.delete(p.get());return ResponseEntity.ok(Map.of("message","삭제 완료"));}
}
