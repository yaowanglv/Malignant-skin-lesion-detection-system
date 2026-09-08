package com.example.springb.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * AI大模型服务
 * 支持DeepSeek、GLM(智谱)、Kimi等主流大模型API调用
 */
@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private static final int DEFAULT_TIMEOUT = 60000;
    private static final int TEST_TIMEOUT = 10000;

    @Resource
    @Lazy
    private ConfigService configService;

    // DeepSeek配置
    @Value("${ai.models.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${ai.models.deepseek.endpoint:https://api.deepseek.com/chat/completions}")
    private String deepseekEndpoint;

    @Value("${ai.models.deepseek.model-name:deepseek-v4-flash}")
    private String deepseekModel;

    // GLM(智谱)配置
    @Value("${ai.models.glm.api-key:}")
    private String glmApiKey;

    @Value("${ai.models.glm.endpoint:https://open.bigmodel.cn/api/paas/v4/chat/completions}")
    private String glmEndpoint;

    @Value("${ai.models.glm.model-name:glm-4}")
    private String glmModel;

    // Kimi配置
    @Value("${ai.models.kimi.api-key:}")
    private String kimiApiKey;

    @Value("${ai.models.kimi.endpoint:https://api.moonshot.cn/v1/chat/completions}")
    private String kimiEndpoint;

    @Value("${ai.models.kimi.model-name:${KIMI_MODEL:kimi-k2.5-preview}}")
    private String kimiModel;

    /**
     * 调用AI模型进行分析
     *
     * @param model  模型名称: deepseek/glm/doubao
     * @param prompt 提示词
     * @return AI分析结果
     */
    public String analyze(String model, String prompt) {
        switch (model.toLowerCase()) {
            case "deepseek":
                return callDeepSeek(prompt);
            case "glm":
                return callGLM(prompt);
            case "kimi":
                return callKimi(prompt);
            default:
                throw new IllegalArgumentException("不支持的AI模型: " + model);
        }
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeek(String prompt) {
        return callDeepSeek(prompt, DEFAULT_TIMEOUT);
    }

    private String callDeepSeek(String prompt, int timeoutMillis) {
        return callDeepSeek(prompt, timeoutMillis, null);
    }

    private String callDeepSeek(String prompt, int timeoutMillis, String apiKeyOverride) {
        String apiKey = normalizeApiKey(resolveApiKey("deepseek", apiKeyOverride));
        if (!isValidApiKey(apiKey)) {
            throw new RuntimeException("DeepSeek API Key未配置");
        }

        JSONObject requestBody = new JSONObject();
        requestBody.set("model", deepseekModel);
        requestBody.set("messages", new JSONArray()
            .put(new JSONObject()
                .set("role", "system")
                .set("content", "你是一位专业的风力发电机缺陷检测与运维分析专家，擅长分析风机图像检测结果并提供巡检维护建议。"))
            .put(new JSONObject()
                .set("role", "user")
                .set("content", prompt))
        );
        requestBody.set("temperature", 0.7);
        requestBody.set("max_tokens", 2000);

        String endpoint = deepseekEndpoint == null ? "" : deepseekEndpoint.trim();
        log.info("调用DeepSeek API, endpoint: {}, model: {}, key: {}", endpoint, deepseekModel, maskApiKey(apiKey));

        HttpResponse response = HttpRequest.post(endpoint)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .timeout(timeoutMillis)
                .execute();

        if (response.getStatus() == 401) {
            log.error("DeepSeek API鉴权失败: status={}, body={}", response.getStatus(), response.body());
            throw new RuntimeException("DeepSeek API鉴权失败(HTTP 401)：请检查数据库或 application.yml 中的密钥是否为 DeepSeek 开放平台的真实 API Key，确认没有空格、引号或占位值");
        }

        return parseResponse(response, "DeepSeek");
    }

    /**
     * 调用GLM(智谱) API
     */
    private String callGLM(String prompt) {
        return callGLM(prompt, DEFAULT_TIMEOUT);
    }

    private String callGLM(String prompt, int timeoutMillis) {
        return callGLM(prompt, timeoutMillis, null);
    }

    private String callGLM(String prompt, int timeoutMillis, String apiKeyOverride) {
        String apiKey = normalizeApiKey(resolveApiKey("glm", apiKeyOverride));
        if (!isValidApiKey(apiKey)) {
            throw new RuntimeException("GLM API Key未配置");
        }

        JSONObject requestBody = new JSONObject();
        requestBody.set("model", glmModel);
        requestBody.set("messages", new JSONArray()
            .put(new JSONObject()
                .set("role", "system")
                .set("content", "你是一位专业的风力发电机缺陷检测与运维分析专家，擅长分析风机图像检测结果并提供巡检维护建议。"))
            .put(new JSONObject()
                .set("role", "user")
                .set("content", prompt))
        );
        requestBody.set("temperature", 0.7);
        requestBody.set("max_tokens", 2000);

        log.info("调用GLM API, model: {}, key: {}", glmModel, maskApiKey(apiKey));

        HttpResponse response = HttpRequest.post(glmEndpoint)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .timeout(timeoutMillis)
                .execute();

        return parseResponse(response, "GLM");
    }

    /**
     * 调用Kimi API
     */
    private String callKimi(String prompt) {
        return callKimi(prompt, DEFAULT_TIMEOUT);
    }

    private String callKimi(String prompt, int timeoutMillis) {
        return callKimi(prompt, timeoutMillis, null);
    }

    private String callKimi(String prompt, int timeoutMillis, String apiKeyOverride) {
        String apiKey = normalizeApiKey(resolveApiKey("kimi", apiKeyOverride));
        if (!isValidApiKey(apiKey)) {
            throw new RuntimeException("Kimi API Key未配置");
        }

        log.info("调用Kimi API, model: {}, key: {}", kimiModel, maskApiKey(apiKey));

        JSONObject requestBody = new JSONObject();
        requestBody.set("model", kimiModel);
        requestBody.set("messages", new JSONArray()
            .put(new JSONObject()
                .set("role", "system")
                .set("content", "你是一位专业的风力发电机缺陷检测与运维分析专家，擅长分析风机图像检测结果并提供巡检维护建议。"))
            .put(new JSONObject()
                .set("role", "user")
                .set("content", prompt))
        );
        requestBody.set("temperature", 0.7);
        requestBody.set("max_tokens", 2000);

        log.debug("Kimi请求体: {}", requestBody.toString());

        HttpResponse response = HttpRequest.post(kimiEndpoint)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .timeout(timeoutMillis)
                .execute();

        log.info("Kimi响应状态: {}, 内容: {}", response.getStatus(), response.body());
        return parseResponse(response, "Kimi");
    }

    /**
     * 解析API响应
     */
    private String parseResponse(HttpResponse response, String modelName) {
        if (response.getStatus() != 200) {
            log.error("{} API响应错误: {}, body: {}", modelName, response.getStatus(), response.body());
            throw new RuntimeException(modelName + " API调用失败: HTTP " + response.getStatus());
        }

        String body = response.body();
        JSONObject jsonResponse = JSONUtil.parseObj(body);

        // 检查错误
        if (jsonResponse.containsKey("error")) {
            JSONObject error = jsonResponse.getJSONObject("error");
            String errorMsg = error.getStr("message", "未知错误");
            log.error("{} API返回错误: {}", modelName, errorMsg);
            throw new RuntimeException(modelName + " API错误: " + errorMsg);
        }

        // 提取内容
        JSONArray choices = jsonResponse.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException(modelName + " API返回结果为空");
        }

        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String content = message.getStr("content", "");

        log.info("{} API调用成功, 返回内容长度: {}", modelName, content.length());
        return content.trim();
    }

    /**
     * 检查指定模型是否已配置API Key
     */
    public boolean isConfigured(String model) {
        return isValidApiKey(resolveApiKey(model));
    }

    /**
     * 使用当前有效API Key测试模型连接。
     */
    public void testConnection(String model) {
        testConnection(model, null);
    }

    /**
     * 使用临时API Key或当前有效API Key测试模型连接。
     */
    public void testConnection(String model, String apiKeyOverride) {
        String normalizedModel = model == null ? "" : model.trim().toLowerCase();
        switch (normalizedModel) {
            case "deepseek":
                callDeepSeek("你好", TEST_TIMEOUT, apiKeyOverride);
                return;
            case "glm":
                callGLM("你好", TEST_TIMEOUT, apiKeyOverride);
                return;
            case "kimi":
                callKimi("你好", TEST_TIMEOUT, apiKeyOverride);
                return;
            default:
                throw new IllegalArgumentException("不支持的AI模型: " + model);
        }
    }

    /**
     * 获取模型配置状态
     */
    public Map<String, Boolean> getConfigStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("deepseek", isConfigured("deepseek"));
        status.put("glm", isConfigured("glm"));
        status.put("kimi", isConfigured("kimi"));
        return status;
    }

    private String normalizeApiKey(String apiKey) {
        if (apiKey == null) {
            return "";
        }
        String key = apiKey.trim();
        if (key.toLowerCase().startsWith("bearer ")) {
            key = key.substring("Bearer ".length()).trim();
        }
        return key;
    }

    private boolean isValidApiKey(String apiKey) {
        String key = normalizeApiKey(apiKey);
        if (key.isEmpty()) {
            return false;
        }
        String lowerKey = key.toLowerCase();
        return !lowerKey.contains("your_")
                && !lowerKey.contains("your-")
                && !lowerKey.contains("api_key_here")
                && !lowerKey.contains("placeholder");
    }

    private String resolveApiKey(String model) {
        return resolveApiKey(model, null);
    }

    private String resolveApiKey(String model, String apiKeyOverride) {
        if (apiKeyOverride != null && !apiKeyOverride.isBlank()) {
            return apiKeyOverride;
        }
        String normalizedModel = model == null ? "" : model.trim().toLowerCase();
        if (!normalizedModel.isEmpty() && configService != null) {
            String dbKey = configService.getConfigValue("ai." + normalizedModel + ".api_key", "");
            if (dbKey != null && !dbKey.isBlank()) {
                return dbKey;
            }
        }
        switch (normalizedModel) {
            case "deepseek":
                return deepseekApiKey;
            case "glm":
                return glmApiKey;
            case "kimi":
                return kimiApiKey;
            default:
                return "";
        }
    }

    private String maskApiKey(String apiKey) {
        String key = normalizeApiKey(apiKey);
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
