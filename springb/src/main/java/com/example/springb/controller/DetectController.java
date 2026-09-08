package com.example.springb.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.example.springb.common.Result;
import com.example.springb.common.UploadPathResolver;
import com.example.springb.entity.Detect;
import com.example.springb.entity.DetectionLog;
import com.example.springb.service.AIService;
import com.example.springb.service.ConfigService;
import com.example.springb.service.DetectService;
import com.example.springb.service.DetectionLogService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/detect")
public class DetectController {

    private static final Logger log = LoggerFactory.getLogger(DetectController.class);
    // 模型根目录：指向 V11-dmt 项目的 skin 文件夹
    private static final String SKIN_MODEL_ROOT = "D:/algorithms/V11-dmt/runs/skin-cancer";

    @Resource
    private DetectService detectService;

    @Resource
    private DetectionLogService detectionLogService;

    @Resource
    private AIService aiService;

    @Resource
    private ConfigService configService;

    // 文件上传路径配置
    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    @Value("${file.access.url:http://localhost:1907/files/}")
    private String fileAccessUrl;

    // Python检测服务地址
    @Value("${python.detect.url:http://localhost:2026}")
    private String pythonDetectUrl;

    // ==================== 基础CRUD接口 ====================

    @GetMapping("/health")
    public Result health() {
        return Result.success("Detect服务运行正常");
    }

    @GetMapping("/pythonHealth")
    public Result pythonHealth() {
        try {
            String url = getPythonDetectUrl() + "/health";
            HttpResponse response = HttpRequest.get(url)
                    .timeout(5000)
                    .execute();

            if (response.getStatus() == 200) {
                return Result.success(JSONUtil.parseObj(response.body()));
            } else {
                return Result.error("Python检测服务响应异常: " + response.getStatus());
            }
        } catch (Exception e) {
            log.error("Python检测服务连接失败", e);
            return Result.error("Python检测服务连接失败: " + e.getMessage());
        }
    }

