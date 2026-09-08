package com.example.springb.service;

import com.example.springb.entity.ModelConfig;
import com.example.springb.entity.SystemConfig;
import com.example.springb.exception.CustomerException;
import com.example.springb.mapper.ConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class ConfigService {
    private static final Set<String> PAGE_TYPES = Set.of("detect", "video");
    private static final Set<String> MODEL_EXTENSIONS = Set.of("pt", "pth", "onnx", "engine", "trt");
    private static final int MAX_SCAN_RESULTS = 500;

    private static final String KEY_THRESHOLD_DETECT = "threshold.detect";
    private static final String KEY_THRESHOLD_VIDEO = "threshold.video";
    private static final String KEY_BACKEND_URL = "global.backend_url";
    private static final String KEY_PYTHON_DETECT_URL = "global.python_detect_url";
    private static final String KEY_AI_DEEPSEEK = "ai.deepseek.api_key";
    private static final String KEY_AI_GLM = "ai.glm.api_key";
    private static final String KEY_AI_KIMI = "ai.kimi.api_key";

    @Resource
    private ConfigMapper configMapper;

    @Resource
    private AIService aiService;

    public List<ModelConfig> getModels(String pageType) {
        String normalizedPageType = normalizePageType(pageType);
        List<ModelConfig> models = configMapper.selectModelsByPageType(normalizedPageType);
        for (ModelConfig model : models) {
            model.setExists(model.getModelPath() != null && Files.isRegularFile(Paths.get(model.getModelPath())));
            if (model.getModelPath() != null && !model.getModelPath().isBlank()) {
                model.setOriginalName(extractDisplayName(Paths.get(model.getModelPath())));
            } else {
                model.setOriginalName(model.getModelName());
            }
        }
        return models;
    }

    public List<ModelConfig> scanAndSaveModels(String pageType, String folderPath, boolean clearExistingFolder) {
        String normalizedPageType = normalizePageType(pageType);
        Path folder = normalizeFolder(folderPath);
        Map<String, String> preservedDisplayNames = new HashMap<>();

        if (clearExistingFolder) {
            for (ModelConfig existingModel : configMapper.selectModelsByPageType(normalizedPageType)) {
                if (!folder.toString().equals(existingModel.getFolderPath())) {
                    continue;
                }
                String existingDisplayName = existingModel.getDisplayName();
                if (existingModel.getModelPath() != null && existingDisplayName != null && !existingDisplayName.isBlank()) {
                    preservedDisplayNames.put(existingModel.getModelPath(), existingDisplayName);
                }
            }
            configMapper.deleteByPageTypeAndFolder(normalizedPageType, folder.toString());
        }

        List<ModelConfig> scanned = scanFolder(normalizedPageType, folder);
        for (ModelConfig model : scanned) {
            String preservedDisplayName = preservedDisplayNames.get(model.getModelPath());
            if (preservedDisplayName != null) {
                model.setDisplayName(preservedDisplayName);
            }
            ModelConfig existing = configMapper.selectModelByPageTypeAndPath(normalizedPageType, model.getModelPath());
            if (existing == null) {
                configMapper.insertModel(model);
            } else {
                model.setId(existing.getId());
                if (existing.getDisplayName() != null && !existing.getDisplayName().isBlank()) {
                    model.setDisplayName(existing.getDisplayName());
                }
                configMapper.updateModel(model);
            }
        }
        return scanned;
    }

    public void deleteModel(Long id) {
        if (id == null) {
            throw new CustomerException("400", "模型ID不能为空");
        }
        configMapper.deleteModelById(id);
    }

    public void updateModelDisplayName(Long id, String displayName) {
        if (id == null) {
            throw new CustomerException("400", "模型ID不能为空");
        }
        String value = displayName == null ? "" : displayName.trim();
        if (value.isEmpty()) {
            throw new CustomerException("400", "自定义名称不能为空");
        }
        if (value.length() > 100) {
            throw new CustomerException("400", "自定义名称不能超过100个字符");
        }
        int affectedRows = configMapper.updateModelDisplayName(id, value);
        if (affectedRows == 0) {
            throw new CustomerException("404", "模型配置不存在");
        }
    }

    public Map<String, Double> getThresholds() {
        ensureDefaults();
        Map<String, Double> thresholds = new HashMap<>();
        thresholds.put("detect", parseThreshold(configMapper.selectValueByKey(KEY_THRESHOLD_DETECT), 0.55));
        thresholds.put("video", parseThreshold(configMapper.selectValueByKey(KEY_THRESHOLD_VIDEO), 0.60));
        return thresholds;
    }

    public Map<String, Double> updateThresholds(Double detect, Double video) {
        if (detect != null) {
            double value = normalizeThreshold(detect);
            configMapper.upsertConfig(KEY_THRESHOLD_DETECT, String.format(Locale.ROOT, "%.2f", value), "图像检测页面默认置信度阈值");
        }
        if (video != null) {
            double value = normalizeThreshold(video);
            configMapper.upsertConfig(KEY_THRESHOLD_VIDEO, String.format(Locale.ROOT, "%.2f", value), "视频检测页面默认置信度阈值");
        }
        return getThresholds();
    }

    public Map<String, String> getGlobalConfig() {
        ensureDefaults();
        Map<String, String> config = new HashMap<>();
        config.put("backendUrl", getConfigValue(KEY_BACKEND_URL, "http://localhost:1907"));
        config.put("pythonDetectUrl", getConfigValue(KEY_PYTHON_DETECT_URL, "http://localhost:2026"));
        return config;
    }

    public Map<String, String> updateGlobalConfig(String backendUrl, String pythonDetectUrl) {
        if (backendUrl != null && !backendUrl.isBlank()) {
            configMapper.upsertConfig(KEY_BACKEND_URL, normalizeUrl(backendUrl), "后端API地址");
        }
        if (pythonDetectUrl != null && !pythonDetectUrl.isBlank()) {
            configMapper.upsertConfig(KEY_PYTHON_DETECT_URL, normalizeUrl(pythonDetectUrl), "Python检测服务地址");
        }
        return getGlobalConfig();
    }

    public Map<String, String> getAiConfig() {
        ensureAiDefaults();
        Map<String, String> config = new HashMap<>();
        config.put("deepseekApiKey", getAiConfigStatusValue("deepseek", KEY_AI_DEEPSEEK));
        config.put("glmApiKey", getAiConfigStatusValue("glm", KEY_AI_GLM));
        config.put("kimiApiKey", getAiConfigStatusValue("kimi", KEY_AI_KIMI));
        return config;
    }

    public Map<String, String> updateAiConfig(String deepseekApiKey, String glmApiKey, String kimiApiKey) {
        if (deepseekApiKey != null) {
            configMapper.upsertConfig(KEY_AI_DEEPSEEK, normalizeApiKeyInput(deepseekApiKey), "DeepSeek API Key");
        }
        if (glmApiKey != null) {
            configMapper.upsertConfig(KEY_AI_GLM, normalizeApiKeyInput(glmApiKey), "GLM(智谱) API Key");
        }
        if (kimiApiKey != null) {
            configMapper.upsertConfig(KEY_AI_KIMI, normalizeApiKeyInput(kimiApiKey), "Kimi API Key");
        }
        return getAiConfig();
    }

    public String testAiConnection(String model) {
        return testAiConnection(model, null);
    }

    public String testAiConnection(String model, String apiKey) {
        String normalizedModel = normalizeAiModel(model);
        String apiKeyOverride = normalizeApiKeyInput(apiKey);
        if (apiKeyOverride.isBlank() && !aiService.isConfigured(normalizedModel)) {
            throw new CustomerException("400", "请先配置 " + getAiModelDisplayName(normalizedModel) + " 的 API Key");
        }
        try {
            aiService.testConnection(normalizedModel, apiKeyOverride.isBlank() ? null : apiKeyOverride);
            return "连接成功";
        } catch (Exception e) {
            throw new CustomerException("500", "连接失败: " + e.getMessage());
        }
    }

    public String getConfigValue(String key, String defaultValue) {
        try {
            String value = configMapper.selectValueByKey(key);
            if (value == null || value.isBlank()) {
                return defaultValue;
            }
            String trimmedValue = value.trim();
            if (KEY_PYTHON_DETECT_URL.equals(key) && isLegacyPythonDetectUrl(trimmedValue)) {
                String upgradedValue = "http://localhost:2026";
                configMapper.upsertConfig(KEY_PYTHON_DETECT_URL, upgradedValue, "Python检测服务地址");
                return upgradedValue;
            }
            return trimmedValue;
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public List<SystemConfig> getAllConfigs() {
        ensureDefaults();
        return configMapper.selectAllConfigs();
    }

    private void ensureDefaults() {
        configMapper.upsertConfig(KEY_THRESHOLD_DETECT, "0.55", "图像检测页面默认置信度阈值");
        configMapper.upsertConfig(KEY_THRESHOLD_VIDEO, "0.60", "视频检测页面默认置信度阈值");
        configMapper.upsertConfig(KEY_BACKEND_URL, "http://localhost:1907", "后端API地址");
        configMapper.upsertConfig(KEY_PYTHON_DETECT_URL, "http://localhost:2026", "Python检测服务地址");
        ensureAiDefaults();
    }

    private void ensureAiDefaults() {
        ensureConfigKey(KEY_AI_DEEPSEEK, "DeepSeek API Key");
        ensureConfigKey(KEY_AI_GLM, "GLM(智谱) API Key");
        ensureConfigKey(KEY_AI_KIMI, "Kimi API Key");
    }

    private void ensureConfigKey(String key, String description) {
        try {
            if (configMapper.selectValueByKey(key) == null) {
                configMapper.upsertConfig(key, "", description);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isLegacyPythonDetectUrl(String value) {
        String normalized = value.replaceAll("/+$", "");
        return "http://localhost:5001".equalsIgnoreCase(normalized)
                || "http://127.0.0.1:5001".equalsIgnoreCase(normalized);
    }

    private List<ModelConfig> scanFolder(String pageType, Path folder) {
        List<ModelConfig> models = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(folder)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isModelFile)
                    .limit(MAX_SCAN_RESULTS)
                    .forEach(path -> models.add(toModelConfig(pageType, folder, path)));
        } catch (IOException e) {
            throw new CustomerException("500", "扫描模型文件夹失败: " + e.getMessage());
        }
        return models;
    }

    private ModelConfig toModelConfig(String pageType, Path folder, Path modelPath) {
        Path normalizedModelPath = modelPath.toAbsolutePath().normalize();
        String modelName = normalizedModelPath.getFileName().toString();
        String modelType = getExtension(modelName);

        ModelConfig model = new ModelConfig();
        model.setPageType(pageType);
        model.setFolderPath(folder.toString());
        model.setDisplayName(extractDisplayName(normalizedModelPath));
        model.setOriginalName(extractDisplayName(normalizedModelPath));
        model.setModelName(modelName);
        model.setModelPath(normalizedModelPath.toString());
        model.setModelType(modelType);
        model.setIsActive(1);
        model.setExists(true);
        return model;
    }

    private String extractDisplayName(Path modelPath) {
        String fileName = modelPath.getFileName().toString();
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        String withoutExt = removeExtension(fileName);
        Path parent = modelPath.getParent();
        Path grandParent = parent == null ? null : parent.getParent();

        if (("best.pt".equals(lowerFileName) || "last.pt".equals(lowerFileName)) && parent != null) {
            String parentName = parent.getFileName().toString();
            if ("weights".equalsIgnoreCase(parentName) && grandParent != null) {
                return grandParent.getFileName().toString();
            }
            return parentName;
        }
        return withoutExt;
    }

    private Path normalizeFolder(String folderPath) {
        if (folderPath == null || folderPath.isBlank()) {
            throw new CustomerException("400", "模型文件夹路径不能为空");
        }
        if (folderPath.indexOf('\0') >= 0) {
            throw new CustomerException("400", "模型文件夹路径不合法");
        }

        Path folder = Paths.get(folderPath.trim()).toAbsolutePath().normalize();
        if (!Files.exists(folder)) {
            throw new CustomerException("400", "目录不存在: " + folder);
        }
        if (!Files.isDirectory(folder)) {
            throw new CustomerException("400", "路径不是目录: " + folder);
        }
        return folder;
    }

    private String normalizePageType(String pageType) {
        String value = pageType == null ? "" : pageType.trim().toLowerCase(Locale.ROOT);
        if (!PAGE_TYPES.contains(value)) {
            throw new CustomerException("400", "页面类型仅支持 detect 或 video");
        }
        return value;
    }

    private boolean isModelFile(Path path) {
        return MODEL_EXTENSIONS.contains(getExtension(path.getFileName().toString()));
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String removeExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private double parseThreshold(String value, double defaultValue) {
        try {
            return normalizeThreshold(Double.parseDouble(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private double normalizeThreshold(Double value) {
        if (value == null || value < 0.05 || value > 0.95) {
            throw new CustomerException("400", "阈值范围必须在 0.05 到 0.95 之间");
        }
        double stepped = Math.round(value * 20.0) / 20.0;
        if (Math.abs(stepped - value) > 0.000001) {
            throw new CustomerException("400", "阈值步长必须为 0.05");
        }
        return stepped;
    }

    private String normalizeUrl(String value) {
        String url = value.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException();
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException();
            }
            return url;
        } catch (Exception e) {
            throw new CustomerException("400", "服务地址格式不正确，请使用 http:// 或 https:// 地址");
        }
    }

    private String normalizeApiKeyInput(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeAiModel(String model) {
        String value = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("deepseek", "glm", "kimi").contains(value)) {
            throw new CustomerException("400", "AI模型仅支持 deepseek、glm 或 kimi");
        }
        return value;
    }

    private String getAiModelDisplayName(String model) {
        switch (model) {
            case "deepseek":
                return "DeepSeek";
            case "glm":
                return "GLM";
            case "kimi":
                return "Kimi";
            default:
                return model;
        }
    }

    private String maskApiKey(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        String value = key.trim();
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private String getAiConfigStatusValue(String model, String key) {
        String dbKey = getConfigValue(key, "");
        if (!dbKey.isBlank()) {
            return aiService.isConfigured(model) ? maskApiKey(dbKey) : "";
        }
        return aiService.isConfigured(model) ? "****" : "";
    }
}
