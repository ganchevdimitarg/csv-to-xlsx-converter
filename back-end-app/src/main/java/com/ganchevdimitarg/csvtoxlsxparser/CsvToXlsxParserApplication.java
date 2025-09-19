package com.ganchevdimitarg.csvtoxlsxparser;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

import static com.ganchevdimitarg.csvtoxlsxparser.CsvToXlsxConverterService.*;

@SpringBootApplication
public class CsvToXlsxParserApplication{

    public static void main(String[] args) {
        SpringApplication.run(CsvToXlsxParserApplication.class, args);
    }
}
