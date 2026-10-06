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

    public JsonNode recommend(DesignRequest request, String dxf) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "未配置 AI_API_KEY。请先设置环境变量后重启服务。");
        }
        try {
            String prompt = "你是橱柜设计顾问和CAD布局规划器。请根据预算、风格、电器需求和DXF墙体图纸给出可执行的设计建议，只返回JSON，不要Markdown。"
                    + "字段必须为 recommendation(string), layoutAdvice(string), warnings(array of string), placements(array)。placements每项包含 name,widthMm,depthMm,xMm,yMm,rotationDeg。"
                    + "DXF中的LINE实体、坐标和毫米尺寸是墙体布局的唯一依据，必须严格沿墙布置，不能假设一字型、L型、U型或任何固定布局，也不能使用A墙/B墙/C墙命名。"
                    + "不得改变墙体尺寸，不得把柜体或电器放到墙体外；异形或信息不足时在warnings中说明。电器只使用用户勾选并提供的尺寸，不得擅自增加未选择的电器。placements坐标以DXF坐标系为准。"
                    + "输入参数：" + mapper.writeValueAsString(request) + "；DXF墙体LINE几何摘要：" + lineSummary(dxf);
            Map<String, Object> body = Map.of("model", model, "temperature", 0.2,
                    "messages", new Object[] { Map.of("role", "system", "content", "输出严格JSON。"),
                            Map.of("role", "user", "content", prompt) });
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                String detail = response.body() == null ? "" : response.body();
                if (detail.length() > 500) detail = detail.substring(0, 500);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI服务返回错误：" + response.statusCode() + " " + detail);
            }
            JsonNode content = mapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
            return mapper.readTree(content.asText().replace("```json", "").replace("```", "").trim());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI方案请求失败", e);
        }
    }

    private String lineSummary(String dxf) {
        String[] lines = dxf.replace("\r", "").split("\n");
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (int i = 0; i < lines.length && count < 2000; i++) {
            if ("LINE".equalsIgnoreCase(lines[i].trim())) {
                out.append("LINE ");
                for (int j = i + 1; j < Math.min(lines.length, i + 30); j++) {
                    String code = lines[j].trim();
                    if ("10".equals(code) || "20".equals(code) || "11".equals(code) || "21".equals(code)) {
                        out.append(code).append('=').append(lines[j + 1].trim()).append(' ');
                    }
                }
                out.append(';');
                count++;
            }
        }
        return out.length() == 0 ? "未找到LINE实体，请在warnings中说明" : out.toString();
    }
}
