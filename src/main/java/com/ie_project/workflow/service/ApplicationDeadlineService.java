package com.ie_project.workflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service für die Überprüfung von Bewerbungsfristen
 * Service for checking application deadlines
 *
 * @author IE Project Team
 */
@Service
public class ApplicationDeadlineService {

    // Deadline for winter semester (from .env file)
    // Deadline für Wintersemester (aus .env-Datei)
    @Value("${application.deadline.winter:2025-08-01}")
    private String winterDeadlineStr;

    // Deadline for summer semester (from .env file)
    // Deadline für Sommersemester (aus .env-Datei)
    @Value("${application.deadline.summer:2025-02-01}")
    private String summerDeadlineStr;

    // Number of months before deadline that applications must be submitted
    // Anzahl Monate vor Deadline, bis wann Bewerbungen eingereicht werden müssen
    @Value("${application.deadline.months.before:2}")
    private int monthsBeforeDeadline;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Überprüft, ob die Bewerbung vor der Deadline eingereicht wurde
     * Checks if the application was submitted before the deadline
     *
     * @param applicationDate Das Datum der Bewerbungseinreichung / The application submission date
     * @return true wenn rechtzeitig, false wenn verspätet / true if on time, false if late
     */
    public boolean isApplicationOnTime(LocalDateTime applicationDate) {

        try {
            // Parse deadlines from configuration
            // Deadlines aus Konfiguration parsen
            LocalDate winterDeadline = LocalDate.parse(winterDeadlineStr, DATE_FORMATTER);
            LocalDate summerDeadline = LocalDate.parse(summerDeadlineStr, DATE_FORMATTER);

            // Convert application date to LocalDate for comparison
            // Bewerbungsdatum zu LocalDate für Vergleich konvertieren
            LocalDate appDate = applicationDate.toLocalDate();

            // Determine which semester deadline applies
            // Bestimmen, welche Semester-Deadline gilt
            LocalDate applicableDeadline = determineApplicableDeadline(appDate, winterDeadline, summerDeadline);

            // Calculate the actual submission deadline (2 months before semester start)
            // Echte Einreichungsdeadline berechnen (2 Monate vor Semesterbeginn)
            LocalDate submissionDeadline = applicableDeadline.minusMonths(monthsBeforeDeadline);

            // Check if application was submitted on time
            // Prüfen, ob Bewerbung rechtzeitig eingereicht wurde
            boolean isOnTime = !appDate.isAfter(submissionDeadline);

            // Log the check for debugging / Check für Debugging protokollieren
            System.out.println("=== DEADLINE CHECK ===");
            System.out.println("Application Date: " + appDate);
            System.out.println("Applicable Semester Deadline: " + applicableDeadline);
            System.out.println("Submission Deadline: " + submissionDeadline);
            System.out.println("Is On Time: " + isOnTime);
            System.out.println("Days difference: " + java.time.temporal.ChronoUnit.DAYS.between(appDate, submissionDeadline));
            System.out.println("======================");

            return isOnTime;

        } catch (Exception e) {
            System.err.println("Error checking application deadline: " + e.getMessage());
            e.printStackTrace();
            // In case of error, assume application is valid to avoid false rejections
            // Bei Fehler annehmen, dass Bewerbung gültig ist, um falsche Ablehnungen zu vermeiden
            return true;
        }
    }
    /**
     * Bestimmt die anwendbare Deadline basierend auf dem Bewerbungsdatum
     * Determines the applicable deadline based on the application date
     *
     * Logic: If application is made in first half of year, it's for winter semester
     * If made in second half, it's for next year's summer semester
     *
     * Logik: Bewerbung in erster Jahreshälfte = Wintersemester
     * Bewerbung in zweiter Jahreshälfte = Sommersemester nächstes Jahr
     */
    private LocalDate determineApplicableDeadline(LocalDate applicationDate, LocalDate winterDeadline, LocalDate summerDeadline) {

        int applicationYear = applicationDate.getYear();
        int applicationMonth = applicationDate.getMonthValue();

        // If application is made January-June: target is winter semester of same year
        // Wenn Bewerbung Januar-Juni: Ziel ist Wintersemester des gleichen Jahres
        if (applicationMonth <= 6) {
            return winterDeadline.withYear(applicationYear);
        }
        // If application is made July-December: target is summer semester of next year
        // Wenn Bewerbung Juli-Dezember: Ziel ist Sommersemester des nächsten Jahres
        else {
            return summerDeadline.withYear(applicationYear + 1);
        }
    }

