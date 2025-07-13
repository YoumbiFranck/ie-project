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
 * Camunda Delegate für die Weiterbearbeitung angenommener Bewerbungen (Type-Safe)
 * Camunda Delegate for continuing processing of accepted applications (Type-Safe)
 *
 * This delegate handles applications that passed the deadline check
 * and prepares them for the next steps in the application process.
 *
 * Dieser Delegate behandelt Bewerbungen, die die Deadline-Prüfung bestanden haben
 * und bereitet sie für die nächsten Schritte im Bewerbungsprozess vor.
 *
 * @author IE Project Team
 */
@Component("continueProcessingDelegate")
public class ContinueProcessingDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== CONTINUE PROCESSING DELEGATE EXECUTED ===");

        try {
            // Get application ID from process variables with type safety
            // Bewerbungs-ID aus Prozessvariablen mit Type Safety holen
            Long applicationId = getApplicationId(execution);

            System.out.println("Application ID: " + applicationId);

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Get deadline check results from process variables with type safety
            // Deadline-Prüfergebnisse aus Prozessvariablen mit Type Safety holen
            Boolean isOnTime = getBooleanVariable(execution, "isApplicationOnTime");
            String deadlineMessage = getStringVariable(execution, "deadlineMessage");
            String targetSemester = getStringVariable(execution, "targetSemester");
            String submissionDeadline = getStringVariable(execution, "submissionDeadline");
            Long daysUntilDeadline = getLongVariable(execution, "daysUntilDeadline");

            // Log the details
            System.out.println("Is On Time: " + isOnTime);
            System.out.println("Target Semester: " + targetSemester);
            System.out.println("Days Until Deadline: " + daysUntilDeadline);

            // Verify that application is indeed on time / Verifizieren, dass Bewerbung tatsächlich rechtzeitig ist
            if (!Boolean.TRUE.equals(isOnTime)) {
                throw new IllegalStateException("Application should be on time but isApplicationOnTime is false / Bewerbung sollte rechtzeitig sein, aber isApplicationOnTime ist false");
            }

            // Update application status to indicate it's being processed
            // Bewerbungsstatus aktualisieren um zu zeigen, dass sie bearbeitet wird
            if (application.getStatus() == Application.ApplicationStatus.DOCUMENT_CHECK) {
                // Status is already correct from previous step
                // Status ist bereits korrekt aus vorherigem Schritt
            }

            // Create success notification / Erfolgsbenachrichtigung erstellen
            String successNotification = createSuccessNotification(
                    application, deadlineMessage, targetSemester, submissionDeadline, daysUntilDeadline
            );

            // Set process variables for next steps / Prozessvariablen für nächste Schritte setzen
            execution.setVariable("applicationAccepted", true);
            execution.setVariable("successNotification", successNotification);
            execution.setVariable("processCompleted", true);
            execution.setVariable("finalStatus", "ACCEPTED_FOR_PROCESSING");
            execution.setVariable("processEndReason", "DEADLINE_CHECK_PASSED");
            execution.setVariable("nextProcessStep", "DOCUMENT_VERIFICATION");
            execution.setVariable("processingContinuedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

            // Log success notification / Erfolgsbenachrichtigung protokollieren
            System.out.println("=== BEWERBUNG ANGENOMMEN / APPLICATION ACCEPTED ===");
            System.out.println(successNotification);
            System.out.println("===================================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Study Program: " + application.getStudyProgram().getName());
            System.out.println("Target Semester: " + targetSemester);
            System.out.println("Days Before Deadline: " + daysUntilDeadline);
            System.out.println("Current Status: " + application.getStatus());
            System.out.println("Final Status: ACCEPTED_FOR_PROCESSING");
            System.out.println("Next Step: DOCUMENT_VERIFICATION");
            System.out.println("Process Completed: YES");
            System.out.println("============================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN CONTINUE PROCESSING DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=============================================");
            throw e;
        }
    }

    /**
     * Erstellt eine detaillierte Erfolgsbenachrichtigung
     * Creates a detailed success notification
     */
    private String createSuccessNotification(Application application, String deadlineMessage,
                                             String targetSemester, String submissionDeadline,
                                             Long daysUntilDeadline) {

        StringBuilder notification = new StringBuilder();

        // Header / Kopfzeile
        notification.append("=== BEWERBUNG ANGENOMMEN / APPLICATION ACCEPTED ===\n\n");

        // Personal information / Persönliche Daten
        notification.append("Bewerber / Applicant: ").append(application.getFirstName())
                .append(" ").append(application.getLastName()).append("\n");
        notification.append("E-Mail: ").append(application.getEmail()).append("\n");
        notification.append("Bewerbungs-ID / Application ID: ").append(application.getId()).append("\n");
        notification.append("Studiengang / Study Program: ").append(application.getStudyProgram().getName()).append("\n\n");

        // Timing information / Zeitinformationen
        notification.append("Eingereicht am / Submitted on: ")
                .append(application.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");

        if (submissionDeadline != null) {
            notification.append("Deadline war / Deadline was: ").append(submissionDeadline).append("\n");
        }

        if (targetSemester != null) {
            notification.append("Ziel-Semester / Target Semester: ").append(targetSemester).append("\n");
        }

        if (daysUntilDeadline != null && daysUntilDeadline > 0) {
            notification.append("Rechtzeitig eingereicht / Submitted on time: ").append(daysUntilDeadline).append(" Tage vor Deadline / Days before deadline\n");
        }

        notification.append("\n");

        // Success message / Erfolgsnachricht
        notification.append("=== BEWERBUNGSSTATUS / APPLICATION STATUS ===\n");
        notification.append("✅ Ihre Bewerbung wurde erfolgreich angenommen!\n");
        notification.append("✅ Your application has been successfully accepted!\n\n");
        notification.append("Die Deadline-Prüfung wurde erfolgreich bestanden.\n");
        notification.append("The deadline check was passed successfully.\n\n");
        notification.append("Status: ANGENOMMEN FÜR WEITERE BEARBEITUNG\n");
        notification.append("Status: ACCEPTED FOR FURTHER PROCESSING\n");

        notification.append("\n");

        // Next steps / Nächste Schritte
        notification.append("=== NÄCHSTE SCHRITTE / NEXT STEPS ===\n");
        notification.append("1. Ihre Bewerbungsunterlagen werden nun geprüft\n");
        notification.append("1. Your application documents will now be reviewed\n\n");
        notification.append("2. Sie erhalten weitere Informationen per E-Mail\n");
        notification.append("2. You will receive further information by email\n\n");
        notification.append("3. Bei Fragen kontaktieren Sie uns unter: bewerbung@riedtal.de\n");
        notification.append("3. For questions, contact us at: bewerbung@riedtal.de\n");

        // Footer / Fußzeile
        notification.append("\n=== UNIVERSITÄT RIEDTAL - ADMISSIONS OFFICE ===");

        return notification.toString();
    }

    /**
     * Helper methods for type-safe variable retrieval
     */
    private Long getApplicationId(DelegateExecution execution) {
        Object applicationIdObj = execution.getVariable("applicationId");

        System.out.println("DEBUG - applicationId raw value: " + applicationIdObj);
        System.out.println("DEBUG - applicationId type: " + (applicationIdObj != null ? applicationIdObj.getClass() : "null"));

        if (applicationIdObj == null) {
            throw new IllegalArgumentException("Application ID not found in process variables / Bewerbungs-ID nicht in Prozessvariablen gefunden");
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
            System.out.println("Warning: Unexpected type for " + variableName + ": " + value.getClass());
            return Boolean.parseBoolean(value.toString());
        }
    }

    private String getStringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);

        if (value == null) {
            return null;
        }

        return value.toString();
    }

    private Long getLongVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);

        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                System.out.println("Warning: Cannot convert " + variableName + " to Long: " + value);
                return null;
            }
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else {
            System.out.println("Warning: Unexpected type for " + variableName + ": " + value.getClass());
            return null;
        }
    }
}