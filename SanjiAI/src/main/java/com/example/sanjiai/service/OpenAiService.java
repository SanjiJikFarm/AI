package com.example.sanjiai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class OpenAiService {

    @Value("${openai.api.url}")
    private String openAiUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 단순 텍스트 기반 요청
     * GPT 모델에 프롬프트를 전달하고 문자열 응답을 반환합니다.
     */
    public String ask(String prompt) {
        try {
            Map<String, Object> request = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "You are a precise assistant that answers only with factual or numeric results, without extra explanations."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "max_tokens", 200,
                    "temperature", 0.2
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    openAiUrl,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("choices")) {
                log.warn("OpenAI 응답 비정상: {}", body);
                return "0";
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices.isEmpty()) {
                log.warn("OpenAI 응답 choices 비어 있음: {}", body);
                return "0";
            }

            Map<String, Object> message = (Map<String, Object>) Objects.requireNonNull(choices.get(0).get("message"));
            String content = Objects.toString(message.get("content"), "").trim();

            return content.isEmpty() ? "0" : content;

        } catch (Exception e) {
            log.error("OpenAI 호출 실패: {}", e.getMessage());
            return "0";
        }
    }

    /**
     * JSON 형식 응답을 요청하는 메서드 (산지, 운송수단 등)
     * 예: {"origin": "칠레", "transport": "선박"}
     */
    public Map<String, Object> askJson(String productName) {
        String prompt = String.format(
                "%s의 주요 생산지(origin)와 일반적인 운송수단(transport)을 JSON 형식으로 반환해줘. " +
                        "예시: {\"origin\": \"칠레\", \"transport\": \"선박\"}", productName);

        try {
            String rawResponse = ask(prompt);

            // 간단한 JSON 파싱 (응답이 JSON 형식일 경우)
            if (rawResponse.trim().startsWith("{") && rawResponse.trim().endsWith("}")) {
                return Map.of("raw", rawResponse);
            } else {
                return Map.of("raw", rawResponse);
            }

        } catch (Exception e) {
            log.warn("OpenAI JSON 요청 실패: {}", e.getMessage());
            return Map.of("origin", "미상", "transport", "트럭");
        }
    }
}
