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
 * Camunda Delegate für die finale Überprüfung des Zahlungsstatus
 * Camunda Delegate for final payment status check
 *
 * This delegate performs the final check of payment status after the second payment timer.
 * It determines whether the student has paid after the reminder or if the admission
 * should be revoked due to non-payment.
 *
 * Dieser Delegate führt die finale Prüfung des Zahlungsstatus nach dem zweiten
 * Zahlungs-Timer durch. Er bestimmt, ob der Student nach der Erinnerung bezahlt hat
 * oder ob die Zulassung wegen Nichtzahlung zurückgezogen werden soll.
 *
 * @author IE Project Team
 */
@Component("finalPaymentCheckDelegate")
public class FinalPaymentCheckDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== FINAL PAYMENT CHECK DELEGATE EXECUTED ===");

        try {
            // Get application and payment information / Bewerbungs- und Zahlungsinformationen holen
            Long applicationId = getApplicationId(execution);
            String admissionReference = getStringVariable(execution, "admissionReference");
            String paymentDeadline = getStringVariable(execution, "paymentDeadline");
            String semesterFeeAmount = getStringVariable(execution, "semesterFeeAmount");
            String studyProgramName = getStringVariable(execution, "studyProgramName");
            String reminderSentAt = getStringVariable(execution, "reminderSentAt");
            String reminderType = getStringVariable(execution, "reminderType");

            System.out.println("Application ID: " + applicationId);
            System.out.println("Reminder Type: " + reminderType);
            System.out.println("Payment Deadline: " + paymentDeadline);

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Check final payment status / Finalen Zahlungsstatus prüfen
            boolean finalPaymentReceived = application.isTuitionFeePaid();

            // Calculate time since reminder / Zeit seit Erinnerung berechnen
            String timeSinceReminder = calculateTimeSinceReminder(reminderSentAt);

            // Check if final deadline has passed / Prüfen ob finale Deadline überschritten ist
            boolean finalDeadlineExpired = checkFinalDeadlineExpired(paymentDeadline);

            // Determine final payment decision / Finale Zahlungsentscheidung bestimmen
            String finalDecision = determineFinalDecision(finalPaymentReceived, finalDeadlineExpired);
            String finalStatus = finalPaymentReceived ? "PAYMENT_COMPLETED" : "PAYMENT_FAILED";

            // Set process variables for final gateway decision / Prozessvariablen für finale Gateway-Entscheidung setzen
            execution.setVariable("finalPaymentReceived", finalPaymentReceived);
            execution.setVariable("finalPaymentCheckCompleted", true);
            execution.setVariable("finalDecision", finalDecision);
            execution.setVariable("finalPaymentStatus", finalStatus);
            execution.setVariable("finalDeadlineExpired", finalDeadlineExpired);
            execution.setVariable("timeSinceReminder", timeSinceReminder);
            execution.setVariable("finalCheckDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

            if (finalPaymentReceived) {
                execution.setVariable("nextProcessStep", "STUDENT_ENROLLMENT");
            } else {
                execution.setVariable("nextProcessStep", "ADMISSION_REVOCATION");
            }

            // Create final payment check notification / Finale Zahlungsprüfungs-Benachrichtigung erstellen
            String finalCheckNotification = createFinalCheckNotification(
                    application, studyProgramName, admissionReference, finalPaymentReceived,
                    paymentDeadline, semesterFeeAmount, timeSinceReminder, finalDecision,
                    finalDeadlineExpired, reminderType
            );

            execution.setVariable("finalPaymentCheckNotification", finalCheckNotification);

            // Log final payment check result / Finales Zahlungsprüfungs-Ergebnis protokollieren
            System.out.println("=== FINALE ZAHLUNGSPRÜFUNG / FINAL PAYMENT CHECK ===");
            System.out.println(finalCheckNotification);
            System.out.println("===================================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== FINAL PAYMENT CHECK PROCESSING COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Study Program: " + studyProgramName);
            System.out.println("Admission Reference: " + admissionReference);
            System.out.println("Final Payment Received: " + finalPaymentReceived);
            System.out.println("Final Decision: " + finalDecision);
            System.out.println("Final Status: " + finalStatus);
            System.out.println("Final Deadline Expired: " + finalDeadlineExpired);
            System.out.println("Time Since Reminder: " + timeSinceReminder);
            System.out.println("Next Step: " + (finalPaymentReceived ? "STUDENT_ENROLLMENT" : "ADMISSION_REVOCATION"));
            System.out.println("================================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN FINAL PAYMENT CHECK DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=============================================");
            throw e;
        }
    }

    /**
     * Berechnet die Zeit seit dem Versand der Erinnerung
     * Calculates time since reminder was sent
     */
    private String calculateTimeSinceReminder(String reminderSentAt) {
        try {
            if (reminderSentAt == null) {
                return "Unbekannt";
            }

            LocalDateTime reminderTime = LocalDateTime.parse(reminderSentAt, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
            LocalDateTime now = LocalDateTime.now();

            long hoursDifference = java.time.temporal.ChronoUnit.HOURS.between(reminderTime, now);
            long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(reminderTime, now);

            if (daysDifference > 0) {
                return daysDifference + " Tag(e) seit Erinnerung";
            } else if (hoursDifference > 0) {
                return hoursDifference + " Stunde(n) seit Erinnerung";
            } else {
                long minutesDifference = java.time.temporal.ChronoUnit.MINUTES.between(reminderTime, now);
                return minutesDifference + " Minute(n) seit Erinnerung";
            }

        } catch (Exception e) {
            return "Zeit-Berechnung fehlgeschlagen";
        }
    }

    /**
     * Prüft ob die finale Deadline überschritten ist
     * Checks if final deadline has expired
     */
    private boolean checkFinalDeadlineExpired(String paymentDeadline) {
        try {
            if (paymentDeadline == null) {
                return true; // Assume expired if no deadline
            }

            LocalDateTime deadline = LocalDateTime.parse(paymentDeadline + " 23:59",
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            LocalDateTime now = LocalDateTime.now();

            return now.isAfter(deadline);

        } catch (Exception e) {
            System.out.println("Warning: Could not parse final payment deadline: " + paymentDeadline);
            return true; // Assume expired on parse error
        }
    }

    /**
     * Bestimmt die finale Entscheidung
     * Determines the final decision
     */
    private String determineFinalDecision(boolean paymentReceived, boolean deadlineExpired) {
        if (paymentReceived) {
            return "PAYMENT_SUCCESS";
        } else if (deadlineExpired) {
            return "REVOKE_ADMISSION";
        } else {
            return "PAYMENT_PENDING"; // Edge case
        }
    }

    /**
     * Erstellt die finale Zahlungsprüfungs-Benachrichtigung
     * Creates the final payment check notification
     */
    private String createFinalCheckNotification(Application application, String studyProgramName, String admissionReference,
                                                boolean finalPaymentReceived, String paymentDeadline, String semesterFeeAmount,
                                                String timeSinceReminder, String finalDecision, boolean finalDeadlineExpired,
                                                String reminderType) {

        StringBuilder notification = new StringBuilder();

        // Header / Kopfzeile
        notification.append("=== FINALE ZAHLUNGSPRÜFUNG / FINAL PAYMENT CHECK ===\n\n");

        // Student information / Studenten-Informationen
        notification.append(" Bewerber / Applicant: ").append(application.getFirstName())
                .append(" ").append(application.getLastName()).append("\n");
        notification.append(" E-Mail: ").append(application.getEmail()).append("\n");
        notification.append(" Bewerbungs-ID / Application ID: ").append(application.getId()).append("\n");
        notification.append(" Studiengang / Study Program: ").append(studyProgramName).append("\n");
        notification.append(" Referenz / Reference: ").append(admissionReference).append("\n\n");

        // Payment timeline / Zahlungs-Timeline
        notification.append("=== ZAHLUNGSVERLAUF / PAYMENT TIMELINE ===\n");
        notification.append(" Betrag / Amount: €").append(semesterFeeAmount).append("\n");
        notification.append(" Ursprüngliche Frist / Original Deadline: ").append(paymentDeadline).append("\n");
        notification.append(" Letzte Erinnerung / Last Reminder: ").append(reminderType).append("\n");
        notification.append("Zeit seit Erinnerung / Time Since Reminder: ").append(timeSinceReminder).append("\n");
        notification.append(" Finale Prüfung am / Final Check on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n\n");

        // Final result / Finales Ergebnis
        notification.append("=== FINALES ERGEBNIS / FINAL RESULT ===\n");

        if (finalPaymentReceived) {
            notification.append(" ZAHLUNG ERFOLGREICH EINGEGANGEN / PAYMENT SUCCESSFULLY RECEIVED\n\n");
            notification.append(" Herzlichen Glückwunsch! Ihr Semesterbeitrag wurde erfolgreich überwiesen.\n");

        } else {
            notification.append(" ZAHLUNG NICHT EINGEGANGEN / PAYMENT NOT RECEIVED\n\n");

            if (finalDeadlineExpired) {
                notification.append(" KRITISCH: Finale Zahlungsfrist ist abgelaufen!\n");
                notification.append(" Ihre Zulassung wird zurückgezogen / Your admission will be revoked\n");
                notification.append(" Der Studienplatz wird an den nächsten Bewerber vergeben\n");
            } else {
                notification.append(" Letzte Gelegenheit zur Zahlung\n");
            }

            notification.append(" Grund für Zulassungsrücknahme:\n");
            notification.append("   • Nichtzahlung des Semesterbeitrags trotz Erinnerung\n");
            notification.append("   • Überschreitung der Zahlungsfrist\n");
            notification.append("   • Keine rechtzeitige Reaktion auf Zahlungsaufforderung\n\n");
        }

        // Decision details / Entscheidungsdetails
        notification.append("\n=== ENTSCHEIDUNGSDETAILS / DECISION DETAILS ===\n");
        notification.append(" Finale Entscheidung / Final Decision: ").append(finalDecision).append("\n");
        notification.append(" Zahlungsstatus / Payment Status: ").append(finalPaymentReceived ? "BEZAHLT" : "NICHT BEZAHLT").append("\n");
        notification.append(" Deadline Status: ").append(finalDeadlineExpired ? "ABGELAUFEN" : "NOCH GÜLTIG").append("\n");



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
