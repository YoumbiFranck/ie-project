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
 * Camunda Delegate für die Behandlung von Zahlungsablehnungen und Zulassungsrücknahmen
 * Camunda Delegate for handling payment rejections and admission revocations
 *
 * This delegate handles the final rejection of applications due to non-payment
 * of semester fees. It revokes the admission and finalizes the rejection process.
 *
 * Dieser Delegate behandelt die finale Ablehnung von Bewerbungen aufgrund von
 * Nichtzahlung der Semesterbeiträge. Er zieht die Zulassung zurück und schließt
 * den Ablehnungsprozess ab.
 *
 * @author IE Project Team
 */
@Component("paymentRejectionDelegate")
public class PaymentRejectionDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== PAYMENT REJECTION DELEGATE EXECUTED ===");

        try {
            // Get application and payment information / Bewerbungs- und Zahlungsinformationen holen
            Long applicationId = getApplicationId(execution);
            String admissionReference = getStringVariable(execution, "admissionReference");
            String paymentDeadline = getStringVariable(execution, "paymentDeadline");
            String semesterFeeAmount = getStringVariable(execution, "semesterFeeAmount");
            String studyProgramName = getStringVariable(execution, "studyProgramName");
            String finalDecision = getStringVariable(execution, "finalDecision");
            String timeSinceReminder = getStringVariable(execution, "timeSinceReminder");
            Boolean finalDeadlineExpired = getBooleanVariable(execution, "finalDeadlineExpired");

            System.out.println("Application ID: " + applicationId);
            System.out.println("Final Decision: " + finalDecision);
            System.out.println("Admission Reference: " + admissionReference);

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Update application status to REJECTED due to payment failure
            // Bewerbungsstatus auf REJECTED wegen Zahlungsausfall aktualisieren
            application.setStatus(Application.ApplicationStatus.REJECTED);
            applicationRepository.save(application);

            // Set final process variables / Finale Prozessvariablen setzen
            execution.setVariable("paymentRejectionCompleted", true);
            execution.setVariable("rejectionReason", "PAYMENT_NOT_RECEIVED");
            execution.setVariable("admissionRevoked", true);
            execution.setVariable("processCompleted", true);
            execution.setVariable("finalStatus", "REJECTED");
            execution.setVariable("processEndReason", "PAYMENT_REJECTION");
            execution.setVariable("rejectionProcessedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

            // Create payment rejection and admission revocation notification
            // Zahlungsablehnungs- und Zulassungsrücknahme-Benachrichtigung erstellen
            String rejectionNotification = createPaymentRejectionNotification(
                    application, studyProgramName, admissionReference, paymentDeadline,
                    semesterFeeAmount, timeSinceReminder, finalDeadlineExpired
            );

            execution.setVariable("paymentRejectionNotification", rejectionNotification);

            // Simulate email sending / E-Mail-Versand simulieren
            simulateRejectionEmailSending(application.getEmail(), admissionReference, "Zulassungsrücknahme");

            // Log payment rejection notification / Zahlungsablehnungs-Benachrichtigung protokollieren
            System.out.println("=== ZULASSUNG ZURÜCKGEZOGEN / ADMISSION REVOKED ===");
            System.out.println(rejectionNotification);
            System.out.println("==================================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== PAYMENT REJECTION PROCESSING COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Study Program: " + studyProgramName);
            System.out.println("Admission Reference: " + admissionReference);
            System.out.println("Rejection Reason: PAYMENT_NOT_RECEIVED");
            System.out.println("Admission Revoked: YES");
            System.out.println("Status Updated: " + application.getStatus());
            System.out.println("Process Completed: YES");
            System.out.println("==============================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN PAYMENT REJECTION DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("===========================================");
            throw e;
        }
    }

    /**
     * Simuliert E-Mail-Versand für Zulassungsrücknahme
     * Simulates email sending for admission revocation
     */
    private void simulateRejectionEmailSending(String recipientEmail, String reference, String documentType) {
        System.out.println("=== REVOCATION EMAIL SIMULATION ===");
        System.out.println(" TO / AN: " + recipientEmail);
        System.out.println(" SUBJECT / BETREFF: " + documentType + " wegen Nichtzahlung - Universität Riedtal (Ref: " + reference + ")");
        System.out.println(" PRIORITY / PRIORITÄT: HIGH");
        System.out.println(" EMAIL TYPE / E-MAIL TYP: ADMISSION_REVOCATION");
        System.out.println(" REVOCATION EMAIL SENT / RÜCKNAHME-E-MAIL VERSENDET");
        System.out.println("===================================");
    }

    /**
     * Erstellt die Zahlungsablehnungs- und Zulassungsrücknahme-Benachrichtigung
     * Creates the payment rejection and admission revocation notification
     */
    private String createPaymentRejectionNotification(Application application, String studyProgramName, String admissionReference,
                                                      String paymentDeadline, String semesterFeeAmount, String timeSinceReminder,
                                                      Boolean finalDeadlineExpired) {

        StringBuilder notification = new StringBuilder();

        // Header / Kopfzeile
        notification.append("=== ZULASSUNGSRÜCKNAHME WEGEN NICHTZAHLUNG ===\n");
        notification.append("=== ADMISSION REVOCATION DUE TO NON-PAYMENT ===\n\n");



        // Student information / Studenten-Informationen
        notification.append("=== BETROFFENER BEWERBER / AFFECTED APPLICANT ===\n");
        notification.append(" Name: ").append(application.getFirstName()).append(" ").append(application.getLastName()).append("\n");
        notification.append(" E-Mail: ").append(application.getEmail()).append("\n");
        notification.append(" Bewerbungs-ID / Application ID: ").append(application.getId()).append("\n");
        notification.append(" Studiengang / Study Program: ").append(studyProgramName).append("\n\n");

        // Payment timeline / Zahlungs-Timeline
        notification.append("=== ZAHLUNGSVERLAUF / PAYMENT TIMELINE ===\n");
        notification.append(" Ausstehender Betrag / Outstanding Amount: €").append(semesterFeeAmount).append("\n");
        notification.append(" Ursprüngliche Zahlungsfrist / Original Payment Deadline: ").append(paymentDeadline).append("\n");
        notification.append(" Erinnerungen versendet / Reminders sent: JA / YES\n");
        notification.append(" Zeit seit letzter Erinnerung / Time since last reminder: ").append(timeSinceReminder).append("\n");
        notification.append(" Finale Prüfung / Final check: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");
        notification.append(" Zahlungsstatus / Payment status: NICHT EINGEGANGEN / NOT RECEIVED\n\n");

        // Official decision / Offizielle Entscheidung
        notification.append("=== OFFIZIELLE ENTSCHEIDUNG / OFFICIAL DECISION ===\n");
        notification.append(" IHRE ZULASSUNG WIRD HIERMIT ZURÜCKGEZOGEN\n");

        // Reasons for revocation / Gründe für Rücknahme
        notification.append("=== GRÜNDE FÜR DIE RÜCKNAHME / REASONS FOR REVOCATION ===\n");
        notification.append("• Nichtzahlung des Semesterbeitrags trotz mehrfacher Aufforderung\n");
        notification.append("• Überschreitung der finalen Zahlungsfrist am ").append(paymentDeadline).append("\n");


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
}
