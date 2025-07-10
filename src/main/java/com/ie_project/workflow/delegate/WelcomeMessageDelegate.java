package com.ie_project.workflow.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Camunda Delegate für das Erstellen der Willkommensnachricht
 * Camunda Delegate for creating the welcome message
 *
 * @author IE Project Team
 */
@Component("welcomeMessageDelegate")
public class WelcomeMessageDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        // Get variables from process context / Variablen aus Prozesskontext holen
        String firstName = (String) execution.getVariable("firstName");
        String lastName = (String) execution.getVariable("lastName");
        String email = (String) execution.getVariable("email");
        String studyProgramName = (String) execution.getVariable("studyProgramName");
        Long applicationId = (Long) execution.getVariable("applicationId");
        String applicationDate = (String) execution.getVariable("applicationDate");

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
        // Nachricht protokollieren (in Produktion könnte diese per E-Mail gesendet werden)
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
        System.out.println("=== WELCOME MESSAGE DELEGATE EXECUTED ===");
        System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
        System.out.println("Activity ID: " + execution.getCurrentActivityId());
        System.out.println("Application ID: " + applicationId);
        System.out.println("Message generated for: " + firstName + " " + lastName);
        System.out.println("==========================================");
    }
}
