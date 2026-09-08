package com.example.springb.common;

import java.io.File;

public final class UploadPathResolver {

    private UploadPathResolver() {
    }

    public static File uploadRoot(String uploadPath) {
        File configured = new File(uploadPath == null || uploadPath.isBlank() ? "uploads/" : uploadPath);
        if (configured.isAbsolute()) {
            return configured;
        }

        File userDir = new File(System.getProperty("user.dir"));
        if ("springb".equalsIgnoreCase(userDir.getName())) {
            File parent = userDir.getParentFile();
            if (parent != null) {
                return new File(parent, uploadPath);
            }
        }

        return new File(userDir, uploadPath);
    }

    public static File resolve(String uploadPath, String relativePath) {
        return new File(uploadRoot(uploadPath), relativePath);
    }
}
