package com.ie_project.workflow.delegate;

import com.ie_project.workflow.entity.Application;
import com.ie_project.workflow.repository.ApplicationRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Camunda Delegate für die Terminierung von Aufnahmeprüfungen
 * Camunda Delegate for scheduling entrance examinations
 *
 * This delegate handles the scheduling of entrance exams for study programs
 * that require an examination as part of their admission process.
 *
 * Dieser Delegate behandelt die Terminierung von Aufnahmeprüfungen für Studiengänge,
 * die eine Prüfung als Teil ihres Zulassungsverfahrens erfordern.
 *
 * @author IE Project Team
 */
@Component("scheduleExamDelegate")
public class ScheduleExamDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== SCHEDULE EXAM DELEGATE EXECUTED ===");

        try {
            // Get application ID and admission type verification
            // Bewerbungs-ID und Zulassungstyp-Verifizierung holen
            Long applicationId = getApplicationId(execution);
            String admissionType = getStringVariable(execution, "admissionType");
            String studyProgramName = getStringVariable(execution, "studyProgramName");
            String studyProgramCode = getStringVariable(execution, "studyProgramCode");

            System.out.println("Application ID: " + applicationId);
            System.out.println("Admission Type: " + admissionType);

            // Verify this is indeed an entrance exam program
            // Verifizieren, dass es sich tatsächlich um ein Programm mit Aufnahmeprüfung handelt
            if (!"ENTRANCE_EXAM".equals(admissionType)) {
                throw new IllegalStateException("Schedule exam delegate called for non-entrance-exam program: " + admissionType);
            }

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Calculate exam scheduling details / Prüfungsterminierung Details berechnen
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime examDate = calculateExamDate(currentTime);
            String examLocation = generateExamLocation(studyProgramCode);
            String examRoom = generateExamRoom(applicationId);
            String examCommittee = generateExamCommittee(studyProgramName);

            // Set process variables for exam scheduling / Prozessvariablen für Prüfungsterminierung setzen
            execution.setVariable("examSchedulingCompleted", true);
            execution.setVariable("examDate", examDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            execution.setVariable("examTime", examDate.format(DateTimeFormatter.ofPattern("HH:mm")));
            execution.setVariable("examDateTime", examDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            execution.setVariable("examLocation", examLocation);
            execution.setVariable("examRoom", examRoom);
            execution.setVariable("examCommittee", examCommittee);
            execution.setVariable("examType", "ENTRANCE_EXAM");
            execution.setVariable("examDuration", "120"); // 2 hours standard
            execution.setVariable("maxExamScore", 100);
            execution.setVariable("passingScore", 60);
            execution.setVariable("scheduledAt", currentTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            execution.setVariable("nextProcessStep", "EXAM_INVITATION");

            // Create detailed exam scheduling notification / Detaillierte Prüfungsterminierung-Benachrichtigung erstellen
            String examSchedulingNotification = createExamSchedulingNotification(
                    application, studyProgramName, studyProgramCode, examDate,
                    examLocation, examRoom, examCommittee
            );

            execution.setVariable("examSchedulingNotification", examSchedulingNotification);

            // Log exam scheduling notification / Prüfungsterminierung-Benachrichtigung protokollieren
            System.out.println("=== AUFNAHMEPRÜFUNG TERMINIERT / ENTRANCE EXAM SCHEDULED ===");
            System.out.println(examSchedulingNotification);
            System.out.println("===========================================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== EXAM SCHEDULING PROCESSING COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Study Program: " + studyProgramName + " (" + studyProgramCode + ")");
            System.out.println("Exam Date: " + examDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            System.out.println("Exam Location: " + examLocation);
            System.out.println("Exam Room: " + examRoom);
            System.out.println("Exam Committee: " + examCommittee);
            System.out.println("Next Step: EXAM_INVITATION");
            System.out.println("Processing Completed: YES");
            System.out.println("============================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN SCHEDULE EXAM DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=======================================");
            throw e;
        }
    }

    /**
     * Berechnet das Prüfungsdatum (standardmäßig 2 Wochen ab heute)
     * Calculates exam date (default 2 weeks from today)
     */
    private LocalDateTime calculateExamDate(LocalDateTime currentTime) {
        // Schedule exam for 2 weeks from now, at 10:00 AM
        // Prüfung für 2 Wochen ab jetzt terminieren, um 10:00 Uhr
        LocalDateTime examDate = currentTime.plusWeeks(2)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        // Ensure it's a weekday (Monday-Friday)
        // Sicherstellen, dass es ein Wochentag ist (Montag-Freitag)
        while (examDate.getDayOfWeek().getValue() > 5) { // Saturday = 6, Sunday = 7
            examDate = examDate.plusDays(1);
        }

        return examDate;
    }

    /**
     * Generiert den Prüfungsort basierend auf dem Studiengang-Code
     * Generates exam location based on study program code
     */
    private String generateExamLocation(String studyProgramCode) {
        switch (studyProgramCode.toUpperCase()) {
            case "MED":
                return "Medizinische Fakultät, Hauptgebäude";
            case "INF":
                return "Informatik-Zentrum, Gebäude A";
            case "BWL":
                return "Wirtschaftswissenschaften, Hörsaalzentrum";
            case "MB":
                return "Maschinenbau-Fakultät, Technikum";
            default:
                return "Hauptgebäude, Prüfungszentrum";
        }
    }

    /**
     * Generiert den Prüfungsraum
     * Generates exam room
     */
    private String generateExamRoom(Long applicationId) {
        // Simple algorithm to distribute across rooms
        // Einfacher Algorithmus zur Verteilung auf Räume
        int roomNumber = (int) (applicationId % 10) + 1;
        return "Raum " + String.format("P-%03d", roomNumber);
    }

    /**
     * Generiert das Prüfungskomitee basierend auf dem Studiengang
     * Generates exam committee based on study program
     */
    private String generateExamCommittee(String studyProgramName) {
        switch (studyProgramName.toLowerCase()) {
            case "medizin":
                return "Prof. Dr. Schmidt, Dr. Weber, Dr. Müller";
            case "informatik":
                return "Prof. Dr. Hansen, Dr. Klein, Dr. Fischer";
            case "betriebswirtschaftslehre":
                return "Prof. Dr. Wagner, Dr. Becker, Dr. Schulz";
            case "maschinenbau":
                return "Prof. Dr. Hoffmann, Dr. Richter, Dr. König";
            default:
                return "Prof. Dr. Standardprüfer, Dr. Allgemein, Dr. Fachbereich";
        }
    }

    /**
     * Erstellt eine detaillierte Prüfungsterminierung-Benachrichtigung
     * Creates a detailed exam scheduling notification
     */
    private String createExamSchedulingNotification(Application application, String studyProgramName,
                                                    String studyProgramCode, LocalDateTime examDate,
                                                    String examLocation, String examRoom, String examCommittee) {

        StringBuilder notification = new StringBuilder();

        // Header / Kopfzeile
        notification.append("=== AUFNAHMEPRÜFUNG TERMINIERT / ENTRANCE EXAM SCHEDULED ===\n\n");

        // Personal information / Persönliche Daten
        notification.append("Bewerber / Applicant: ").append(application.getFirstName())
                .append(" ").append(application.getLastName()).append("\n");
        notification.append("E-Mail: ").append(application.getEmail()).append("\n");
        notification.append("Bewerbungs-ID / Application ID: ").append(application.getId()).append("\n");
        notification.append("Studiengang / Study Program: ").append(studyProgramName)
                .append(" (").append(studyProgramCode).append(")\n\n");

        // Exam details / Prüfungsdetails
        notification.append("=== PRÜFUNGSDETAILS / EXAM DETAILS ===\n");
        notification.append(" Datum / Date: ").append(examDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("\n");
        notification.append(" Uhrzeit / Time: ").append(examDate.format(DateTimeFormatter.ofPattern("HH:mm"))).append(" Uhr\n");
        notification.append(" Ort / Location: ").append(examLocation).append("\n");
        notification.append(" Raum / Room: ").append(examRoom).append("\n");
        notification.append(" Dauer / Duration: 120 Minuten / minutes\n");
        notification.append(" Maximale Punktzahl / Max Score: 100 Punkte / points\n");
        notification.append(" Bestehensgrenze / Passing Score: 60 Punkte / points\n\n");

        // Examination committee / Prüfungskomitee
        notification.append("=== PRÜFUNGSKOMITEE / EXAMINATION COMMITTEE ===\n");
        notification.append("Prüfer / Examiners: ").append(examCommittee).append("\n\n");




        return notification.toString();
    }

    /**
     * Helper methods for type-safe variable retrieval
     */
    private Long getApplicationId(DelegateExecution execution) {
        Object applicationIdObj = execution.getVariable("applicationId");

        if (applicationIdObj == null) {
            throw new IllegalArgumentException("Application ID not found in process variables");
        }

        if (applicationIdObj instanceof Long) {
            return (Long) applicationIdObj;
        } else if (applicationIdObj instanceof String) {
            try {
                return Long.parseLong((String) applicationIdObj);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert applicationId to Long: " + applicationIdObj);
            }
        } else if (applicationIdObj instanceof Integer) {
            return ((Integer) applicationIdObj).longValue();
        } else {
            throw new IllegalArgumentException("Unexpected type for applicationId: " + applicationIdObj.getClass());
        }
    }

    private String getStringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);
        return value != null ? value.toString() : null;
    }
}
