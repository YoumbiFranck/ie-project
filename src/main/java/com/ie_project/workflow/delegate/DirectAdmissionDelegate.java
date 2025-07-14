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
 * Camunda Delegate für die direkte Zulassung (zulassungsfreie Studiengänge)
 * Camunda Delegate for direct admission (open admission study programs)
 *
 * This delegate handles applications for study programs with open admission.
 * These applications are automatically accepted without further selection process.
 *
 * Dieser Delegate behandelt Bewerbungen für zulassungsfreie Studiengänge.
 * Diese Bewerbungen werden automatisch ohne weiteres Auswahlverfahren angenommen.
 *
 * @author IE Project Team
 */
@Component("directAdmissionDelegate")
public class DirectAdmissionDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== DIRECT ADMISSION DELEGATE EXECUTED ===");

        try {
            // Get application ID from process variables with type safety
            // Bewerbungs-ID aus Prozessvariablen mit Type Safety holen
            Long applicationId = getApplicationId(execution);

            System.out.println("Application ID: " + applicationId);

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Get study program information from process variables
            // Studiengang-Informationen aus Prozessvariablen holen
            String studyProgramName = getStringVariable(execution, "studyProgramName");
            String studyProgramCode = getStringVariable(execution, "studyProgramCode");
            String admissionType = getStringVariable(execution, "admissionType");

            // Verify this is indeed an open admission program
            // Verifizieren, dass es sich tatsächlich um einen zulassungsfreien Studiengang handelt
            if (!"OPEN".equals(admissionType)) {
                throw new IllegalStateException("Direct admission delegate called for non-open admission program: " + admissionType);
            }

            // Update application status to ACCEPTED / Bewerbungsstatus auf ACCEPTED aktualisieren
            application.setStatus(Application.ApplicationStatus.ACCEPTED);
            applicationRepository.save(application);

            // Set process variables for next steps / Prozessvariablen für nächste Schritte setzen
            execution.setVariable("directAdmissionCompleted", true);
            execution.setVariable("admissionDecision", "ACCEPTED");
            execution.setVariable("admissionReason", "OPEN_ADMISSION");
            execution.setVariable("admissionProcessedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            execution.setVariable("nextProcessStep", "ADMISSION_LETTER");

            // Create detailed admission notification / Detaillierte Zulassungsbenachrichtigung erstellen
            String admissionNotification = createDirectAdmissionNotification(
                    application, studyProgramName, studyProgramCode
            );

            execution.setVariable("directAdmissionNotification", admissionNotification);

            // Log direct admission notification / Direkte Zulassungsbenachrichtigung protokollieren
            System.out.println("=== DIREKTE ZULASSUNG / DIRECT ADMISSION ===");
            System.out.println(admissionNotification);
            System.out.println("============================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== DIRECT ADMISSION PROCESSING COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Study Program: " + studyProgramName + " (" + studyProgramCode + ")");
            System.out.println("Admission Type: " + admissionType);
            System.out.println("Decision: ACCEPTED (Direct Admission)");
            System.out.println("Status Updated: " + application.getStatus());
            System.out.println("Next Step: ADMISSION_LETTER");
            System.out.println("Processing Completed: YES");
            System.out.println("=============================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN DIRECT ADMISSION DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==========================================");
            throw e;
        }
    }

    /**
     * Erstellt eine detaillierte Benachrichtigung für die direkte Zulassung
     * Creates a detailed notification for direct admission
     */
    private String createDirectAdmissionNotification(Application application, String studyProgramName, String studyProgramCode) {

        StringBuilder notification = new StringBuilder();

        // Header / Kopfzeile
        notification.append("=== DIREKTE ZULASSUNG ERHALTEN / DIRECT ADMISSION GRANTED ===\n\n");

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

        // Admission information / Zulassungsinformationen
        notification.append("=== ZULASSUNGSINFORMATIONEN / ADMISSION INFORMATION ===\n");
        notification.append(" Herzlichen Glückwunsch! Sie wurden direkt zugelassen!\n");
        notification.append("Status: ANGENOMMEN / ACCEPTED\n");



        // Footer / Fußzeile
        notification.append("\n=== UNIVERSITÄT RIEDTAL - ZULASSUNGSSTELLE ===");
        notification.append("\n=== UNIVERSITY RIEDTAL - ADMISSIONS OFFICE ===");

        return notification.toString();
    }

    /**
     * Helper methods for type-safe variable retrieval
     * Hilfsmethoden für typsichere Variablen-Abfrage
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

    private String getStringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);

        if (value == null) {
            return null;
        }

        return value.toString();
    }
}
