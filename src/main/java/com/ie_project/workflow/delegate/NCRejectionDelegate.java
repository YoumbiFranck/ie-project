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
 * Camunda Delegate für die Behandlung von NC-Ablehnungen
 * Camunda Delegate for handling NC (Numerus Clausus) rejections
 *
 * This delegate handles applications that were rejected in the NC selection process.
 * It updates the database status and creates detailed rejection notifications.
 *
 * Dieser Delegate behandelt Bewerbungen, die im NC-Auswahlverfahren abgelehnt wurden.
 * Er aktualisiert den Datenbankstatus und erstellt detaillierte Ablehnungsbenachrichtigungen.
 *
 * @author IE Project Team
 */
@Component("ncRejectionDelegate")
public class NCRejectionDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== NC REJECTION DELEGATE EXECUTED ===");

        try {
            // Get application ID and rejection details from process variables
            // Bewerbungs-ID und Ablehnungsdetails aus Prozessvariablen holen
            Long applicationId = getApplicationId(execution);
            String ncAdmissionDecision = getStringVariable(execution, "ncAdmissionDecision");
            String ncAdmissionReason = getStringVariable(execution, "ncAdmissionReason");
            Integer finalRank = getIntegerVariable(execution, "finalRank");
            Double finalGrade = getDoubleVariable(execution, "finalGrade");
            Integer totalApplications = getIntegerVariable(execution, "totalApplicationsWithGrades");
            Integer maxStudents = getIntegerVariable(execution, "maxStudents");
            String studyProgramName = getStringVariable(execution, "studyProgramName");
            String studyProgramCode = getStringVariable(execution, "studyProgramCode");

            System.out.println("Application ID: " + applicationId);
            System.out.println("NC Decision: " + ncAdmissionDecision);
            System.out.println("Rejection Reason: " + ncAdmissionReason);

            // Verify this is indeed a rejection / Verifizieren dass es sich tatsächlich um eine Ablehnung handelt
            if (!"REJECTED".equals(ncAdmissionDecision)) {
                throw new IllegalStateException("NC Rejection delegate called for non-rejected application: " + ncAdmissionDecision);
            }

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Update application status to REJECTED / Bewerbungsstatus auf REJECTED aktualisieren
            application.setStatus(Application.ApplicationStatus.REJECTED);
            applicationRepository.save(application);

            // Set final process variables / Finale Prozessvariablen setzen
            execution.setVariable("ncRejectionCompleted", true);
            execution.setVariable("rejectionReason", "NC_INSUFFICIENT_RANK");
            execution.setVariable("processCompleted", true);
            execution.setVariable("finalStatus", "REJECTED");
            execution.setVariable("processEndReason", "NC_REJECTION");
            execution.setVariable("rejectionProcessedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

            // Create detailed NC rejection notification / Detaillierte NC-Ablehnungsbenachrichtigung erstellen
            String rejectionNotification = createNCRejectionNotification(
                    application, studyProgramName, studyProgramCode, finalRank, finalGrade,
                    totalApplications, maxStudents, ncAdmissionReason
            );

            execution.setVariable("ncRejectionNotification", rejectionNotification);

            // Log NC rejection notification / NC-Ablehnungsbenachrichtigung protokollieren
            System.out.println("=== NC ABLEHNUNGSBENACHRICHTIGUNG / NC REJECTION NOTIFICATION ===");
            System.out.println(rejectionNotification);
            System.out.println("================================================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== NC REJECTION PROCESSING COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Study Program: " + studyProgramName + " (" + studyProgramCode + ")");
            System.out.println("Final Rank: " + finalRank + " / " + totalApplications);
            System.out.println("Final Grade: " + finalGrade);
            System.out.println("Rejection Reason: " + ncAdmissionReason);
            System.out.println("Status Updated: " + application.getStatus());
            System.out.println("Process Completed: YES");
            System.out.println("=========================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN NC REJECTION DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("======================================");
            throw e;
        }
    }

    /**
     * Erstellt eine detaillierte NC-Ablehnungsbenachrichtigung
     * Creates a detailed NC rejection notification
     */
    private String createNCRejectionNotification(Application application, String studyProgramName, String studyProgramCode,
                                                 Integer finalRank, Double finalGrade, Integer totalApplications,
                                                 Integer maxStudents, String rejectionReason) {

        StringBuilder notification = new StringBuilder();

        // Header / Kopfzeile
        notification.append("=== BEWERBUNG ABGELEHNT - NC VERFAHREN / APPLICATION REJECTED - NC PROCESS ===\n\n");

        // Personal information / Persönliche Daten
        notification.append("Bewerber / Applicant: ").append(application.getFirstName())
                .append(" ").append(application.getLastName()).append("\n");
        notification.append("E-Mail: ").append(application.getEmail()).append("\n");
        notification.append("Bewerbungs-ID / Application ID: ").append(application.getId()).append("\n");
        notification.append("Studiengang / Study Program: ").append(studyProgramName)
                .append(" (").append(studyProgramCode).append(")\n\n");

        // Timing information / Zeitinformationen
        notification.append("Eingereicht am / Submitted on: ")
                .append(application.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");
        notification.append("Bearbeitet am / Processed on: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n\n");

        // NC process results / NC-Verfahren Ergebnisse
        notification.append("=== ERGEBNISSE DES NC-VERFAHRENS / NC PROCESS RESULTS ===\n");
        notification.append("Ihre Abiturnote / Your high school grade: ").append(String.format("%.1f", finalGrade)).append("\n");
        notification.append("Ihr Rangplatz / Your rank: ").append(finalRank).append(" von / of ").append(totalApplications).append(" Bewerbungen\n");
        notification.append("Verfügbare Studienplätze / Available seats: ").append(maxStudents).append("\n");
        notification.append("Benötigter Rangplatz / Required rank: 1 - ").append(maxStudents).append("\n\n");

        // Rejection reason / Ablehnungsgrund
        notification.append("=== ABLEHNUNGSGRUND / REJECTION REASON ===\n");

        switch (rejectionReason != null ? rejectionReason : "INSUFFICIENT_RANK") {
            case "INSUFFICIENT_RANK":
                notification.append(" Ihr Rangplatz reicht leider nicht für eine Zulassung aus.\n");
                notification.append("Ihr Rangplatz ").append(finalRank).append(" liegt außerhalb der verfügbaren ")
                        .append(maxStudents).append(" Studienplätze.\n");
                notification.append("Your rank ").append(finalRank).append(" is outside the available ")
                        .append(maxStudents).append(" study places.\n");
                break;
            default:
                notification.append(" Ihre Bewerbung konnte im NC-Verfahren nicht berücksichtigt werden.\n");
        }

        notification.append("\n");



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

    private Integer getIntegerVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);

        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private Double getDoubleVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);

        if (value == null) {
            return null;
        }

        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Float) {
            return ((Float) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }
}
