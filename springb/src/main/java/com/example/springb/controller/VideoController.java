package com.example.springb.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.example.springb.common.Result;
import com.example.springb.common.UploadPathResolver;
import com.example.springb.entity.VideoDetect;
import com.example.springb.service.AIService;
import com.example.springb.service.ConfigService;
import com.example.springb.service.VideoDetectService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/video")
public class VideoController {

    private static final Logger log = LoggerFactory.getLogger(VideoController.class);

    @Resource
    private VideoDetectService videoDetectService;

    @Resource
    private AIService aiService;

    @Resource
    private ConfigService configService;

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    @Value("${file.access.url:http://localhost:1907/files/}")
    private String fileAccessUrl;

    @Value("${python.detect.url:http://localhost:2026}")
    private String pythonDetectUrl;

    @Value("${python.video-convert.url:http://localhost:2026}")
    private String pythonVideoConvertUrl;

    private static final List<String> ALLOWED_VIDEO_FORMATS = Arrays.asList("mp4", "avi", "mov", "mkv", "wmv", "flv", "webm");
    private static final List<String> ALLOWED_IMAGE_FORMATS = Arrays.asList("jpg", "jpeg", "png", "bmp", "tiff", "webp");
    private static final List<String> BATCH_OUTPUT_FORMATS = Arrays.asList("mp4", "avi", "webm");
    private static final List<String> BATCH_RESOLUTIONS = Arrays.asList("original", "1920x1080", "1280x720", "640x480");
    private static final long MAX_BATCH_IMAGE_SIZE = 50L * 1024 * 1024;
    private static final int MAX_BATCH_IMAGE_COUNT = 500;

    // ==================== 基础CRUD接口 ====================

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result health() {
        return Result.success("Video检测服务运行正常");
    }

