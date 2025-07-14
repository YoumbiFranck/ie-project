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
 * Camunda Delegate für das Versenden von Zahlungserinnerungen
 * Camunda Delegate for sending payment reminders
 *
 * This delegate handles the sending of payment reminders to admitted students
 * who have not yet paid their semester fee within the expected timeframe.
 *
 * Dieser Delegate behandelt das Versenden von Zahlungserinnerungen an zugelassene
 * Studenten, die ihren Semesterbeitrag noch nicht im erwarteten Zeitrahmen bezahlt haben.
 *
 * @author IE Project Team
 */
@Component("paymentReminderDelegate")
public class PaymentReminderDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== PAYMENT REMINDER DELEGATE EXECUTED ===");

        try {
            // Get application and payment information / Bewerbungs- und Zahlungsinformationen holen
            Long applicationId = getApplicationId(execution);
            String admissionReference = getStringVariable(execution, "admissionReference");
            String paymentDeadline = getStringVariable(execution, "paymentDeadline");
            String semesterFeeAmount = getStringVariable(execution, "semesterFeeAmount");
            String studyProgramName = getStringVariable(execution, "studyProgramName");
            String paymentStatus = getStringVariable(execution, "paymentStatus");
            Boolean deadlineExpired = getBooleanVariable(execution, "deadlineExpired");

            System.out.println("Application ID: " + applicationId);
            System.out.println("Payment Status: " + paymentStatus);
            System.out.println("Deadline Expired: " + deadlineExpired);

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Determine reminder type and urgency / Erinnerungstyp und Dringlichkeit bestimmen
            String reminderType = determineReminderType(deadlineExpired, paymentStatus);
            String urgencyLevel = determineUrgencyLevel(deadlineExpired, paymentStatus);

            // Calculate days remaining or overdue / Verbleibende oder überfällige Tage berechnen
            String daysInfo = calculateDaysInfo(paymentDeadline, deadlineExpired);

            // Set process variables / Prozessvariablen setzen
            execution.setVariable("paymentReminderSent", true);
            execution.setVariable("reminderType", reminderType);
            execution.setVariable("urgencyLevel", urgencyLevel);
            execution.setVariable("daysInfo", daysInfo);
            execution.setVariable("reminderSentAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            execution.setVariable("nextProcessStep", "SECOND_PAYMENT_TIMER");

            // Create payment reminder message / Zahlungserinnerungs-Nachricht erstellen
            String paymentReminder = createPaymentReminderMessage(
                    application, studyProgramName, admissionReference, paymentDeadline,
                    semesterFeeAmount, reminderType, urgencyLevel, daysInfo, deadlineExpired
            );

            execution.setVariable("paymentReminderMessage", paymentReminder);

            // Simulate email sending / E-Mail-Versand simulieren
            simulateReminderEmailSending(application.getEmail(), admissionReference, reminderType, urgencyLevel);

            // Log payment reminder / Zahlungserinnerung protokollieren
            System.out.println("=== ZAHLUNGSERINNERUNG VERSENDET / PAYMENT REMINDER SENT ===");
            System.out.println(paymentReminder);
            System.out.println("============================================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== PAYMENT REMINDER PROCESSING COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Study Program: " + studyProgramName);
            System.out.println("Admission Reference: " + admissionReference);
            System.out.println("Reminder Type: " + reminderType);
            System.out.println("Urgency Level: " + urgencyLevel);
            System.out.println("Days Info: " + daysInfo);
            System.out.println("Email Sent To: " + application.getEmail());
            System.out.println("Next Step: SECOND_PAYMENT_TIMER");
            System.out.println("===============================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN PAYMENT REMINDER DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==========================================");
            throw e;
        }
    }

    /**
     * Bestimmt den Typ der Erinnerung
     * Determines the type of reminder
     */
    private String determineReminderType(Boolean deadlineExpired, String paymentStatus) {
        if (Boolean.TRUE.equals(deadlineExpired)) {
            return "OVERDUE_NOTICE";
        } else if ("PENDING".equals(paymentStatus)) {
            return "FRIENDLY_REMINDER";
        } else {
            return "STANDARD_REMINDER";
        }
    }

    /**
     * Bestimmt die Dringlichkeitsstufe
     * Determines the urgency level
     */
    private String determineUrgencyLevel(Boolean deadlineExpired, String paymentStatus) {
        if (Boolean.TRUE.equals(deadlineExpired)) {
            return "URGENT";
        } else if ("PENDING".equals(paymentStatus)) {
            return "NORMAL";
        } else {
            return "LOW";
        }
    }

    /**
     * Berechnet Informationen über verbleibende oder überfällige Tage
     * Calculates information about remaining or overdue days
     */
    private String calculateDaysInfo(String paymentDeadline, Boolean deadlineExpired) {
        try {
            if (paymentDeadline == null) {
                return "Deadline unbekannt";
            }

            LocalDateTime deadline = LocalDateTime.parse(paymentDeadline + " 23:59",
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            LocalDateTime now = LocalDateTime.now();

            long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), deadline.toLocalDate());

            if (Boolean.TRUE.equals(deadlineExpired) || daysDifference < 0) {
                return Math.abs(daysDifference) + " Tage überfällig";
            } else if (daysDifference == 0) {
                return "Deadline heute!";
            } else {
                return daysDifference + " Tage verbleibend";
            }

        } catch (Exception e) {
            return "Deadline-Berechnung fehlgeschlagen";
        }
    }

    /**
     * Simuliert E-Mail-Versand für Zahlungserinnerung
     * Simulates email sending for payment reminder
     */
    private void simulateReminderEmailSending(String recipientEmail, String reference, String reminderType, String urgencyLevel) {
        System.out.println("=== REMINDER EMAIL SIMULATION ===");
        System.out.println(" TO / AN: " + recipientEmail);

        String subject;
        String priority;

        switch (reminderType) {
            case "OVERDUE_NOTICE":
                subject = "DRINGEND: Überfällige Zahlung - Semesterbeitrag";
                priority = "HIGH";
                break;
            case "FRIENDLY_REMINDER":
                subject = "Erinnerung: Semesterbeitrag - Universität Riedtal";
                priority = "NORMAL";
                break;
            default:
                subject = "Zahlungserinnerung - Universität Riedtal";
                priority = "NORMAL";
        }

        System.out.println(" SUBJECT / BETREFF: " + subject + " (Ref: " + reference + ")");
        System.out.println(" PRIORITY / PRIORITÄT: " + priority);
        System.out.println(" REMINDER TYPE / ERINNERUNGSTYP: " + reminderType);
        System.out.println(" URGENCY / DRINGLICHKEIT: " + urgencyLevel);
        System.out.println(" REMINDER EMAIL SENT / ERINNERUNGS-E-MAIL VERSENDET");
        System.out.println("=================================");
    }

    /**
     * Erstellt die Zahlungserinnerungs-Nachricht
     * Creates the payment reminder message
     */
    private String createPaymentReminderMessage(Application application, String studyProgramName, String admissionReference,
                                                String paymentDeadline, String semesterFeeAmount, String reminderType,
                                                String urgencyLevel, String daysInfo, Boolean deadlineExpired) {

        StringBuilder reminder = new StringBuilder();

        // Header based on urgency / Kopfzeile basierend auf Dringlichkeit
        if (Boolean.TRUE.equals(deadlineExpired)) {
            reminder.append("=== DRINGENDE ZAHLUNGSERINNERUNG / URGENT PAYMENT REMINDER ===\n\n");
            reminder.append("ACHTUNG: ZAHLUNGSFRIST ÜBERSCHRITTEN! \n");
            reminder.append(" WARNING: PAYMENT DEADLINE EXCEEDED! \n\n");
        } else {
            reminder.append("=== ZAHLUNGSERINNERUNG / PAYMENT REMINDER ===\n\n");
        }


        reminder.append(" Referenz / Reference: ").append(admissionReference).append("\n\n");

        // Student information / Studenten-Informationen
        reminder.append("=== STUDENTEN DATEN / STUDENT DATA ===\n");
        reminder.append(" Name: ").append(application.getFirstName()).append(" ").append(application.getLastName()).append("\n");
        reminder.append(" E-Mail: ").append(application.getEmail()).append("\n");
        reminder.append(" Studiengang / Study Program: ").append(studyProgramName).append("\n\n");

        // Payment information / Zahlungsinformationen
        reminder.append("=== ZAHLUNGSINFORMATIONEN / PAYMENT INFORMATION ===\n");
        reminder.append(" Offener Betrag / Outstanding Amount: €").append(semesterFeeAmount).append("\n");
        reminder.append(" Ursprüngliche Frist / Original Deadline: ").append(paymentDeadline).append("\n");
        reminder.append(" Status: ").append(daysInfo).append("\n\n");

        // Urgency-specific message / Dringlichkeits-spezifische Nachricht
        if (Boolean.TRUE.equals(deadlineExpired)) {
            reminder.append("=== DRINGENDE MASSNAHMEN ERFORDERLICH ===\n");
            reminder.append(" Ihre Zulassung ist durch die verspätete Zahlung gefährdet!\n");
            reminder.append("Bitte überweisen Sie den Betrag SOFORT, um Ihre Zulassung zu sichern.\n");
        } else {
            reminder.append("=== FREUNDLICHE ERINNERUNG / FRIENDLY REMINDER ===\n");
            reminder.append(" Wir erinnern Sie daran, dass Ihr Semesterbeitrag noch aussteht.\n");
            reminder.append("Bitte überweisen Sie den Betrag bis zum ").append(paymentDeadline).append(".\n");
        }


        // Next steps / Nächste Schritte
        reminder.append("=== NÄCHSTE SCHRITTE / NEXT STEPS ===\n");
        if (Boolean.TRUE.equals(deadlineExpired)) {
            reminder.append("1.  SOFORTIGE Überweisung des Semesterbeitrags\n");
            reminder.append("3. ️ Bei weiterer Verzögerung: Zulassung wird zurückgezogen\n");
        } else {
            reminder.append("1.  Überweisung bis ").append(paymentDeadline).append("\n");
            reminder.append("2.  Automatische Bestätigung nach Zahlungseingang\n");
        }





        return reminder.toString();
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
