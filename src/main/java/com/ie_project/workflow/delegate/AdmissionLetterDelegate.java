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
 * Camunda Delegate für das Versenden von Zulassungsbescheiden
 * Camunda Delegate for sending admission letters
 *
 * This delegate handles the sending of official admission letters to successful applicants
 * from all admission paths (direct, NC, entrance exam).
 *
 * Dieser Delegate behandelt das Versenden von offiziellen Zulassungsbescheiden an erfolgreiche
 * Bewerber aus allen Zulassungswegen (direkt, NC, Aufnahmeprüfung).
 *
 * @author IE Project Team
 */
@Component("admissionLetterDelegate")
public class AdmissionLetterDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== ADMISSION LETTER DELEGATE EXECUTED ===");

        try {
            // Get application information / Bewerbungsinformationen holen
            Long applicationId = getApplicationId(execution);
            String studyProgramName = getStringVariable(execution, "studyProgramName");
            String studyProgramCode = getStringVariable(execution, "studyProgramCode");
            String admissionType = getStringVariable(execution, "admissionType");

            // Get admission path specific information / Zulassungsweg-spezifische Informationen holen
            String admissionReason = determineAdmissionReason(execution, admissionType);
            String additionalInfo = getAdmissionPathDetails(execution, admissionType);

            System.out.println("Application ID: " + applicationId);
            System.out.println("Admission Type: " + admissionType);
            System.out.println("Admission Reason: " + admissionReason);

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Update application status to ACCEPTED / Bewerbungsstatus auf ACCEPTED aktualisieren
            application.setStatus(Application.ApplicationStatus.ACCEPTED);
            applicationRepository.save(application);

            // Generate admission letter reference / Zulassungsbescheid-Referenz generieren
            String admissionReference = generateAdmissionReference(applicationId, studyProgramCode);

            // Calculate semester fee deadline / Semesterbeitrag-Deadline berechnen
            String paymentDeadline = calculatePaymentDeadline();

            // Set process variables / Prozessvariablen setzen
            execution.setVariable("admissionLetterSent", true);
            execution.setVariable("admissionReference", admissionReference);
            execution.setVariable("admissionReason", admissionReason);
            execution.setVariable("paymentDeadline", paymentDeadline);
            execution.setVariable("semesterFeeAmount", "350.00"); // Standard fee
            execution.setVariable("admissionLetterSentAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            execution.setVariable("nextProcessStep", "PAYMENT_PROCESS");

            // Create official admission letter / Offiziellen Zulassungsbescheid erstellen
            String admissionLetter = createOfficialAdmissionLetter(
                    application, studyProgramName, studyProgramCode, admissionType,
                    admissionReason, additionalInfo, admissionReference, paymentDeadline
            );

            execution.setVariable("officialAdmissionLetter", admissionLetter);

            // Simulate email sending / E-Mail-Versand simulieren
            simulateEmailSending(application.getEmail(), admissionReference, "Zulassungsbescheid");

            // Log admission letter / Zulassungsbescheid protokollieren
            System.out.println("=== ZULASSUNGSBESCHEID VERSENDET / ADMISSION LETTER SENT ===");
            System.out.println(admissionLetter);
            System.out.println("===========================================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== ADMISSION LETTER PROCESSING COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Study Program: " + studyProgramName + " (" + studyProgramCode + ")");
            System.out.println("Admission Type: " + admissionType);
            System.out.println("Admission Reference: " + admissionReference);
            System.out.println("Payment Deadline: " + paymentDeadline);
            System.out.println("Status Updated: " + application.getStatus());
            System.out.println("Next Step: PAYMENT_PROCESS");
            System.out.println("=============================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN ADMISSION LETTER DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==========================================");
            throw e;
        }
    }

    /**
     * Bestimmt den Zulassungsgrund basierend auf dem Zulassungsweg
     * Determines admission reason based on admission path
     */
    private String determineAdmissionReason(DelegateExecution execution, String admissionType) {
        switch (admissionType) {
            case "OPEN":
                return "DIRECT_ADMISSION";
            case "NUMERUS_CLAUSUS":
                String ncDecision = getStringVariable(execution, "ncAdmissionReason");
                return ncDecision != null ? ncDecision : "NC_RANKING";
            case "ENTRANCE_EXAM":
                return "EXAM_PASSED";
            default:
                return "STANDARD_ADMISSION";
        }
    }

    /**
     * Holt zusätzliche Details basierend auf dem Zulassungsweg
     * Gets additional details based on admission path
     */
    private String getAdmissionPathDetails(DelegateExecution execution, String admissionType) {
        StringBuilder details = new StringBuilder();

        switch (admissionType) {
            case "OPEN":
                details.append("Zulassungsfreier Studiengang / Open admission program");
                break;
            case "NUMERUS_CLAUSUS":
                String rank = getStringVariable(execution, "currentApplicationRank");
                String grade = getStringVariable(execution, "currentApplicationGrade");
                if (rank != null && grade != null) {
                    details.append("NC-Rangplatz: ").append(rank).append(" | Note: ").append(grade);
                }
                break;
            case "ENTRANCE_EXAM":
                String examScore = getStringVariable(execution, "examScore");
                String maxScore = getStringVariable(execution, "maxScore");
                String examDate = getStringVariable(execution, "examDate");
                if (examScore != null && maxScore != null) {
                    details.append("Prüfung bestanden: ").append(examScore).append("/").append(maxScore);
                    if (examDate != null) {
                        details.append(" (").append(examDate).append(")");
                    }
                }
                break;
        }

        return details.toString();
    }

    /**
     * Generiert Zulassungsbescheid-Referenznummer
     * Generates admission letter reference number
     */
    private String generateAdmissionReference(Long applicationId, String studyProgramCode) {
        String year = String.valueOf(LocalDateTime.now().getYear());
        return String.format("ZUL-%s-%s-%06d", studyProgramCode, year, applicationId);
    }

    /**
     * Berechnet Zahlungsfrist (4 Wochen ab heute)
     * Calculates payment deadline (4 weeks from today)
     */
    private String calculatePaymentDeadline() {
        LocalDateTime deadline = LocalDateTime.now().plusWeeks(4);
        return deadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    /**
     * Simuliert E-Mail-Versand
     * Simulates email sending
     */
    private void simulateEmailSending(String recipientEmail, String reference, String documentType) {
        System.out.println("=== EMAIL SIMULATION / E-MAIL SIMULATION ===");
        System.out.println(" TO / AN: " + recipientEmail);
        System.out.println(" SUBJECT / BETREFF: " + documentType + " - Universität Riedtal (Ref: " + reference + ")");
        System.out.println(" EMAIL SUCCESSFULLY SENT / E-MAIL ERFOLGREICH VERSENDET");
        System.out.println("===========================================");
    }

    /**
     * Erstellt den offiziellen Zulassungsbescheid
     * Creates the official admission letter
     */
    private String createOfficialAdmissionLetter(Application application, String studyProgramName, String studyProgramCode,
                                                 String admissionType, String admissionReason, String additionalInfo,
                                                 String admissionReference, String paymentDeadline) {

        StringBuilder letter = new StringBuilder();

        // Header / Kopfzeile
        letter.append("=== OFFIZIELLER ZULASSUNGSBESCHEID ===\n");

        // University header / Universitäts-Kopfzeile
        letter.append(" UNIVERSITÄT RIEDTAL\n");
        letter.append("Zulassungsstelle / Admissions Office\n");
        letter.append(" E-Mail: zulassung@riedtal.de\n");
        letter.append(" Telefon: +49 123 456-789\n");
        letter.append(" Referenz / Reference: ").append(admissionReference).append("\n");
        letter.append(" Datum / Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("\n\n");

        // Applicant information / Bewerber-Informationen
        letter.append("=== BEWERBER DATEN / APPLICANT DATA ===\n");
        letter.append(" Name: ").append(application.getFirstName()).append(" ").append(application.getLastName()).append("\n");
        letter.append(" E-Mail: ").append(application.getEmail()).append("\n");
        letter.append(" Bewerbungs-ID / Application ID: ").append(application.getId()).append("\n\n");

        // Study program information / Studiengang-Informationen
        letter.append("=== STUDIENGANG / STUDY PROGRAM ===\n");
        letter.append(" Studiengang / Program: ").append(studyProgramName).append("\n");
        letter.append(" Studiengang-Code / Program Code: ").append(studyProgramCode).append("\n");
        letter.append(" Zulassungsart / Admission Type: ");

        switch (admissionType) {
            case "OPEN":
                letter.append("Zulassungsfrei / Open Admission");
                break;
            case "NUMERUS_CLAUSUS":
                letter.append("Numerus Clausus");
                break;
            case "ENTRANCE_EXAM":
                letter.append("Aufnahmeprüfung / Entrance Exam");
                break;
            default:
                letter.append(admissionType);
        }
        letter.append("\n");

        if (!additionalInfo.isEmpty()) {
            letter.append(" Details: ").append(additionalInfo).append("\n");
        }
        letter.append("\n");

        // Admission decision / Zulassungsentscheidung
        letter.append("=== ZULASSUNGSENTSCHEIDUNG / ADMISSION DECISION ===\n");
        letter.append(" HERZLICHEN GLÜCKWUNSCH! / CONGRATULATIONS!\n\n");
        letter.append(" Sie sind für den Studiengang ").append(studyProgramName).append(" zugelassen!\n");

        return letter.toString();
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
