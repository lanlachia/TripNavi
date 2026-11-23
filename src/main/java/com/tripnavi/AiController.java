package com.tripnavi;
import com.google.gson.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
@RestController @RequestMapping("/api/recommendations")
public class AiController {
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
 @PostMapping public ResponseEntity<?> recommend(@RequestBody Map<String,Object> req){
  String key=System.getenv("NVIDIA_API_KEY");
  if (key == null || key.isBlank()) {
    return ResponseEntity.status(503)
        .body(Map.of("message", "NVIDIA_API_KEY 환경변수가 없습니다."));
}
  try{
   String description=String.valueOf(req.getOrDefault("description", ""));
   String tags=String.valueOf(req.getOrDefault("tags", ""));
   String prompt="한국 여행 코스 추천 전문가입니다. 사용자 요청에 맞는 실제 장소 3~5곳을 방문 순서대로 추천하세요. 반드시 JSON 객체만 반환: {\"places\":[{\"name\":\"장소명\",\"address\":\"지역 및 주소\",\"reason\":\"추천 이유\"}]}. 정확하지 않은 주소는 추측하지 마세요. 추천 장소의 존재와 좌표는 별도 지도 검색으로 검증합니다. 조건: "+description+"; 태그: "+tags+"; 지도 중심 위도: "+req.get("latitude")+" 경도: "+req.get("longitude");
   JsonObject body = new JsonObject();
body.addProperty("model", "nvidia/nemotron-3.5-lightning-30b-a3b");
body.addProperty("temperature", 0.3);
body.addProperty("max_tokens", 4096);
JsonObject chatTemplate = new JsonObject();
chatTemplate.addProperty("enable_thinking", false);
body.add("chat_template_kwargs", chatTemplate);
   //JsonObject format=new JsonObject();format.addProperty("type","json_object");body.add("response_format",format);

   JsonArray messages=new JsonArray();JsonObject message=new JsonObject();message.addProperty("role","user");message.addProperty("content",prompt);messages.add(message);body.add("messages",messages);
   HttpRequest request=HttpRequest.newBuilder(URI.create("https://integrate.api.nvidia.com/v1/chat/completions"))
    .header("Content-Type","application/json").header("Authorization","Bearer "+key)
    .timeout(Duration.ofSeconds(120)).POST(HttpRequest.BodyPublishers.ofString(body.toString(),StandardCharsets.UTF_8)).build();
   HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
   System.out.println("NVIDIA 응답: " + response.body());
   if(response.statusCode()!=200){
    String detail="";
    try{JsonObject err=JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("error");if(err!=null&&err.has("message"))detail=err.get("message").getAsString();}catch(Exception ignored){}
    return ResponseEntity.status(502).body(Map.of("message","NVIDIA AI 요청 실패 (HTTP "+response.statusCode()+"): "+detail));
   }
   JsonObject data=JsonParser.parseString(response.body()).getAsJsonObject();
   JsonObject choice = data.getAsJsonArray("choices")
    .get(0).getAsJsonObject();

JsonObject aiMessage = choice.getAsJsonObject("message");

if (!aiMessage.has("content")
        || aiMessage.get("content").isJsonNull()) {

    String reason = choice.has("finish_reason")
        ? choice.get("finish_reason").toString()
        : "unknown";

    return ResponseEntity.status(502).body(Map.of(
        "message", "AI가 추천 결과를 완성하지 못했습니다. 종료 사유: " + reason
    ));
}

String result = aiMessage.get("content").getAsString();
   JsonObject parsed=JsonParser.parseString(result).getAsJsonObject();return ResponseEntity.ok(parsed.toString());
  } catch (Exception e) {
    e.printStackTrace();
    return ResponseEntity.status(502)
        .body(Map.of(
            "message", "AI 추천 처리 실패: " + e.getClass().getSimpleName()
        ));
}
 }
}
