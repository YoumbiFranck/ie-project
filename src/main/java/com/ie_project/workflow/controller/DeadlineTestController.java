package com.ie_project.workflow.controller;

import com.ie_project.workflow.service.ApplicationDeadlineService;
import com.ie_project.workflow.service.DeadlineTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller für das Testen der Deadline-Funktionalität
 * REST Controller for testing deadline functionality
 *
 * This controller provides endpoints to test the deadline logic
 * without going through the full application process.
 *
 * Dieser Controller bietet Endpoints zum Testen der Deadline-Logik
 * ohne den vollständigen Bewerbungsprozess zu durchlaufen.
 *
 * @author IE Project Team
 */
@RestController
@RequestMapping("/api/test/deadlines")
@CrossOrigin(origins = "*")
public class DeadlineTestController {

    @Autowired
    private ApplicationDeadlineService deadlineService;

    @Autowired
    private DeadlineTestService testService;

    /**
     * Testet eine spezifische Deadline
     * Tests a specific deadline
     *
     * @param dateTime Format: "2025-04-15T10:30:00"
     * @return Deadline check result / Deadline-Prüfergebnis
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkDeadline(
            @RequestParam("dateTime") String dateTime) {

        try {
            LocalDateTime applicationDate = LocalDateTime.parse(dateTime);

            boolean isOnTime = deadlineService.isApplicationOnTime(applicationDate);
            ApplicationDeadlineService.DeadlineCheckResult result =
                    deadlineService.getDeadlineCheckResult(applicationDate);

            String message = deadlineService.createDeadlineMessage(result, "Test Student");

            Map<String, Object> response = new HashMap<>();
            response.put("applicationDate", applicationDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            response.put("isOnTime", isOnTime);
            response.put("targetSemester", result.getSemesterType());
            response.put("semesterStartDate", result.getSemesterStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            response.put("submissionDeadline", result.getSubmissionDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            response.put("daysUntilDeadline", result.getDaysUntilDeadline());
            response.put("status", isOnTime ? "ACCEPTED" : "REJECTED");
            response.put("message", message);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid date format or processing error");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("expectedFormat", "YYYY-MM-DDTHH:MM:SS (e.g., 2025-04-15T10:30:00)");
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Testet die aktuelle Zeit gegen Deadlines
     * Tests current time against deadlines
     */
    @GetMapping("/check-now")
    public ResponseEntity<Map<String, Object>> checkCurrentTime() {

        LocalDateTime now = LocalDateTime.now();

        boolean isOnTime = deadlineService.isApplicationOnTime(now);
        ApplicationDeadlineService.DeadlineCheckResult result =
                deadlineService.getDeadlineCheckResult(now);

        String message = deadlineService.createDeadlineMessage(result, "Test Student");

        Map<String, Object> response = new HashMap<>();
        response.put("currentDateTime", now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        response.put("isOnTime", isOnTime);
        response.put("targetSemester", result.getSemesterType());
        response.put("semesterStartDate", result.getSemesterStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        response.put("submissionDeadline", result.getSubmissionDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        response.put("daysUntilDeadline", result.getDaysUntilDeadline());
        response.put("status", isOnTime ? "ACCEPTED" : "REJECTED");
        response.put("message", message);

        return ResponseEntity.ok(response);
    }

    /**
     * Führt alle Test-Szenarien aus
     * Runs all test scenarios
     */
    @GetMapping("/run-all-tests")
    public ResponseEntity<Map<String, Object>> runAllTests() {

        try {
            // Capture console output would require more complex setup
            // Here we just trigger the tests and return a simple response
            testService.runDeadlineTests();
            testService.testCurrentTime();
            testService.printDeadlineConfiguration();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "All deadline tests executed successfully");
            response.put("note", "Check console output for detailed results");
            response.put("timestamp", LocalDateTime.now());
            response.put("testsRun", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error running tests");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Zeigt die aktuelle Deadline-Konfiguration
     * Shows current deadline configuration
     */
    @GetMapping("/configuration")
    public ResponseEntity<Map<String, Object>> getConfiguration() {

        // Test with sample dates to extract configuration
        LocalDateTime winterTestDate = LocalDateTime.of(2025, 4, 1, 12, 0);
        LocalDateTime summerTestDate = LocalDateTime.of(2024, 10, 1, 12, 0);

        ApplicationDeadlineService.DeadlineCheckResult winterResult =
                deadlineService.getDeadlineCheckResult(winterTestDate);
        ApplicationDeadlineService.DeadlineCheckResult summerResult =
                deadlineService.getDeadlineCheckResult(summerTestDate);

        Map<String, Object> config = new HashMap<>();
        config.put("winterSemesterStart", winterResult.getSemesterStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        config.put("winterApplicationDeadline", winterResult.getSubmissionDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        config.put("summerSemesterStart", summerResult.getSemesterStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        config.put("summerApplicationDeadline", summerResult.getSubmissionDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        config.put("monthsBeforeDeadline", 2);
        config.put("note", "Applications must be submitted at least 2 months before semester start");
        config.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(config);
    }
}