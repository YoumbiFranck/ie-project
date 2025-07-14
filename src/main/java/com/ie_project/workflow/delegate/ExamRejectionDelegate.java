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
 * Camunda Delegate für die Behandlung von Prüfungsablehnungen
 * Camunda Delegate for handling exam rejections
 *
 * This delegate handles applications that failed the entrance examination.
 * It updates the database status and creates rejection notifications.
 *
 * Dieser Delegate behandelt Bewerbungen, die die Aufnahmeprüfung nicht bestanden haben.
 * Er aktualisiert den Datenbankstatus und erstellt Ablehnungsbenachrichtigungen.
 *
 * @author IE Project Team
 */
@Component("examRejectionDelegate")
public class ExamRejectionDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== EXAM REJECTION DELEGATE EXECUTED ===");

        try {
            // Get application ID and exam results from process variables
            // Bewerbungs-ID und Prüfungsergebnisse aus Prozessvariablen holen
            Long applicationId = getApplicationId(execution);
            Boolean examPassed = getBooleanVariable(execution, "examPassed");
            String examScore = getStringVariable(execution, "examScore");
            String maxScore = getStringVariable(execution, "maxScore");
            String examDate = getStringVariable(execution, "examDate");
            String examiner = getStringVariable(execution, "examiner");
            String studyProgramName = getStringVariable(execution, "studyProgramName");
            String studyProgramCode = getStringVariable(execution, "studyProgramCode");

            System.out.println("Application ID: " + applicationId);
            System.out.println("Exam Passed: " + examPassed);
            System.out.println("Exam Score: " + examScore + "/" + maxScore);

            // Verify this is indeed a failed exam / Verifizieren dass es sich tatsächlich um eine nicht bestandene Prüfung handelt
            if (Boolean.TRUE.equals(examPassed)) {
                throw new IllegalStateException("Exam rejection delegate called for passed exam");
            }

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Update application status to REJECTED / Bewerbungsstatus auf REJECTED aktualisieren
            application.setStatus(Application.ApplicationStatus.REJECTED);
            applicationRepository.save(application);

            // Set final process variables / Finale Prozessvariablen setzen
            execution.setVariable("examRejectionCompleted", true);
            execution.setVariable("rejectionReason", "EXAM_FAILED");
            execution.setVariable("processCompleted", true);
            execution.setVariable("finalStatus", "REJECTED");
            execution.setVariable("processEndReason", "EXAM_REJECTION");
            execution.setVariable("rejectionProcessedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

            // Create exam rejection notification / Prüfungsablehnungs-Benachrichtigung erstellen
            String rejectionNotification = createExamRejectionNotification(
                    application, studyProgramName, studyProgramCode, examScore, maxScore, examDate, examiner
            );

            execution.setVariable("examRejectionNotification", rejectionNotification);

            // Log exam rejection notification / Prüfungsablehnungs-Benachrichtigung protokollieren
            System.out.println("=== PRÜFUNGSABLEHNUNG / EXAM REJECTION ===");
            System.out.println(rejectionNotification);
            System.out.println("==========================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== EXAM REJECTION PROCESSING COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Study Program: " + studyProgramName + " (" + studyProgramCode + ")");
            System.out.println("Exam Score: " + examScore + "/" + maxScore);
            System.out.println("Exam Date: " + examDate);
            System.out.println("Examiner: " + examiner);
            System.out.println("Status Updated: " + application.getStatus());
            System.out.println("Process Completed: YES");
            System.out.println("===========================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN EXAM REJECTION DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================");
            throw e;
        }
    }

    /**
     * Erstellt eine kompakte Prüfungsablehnungs-Benachrichtigung
     * Creates a compact exam rejection notification
     */
    private String createExamRejectionNotification(Application application, String studyProgramName, String studyProgramCode,
                                                   String examScore, String maxScore, String examDate, String examiner) {

        StringBuilder notification = new StringBuilder();

        // Header / Kopfzeile
        notification.append("=== BEWERBUNG ABGELEHNT - PRÜFUNG NICHT BESTANDEN ===\n");


        // Student information / Studenten-Informationen
        notification.append("Bewerber / Applicant: ").append(application.getFirstName())
                .append(" ").append(application.getLastName()).append("\n");
        notification.append("E-Mail: ").append(application.getEmail()).append("\n");
        notification.append("Studiengang / Study Program: ").append(studyProgramName)
                .append(" (").append(studyProgramCode).append(")\n\n");

        // Exam results / Prüfungsergebnisse
        notification.append("Prüfungsdatum / Exam Date: ").append(examDate).append("\n");
        notification.append("Prüfungsergebnis / Exam Result: ").append(examScore).append("/").append(maxScore).append(" Punkte\n");
        notification.append("Prüfer / Examiner: ").append(examiner).append("\n\n");

        // Rejection message / Ablehnungsnachricht
        notification.append(" Leider haben Sie die Aufnahmeprüfung nicht bestanden.\n");


        // Next steps / Nächste Schritte
        notification.append("Sie können sich für das nächste Semester erneut bewerben.\n");





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

    private Boolean getBooleanVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);

        if (value == null) {
            return null;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        } else {
            return Boolean.parseBoolean(value.toString());
        }
    }

    private String getStringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);
        return value != null ? value.toString() : null;
    }
}