    @GetMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             Detect detect) {
        PageInfo<Detect> pageInfo = detectService.selectPage(pageNum, pageSize, detect);
        return Result.success(pageInfo);
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Detect detect = detectService.selectById(id);
        return Result.success(detect);
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        detectService.deleteById(id);
        return Result.success();
    }

    // ==================== 图像上传与检测接口 ====================

    @PostMapping("/upload")
    public Result uploadImage(@RequestParam("file") MultipartFile file,
                              @RequestParam("userId") Integer userId,
                              @RequestParam("userName") String userName) {
        log.info("收到上传请求: userId={}, userName={}, fileName={}, fileSize={}",
                userId, userName, file.getOriginalFilename(), file.getSize());

        try {
            String originalName = file.getOriginalFilename();
            String ext = FileUtil.extName(originalName);
            String newFileName = UUID.randomUUID() + "." + ext;

            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String relativePath = "detect/" + dateDir + "/" + newFileName;

            File dir = UploadPathResolver.resolve(uploadPath, "detect/" + dateDir);
            File destFile = UploadPathResolver.resolve(uploadPath, relativePath);
            String fullDir = dir.getAbsolutePath();
            String fullPath = destFile.getAbsolutePath();

            log.info("文件保存目录: {}", fullDir);
            log.info("文件保存路径: {}", fullPath);

            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                log.info("创建目录: {}, 结果: {}", fullDir, created);
                if (!created) {
                    return Result.error("创建上传目录失败: " + fullDir);
                }
            }

            file.transferTo(destFile);
            log.info("文件保存成功: {}", fullPath);

            Detect detect = new Detect();
            detect.setUserId(userId);
            detect.setUserName(userName);
            detect.setOriginalImageName(originalName);
            detect.setOriginalImageUrl(relativePath);
            detect.setOriginalImageSize(file.getSize());
            detect.setOriginalImageFormat(ext);
            detect.setDetectStatus(0);
            detect.setAiStatus(0);

            detectService.add(detect);
            log.info("检测记录创建成功: id={}", detect.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("id", detect.getId());
            result.put("originalImageUrl", fileAccessUrl + relativePath);
            result.put("originalImageName", originalName);

            return Result.success(result);

        } catch (IOException e) {
            log.error("图像上传失败", e);
            return Result.error("图像上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("图像上传异常", e);
            return Result.error("图像上传异常: " + e.getMessage());
        }
    }

    @PostMapping("/startDetect/{id}")
    public Result startDetect(@PathVariable Integer id,
                              @RequestParam(required = false) String ptPath,
                              @RequestParam(required = false) Double conf) {
        if (ptPath == null || ptPath.isEmpty()) {
            ptPath = findDefaultModelPath();
        }
        if (conf == null) {
            conf = 0.25;
        }
        try {
            Detect detect = detectService.selectById(id);
            if (detect == null) {
                return Result.error("检测记录不存在");
            }

            detect.setDetectStatus(1);
            detectService.update(detect);

            log.info("开始检测记录: {}, ptPath={}, conf={}", id, ptPath, conf);

            File imageFile = UploadPathResolver.resolve(uploadPath, detect.getOriginalImageUrl());
            if (!imageFile.exists()) {
                throw new RuntimeException("图像文件不存在: " + imageFile.getAbsolutePath());
            }

            String url = getPythonDetectUrl() + "/detect";
            log.info("调用Python检测服务: {}", url);

            HttpResponse response = HttpRequest.post(url)
                    .form("file", imageFile)
                    .form("pt_path", ptPath)
                    .form("conf", String.valueOf(conf))
                    .form("mode", "detect")
                    .form("return_image", "true")
                    .form("save_result", "false")
                    .timeout(120000)
                    .execute();

            if (response.getStatus() != 200) {
                throw new RuntimeException("Python服务响应异常: " + response.getStatus());
            }

            String responseBody = response.body();
            log.debug("Python服务响应: {}", responseBody);

            cn.hutool.json.JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
            int code = jsonResponse.getInt("code", -1);

            if (code != 0) {
                String message = jsonResponse.getStr("message", "检测失败");
                throw new RuntimeException(message);
            }

            cn.hutool.json.JSONObject data = jsonResponse.getJSONObject("data");
            cn.hutool.json.JSONObject detectionData = data.getJSONObject("detection");

            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String resultImageBase64 = data.getStr("result_image_base64");
            String resultImagePath = saveBase64Image(dateDir, "result", id, resultImageBase64, "jpg");

            if (resultImagePath != null) {
                detectionData.set("result_image_url", resultImagePath);
            } else {
                log.warn("Python服务未返回结果图像base64");
            }

            detect.setDetectStatus(2);
            detect.setDetectionData(detectionData.toString());
            if (resultImagePath != null) {
                detect.setResultImageUrl(resultImagePath);
            }
            detectService.update(detect);

            try {
                DetectionLog logEntry = new DetectionLog();
                logEntry.setDetectId(detect.getId());
                logEntry.setUserId(detect.getUserId());
                logEntry.setUserName(detect.getUserName());
                logEntry.setModelName(ptPath);
                logEntry.setConfThreshold(conf);
                logEntry.setAiModel(detect.getAiModel());
                logEntry.setCreateTime(LocalDateTime.now());

                if (detectionData.containsKey("tumor_type")) {
                    logEntry.setTumorType(detectionData.getStr("tumor_type"));
                }
                if (detectionData.containsKey("boxes") && !detectionData.getJSONArray("boxes").isEmpty()) {
                    cn.hutool.json.JSONArray boxes = detectionData.getJSONArray("boxes");
                    double maxConf = 0;
                    for (int i = 0; i < boxes.size(); i++) {
                        cn.hutool.json.JSONObject box = boxes.getJSONObject(i);
                        if (box.containsKey("confidence")) {
                            maxConf = Math.max(maxConf, box.getDouble("confidence"));
                        }
                    }
                    if (maxConf > 0) {
                        logEntry.setConfidence(maxConf);
                    }
                } else if (detectionData.containsKey("confidence")) {
                    logEntry.setConfidence(detectionData.getDouble("confidence"));
                }

                detectionLogService.addLog(logEntry);
                log.info("检测日志记录成功: detectId={}", detect.getId());
            } catch (Exception ex) {
                log.error("记录检测日志失败", ex);
            }

            return Result.success(detect);

        } catch (Exception e) {
            log.error("检测失败", e);
            Detect detect = new Detect();
            detect.setId(id);
            detect.setDetectStatus(3);
            detectService.update(detect);
            return Result.error("检测失败: " + e.getMessage());
        }
    }

    private String saveBase64Image(String dateDir, String prefix, Integer id, String base64Image, String extension) {
        if (base64Image == null || base64Image.isEmpty()) {
            return null;
        }

        String resultFileName = prefix + "_" + id + "_" + System.currentTimeMillis() + "." + extension;
        String resultRelativePath = "detect/" + dateDir + "/" + resultFileName;
        File resultFile = UploadPathResolver.resolve(uploadPath, resultRelativePath);

        File resultDir = UploadPathResolver.resolve(uploadPath, "detect/" + dateDir);
        if (!resultDir.exists()) {
            resultDir.mkdirs();
        }

        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        FileUtil.writeBytes(imageBytes, resultFile);
        log.info("结果图像已保存: {}, 大小: {} bytes", resultFile.getAbsolutePath(), imageBytes.length);
        return resultRelativePath;
    }

    @PostMapping("/detectDirect")
    public Result detectDirect(@RequestParam("file") MultipartFile file,
                               @RequestParam("ptPath") String ptPath,
                               @RequestParam(required = false, defaultValue = "0.25") Double conf,
                               @RequestParam(required = false) Integer userId,
                               @RequestParam(required = false) String userName) {
        log.info("收到直接检测请求: ptPath={}, conf={}, fileName={}", ptPath, conf, file.getOriginalFilename());

        File tempFile = null;
        try {
            String url = getPythonDetectUrl() + "/detect";
            log.info("调用Python服务URL: {}", url);

            tempFile = File.createTempFile("detect_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);
            log.info("临时文件创建成功: {}, 大小: {} bytes", tempFile.getAbsolutePath(), tempFile.length());

            java.util.HashMap<String, Object> paramMap = new java.util.HashMap<>();
            paramMap.put("file", tempFile);
            paramMap.put("pt_path", ptPath);
            paramMap.put("conf", String.valueOf(conf));
            paramMap.put("save_result", "false");
            paramMap.put("return_image", "true");

            HttpResponse response = HttpRequest.post(url)
                    .form(paramMap)
                    .timeout(120000)
                    .execute();

            log.info("Python服务响应状态: {}", response.getStatus());

            if (response.getStatus() != 200) {
                log.error("Python服务响应异常: {}, 响应体: {}", response.getStatus(), response.body());
                return Result.error("Python服务响应异常: " + response.getStatus());
            }

            String responseBody = response.body();
            cn.hutool.json.JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
            int code = jsonResponse.getInt("code", -1);

            if (code != 0) {
                String message = jsonResponse.getStr("message", "检测失败");
                log.error("Python服务返回错误: {}", message);
                return Result.error(message);
            }

            cn.hutool.json.JSONObject data = jsonResponse.getJSONObject("data");
            String resultImageBase64 = data.getStr("result_image_base64");
            String resultImageUrl = null;

            if (userId != null && resultImageBase64 != null && !resultImageBase64.isEmpty()) {
                String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
                String resultFileName = "result_" + System.currentTimeMillis() + ".jpg";
                String resultRelativePath = "detect/" + dateDir + "/" + resultFileName;
                File resultFile = UploadPathResolver.resolve(uploadPath, resultRelativePath);

                File resultDir = UploadPathResolver.resolve(uploadPath, "detect/" + dateDir);
                if (!resultDir.exists()) {
                    resultDir.mkdirs();
                }

                byte[] imageBytes = Base64.getDecoder().decode(resultImageBase64);
                FileUtil.writeBytes(imageBytes, resultFile);
                resultImageUrl = fileAccessUrl + resultRelativePath;

                Detect detect = new Detect();
                detect.setUserId(userId);
                detect.setUserName(userName != null ? userName : "匿名用户");
                detect.setOriginalImageName(file.getOriginalFilename());
                detect.setOriginalImageSize(file.getSize());
                detect.setOriginalImageFormat(FileUtil.extName(file.getOriginalFilename()));
                detect.setDetectStatus(2);
                detect.setDetectionData(data.getJSONObject("detection").toString());
                detect.setResultImageUrl(resultRelativePath);
                detect.setAiStatus(0);

                detectService.add(detect);
                data.set("recordId", detect.getId());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("detection", data.getJSONObject("detection"));
            result.put("resultImageBase64", resultImageBase64);
            result.put("resultImageUrl", resultImageUrl);
            result.put("confThreshold", data.get("conf_threshold"));
            result.put("modelPath", data.get("model_path"));
            result.put("recordId", data.get("recordId"));

            return Result.success(result);

        } catch (Exception e) {
            log.error("直接检测失败", e);
            return Result.error("检测失败: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.debug("临时文件删除{}: {}", deleted ? "成功" : "失败", tempFile.getAbsolutePath());
            }
        }
    }

    @GetMapping("/listModels")
    public Result listModels() {
        try {
            java.util.List<java.util.Map<String, String>> models = new java.util.ArrayList<>();

            java.io.File modelFolder = new java.io.File(SKIN_MODEL_ROOT);
            if (!modelFolder.exists() || !modelFolder.isDirectory()) {
                return Result.error("模型目录不存在: " + SKIN_MODEL_ROOT);
            }

            java.util.List<java.io.File> bestPtFiles = new java.util.ArrayList<>();
            collectBestPtFiles(modelFolder, bestPtFiles);

            for (java.io.File ptFile : bestPtFiles) {
                java.util.Map<String, String> model = new java.util.HashMap<>();
                String modelLabel = extractModelLabel(ptFile, modelFolder);
                model.put("label", modelLabel);
                model.put("value", ptFile.getAbsolutePath());
                models.add(model);
            }

            models.sort(java.util.Comparator.comparing(m -> m.get("label")));

            log.info("扫描到 {} 个 best.pt 模型文件", models.size());
            return Result.success(models);
        } catch (Exception e) {
            log.error("获取模型列表失败", e);
            return Result.error("获取模型列表失败: " + e.getMessage());
        }
    }

    private void collectBestPtFiles(java.io.File dir, java.util.List<java.io.File> result) {
        java.io.File[] files = dir.listFiles();
        if (files == null) return;
        for (java.io.File file : files) {
            if (file.isDirectory()) {
                collectBestPtFiles(file, result);
            } else if ("best.pt".equals(file.getName())) {
                result.add(file);
            }
        }
    }

    private String extractModelLabel(java.io.File bestPtFile, java.io.File rootDir) {
        try {
            java.io.File weightsDir = bestPtFile.getParentFile();
            if (weightsDir == null) {
                return bestPtFile.getName();
            }
            java.io.File modelDir = weightsDir.getParentFile();
            if (modelDir != null) {
                return modelDir.getName();
            }
            String fileName = bestPtFile.getName();
            if (fileName.endsWith(".pt")) {
                fileName = fileName.substring(0, fileName.length() - 3);
            }
            return fileName;
        } catch (Exception e) {
            log.warn("提取模型标签失败: {}", bestPtFile.getAbsolutePath(), e);
            return bestPtFile.getName().replace(".pt", "");
        }
    }

    @PostMapping("/aiAnalysis/{id}")
    public Result aiAnalysis(@PathVariable Integer id,
                             @RequestParam String model) {
        try {
            Detect detect = detectService.selectById(id);
            if (detect == null) {
                return Result.error("检测记录不存在");
            }

            if (detect.getDetectStatus() != 2) {
                return Result.error("请先完成图像检测");
            }

            if (!aiService.isConfigured(model)) {
                return Result.error("AI模型 '" + model + "' 未配置API Key，请联系管理员配置");
            }

            detect.setAiStatus(1);
            detect.setAiModel(model);
            detectService.update(detect);

            log.info("开始AI分析记录: {}, 模型: {}", id, model);

            String prompt = buildAnalysisPrompt(detect);
            String aiResult = cleanAiAnalysisText(aiService.analyze(model, prompt));

            detect.setAiAnalysisResult(aiResult);
            detect.setAiAnalysisTime(LocalDateTime.now());
            detect.setAiStatus(2);
            detectService.update(detect);

            log.info("AI分析完成: detectId={}, model={}", id, model);
            return Result.success(detect);

        } catch (Exception e) {
            log.error("AI分析失败", e);
            Detect detect = new Detect();
            detect.setId(id);
            detect.setAiStatus(3);
            detectService.update(detect);
            return Result.error("AI分析失败: " + e.getMessage());
        }
    }

    @GetMapping("/aiConfigStatus")
    public Result getAiConfigStatus() {
        return Result.success(aiService.getConfigStatus());
    }

    private String buildAnalysisPrompt(Detect detect) {
        String detectionData = detect.getDetectionData();
        return String.format(
            "作为一位专业的皮肤癌检测与诊断分析专家，请根据以下皮肤镜检测结果进行详细分析：\n\n" +
            "【检测数据】\n%s\n\n" +
            "请提供：\n" +
            "1. 病变类型分析 - 良性皮肤肿瘤(Benign)还是恶性皮肤癌(Malignant)\n" +
            "2. 位置与范围评估\n" +
            "3. 严重程度初步判断\n" +
            "4. 可能原因分析\n" +
            "5. 临床处置建议\n" +
            "6. 复检与复查建议\n" +
            "7. 注意事项\n\n" +
            "输出要求：请使用纯文本中文输出，不要使用Markdown格式；不要使用星号*、井号#、反引号`、项目符号等特殊格式符号；标题直接写中文标题加冒号。\n" +
            "最后必须单独输出这一句话：本分析报告仅供参考，仅为辅助检测分析，不替代专业病理诊断和临床决策",
            detectionData
        );
    }

    private String findDefaultModelPath() {
        java.io.File modelFolder = new java.io.File(SKIN_MODEL_ROOT);
        if (!modelFolder.exists() || !modelFolder.isDirectory()) {
            log.warn("模型目录不存在: {}", SKIN_MODEL_ROOT);
            return SKIN_MODEL_ROOT;
        }

        java.util.List<java.io.File> bestPtFiles = new java.util.ArrayList<>();
        collectBestPtFiles(modelFolder, bestPtFiles);

        if (bestPtFiles.isEmpty()) {
            log.warn("未找到任何 best.pt 模型文件");
            return SKIN_MODEL_ROOT;
        }

        bestPtFiles.sort(java.util.Comparator.comparing(java.io.File::getAbsolutePath));
        java.io.File firstBestPt = bestPtFiles.get(0);
        log.info("找到默认模型: {} ({})", extractModelLabel(firstBestPt, modelFolder), firstBestPt.getAbsolutePath());
        return firstBestPt.getAbsolutePath();
    }

    private String getPythonDetectUrl() {
        return configService.getConfigValue("global.python_detect_url", pythonDetectUrl);
    }

    private String cleanAiAnalysisText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\r\n", "\n")
                .replaceAll("[*#`]+", "")
                .replaceAll("(?m)^\\s*[-•]\\s+", "")
                .replaceAll("(?m)^\\s*>\\s*", "")
                .trim();
    }
}
