package com.ie_project.workflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Camunda Delegate für die Behandlung unvollständiger Dokumente (Type-Safe)
 * Camunda Delegate for handling incomplete documents (Type-Safe)
 *
 * @author IE Project Team
 */
@Component("incompleteDocumentsDelegate")
public class IncompleteDocumentsDelegate implements JavaDelegate {


    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== INCOMPLETE DOCUMENTS DELEGATE EXECUTED ===");

        try {
            // Get document verification details from User Task form with type safety
            Long applicationId = getApplicationId(execution);
            Boolean documentsComplete = getBooleanVariable(execution, "documentsComplete");
            String missingDocuments = getStringVariable(execution, "missingDocuments");
            String verificationNotes = getStringVariable(execution, "verificationNotes");
            String verifiedBy = getStringVariable(execution, "verifiedBy");

            // Log the details / Details protokollieren
            System.out.println("Application ID: " + applicationId);
            System.out.println("Documents Complete: " + documentsComplete);
            System.out.println("Missing Documents: " + missingDocuments);
            System.out.println("Verification Notes: " + verificationNotes);
            System.out.println("Verified By: " + verifiedBy);

            // Verify that documents are indeed incomplete / Verifizieren, dass Dokumente tatsächlich unvollständig sind
            if (Boolean.TRUE.equals(documentsComplete)) {
                throw new IllegalStateException("Documents should be incomplete but documentsComplete is true");
            }

            // Create notification message for incomplete documents without database access
            String message = String.format(
                    "DOKUMENTE UNVOLLSTÄNDIG für Bewerbung %d\n" +
                            "Fehlende Dokumente: %s\n" +
                            "Anmerkungen: %s\n" +
                            "Geprüft von: %s\n" +
                            "Der Bewerber muss die fehlenden Dokumente nachreichen.\n" +
                            "Prozess kehrt zur Deadline-Prüfung zurück.",
                    applicationId, missingDocuments, verificationNotes, verifiedBy
            );

            System.out.println("=== INCOMPLETE DOCUMENTS NOTIFICATION ===");
            System.out.println(message);
            System.out.println("==========================================");

            // Set process variables for tracking and potential re-processing
            execution.setVariable("documentsIncomplete", true);
            execution.setVariable("documentsCompletionRequired", true);
            execution.setVariable("incompleteNotification", message);
            execution.setVariable("missingDocumentsList", missingDocuments != null ? missingDocuments : "");
            execution.setVariable("lastVerificationDate", java.time.LocalDateTime.now().toString());
            execution.setVariable("lastVerifiedBy", verifiedBy);
            execution.setVariable("requiresRecheck", true);

            // Increment verification attempts
            Integer attempts = (Integer) execution.getVariable("verificationAttempts");
            attempts = (attempts != null) ? attempts + 1 : 1;
            execution.setVariable("verificationAttempts", attempts);

            System.out.println("=== RETURNING TO DEADLINE CHECK ===");
            System.out.println("Process will return to welcome message and deadline check");
            System.out.println("Verification attempt: " + attempts);
            System.out.println("====================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN INCOMPLETE DOCUMENTS DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("===============================================");
            throw e;
        }
    }

    private Long getApplicationId(DelegateExecution execution) {
        Object applicationIdObj = execution.getVariable("applicationId");

        System.out.println("DEBUG - applicationId raw value: " + applicationIdObj);
        System.out.println("DEBUG - applicationId type: " + (applicationIdObj != null ? applicationIdObj.getClass() : "null"));

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

    /**
     * Récupère une variable Boolean en gérant les conversions
     */
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

    /**
     * Récupère une variable String en gérant les conversions
     */
    private String getStringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);

        if (value == null) {
            return null;
        }

        return value.toString();
    }

}