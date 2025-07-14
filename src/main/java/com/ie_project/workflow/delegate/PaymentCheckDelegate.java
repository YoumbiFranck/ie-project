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
 * Camunda Delegate für die Überprüfung des Zahlungsstatus
 * Camunda Delegate for checking payment status
 *
 * This delegate checks whether the semester fee has been paid by the admitted student.
 * It reads the payment status from the database and sets process variables accordingly.
 *
 * Dieser Delegate prüft, ob der Semesterbeitrag vom zugelassenen Studenten bezahlt wurde.
 * Er liest den Zahlungsstatus aus der Datenbank und setzt entsprechende Prozessvariablen.
 *
 * @author IE Project Team
 */
@Component("paymentCheckDelegate")
public class PaymentCheckDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== PAYMENT CHECK DELEGATE EXECUTED ===");

        try {
            // Get application information / Bewerbungsinformationen holen
            Long applicationId = getApplicationId(execution);
            String admissionReference = getStringVariable(execution, "admissionReference");
            String paymentDeadline = getStringVariable(execution, "paymentDeadline");
            String semesterFeeAmount = getStringVariable(execution, "semesterFeeAmount");
            String studyProgramName = getStringVariable(execution, "studyProgramName");

            System.out.println("Application ID: " + applicationId);
            System.out.println("Admission Reference: " + admissionReference);
            System.out.println("Payment Deadline: " + paymentDeadline);

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Check payment status from database / Zahlungsstatus aus Datenbank prüfen
            boolean paymentReceived = application.isTuitionFeePaid();

            // Check if payment deadline has passed / Prüfen ob Zahlungsfrist abgelaufen ist
            boolean deadlineExpired = checkIfDeadlineExpired(paymentDeadline);

            // Determine payment status / Zahlungsstatus bestimmen
            String paymentStatus = determinePaymentStatus(paymentReceived, deadlineExpired);

            // Set process variables for gateway decision / Prozessvariablen für Gateway-Entscheidung setzen
            execution.setVariable("paymentReceived", paymentReceived);
            execution.setVariable("paymentCheckCompleted", true);
            execution.setVariable("paymentStatus", paymentStatus);
            execution.setVariable("deadlineExpired", deadlineExpired);
            execution.setVariable("paymentCheckDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

            if (paymentReceived) {
                execution.setVariable("nextProcessStep", "STUDENT_ENROLLMENT");
            } else {
                execution.setVariable("nextProcessStep", "PAYMENT_REMINDER");
            }

            // Create payment check notification / Zahlungsprüfungs-Benachrichtigung erstellen
            String paymentCheckNotification = createPaymentCheckNotification(
                    application, studyProgramName, admissionReference, paymentReceived,
                    paymentDeadline, semesterFeeAmount, deadlineExpired, paymentStatus
            );

            execution.setVariable("paymentCheckNotification", paymentCheckNotification);

            // Log payment check result / Zahlungsprüfungs-Ergebnis protokollieren
            System.out.println("=== ZAHLUNGSPRÜFUNG / PAYMENT CHECK ===");
            System.out.println(paymentCheckNotification);
            System.out.println("======================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== PAYMENT CHECK PROCESSING COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Study Program: " + studyProgramName);
            System.out.println("Admission Reference: " + admissionReference);
            System.out.println("Payment Received: " + paymentReceived);
            System.out.println("Payment Status: " + paymentStatus);
            System.out.println("Deadline Expired: " + deadlineExpired);
            System.out.println("Next Step: " + (paymentReceived ? "STUDENT_ENROLLMENT" : "PAYMENT_REMINDER"));
            System.out.println("==========================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN PAYMENT CHECK DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=======================================");
            throw e;
        }
    }

    /**
     * Prüft ob die Zahlungsfrist abgelaufen ist
     * Checks if payment deadline has expired
     */
    private boolean checkIfDeadlineExpired(String paymentDeadline) {
        try {
            if (paymentDeadline == null) {
                return false;
            }

            LocalDateTime deadline = LocalDateTime.parse(paymentDeadline + " 23:59",
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            LocalDateTime now = LocalDateTime.now();

            return now.isAfter(deadline);

        } catch (Exception e) {
            System.out.println("Warning: Could not parse payment deadline: " + paymentDeadline);
            return false;
        }
    }

    /**
     * Bestimmt den Zahlungsstatus basierend auf Zahlung und Deadline
     * Determines payment status based on payment and deadline
     */
    private String determinePaymentStatus(boolean paymentReceived, boolean deadlineExpired) {
        if (paymentReceived) {
            return "PAID";
        } else if (deadlineExpired) {
            return "OVERDUE";
        } else {
            return "PENDING";
        }
    }

    /**
     * Erstellt eine Zahlungsprüfungs-Benachrichtigung
     * Creates a payment check notification
     */
    private String createPaymentCheckNotification(Application application, String studyProgramName,
                                                  String admissionReference, boolean paymentReceived,
                                                  String paymentDeadline, String semesterFeeAmount,
                                                  boolean deadlineExpired, String paymentStatus) {

        StringBuilder notification = new StringBuilder();

        // Header / Kopfzeile
        notification.append("=== ZAHLUNGSPRÜFUNG / PAYMENT CHECK ===\n\n");

        // Student information / Studenten-Informationen
        notification.append(" Bewerber / Applicant: ").append(application.getFirstName())
                .append(" ").append(application.getLastName()).append("\n");
        notification.append("E-Mail: ").append(application.getEmail()).append("\n");
        notification.append(" Bewerbungs-ID / Application ID: ").append(application.getId()).append("\n");
        notification.append(" Studiengang / Study Program: ").append(studyProgramName).append("\n");
        notification.append(" Referenz / Reference: ").append(admissionReference).append("\n\n");

        // Payment details / Zahlungsdetails
        notification.append("=== ZAHLUNGSDETAILS / PAYMENT DETAILS ===\n");
        notification.append(" Betrag / Amount: €").append(semesterFeeAmount).append("\n");
        notification.append(" Frist / Deadline: ").append(paymentDeadline).append("\n");
        notification.append(" Status: ").append(paymentStatus).append("\n");
        notification.append(" Prüfung am / Checked on: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n\n");

        // Payment result / Zahlungsergebnis
        notification.append("=== PRÜFUNGSERGEBNIS / CHECK RESULT ===\n");

        if (paymentReceived) {
            notification.append(" ZAHLUNG EINGEGANGEN / PAYMENT RECEIVED\n");
            notification.append(" Der Semesterbeitrag wurde erfolgreich überwiesen\n");
        } else {
            notification.append(" ZAHLUNG NOCH NICHT EINGEGANGEN / PAYMENT NOT YET RECEIVED\n");

            if (deadlineExpired) {
                notification.append(" ACHTUNG: Zahlungsfrist ist abgelaufen!\n");
                notification.append(" Zahlungserinnerung wird versendet\n");
            } else {
                notification.append(" Zahlungsfrist läuft noch bis ").append(paymentDeadline).append("\n");
                notification.append(" Erinnerung wird versendet\n");
            }
        }



        // Footer / Fußzeile
        notification.append("\n=== UNIVERSITÄT RIEDTAL - FINANCIAL OFFICE ===");

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
