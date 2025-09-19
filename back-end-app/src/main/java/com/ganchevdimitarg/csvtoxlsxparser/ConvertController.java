package com.ganchevdimitarg.csvtoxlsxparser;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static com.ganchevdimitarg.csvtoxlsxparser.FileUtil.UPLOAD_DIR;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:3000")
public class ConvertController {

    private final CsvToXlsxConverterService csvToXlsxConverterService;

    public ConvertController(CsvToXlsxConverterService csvToXlsxConverterService) {
        this.csvToXlsxConverterService = csvToXlsxConverterService;
    }

    @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> convert(@RequestParam("file") MultipartFile file, @RequestParam String outputFileName) {
        try {
            // Convert CSV to XLSX using CsvToXlsxConverterService
            byte[] convertedFileContent = csvToXlsxConverterService.convert(file.getInputStream());

            // Save the converted file using FileUtil
            Path savedFilePath = FileUtil.saveFile(outputFileName, convertedFileContent);

            return ResponseEntity.ok("File saved successfully at: " + savedFilePath.toString());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error converting or saving file");
        }
    }

    @GetMapping(value = "/download/{outputFileName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadFile(@PathVariable String outputFileName) {
        try {
            byte[] fileContent = FileUtil.readFile(outputFileName);

            ByteArrayResource resource = new ByteArrayResource(fileContent);

            FileUtil.deleteFile(outputFileName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFileName + "\"")
                    .contentLength(fileContent.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping(value = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getAvailableFiles() {
        try {
            File folder = new File(UPLOAD_DIR);
            List<String> files = Arrays.asList(folder.list());
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}