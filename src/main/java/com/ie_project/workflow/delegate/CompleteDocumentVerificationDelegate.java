package com.ie_project.workflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Camunda Delegate für den Abschluss der Dokumentenprüfung (Type-Safe)
 * Camunda Delegate for completing document verification (Type-Safe)
 *
 * @author IE Project Team
 */
@Component("completeDocumentVerificationDelegate")
public class CompleteDocumentVerificationDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== COMPLETE DOCUMENT VERIFICATION DELEGATE EXECUTED ===");

        try {
            // Get document verification details from User Task form with type safety
            // Dokumentenprüfungsdetails aus User Task Formular mit Type-Safety holen
            Long applicationId = getApplicationId(execution);
            Boolean documentsComplete = getBooleanVariable(execution, "documentsComplete");
            String verificationNotes = getStringVariable(execution, "verificationNotes");
            String verifiedBy = getStringVariable(execution, "verifiedBy");

            // Log the details / Details protokollieren
            System.out.println("Application ID: " + applicationId);
            System.out.println("Documents Complete: " + documentsComplete);
            System.out.println("Verification Notes: " + verificationNotes);
            System.out.println("Verified By: " + verifiedBy);

            // Verify that documents are indeed complete / Verifizieren, dass Dokumente tatsächlich vollständig sind
            if (!Boolean.TRUE.equals(documentsComplete)) {
                throw new IllegalStateException("Documents should be complete but documentsComplete is not true");
            }

            // Create success notification without database access
            // Erfolgsbenachrichtigung ohne Datenbankzugriff erstellen
            String message = String.format(
                    "DOKUMENTENPRÜFUNG ERFOLGREICH ABGESCHLOSSEN für Bewerbung %d\n" +
                            "Alle Dokumente sind vollständig und geprüft.\n" +
                            "Anmerkungen: %s\n" +
                            "Geprüft von: %s\n" +
                            "Status: ANGENOMMEN - Bewerbung kann zum nächsten Schritt.",
                    applicationId, verificationNotes, verifiedBy
            );

            System.out.println("=== DOCUMENT VERIFICATION COMPLETED ===");
            System.out.println(message);
            System.out.println("========================================");

            // Set final process variables / Finale Prozessvariablen setzen
            execution.setVariable("documentsVerified", true);
            execution.setVariable("documentsComplete", true);
            execution.setVariable("completionNotification", message);
            execution.setVariable("processCompleted", true);
            execution.setVariable("finalStatus", "DOCUMENTS_VERIFIED_COMPLETE");
            execution.setVariable("nextProcessStep", "SELECTION_PROCESS");
            execution.setVariable("verificationCompletedAt", java.time.LocalDateTime.now().toString());

            System.out.println("=== PROCESS COMPLETED SUCCESSFULLY ===");
            System.out.println("Application is ready for next phase: SELECTION_PROCESS");
            System.out.println("=======================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN COMPLETE DOCUMENT VERIFICATION DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================================");
            throw e; // Re-throw to let Camunda handle it
        }
    }

    /**
     * Récupère l'applicationId en gérant les conversions de type
     * Gets applicationId while handling type conversions
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

    /**
     * Récupère une variable Boolean en gérant les conversions
     * Gets a Boolean variable while handling conversions
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
     * Gets a String variable while handling conversions
     */
    private String getStringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);

        if (value == null) {
            return null;
        }

        return value.toString();
    }
}