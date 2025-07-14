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
 * Camunda Delegate für das Versenden von Prüfungseinladungen
 * Camunda Delegate for sending examination invitations
 *
 * This delegate handles the sending of official examination invitations
 * to applicants who need to take an entrance exam.
 *
 * Dieser Delegate behandelt das Versenden von offiziellen Prüfungseinladungen
 * an Bewerber, die eine Aufnahmeprüfung absolvieren müssen.
 *
 * @author IE Project Team
 */
@Component("examInvitationDelegate")
public class ExamInvitationDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== EXAM INVITATION DELEGATE EXECUTED ===");

        try {
            // Get application and exam scheduling information
            // Bewerbungs- und Prüfungsterminierung-Informationen holen
            Long applicationId = getApplicationId(execution);
            String studyProgramName = getStringVariable(execution, "studyProgramName");
            String studyProgramCode = getStringVariable(execution, "studyProgramCode");
            String examDate = getStringVariable(execution, "examDate");
            String examTime = getStringVariable(execution, "examTime");
            String examDateTime = getStringVariable(execution, "examDateTime");
            String examLocation = getStringVariable(execution, "examLocation");
            String examRoom = getStringVariable(execution, "examRoom");
            String examCommittee = getStringVariable(execution, "examCommittee");
            String examDuration = getStringVariable(execution, "examDuration");
            String maxExamScore = getStringVariable(execution, "maxExamScore");
            String passingScore = getStringVariable(execution, "passingScore");

            System.out.println("Application ID: " + applicationId);
            System.out.println("Exam Date: " + examDateTime);
            System.out.println("Exam Location: " + examLocation);

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Generate invitation reference number / Einladungs-Referenznummer generieren
            String invitationReference = generateInvitationReference(applicationId, examDate);

            // Generate QR code for exam check-in (simulation) / QR-Code für Prüfungs-Check-in generieren (Simulation)
            String qrCodeData = generateQRCodeData(applicationId, examDate, examRoom);

            // Set process variables for invitation / Prozessvariablen für Einladung setzen
            execution.setVariable("examInvitationSent", true);
            execution.setVariable("invitationReference", invitationReference);
            execution.setVariable("qrCodeData", qrCodeData);
            execution.setVariable("invitationSentAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            execution.setVariable("examConfirmationRequired", true);
            execution.setVariable("confirmationDeadline", calculateConfirmationDeadline(examDate));
            execution.setVariable("nextProcessStep", "EXAM_EXECUTION");

            // Create official exam invitation / Offizielle Prüfungseinladung erstellen
            String examInvitation = createOfficialExamInvitation(
                    application, studyProgramName, studyProgramCode, examDate, examTime,
                    examLocation, examRoom, examCommittee, examDuration, maxExamScore,
                    passingScore, invitationReference, qrCodeData
            );

            execution.setVariable("officialExamInvitation", examInvitation);

            // Simulate email sending / E-Mail-Versand simulieren
            simulateEmailSending(application.getEmail(), invitationReference, examInvitation);

            // Log exam invitation / Prüfungseinladung protokollieren
            System.out.println("=== PRÜFUNGSEINLADUNG VERSENDET / EXAM INVITATION SENT ===");
            System.out.println(examInvitation);
            System.out.println("=========================================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== EXAM INVITATION PROCESSING COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Study Program: " + studyProgramName + " (" + studyProgramCode + ")");
            System.out.println("Exam Date: " + examDateTime);
            System.out.println("Invitation Reference: " + invitationReference);
            System.out.println("Email Sent To: " + application.getEmail());
            System.out.println("Next Step: EXAM_EXECUTION");
            System.out.println("Processing Completed: YES");
            System.out.println("============================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN EXAM INVITATION DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=========================================");
            throw e;
        }
    }

    /**
     * Generiert eine Einladungs-Referenznummer
     * Generates an invitation reference number
     */
    private String generateInvitationReference(Long applicationId, String examDate) {
        // Format: EXAM-YYYYMMDD-APPID
        String dateStr = examDate.replaceAll("\\.", "");
        return String.format("EXAM-%s-%06d", dateStr, applicationId);
    }

    /**
     * Generiert QR-Code Daten für Check-in
     * Generates QR code data for check-in
     */
    private String generateQRCodeData(Long applicationId, String examDate, String examRoom) {
        return String.format("RIEDTAL-EXAM|APP:%d|DATE:%s|ROOM:%s", applicationId, examDate, examRoom);
    }

    /**
     * Berechnet die Bestätigungsfrist (3 Tage vor Prüfung)
     * Calculates confirmation deadline (3 days before exam)
     */
    private String calculateConfirmationDeadline(String examDate) {
        try {
            LocalDateTime examDateTime = LocalDateTime.parse(examDate + " 10:00", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            LocalDateTime deadline = examDateTime.minusDays(3);
            return deadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (Exception e) {
            // Fallback
            return "TBD";
        }
    }

    /**
     * Simuliert E-Mail-Versand
     * Simulates email sending
     */
    private void simulateEmailSending(String recipientEmail, String invitationReference, String invitation) {
        System.out.println("=== EMAIL SIMULATION / E-MAIL SIMULATION ===");
        System.out.println(" TO / AN: " + recipientEmail);
        System.out.println(" SUBJECT / BETREFF: Einladung zur Aufnahmeprüfung - Universität Riedtal (Ref: " + invitationReference + ")");
        System.out.println(" CONTENT LENGTH / INHALTSLÄNGE: " + invitation.length() + " Zeichen");
        System.out.println(" EMAIL SUCCESSFULLY SENT / E-MAIL ERFOLGREICH VERSENDET");
        System.out.println("===========================================");
    }

    /**
     * Erstellt die offizielle Prüfungseinladung
     * Creates the official exam invitation
     */
    private String createOfficialExamInvitation(Application application, String studyProgramName, String studyProgramCode,
                                                String examDate, String examTime, String examLocation, String examRoom,
                                                String examCommittee, String examDuration, String maxExamScore,
                                                String passingScore, String invitationReference, String qrCodeData) {

        StringBuilder invitation = new StringBuilder();

        // Header / Kopfzeile
        invitation.append("=== OFFIZIELLE EINLADUNG ZUR AUFNAHMEPRÜFUNG ===\n");


        // University and reference / Universität und Referenz
        invitation.append(" UNIVERSITÄT RIEDTAL\n");
        invitation.append(" E-Mail: pruefungsamt@riedtal.de\n");
        invitation.append(" Telefon: +49 123 456-789\n");
        invitation.append(" Referenz / Reference: ").append(invitationReference).append("\n\n");

        // Personal information / Persönliche Daten
        invitation.append("=== BEWERBER DATEN / APPLICANT DATA ===\n");
        invitation.append(" Name: ").append(application.getFirstName()).append(" ").append(application.getLastName()).append("\n");
        invitation.append(" E-Mail: ").append(application.getEmail()).append("\n");
        invitation.append(" Bewerbungs-ID / Application ID: ").append(application.getId()).append("\n");
        invitation.append(" Studiengang / Study Program: ").append(studyProgramName).append(" (").append(studyProgramCode).append(")\n\n");

        // Exam details / Prüfungsdetails
        invitation.append("=== PRÜFUNGSDETAILS / EXAMINATION DETAILS ===\n");
        invitation.append(" Datum / Date: ").append(examDate).append("\n");
        invitation.append(" Uhrzeit / Time: ").append(examTime).append(" Uhr\n");
        invitation.append(" Ort / Location: ").append(examLocation).append("\n");
        invitation.append(" Raum / Room: ").append(examRoom).append("\n");
        invitation.append(" Dauer / Duration: ").append(examDuration).append(" Minuten / minutes\n");
        invitation.append(" Maximale Punktzahl / Maximum Score: ").append(maxExamScore).append(" Punkte / points\n");
        invitation.append(" Bestehensgrenze / Passing Score: ").append(passingScore).append(" Punkte / points (").append(passingScore).append("%)\n\n");

        // Examination committee / Prüfungskomitee
        invitation.append("=== PRÜFUNGSKOMITEE / EXAMINATION COMMITTEE ===\n");
        invitation.append(" Prüfer / Examiners:\n");
        String[] examiners = examCommittee.split(", ");
        for (String examiner : examiners) {
            invitation.append("   • ").append(examiner.trim()).append("\n");
        }
        invitation.append("\n");



        // Exam content information / Prüfungsinhalt-Informationen
        invitation.append("=== PRÜFUNGSINHALT / EXAM CONTENT ===\n");
        switch (studyProgramCode.toUpperCase()) {
            case "MED":
                invitation.append("• Naturwissenschaftliche Grundlagen / Scientific fundamentals\n");
                invitation.append("• Logisches Denken / Logical reasoning\n");
                invitation.append("• Medizinische Grundkenntnisse / Basic medical knowledge\n");
                invitation.append("• Ethische Fragestellungen / Ethical questions\n");
                break;
            case "INF":
                invitation.append("• Mathematische Grundlagen / Mathematical fundamentals\n");
                invitation.append("• Logik und Algorithmik / Logic and algorithms\n");
                invitation.append("• Grundlagen der Programmierung / Programming basics\n");
                invitation.append("• Problemlösungsfähigkeiten / Problem-solving skills\n");
                break;
            default:
                invitation.append("• Fachspezifische Grundlagen / Subject-specific fundamentals\n");
                invitation.append("• Allgemeinwissen / General knowledge\n");
                invitation.append("• Analytisches Denken / Analytical thinking\n");
                invitation.append("• Kommunikationsfähigkeiten / Communication skills\n");
        }
        invitation.append("\n");







        return invitation.toString();
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