    /**
     * Gibt eine detaillierte Deadline-Information zurück
     * Returns detailed deadline information
     */
    public DeadlineCheckResult getDeadlineCheckResult(LocalDateTime applicationDate) {

        try {
            LocalDate winterDeadline = LocalDate.parse(winterDeadlineStr, DATE_FORMATTER);
            LocalDate summerDeadline = LocalDate.parse(summerDeadlineStr, DATE_FORMATTER);
            LocalDate appDate = applicationDate.toLocalDate();

            LocalDate applicableDeadline = determineApplicableDeadline(appDate, winterDeadline, summerDeadline);
            LocalDate submissionDeadline = applicableDeadline.minusMonths(monthsBeforeDeadline);

            boolean isOnTime = !appDate.isAfter(submissionDeadline);
            long daysUntilDeadline = java.time.temporal.ChronoUnit.DAYS.between(appDate, submissionDeadline);

            String semesterType = applicableDeadline.equals(winterDeadline.withYear(applicableDeadline.getYear()))
                    ? "Wintersemester / Winter Semester"
                    : "Sommersemester / Summer Semester";

            return new DeadlineCheckResult(
                    isOnTime,
                    submissionDeadline,
                    applicableDeadline,
                    semesterType,
                    daysUntilDeadline
            );

        } catch (Exception e) {
            System.err.println("Error creating deadline check result: " + e.getMessage());
            // Return a default "valid" result in case of error
            // Standard "gültig" Ergebnis bei Fehler zurückgeben
            return new DeadlineCheckResult(true, LocalDate.now(), LocalDate.now(), "Unknown", 0);
        }
    }

    /**
     * Erstellt eine benutzerfreundliche Nachricht über das Deadline-Ergebnis
     * Creates a user-friendly message about the deadline result
     */
    public String createDeadlineMessage(DeadlineCheckResult result, String applicantName) {

        if (result.isOnTime()) {
            return String.format(
                    "Bewerbung von %s rechtzeitig eingereicht.\n" +
                            "Ziel-Semester: %s (Beginn: %s)\n" +
                            "Einreichungsdeadline war: %s\n" +
                            "Status: AKZEPTIERT - Bewerbung wird weiterbearbeitet.",
                    applicantName,
                    result.getSemesterType(),
                    result.getSemesterStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    result.getSubmissionDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            );
        } else {
            return String.format(
                    "Bewerbung von %s zu spät eingereicht.\n" +
                            "Ziel-Semester: %s (Beginn: %s)\n" +
                            "Einreichungsdeadline war: %s\n" +
                            "Verspätung: %d Tage\n" +
                            "Status: ABGELEHNT - Bewerbung kann nicht bearbeitet werden.",
                    applicantName,
                    result.getSemesterType(),
                    result.getSemesterStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    result.getSubmissionDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    Math.abs(result.getDaysUntilDeadline())
            );
        }
    }

    /**
     * Inner class für detaillierte Deadline-Ergebnisse
     * Inner class for detailed deadline results
     */
    public static class DeadlineCheckResult {
        private final boolean onTime;
        private final LocalDate submissionDeadline;
        private final LocalDate semesterStartDate;
        private final String semesterType;
        private final long daysUntilDeadline;

        public DeadlineCheckResult(boolean onTime, LocalDate submissionDeadline, LocalDate semesterStartDate,
                                   String semesterType, long daysUntilDeadline) {
            this.onTime = onTime;
            this.submissionDeadline = submissionDeadline;
            this.semesterStartDate = semesterStartDate;
            this.semesterType = semesterType;
            this.daysUntilDeadline = daysUntilDeadline;
        }

        // Getters
        public boolean isOnTime() { return onTime; }
        public LocalDate getSubmissionDeadline() { return submissionDeadline; }
        public LocalDate getSemesterStartDate() { return semesterStartDate; }
        public String getSemesterType() { return semesterType; }
        public long getDaysUntilDeadline() { return daysUntilDeadline; }
    }
}
