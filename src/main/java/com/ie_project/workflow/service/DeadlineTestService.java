package com.ie_project.workflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Test Service für die Überprüfung der Deadline-Logik
 * Test Service for checking deadline logic
 *
 * This service provides methods to test the deadline functionality
 * with different scenarios and dates.
 *
 * Dieser Service bietet Methoden zum Testen der Deadline-Funktionalität
 * mit verschiedenen Szenarien und Daten.
 *
 * @author IE Project Team
 */
@Service
public class DeadlineTestService {

    @Autowired
    private ApplicationDeadlineService deadlineService;

    /**
     * Testet verschiedene Deadline-Szenarien
     * Tests various deadline scenarios
     */
    public void runDeadlineTests() {

        System.out.println("=== DEADLINE LOGIC TEST SCENARIOS ===");
        System.out.println("======================================");

        // Test scenarios / Test-Szenarien
        testScenario("Early Winter Application (On Time)", LocalDateTime.of(2025, 4, 15, 10, 0));
        testScenario("Late Winter Application (Too Late)", LocalDateTime.of(2025, 7, 15, 10, 0));
        testScenario("Early Summer Application (On Time)", LocalDateTime.of(2024, 10, 15, 10, 0));
        testScenario("Late Summer Application (Too Late)", LocalDateTime.of(2024, 12, 15, 10, 0));
        testScenario("Borderline Case - Exactly on Deadline", LocalDateTime.of(2025, 6, 1, 23, 59));
        testScenario("Borderline Case - One day late", LocalDateTime.of(2025, 6, 2, 0, 1));

        System.out.println("======================================");
        System.out.println("=== ALL DEADLINE TESTS COMPLETED ===");
    }

    /**
     * Testet ein einzelnes Szenario
     * Tests a single scenario
     */
    private void testScenario(String scenarioName, LocalDateTime applicationDate) {

        System.out.println("\n--- " + scenarioName + " ---");
        System.out.println("Application Date: " + applicationDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

        try {
            // Check if application is on time / Prüfen ob Bewerbung rechtzeitig ist
            boolean isOnTime = deadlineService.isApplicationOnTime(applicationDate);

            // Get detailed result / Detailliertes Ergebnis holen
            ApplicationDeadlineService.DeadlineCheckResult result =
                    deadlineService.getDeadlineCheckResult(applicationDate);

            // Create message / Nachricht erstellen
            String message = deadlineService.createDeadlineMessage(result, "Test Student");

            System.out.println("Result: " + (isOnTime ? "ON TIME" : "TOO LATE"));
            System.out.println("Target Semester: " + result.getSemesterType());
            System.out.println("Submission Deadline: " + result.getSubmissionDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            System.out.println("Days until/past deadline: " + result.getDaysUntilDeadline());
            System.out.println("Message: " + message.substring(0, Math.min(100, message.length())) + "...");

        } catch (Exception e) {
            System.err.println("ERROR in scenario: " + e.getMessage());
        }

        System.out.println("--------------------");
    }

    /**
     * Testet die aktuelle Zeit gegen die Deadlines
     * Tests current time against deadlines
     */
    public void testCurrentTime() {

        LocalDateTime now = LocalDateTime.now();
        System.out.println("\n=== TESTING CURRENT TIME ===");
        System.out.println("Current DateTime: " + now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

        testScenario("Current Time Test", now);

        System.out.println("============================");
    }

    /**
     * Gibt eine Übersicht über die konfigurierten Deadlines aus
     * Outputs an overview of configured deadlines
     */
    public void printDeadlineConfiguration() {

        System.out.println("\n=== DEADLINE CONFIGURATION OVERVIEW ===");

        // Test with sample dates to extract configuration / Mit Beispieldaten testen um Konfiguration zu extrahieren
        LocalDateTime winterTestDate = LocalDateTime.of(2025, 4, 1, 12, 0);
        LocalDateTime summerTestDate = LocalDateTime.of(2024, 10, 1, 12, 0);

        ApplicationDeadlineService.DeadlineCheckResult winterResult =
                deadlineService.getDeadlineCheckResult(winterTestDate);
        ApplicationDeadlineService.DeadlineCheckResult summerResult =
                deadlineService.getDeadlineCheckResult(summerTestDate);

        System.out.println("Winter Semester Start: " + winterResult.getSemesterStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        System.out.println("Winter Application Deadline: " + winterResult.getSubmissionDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        System.out.println("Summer Semester Start: " + summerResult.getSemesterStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        System.out.println("Summer Application Deadline: " + summerResult.getSubmissionDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        System.out.println("========================================");
    }
}
