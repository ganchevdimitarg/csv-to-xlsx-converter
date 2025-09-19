package com.ganchevdimitarg.csvtoxlsxparser;

import module java.base;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;


@Service
public class CsvToXlsxConverterService {

    /**
     * Converts a CSV file from resources to XLSX format (Java 24 style)
     *
     * @param resourcePath Path to the CSV file in resources (e.g., "data.csv" or "data/input.csv")
     * @param xlsxFilePath Path for the output XLSX file
     * @param delimiter CSV delimiter (default: comma)
     * @return File object representing the created XLSX file
     * @throws IOException if file operations fail
     */
    public File convertCsvFromResourcesToXlsx(String resourcePath, String xlsxFilePath, String delimiter)
            throws IOException {

        // Get resource as InputStream using modern Java approach
        try (var inputStream = CsvToXlsxConverterService.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("CSV resource not found: " + resourcePath);
            }

            return convertCsvStreamToXlsx(inputStream, xlsxFilePath, delimiter);
        }
    }

    /**
     * Converts a CSV file to XLSX format
     *
     * @param csvFilePath Path to the input CSV file
     * @param xlsxFilePath Path for the output XLSX file
     * @param delimiter CSV delimiter (default: comma)
     * @return File object representing the created XLSX file
     * @throws IOException if file operations fail
     */
    public File convertCsvToXlsx(String csvFilePath, String xlsxFilePath, String delimiter)
            throws IOException {

        // Validate input file exists
        var csvPath = Paths.get(csvFilePath);
        if (!Files.exists(csvPath)) {
            throw new FileNotFoundException("CSV file not found: " + csvFilePath);
        }

        try (var inputStream = Files.newInputStream(csvPath)) {
            return convertCsvStreamToXlsx(inputStream, xlsxFilePath, delimiter);
        }
    }

    /**
     * Converts a CSV InputStream to XLSX format (Core conversion logic)
     */
    public File convertCsvStreamToXlsx(InputStream csvInputStream, String xlsxFilePath, String delimiter)
            throws IOException {

        // Create workbook and sheet
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Data");

            // Read CSV and populate Excel sheet
            try (var reader = new BufferedReader(new InputStreamReader(csvInputStream))) {
                String line;
                int rowNum = 0;

                while ((line = reader.readLine()) != null) {
                    var row = sheet.createRow(rowNum++);
                    var cells = parseCsvLine(line, delimiter);

                    for (int i = 0; i < cells.length; i++) {
                        var cell = row.createCell(i);
                        var cellValue = cells[i].trim();

                        // Try to detect and set appropriate cell type
                        setCellValue(cell, cellValue);
                    }
                }
            }

            // Auto-size columns for better appearance
            var firstRow = sheet.getRow(0);
            if (firstRow != null) {
                for (int i = 0; i < firstRow.getLastCellNum(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // Write to XLSX file
            var xlsxPath = Paths.get(xlsxFilePath);

            // Only create directories if parent exists
            var parentDir = xlsxPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            try (var outputStream = Files.newOutputStream(xlsxPath)) {
                workbook.write(outputStream);
            }

            return xlsxPath.toFile();
        }
    }

    /**
     * Overloaded method for resources with default comma delimiter
     */
    public File convertCsvFromResourcesToXlsx(String resourcePath, String xlsxFilePath) throws IOException {
        return convertCsvFromResourcesToXlsx(resourcePath, xlsxFilePath, ",");
    }

    /**
     * Overloaded method with default comma delimiter
     */
    public File convertCsvToXlsx(String csvFilePath, String xlsxFilePath) throws IOException {
        return convertCsvToXlsx(csvFilePath, xlsxFilePath, ",");
    }

    /**
     * Parses a CSV line handling quoted fields and embedded delimiters
     */
    private String[] parseCsvLine(String line, String delimiter) {
        var fields = new ArrayList<String>();
        var currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Handle escaped quotes
                    currentField.append('"');
                    i++; // Skip the next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter.charAt(0) && !inQuotes && delimiter.length() == 1) {
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else if (line.substring(i).startsWith(delimiter) && !inQuotes) {
                fields.add(currentField.toString());
                currentField.setLength(0);
                i += delimiter.length() - 1; // Skip the rest of the delimiter
            } else {
                currentField.append(c);
            }
        }

        // Add the last field
        fields.add(currentField.toString());

        return fields.toArray(String[]::new);
    }

    /**
     * Sets cell value with appropriate type detection
     */
    private void setCellValue(Cell cell, String value) {
        if (value.isEmpty()) {
            cell.setCellValue("");
            return;
        }

        // Remove quotes if present
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            value = value.substring(1, value.length() - 1);
        }

        // Try to parse as number
        try {
            if (value.contains(".")) {
                var doubleValue = Double.parseDouble(value);
                cell.setCellValue(doubleValue);
            } else {
                var longValue = Long.parseLong(value);
                cell.setCellValue(longValue);
            }
        } catch (NumberFormatException e) {
            // Try to parse as boolean
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                cell.setCellValue(Boolean.parseBoolean(value));
            } else {
                // Default to string
                cell.setCellValue(value);
            }
        }
    }

    /**
     * Spring Boot compatible method for converting CSV from resources
     */
    public File convertCsvFromResourcesInSpringBoot(String resourcePath, String xlsxFilePath)
            throws IOException {
        return convertCsvFromResourcesInSpringBoot(resourcePath, xlsxFilePath, ",");
    }

    /**
     * Spring Boot compatible method for converting CSV from resources with custom delimiter
     */
    public File convertCsvFromResourcesInSpringBoot(String resourcePath, String xlsxFilePath, String delimiter)
            throws IOException {

        // Try multiple class loaders for Spring Boot compatibility
        InputStream inputStream;

        // Try current class loader first
        inputStream = CsvToXlsxConverterService.class.getClassLoader().getResourceAsStream(resourcePath);

        // Try context class loader if first attempt fails
        if (inputStream == null) {
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        }

        // Try system class loader as last resort
        if (inputStream == null) {
            inputStream = ClassLoader.getSystemResourceAsStream(resourcePath);
        }

        if (inputStream == null) {
            throw new FileNotFoundException("CSV resource not found in classpath: " + resourcePath +
                    "\nMake sure the file is in src/main/resources/" + resourcePath);
        }


        return convertCsvStreamToXlsx(inputStream, xlsxFilePath, delimiter);

    }

    /**
     * Converts a CSV InputStream to XLSX format
     *
     * @param inputStream Input stream of the CSV file
     * @param xlsxFilePath Path for the output XLSX file
     * @return File object representing the created XLSX file
     * @throws IOException if file operations fail
     */
    public File convertCsvToXlsx(InputStream inputStream, String xlsxFilePath) throws IOException {
        // Create a temporary file to store the CSV content
        Path tempFile = Files.createTempFile("temp", ".csv");

        // Write InputStream content to temporary file
        try (var outputStream = new FileOutputStream(tempFile.toFile())) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        // Convert the CSV file to XLSX
        return convertCsvToXlsx(tempFile.toString(), xlsxFilePath);
    }

    /**
     * Converts a CSV InputStream to XLSX format
     *
     * @param csvInputStream Input stream of the CSV file
     * @return byte array containing the converted XLSX file
     * @throws IOException if file operations fail
     */
    public byte[] convert(InputStream csvInputStream) throws IOException {
        // Create workbook and sheet
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Data");

            // Read CSV and populate Excel sheet
            try (var reader = new BufferedReader(new InputStreamReader(csvInputStream))) {
                String line;
                int rowNum = 0;

                while ((line = reader.readLine()) != null) {
                    var row = sheet.createRow(rowNum++);
                    var cells = parseCsvLine(line, ",");

                    for (int i = 0; i < cells.length; i++) {
                        var cell = row.createCell(i);
                        var cellValue = cells[i].trim();

                        // Try to detect and set appropriate cell type
                        setCellValue(cell, cellValue);
                    }
                }
            }

            // Auto-size columns for better appearance
            var firstRow = sheet.getRow(0);
            if (firstRow != null) {
                for (int i = 0; i < firstRow.getLastCellNum(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // Write to byte array output stream
            try (var outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }
    }
}