package com.cabinetai.ai;

import com.cabinetai.design.model.DesignRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiDesignService {
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public AiDesignService(ObjectMapper mapper, @Value("${ai.api-url}") String apiUrl,
            @Value("${ai.api-key}") String apiKey, @Value("${ai.model}") String model) {
        this.mapper = mapper;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public JsonNode recommend(DesignRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "未配置 AI_API_KEY。请先设置环境变量后重启服务。");
        }
        try {
            String prompt = "你是橱柜设计顾问。请根据输入给出可执行的初步建议，只返回JSON，不要Markdown。"
                    + "字段必须为 recommendation(string), layoutAdvice(string), warnings(array of string)。"
                    + "不要编造SKU或价格，尺寸仅作建议，必须提醒复尺。输入：" + mapper.writeValueAsString(request);
            Map<String, Object> body = Map.of("model", model, "temperature", 0.2,
                    "messages", new Object[] { Map.of("role", "system", "content", "输出严格JSON。"),
                            Map.of("role", "user", "content", prompt) });
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI服务返回错误：" + response.statusCode());
            }
            JsonNode content = mapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
            return mapper.readTree(content.asText().replace("```json", "").replace("```", "").trim());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI方案请求失败", e);
        }
    }
}
