package com.example.springb.controller;

import com.example.springb.common.Result;
import com.example.springb.entity.ModelConfig;
import com.example.springb.service.ConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/config")
public class ConfigController {

    @Resource
    private ConfigService configService;

    @GetMapping("/models")
    public Result getModels(@RequestParam String pageType) {
        return Result.success(configService.getModels(pageType));
    }

    @PostMapping("/models/folder")
    public Result addModelFolder(@RequestBody ModelFolderRequest request) {
        List<ModelConfig> models = configService.scanAndSaveModels(request.getPageType(), request.getFolderPath(), false);
        return Result.success(models);
    }

    @PostMapping("/models/scan")
    public Result rescanModels(@RequestBody ModelFolderRequest request) {
        List<ModelConfig> models = configService.scanAndSaveModels(request.getPageType(), request.getFolderPath(), true);
        return Result.success(models);
    }

    @DeleteMapping("/models/{id}")
    public Result deleteModel(@PathVariable Long id) {
        configService.deleteModel(id);
        return Result.success();
    }

    @PutMapping("/models/{id}/name")
    public Result updateModelDisplayName(@PathVariable Long id, @RequestBody ModelNameRequest request) {
        configService.updateModelDisplayName(id, request.getDisplayName());
        return Result.success();
    }

    @GetMapping("/thresholds")
    public Result getThresholds() {
        return Result.success(configService.getThresholds());
    }

    @PutMapping("/thresholds")
    public Result updateThresholds(@RequestBody ThresholdRequest request) {
        return Result.success(configService.updateThresholds(request.getDetect(), request.getVideo()));
    }

    @GetMapping("/global")
    public Result getGlobalConfig() {
        return Result.success(configService.getGlobalConfig());
    }

    @PutMapping("/global")
    public Result updateGlobalConfig(@RequestBody GlobalConfigRequest request) {
        return Result.success(configService.updateGlobalConfig(request.getBackendUrl(), request.getPythonDetectUrl()));
    }

    @GetMapping("/ai")
    public Result getAiConfig() {
        return Result.success(configService.getAiConfig());
    }

    @PutMapping("/ai")
    public Result updateAiConfig(@RequestBody AiConfigRequest request) {
        return Result.success(configService.updateAiConfig(
                request.getDeepseekApiKey(),
                request.getGlmApiKey(),
                request.getKimiApiKey()
        ));
    }

    @PostMapping("/ai/test")
    public Result testAiConnection(@RequestBody AiTestRequest request) {
        return Result.success(configService.testAiConnection(request.getModel(), request.getApiKey()));
    }

    @GetMapping("/all")
    public Result getAllConfigs() {
        return Result.success(configService.getAllConfigs());
    }

    public static class ModelFolderRequest {
        private String pageType;
        private String folderPath;

        public String getPageType() {
            return pageType;
        }

        public void setPageType(String pageType) {
            this.pageType = pageType;
        }

        public String getFolderPath() {
            return folderPath;
        }

        public void setFolderPath(String folderPath) {
            this.folderPath = folderPath;
        }
    }

    public static class ThresholdRequest {
        private Double detect;
        private Double video;

        public Double getDetect() {
            return detect;
        }

        public void setDetect(Double detect) {
            this.detect = detect;
        }

        public Double getVideo() {
            return video;
        }

        public void setVideo(Double video) {
            this.video = video;
        }
    }

    public static class ModelNameRequest {
        private String displayName;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    public static class GlobalConfigRequest {
        private String backendUrl;
        private String pythonDetectUrl;

        public String getBackendUrl() {
            return backendUrl;
        }

        public void setBackendUrl(String backendUrl) {
            this.backendUrl = backendUrl;
        }

        public String getPythonDetectUrl() {
            return pythonDetectUrl;
        }

        public void setPythonDetectUrl(String pythonDetectUrl) {
            this.pythonDetectUrl = pythonDetectUrl;
        }
    }

    public static class AiConfigRequest {
        private String deepseekApiKey;
        private String glmApiKey;
        private String kimiApiKey;

        public String getDeepseekApiKey() {
            return deepseekApiKey;
        }

        public void setDeepseekApiKey(String deepseekApiKey) {
            this.deepseekApiKey = deepseekApiKey;
        }

        public String getGlmApiKey() {
            return glmApiKey;
        }

        public void setGlmApiKey(String glmApiKey) {
            this.glmApiKey = glmApiKey;
        }

        public String getKimiApiKey() {
            return kimiApiKey;
        }

        public void setKimiApiKey(String kimiApiKey) {
            this.kimiApiKey = kimiApiKey;
        }
    }

    public static class AiTestRequest {
        private String model;
        private String apiKey;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
