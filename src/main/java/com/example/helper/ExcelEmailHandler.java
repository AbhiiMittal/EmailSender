package com.example.helper;

import com.example.entity.EmailDetails;
import com.example.entity.EmailEntity;
import com.example.entity.EmailJob;
import com.example.entity.ResultSet;
import com.example.repository.EmailDetailsRepo;
import com.example.repository.EmailEntityRepo;
import com.example.repository.EmailJobRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class ExcelEmailHandler {

    @Autowired
    private EmailJobRepo emailJobRepo; // You will need to create this repository

    @Transactional
    public void processAndSaveExcel(String file, String currentUser,String executionTime,String subject,String body) {
        log.info("Starting to process Excel file: {} for user: {}", file, currentUser);

        // We will cache Jobs instead of just Details.
        // If 500 rows have the exact same Subject/Body, they all go into ONE Job.
        Map<String, EmailJob> jobCache = new HashMap<>();
        int totalRecipients = 0;

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(file)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found: " + file);
            }

            try (Workbook workbook = WorkbookFactory.create(inputStream)) {
                Sheet sheet = workbook.getSheetAt(0);

                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // Skip header

                    String toEmail = getCellValue(row.getCell(0));

                    if (toEmail.isEmpty() || subject.isEmpty() || body.isEmpty()) {
                        continue;
                    }

                    // 1. Check if we already created a Job for this Subject/Body combo
                    String cacheKey = subject + "|||" + body;

                    EmailJob currentJob = jobCache.computeIfAbsent(cacheKey, key -> {
                        // Create the Template
                        EmailDetails details = new EmailDetails();
                        details.setSubject(subject);
                        details.setBody(body);

                        // Create the Job
                        EmailJob newJob = new EmailJob();
                        newJob.setStatus("PENDING");
                        newJob.setUser(currentUser);
                        newJob.setEmailDetails(details);
                        newJob.setExecutionTime(LocalDateTime.parse(executionTime));
                        return newJob;
                    });

                    // 2. Create the Recipient and link it to the Job
                    EmailEntity emailEntity = new EmailEntity();
                    emailEntity.setToEmail(toEmail);
                    emailEntity.setStatus("PENDING");

                    currentJob.addEmailEntity(emailEntity); // Links both sides!
                    totalRecipients++;
                }
            }

            // 3. Save all Jobs (Cascading will automatically save Details and Entities)
            log.info("Finished parsing. Saving {} unique Email Jobs containing {} total recipients...",
                    jobCache.size(), totalRecipients);

            emailJobRepo.saveAll(jobCache.values());

            log.info("Successfully saved all jobs and entities to the database.");

        } catch (Exception e) {
            log.error("Failed to process Excel file. Error: {}", e.getMessage(), e);
            throw new RuntimeException("Excel processing failed", e);
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }
}