    /**
     * 下载检测结果视频（触发浏览器下载对话框）
     */
    @GetMapping("/download/{id}")
    public void downloadVideo(@PathVariable Integer id, HttpServletResponse response) {
        try {
            VideoDetect video = videoDetectService.selectById(id);
            if (video == null || video.getResultVideoUrl() == null || video.getResultVideoUrl().isEmpty()) {
                response.setStatus(404);
                response.getWriter().write("视频不存在");
                return;
            }

            String videoUrl = video.getResultVideoUrl();
            String fileName = video.getOriginalVideoName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "result_video_" + id + ".mp4";
            }
            if (!fileName.contains(".")) {
                fileName = fileName + ".mp4";
            }
            java.io.File videoFile;

            if (videoUrl.startsWith("http")) {
                // Python服务返回的完整URL - 代理下载
                String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
                response.setContentType("video/mp4");
                response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

                java.net.URL remoteUrl = new java.net.URL(videoUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) remoteUrl.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(60000);

                try (java.io.InputStream is = conn.getInputStream();
                     java.io.OutputStream os = response.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    os.flush();
                }
                conn.disconnect();
                return;
            } else {
                // 本地文件路径
                videoFile = UploadPathResolver.resolve(uploadPath, videoUrl);
            }

            if (!videoFile.exists() || !videoFile.isFile()) {
                response.setStatus(404);
                response.getWriter().write("视频文件不存在: " + videoFile.getAbsolutePath());
                return;
            }

            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");

            response.setContentType("video/mp4");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            response.setHeader("Content-Length", String.valueOf(videoFile.length()));

            try (java.io.FileInputStream fis = new java.io.FileInputStream(videoFile);
                 java.io.OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } catch (Exception e) {
            log.error("视频下载失败", e);
            try {
                response.setStatus(500);
                response.getWriter().write("下载失败: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }

    /**
     * 分页查询视频检测记录
     */
    @GetMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             VideoDetect videoDetect) {
        return Result.success(videoDetectService.selectPage(pageNum, pageSize, videoDetect));
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        VideoDetect videoDetect = videoDetectService.selectById(id);
        return Result.success(videoDetect);
    }

    /**
     * 删除视频检测记录
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        videoDetectService.deleteById(id);
        return Result.success();
    }

    // ==================== 视频上传与检测接口 ====================

    /**
     * 上传原始视频并创建检测记录
     */
    @PostMapping("/upload")
    public Result uploadVideo(@RequestParam("file") MultipartFile file,
                              @RequestParam("userId") Integer userId,
                              @RequestParam("userName") String userName) {
        log.info("收到视频上传请求: userId={}, userName={}, fileName={}, fileSize={}",
                userId, userName, file.getOriginalFilename(), file.getSize());

        try {
            String originalName = file.getOriginalFilename();
            String ext = FileUtil.extName(originalName);

            // 验证视频格式
            if (!ALLOWED_VIDEO_FORMATS.contains(ext.toLowerCase())) {
                return Result.error("不支持的视频格式: " + ext + "，请上传 mp4/avi/mov/mkv/wmv/flv/webm 格式");
            }

            // 生成唯一文件名
            String newFileName = UUID.randomUUID() + "." + ext;

            // 创建日期目录
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String relativePath = "video/" + dateDir + "/" + newFileName;

            // 构建完整路径
            File dir = UploadPathResolver.resolve(uploadPath, "video/" + dateDir);
            File destFile = UploadPathResolver.resolve(uploadPath, relativePath);
            String fullDir = dir.getAbsolutePath();
            String fullPath = destFile.getAbsolutePath();

            log.info("视频保存目录: {}", fullDir);
            log.info("视频保存路径: {}", fullPath);

            // 确保目录存在
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                log.info("创建目录: {}, 结果: {}", fullDir, created);
                if (!created) {
                    return Result.error("创建上传目录失败: " + fullDir);
                }
            }

            // 保存文件
            file.transferTo(destFile);
            log.info("视频保存成功: {}", fullPath);

            // 创建检测记录
            VideoDetect videoDetect = new VideoDetect();
            videoDetect.setUserId(userId);
            videoDetect.setUserName(userName);
            videoDetect.setOriginalVideoName(originalName);
            videoDetect.setOriginalVideoUrl(relativePath);
            videoDetect.setOriginalVideoSize(file.getSize());
            videoDetect.setOriginalVideoFormat(ext);
            videoDetect.setDetectStatus(0); // 待检测
            videoDetect.setAiStatus(0);     // 未分析
            videoDetect.setSourceType("upload");

            videoDetectService.add(videoDetect);
            log.info("视频检测记录创建成功: id={}", videoDetect.getId());

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("id", videoDetect.getId());
            result.put("originalVideoUrl", fileAccessUrl + relativePath);
            result.put("originalVideoName", originalName);

            return Result.success(result);

        } catch (IOException e) {
            log.error("视频上传失败", e);
            return Result.error("视频上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("视频上传异常", e);
            return Result.error("视频上传异常: " + e.getMessage());
        }
    }

    /**
     * 批量图片合成为视频并创建待检测记录
     */
    @PostMapping("/imagesToVideo")
    public Result imagesToVideo(@RequestParam("images") MultipartFile[] images,
                                @RequestParam(value = "durationPerImage", defaultValue = "0.3") Double durationPerImage,
                                @RequestParam(value = "outputFormat", defaultValue = "mp4") String outputFormat,
                                @RequestParam(value = "resolution", defaultValue = "original") String resolution,
                                @RequestParam(value = "userId", required = false) Integer userId,
                                @RequestParam(value = "userName", required = false) String userName) {
        File tempDir = null;
        List<File> tempFiles = new ArrayList<>();

        try {
            if (images == null || images.length == 0) {
                return Result.error("未上传图片文件");
            }
            if (images.length > MAX_BATCH_IMAGE_COUNT) {
                return Result.error("图片数量超过限制（最多500张）");
            }

            durationPerImage = durationPerImage == null ? 0.3 : durationPerImage;
            if (durationPerImage < 0.1 || durationPerImage > 5.0) {
                return Result.error("每张图时长需在0.1到5.0秒之间");
            }

            outputFormat = normalizeLower(outputFormat, "mp4");
            resolution = normalizeLower(resolution, "original");
            if (!BATCH_OUTPUT_FORMATS.contains(outputFormat)) {
                return Result.error("输出格式仅支持 MP4、AVI、WebM");
            }
            if (!BATCH_RESOLUTIONS.contains(resolution)) {
                return Result.error("不支持的分辨率参数: " + resolution);
            }

            tempDir = Files.createTempDirectory("skin_batch_images_").toFile();
            int validIndex = 0;
            for (MultipartFile image : images) {
                if (image == null || image.isEmpty()) {
                    continue;
                }
                if (image.getSize() > MAX_BATCH_IMAGE_SIZE) {
                    log.warn("跳过超大图片: {}, size={}", image.getOriginalFilename(), image.getSize());
                    continue;
                }

                String originalName = image.getOriginalFilename();
                String ext = normalizeLower(FileUtil.extName(originalName), "");
                if (!ALLOWED_IMAGE_FORMATS.contains(ext)) {
                    log.warn("跳过非图片文件: {}", originalName);
                    continue;
                }

                String safeFileName = String.format(Locale.ROOT, "%04d_%s.%s", validIndex++, UUID.randomUUID(), ext);
                File savedFile = new File(tempDir, safeFileName);
                image.transferTo(savedFile);
                tempFiles.add(savedFile);
            }

            if (tempFiles.isEmpty()) {
                return Result.error("未找到有效图片文件，请选择 jpg/png/bmp/tiff/webp 图片");
            }

            cn.hutool.json.JSONObject pythonData = requestImagesToVideo(tempFiles, durationPerImage, outputFormat, resolution);
            String relativePythonPath = pythonData.getStr("video_url");
            String generatedVideoUrl;
            if (relativePythonPath != null && !relativePythonPath.isEmpty()) {
                generatedVideoUrl = getPythonVideoConvertUrl() + "/" + relativePythonPath.replace("\\", "/");
            } else {
                generatedVideoUrl = pythonData.getStr("video_url_full");
                if (generatedVideoUrl == null || generatedVideoUrl.isEmpty()) {
                    return Result.error("Python服务未返回生成视频地址");
                }
            }

            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String newFileName = UUID.randomUUID() + "." + outputFormat;
            String relativePath = "video/" + dateDir + "/" + newFileName;
            File outputDir = UploadPathResolver.resolve(uploadPath, "video/" + dateDir);
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                return Result.error("创建视频保存目录失败: " + outputDir.getAbsolutePath());
            }

            File localVideoFile = UploadPathResolver.resolve(uploadPath, relativePath);
            downloadFile(generatedVideoUrl, localVideoFile);
            if (!localVideoFile.exists() || localVideoFile.length() == 0) {
                return Result.error("生成视频保存失败");
            }
            if (localVideoFile.length() > 500L * 1024 * 1024) {
                localVideoFile.delete();
                return Result.error("生成视频超过500MB限制");
            }

            VideoDetect videoDetect = new VideoDetect();
            videoDetect.setUserId(userId != null ? userId : 1);
            videoDetect.setUserName((userName != null && !userName.isBlank()) ? userName : "管理员");
            videoDetect.setOriginalVideoName("批量图片生成_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + "." + outputFormat);
            videoDetect.setOriginalVideoUrl(relativePath);
            videoDetect.setOriginalVideoSize(localVideoFile.length());
            videoDetect.setOriginalVideoFormat(outputFormat);
            videoDetect.setDetectStatus(0);
            videoDetect.setAiStatus(0);
            videoDetect.setSourceType("batch_image");
            videoDetect.setTotalFrames(pythonData.getInt("total_frames", tempFiles.size()));
            videoDetect.setFps(pythonData.getDouble("fps", 0.0));
            videoDetect.setDuration(pythonData.getDouble("duration", 0.0));

            videoDetectService.add(videoDetect);

            Map<String, Object> result = new HashMap<>();
            result.put("id", videoDetect.getId());
            result.put("recordId", videoDetect.getId());
            result.put("videoUrl", fileAccessUrl + relativePath);
            result.put("originalVideoUrl", fileAccessUrl + relativePath);
            result.put("relativePath", relativePath);
            result.put("totalFrames", videoDetect.getTotalFrames());
            result.put("duration", videoDetect.getDuration());
            result.put("fps", videoDetect.getFps());
            result.put("imageCount", pythonData.getInt("image_count", tempFiles.size()));
            result.put("sourceType", "batch_image");

            return Result.success(result);

        } catch (Exception e) {
            log.error("批量图片转视频失败", e);
            return Result.error("转换失败: " + e.getMessage());
        } finally {
            for (File file : tempFiles) {
                if (file != null && file.exists()) {
                    boolean deleted = file.delete();
                    log.debug("清理临时图片{}: {}", deleted ? "成功" : "失败", file.getAbsolutePath());
                }
            }
            if (tempDir != null && tempDir.exists()) {
                boolean deleted = tempDir.delete();
                log.debug("清理临时目录{}: {}", deleted ? "成功" : "失败", tempDir.getAbsolutePath());
            }
        }
    }

    /**
     * 调用Python检测服务进行视频检测
     *
     * 参数:
     * - id: 检测记录ID
     * - ptPath: 模型文件路径 (可选)
     * - conf: 置信度阈值 (可选，默认0.25)
     * - skipFrames: 跳帧处理 (可选，默认0=每帧处理)
     */
    @PostMapping("/startDetect/{id}")
    public Result startDetect(@PathVariable Integer id,
                              @RequestParam(required = false) String ptPath,
                              @RequestParam(required = false) Double conf,
                              @RequestParam(required = false, defaultValue = "0") Integer skipFrames) {
        if (ptPath == null || ptPath.isEmpty()) {
            ptPath = findDefaultModelPath();
        }
        if (conf == null) {
            conf = 0.25;
        }

        try {
            VideoDetect videoDetect = videoDetectService.selectById(id);
            if (videoDetect == null) {
                return Result.error("检测记录不存在");
            }

            // 更新为检测中状态
            videoDetect.setDetectStatus(1);
            videoDetectService.update(videoDetect);

            log.info("开始视频检测记录: {}, ptPath={}, conf={}, skipFrames={}", id, ptPath, conf, skipFrames);

            // 构建视频完整路径
            File videoFile = UploadPathResolver.resolve(uploadPath, videoDetect.getOriginalVideoUrl());
            if (!videoFile.exists()) {
                throw new RuntimeException("视频文件不存在: " + videoFile.getAbsolutePath());
            }

            // 调用Python检测服务（使用更长的超时时间，视频处理较慢）
            String url = getPythonDetectUrl() + "/detect_video";
            log.info("调用Python视频检测服务: {}", url);

            HttpResponse response = HttpRequest.post(url)
                    .form("file", videoFile)
                    .form("pt_path", ptPath)
                    .form("conf", String.valueOf(conf))
                    .form("skip_frames", String.valueOf(skipFrames))
                    .timeout(600000) // 10分钟超时
                    .execute();

            if (response.getStatus() != 200) {
                throw new RuntimeException("Python服务响应异常: " + response.getStatus());
            }

            // 解析响应
            String responseBody = response.body();
            log.debug("Python视频检测服务响应: {}", responseBody);

            cn.hutool.json.JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
            int code = jsonResponse.getInt("code", -1);

            if (code != 0) {
                String message = jsonResponse.getStr("message", "视频检测失败");
                throw new RuntimeException(message);
            }

            // 解析检测结果
            cn.hutool.json.JSONObject data = jsonResponse.getJSONObject("data");

            // 获取结果视频URL（优先使用Python直连URL，避免文件拷贝和编码兼容问题）
            String resultVideoUrl = data.getStr("result_video_url");
            log.info("Python结果视频URL: {}", resultVideoUrl);

            // 构建detection_data（包含检测汇总和帧数据）
            cn.hutool.json.JSONObject detectionSummary = data.getJSONObject("detection_summary");
            cn.hutool.json.JSONObject videoProperties = data.getJSONObject("video_properties");
            cn.hutool.json.JSONArray frameResults = data.getJSONArray("frame_results");

            // 将完整数据合并为一个JSON对象存储
            cn.hutool.json.JSONObject detectionData = new cn.hutool.json.JSONObject();
            if (detectionSummary != null) {
                detectionData.putAll(detectionSummary);
            }
            if (videoProperties != null) {
                detectionData.putAll(videoProperties);
            }
            if (frameResults != null) {
                detectionData.set("frame_results", frameResults);
            }

            // 更新检测记录
            videoDetect.setDetectStatus(2); // 检测完成
            videoDetect.setDetectionData(detectionData.toString());
            if (resultVideoUrl != null && !resultVideoUrl.isEmpty()) {
                videoDetect.setResultVideoUrl(resultVideoUrl);
            }
            if (videoProperties != null) {
                videoDetect.setTotalFrames(videoProperties.getInt("total_frames"));
                videoDetect.setFps(videoProperties.getDouble("fps"));
                videoDetect.setDuration(videoProperties.getDouble("duration"));
            }
            videoDetectService.update(videoDetect);

            return Result.success(videoDetect);

        } catch (Exception e) {
            log.error("视频检测失败", e);
            VideoDetect update = new VideoDetect();
            update.setId(id);
            update.setDetectStatus(3); // 检测失败
            videoDetectService.update(update);
            return Result.error("视频检测失败: " + e.getMessage());
        }
    }

    // ==================== AI分析接口 ====================

    /**
     * 调用AI大模型进行视频检测结果辅助分析
     */
    @PostMapping("/aiAnalysis/{id}")
    public Result aiAnalysis(@PathVariable Integer id,
                             @RequestParam String model) {
        try {
            VideoDetect videoDetect = videoDetectService.selectById(id);
            if (videoDetect == null) {
                return Result.error("检测记录不存在");
            }

            if (videoDetect.getDetectStatus() != 2) {
                return Result.error("请先完成视频检测");
            }

            if (!aiService.isConfigured(model)) {
                return Result.error("AI模型 '" + model + "' 未配置API Key，请联系管理员配置");
            }

            videoDetect.setAiStatus(1);
            videoDetect.setAiModel(model);
            videoDetectService.update(videoDetect);

            log.info("开始AI分析视频记录: {}, 模型: {}", id, model);

            String prompt = buildAnalysisPrompt(videoDetect);
            String aiResult = cleanAiAnalysisText(aiService.analyze(model, prompt));

            videoDetect.setAiAnalysisResult(aiResult);
            videoDetect.setAiAnalysisTime(LocalDateTime.now());
            videoDetect.setAiStatus(2);
            videoDetectService.update(videoDetect);

            log.info("AI分析完成: videoDetectId={}, model={}", id, model);
            return Result.success(videoDetect);

        } catch (Exception e) {
            log.error("AI分析失败", e);
            VideoDetect update = new VideoDetect();
            update.setId(id);
            update.setAiStatus(3);
            videoDetectService.update(update);
            return Result.error("AI分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取AI模型配置状态
     */
    @GetMapping("/aiConfigStatus")
    public Result getAiConfigStatus() {
        return Result.success(aiService.getConfigStatus());
    }

    // ==================== 模型列表接口 ====================

    /**
     * 列出可用的检测模型（复用与detect相同的模型目录）
     */
    @GetMapping("/listModels")
    public Result listModels() {
        try {
            String skinModelRoot = "D:/algorithms/V11-dmt/runs/skin-cancer";
            java.util.List<java.util.Map<String, String>> models = new java.util.ArrayList<>();

            java.io.File modelFolder = new java.io.File(skinModelRoot);
            if (!modelFolder.exists() || !modelFolder.isDirectory()) {
                return Result.error("模型目录不存在: " + skinModelRoot);
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

    // ==================== 私有方法 ====================

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
            if (weightsDir == null) return bestPtFile.getName();
            java.io.File modelDir = weightsDir.getParentFile();
            if (modelDir != null) return modelDir.getName();
            String fileName = bestPtFile.getName();
            if (fileName.endsWith(".pt")) fileName = fileName.substring(0, fileName.length() - 3);
            return fileName;
        } catch (Exception e) {
            log.warn("提取模型标签失败: {}", bestPtFile.getAbsolutePath(), e);
            return bestPtFile.getName().replace(".pt", "");
        }
    }

    private String findDefaultModelPath() {
        String skinModelRoot = "D:/algorithms/V11-dmt/runs/skin-cancer";
        java.io.File modelFolder = new java.io.File(skinModelRoot);
        if (!modelFolder.exists() || !modelFolder.isDirectory()) {
            log.warn("模型目录不存在: {}", skinModelRoot);
            return skinModelRoot;
        }
        java.util.List<java.io.File> bestPtFiles = new java.util.ArrayList<>();
        collectBestPtFiles(modelFolder, bestPtFiles);
        if (bestPtFiles.isEmpty()) {
            log.warn("未找到任何 best.pt 模型文件");
            return skinModelRoot;
        }
        bestPtFiles.sort(java.util.Comparator.comparing(java.io.File::getAbsolutePath));
        java.io.File firstBestPt = bestPtFiles.get(0);
        log.info("找到默认模型: {} ({})", extractModelLabel(firstBestPt, modelFolder), firstBestPt.getAbsolutePath());
        return firstBestPt.getAbsolutePath();
    }

    private String normalizeLower(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private cn.hutool.json.JSONObject requestImagesToVideo(List<File> images,
                                                           Double durationPerImage,
                                                           String outputFormat,
                                                           String resolution) {
        String url = getPythonVideoConvertUrl() + "/images_to_video";
        log.info("调用Python图片转视频服务: {}, imageCount={}", url, images.size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (File image : images) {
            body.add("images", new FileSystemResource(image));
        }
        body.add("duration_per_image", String.valueOf(durationPerImage));
        body.add("output_format", outputFormat);
        body.add("resolution", resolution);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30000);
        requestFactory.setReadTimeout(300000);

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Python服务响应异常: " + response.getStatusCode().value());
        }

        cn.hutool.json.JSONObject jsonResponse = JSONUtil.parseObj(response.getBody());
        int code = jsonResponse.getInt("code", -1);
        if (code != 200 && code != 0) {
            throw new RuntimeException(jsonResponse.getStr("msg", "视频生成失败"));
        }

        cn.hutool.json.JSONObject data = jsonResponse.getJSONObject("data");
        if (data == null) {
            throw new RuntimeException("Python服务未返回视频数据");
        }
        return data;
    }

    private void downloadFile(String fileUrl, File targetFile) throws IOException {
        log.info("下载生成视频: {} -> {}", fileUrl, targetFile.getAbsolutePath());
        try (InputStream inputStream = new URL(fileUrl).openStream();
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private String getPythonDetectUrl() {
        return configService.getConfigValue("global.python_detect_url", pythonDetectUrl);
    }

    private String getPythonVideoConvertUrl() {
        return configService.getConfigValue("global.python_detect_url", pythonVideoConvertUrl);
    }

    /**
     * 构建AI分析Prompt（视频版）
     */
    private String buildAnalysisPrompt(VideoDetect videoDetect) {
        String detectionData = videoDetect.getDetectionData();
        return String.format(
            "作为一位专业的皮肤癌视频分析专家，请根据以下视频检测结果进行详细分析：\n\n" +
            "【视频检测数据】\n%s\n\n" +
            "【分析要求】\n" +
            "1. 病变类别分析 - 视频中检测到的主要皮肤病变类型及其分布\n" +
            "2. 严重程度评估 - 基于病变出现频率和置信度评估皮肤的总体状况\n" +
            "3. 时序变化分析 - 分析病变在视频不同帧中的变化情况\n" +
            "4. 可能原因分析 - 结合病变类型分析可能的原因\n" +
            "5. 临床处置建议 - 给出具体的临床建议和随访计划\n" +
            "6. 复检与复查建议 - 建议的复查周期和关注重点\n" +
            "7. 注意事项\n\n" +
            "输出要求：请使用纯文本中文输出，不要使用Markdown格式；不要使用星号*、井号#、反引号`、项目符号等特殊格式符号；标题直接写中文标题加冒号。\n" +
            "最后必须单独输出这一句话：本分析报告仅供参考，仅为辅助检测分析，不替代专业病理诊断和临床决策",
            detectionData
        );
    }

    /**
     * 清理AI返回的Markdown格式符号
     */
    private String cleanAiAnalysisText(String text) {
        if (text == null) return "";
        return text
                .replace("\r\n", "\n")
                .replaceAll("[*#`]+", "")
                .replaceAll("(?m)^\\s*[-•]\\s+", "")
                .replaceAll("(?m)^\\s*>\\s*", "")
                .trim();
    }
}
