package com.example.springb.controller;

import com.example.springb.common.UploadPathResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件访问控制器
 * 提供上传文件的访问接口
 */
@RestController
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    /**
     * 访问文件
     * 路径格式: /files/detect/202401/xxx.jpg
     */
    @RequestMapping("/files/**")
    public void getFile(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 获取请求URI，提取文件路径
            String requestUri = request.getRequestURI();
            String contextPath = request.getContextPath();
            String filePath;
            
            // 去除contextPath前缀
            if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
                requestUri = requestUri.substring(contextPath.length());
            }
            
            // 提取/files/后面的路径
            if (requestUri.startsWith("/files/")) {
                filePath = requestUri.substring("/files/".length());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            // 安全检查：防止目录遍历攻击
            if (filePath.contains("..") || filePath.contains("~")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            
            // 构建完整文件路径
            File file = UploadPathResolver.resolve(uploadPath, filePath);
            String fullPath = file.getAbsolutePath();
            
            log.debug("访问文件: {}", fullPath);
            
            // 检查文件是否存在且是文件
            if (!file.exists() || !file.isFile()) {
                log.warn("文件不存在: {}", fullPath);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 设置Content-Type
            String fileName = file.getName();
            String ext = "";
            int lastDotIndex = fileName.lastIndexOf(".");
            if (lastDotIndex > 0) {
                ext = fileName.substring(lastDotIndex + 1).toLowerCase();
            }
            
            String contentType = "application/octet-stream";
            switch (ext) {
                case "jpg":
                case "jpeg":
                    contentType = "image/jpeg";
                    break;
                case "png":
                    contentType = "image/png";
                    break;
                case "gif":
                    contentType = "image/gif";
                    break;
                case "bmp":
                    contentType = "image/bmp";
                    break;
                case "dcm":
                    contentType = "application/dicom";
                    break;
                case "mp4":
                    contentType = "video/mp4";
                    break;
                case "avi":
                    contentType = "video/x-msvideo";
                    break;
                case "mov":
                    contentType = "video/quicktime";
                    break;
                case "mkv":
                    contentType = "video/x-matroska";
                    break;
                case "wmv":
                    contentType = "video/x-ms-wmv";
                    break;
                case "flv":
                    contentType = "video/x-flv";
                    break;
                case "webm":
                    contentType = "video/webm";
                    break;
            }
            
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            response.setHeader("Accept-Ranges", "bytes");

            String rangeHeader = request.getHeader("Range");
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                writeRangeResponse(file, contentType, rangeHeader, response);
                log.debug("文件分段访问成功: {}, range={}", filePath, rangeHeader);
                return;
            }

            response.setContentLengthLong(file.length());
            
            // 读取文件并写入响应
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                os.flush();
            }
            
            log.debug("文件访问成功: {}", filePath);
            
        } catch (Exception e) {
            log.error("文件访问失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void writeRangeResponse(File file, String contentType, String rangeHeader, HttpServletResponse response) throws Exception {
        long fileLength = file.length();
        long start = 0;
        long end = fileLength - 1;

        String range = rangeHeader.substring("bytes=".length()).trim();
        int dashIndex = range.indexOf('-');
        if (dashIndex >= 0) {
            String startText = range.substring(0, dashIndex).trim();
            String endText = range.substring(dashIndex + 1).trim();

            if (!startText.isEmpty()) {
                start = Long.parseLong(startText);
            }
            if (!endText.isEmpty()) {
                end = Long.parseLong(endText);
            }
        }

        if (start < 0 || start >= fileLength || end < start) {
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            response.setHeader("Content-Range", "bytes */" + fileLength);
            return;
        }

        end = Math.min(end, fileLength - 1);
        long contentLength = end - start + 1;

        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setContentType(contentType);
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        response.setHeader("Accept-Ranges", "bytes");
        response.setContentLengthLong(contentLength);

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            long skipped = fis.skip(start);
            while (skipped < start) {
                long nextSkipped = fis.skip(start - skipped);
                if (nextSkipped <= 0) {
                    break;
                }
                skipped += nextSkipped;
            }

            byte[] buffer = new byte[8192];
            long remaining = contentLength;
            while (remaining > 0) {
                int len = fis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (len == -1) {
                    break;
                }
                os.write(buffer, 0, len);
                remaining -= len;
            }
            os.flush();
        }
    }
}
