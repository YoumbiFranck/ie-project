package com.ie_project.workflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Camunda Delegate für das Erstellen der Willkommensnachricht (Type-Safe)
 * Camunda Delegate for creating the welcome message (Type-Safe)
 *
 * @author IE Project Team
 */
@Component("welcomeMessageDelegate")
public class WelcomeMessageDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== WELCOME MESSAGE DELEGATE EXECUTED ===");

        try {
            // Get variables from process context with type safety
            String firstName = getStringVariable(execution, "firstName");
            String lastName = getStringVariable(execution, "lastName");
            String email = getStringVariable(execution, "email");
            String studyProgramName = getStringVariable(execution, "studyProgramName");
            Long applicationId = getApplicationId(execution);
            String applicationDate = getStringVariable(execution, "applicationDate");

            // Log the details
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + firstName + " " + lastName);
            System.out.println("Email: " + email);
            System.out.println("Study Program: " + studyProgramName);
            System.out.println("Application Date: " + applicationDate);

            // Create welcome message in German / Willkommensnachricht auf Deutsch erstellen
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Hallo ").append(firstName).append(" ").append(lastName).append("!\n\n");
            messageBuilder.append("Ihre Bewerbung wurde erfolgreich eingereicht:\n");
            messageBuilder.append("- Studiengang: ").append(studyProgramName).append("\n");
            messageBuilder.append("- E-Mail: ").append(email).append("\n");
            messageBuilder.append("- Bewerbungs-ID: ").append(applicationId).append("\n");
            messageBuilder.append("- Eingangsdatum: ").append(applicationDate).append("\n\n");
            messageBuilder.append("Ihre Bewerbung wird nun bearbeitet. Sie erhalten weitere Informationen per E-Mail.\n");
            messageBuilder.append("Status: Bewerbung erfolgreich eingegangen.");

            String message = messageBuilder.toString();

            // Log the message (in production, this could be sent via email)
            System.out.println("=== WILLKOMMENSNACHRICHT ===");
            System.out.println(message);
            System.out.println("============================");

            // Set process variables for tracking / Prozessvariablen für Verfolgung setzen
            execution.setVariable("welcomeMessage", message);
            execution.setVariable("welcomeMessageSent", true);
            execution.setVariable("processStatus", "BEWERBUNG_EINGEGANGEN");
            execution.setVariable("nextStep", "DOKUMENTENPRÜFUNG");
            execution.setVariable("messageGeneratedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

            // Log success / Erfolg protokollieren
            System.out.println("=== WELCOME MESSAGE DELEGATE EXECUTED SUCCESSFULLY ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Message generated for: " + firstName + " " + lastName);
            System.out.println("======================================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN WELCOME MESSAGE DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=========================================");
            throw e;
        }
    }

    /**
     * Helper methods for type-safe variable retrieval
     */
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

    private String getStringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);

        if (value == null) {
            return null;
        }

        return value.toString();
    }
}