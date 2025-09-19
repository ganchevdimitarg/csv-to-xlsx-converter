package com.ganchevdimitarg.csvtoxlsxparser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class FileUtil {
    private  static final Logger LOG = Logger.getLogger(FileUtil.class.getName());

    public static final String UPLOAD_DIR = "/resource/xlsx";

    public static Path saveFile(String fileName, byte[] fileContent) throws IOException {
        Path filePath = Paths.get(UPLOAD_DIR, fileName);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, fileContent);
        return filePath;
    }

    public static byte[] readFile(String fileName) throws IOException {
        Path filePath = Paths.get(UPLOAD_DIR, fileName);
        return Files.readAllBytes(filePath);
    }

    public static void deleteFile(String fileName) {
        Path filePath = Paths.get(UPLOAD_DIR, fileName);
        try {
            Files.delete(filePath);
            LOG.info("File deleted successfully: " + fileName);
        } catch (IOException e) {
            LOG.warning("Error deleting file: " + fileName);
            e.printStackTrace();
        }

    }
}