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
 * Camunda Delegate für die Behandlung abgelehnter Bewerbungen
 * Camunda Delegate for handling rejected applications
 *
 * This delegate handles applications that were rejected due to deadline violations
 * or other reasons. It sends rejection notifications and finalizes the rejection process.
 *
 * Dieser Delegate behandelt Bewerbungen, die aufgrund von Fristverletzungen oder
 * anderen Gründen abgelehnt wurden. Er sendet Ablehnungsbenachrichtigungen und
 * schließt den Ablehnungsprozess ab.
 *
 * @author IE Project Team
 */
@Component("rejectionHandlerDelegate")
public class RejectionHandlerDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        // Get application ID from process variables
        // Bewerbungs-ID aus Prozessvariablen holen
        Long applicationId = (Long) execution.getVariable("applicationId");

        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID not found in process variables / Bewerbungs-ID nicht in Prozessvariablen gefunden");
        }

        // Find application in database / Bewerbung in Datenbank finden
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

        // Get rejection details from process variables
        // Ablehnungsdetails aus Prozessvariablen holen
        String rejectionReason = (String) execution.getVariable("rejectionReason");
        String deadlineMessage = (String) execution.getVariable("deadlineMessage");
        String submissionDeadline = (String) execution.getVariable("submissionDeadline");
        String targetSemester = (String) execution.getVariable("targetSemester");
        Long daysLate = (Long) execution.getVariable("daysUntilDeadline");

        // Ensure application status is REJECTED / Sicherstellen, dass Bewerbungsstatus REJECTED ist
        if (application.getStatus() != Application.ApplicationStatus.REJECTED) {
            application.setStatus(Application.ApplicationStatus.REJECTED);
            applicationRepository.save(application);
        }

        // Create detailed rejection message / Detaillierte Ablehnungsnachricht erstellen
        String rejectionNotification = createRejectionNotification(
                application, rejectionReason, deadlineMessage, submissionDeadline, targetSemester, daysLate
        );

        // Set final process variables / Finale Prozessvariablen setzen
        execution.setVariable("rejectionNotificationSent", true);
        execution.setVariable("rejectionNotification", rejectionNotification);
        execution.setVariable("processCompleted", true);
        execution.setVariable("finalStatus", "REJECTED");
        execution.setVariable("processEndReason", "APPLICATION_REJECTED");
        execution.setVariable("rejectionProcessedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

        // Log rejection notification / Ablehnungsbenachrichtigung protokollieren
        System.out.println("=== ABLEHNUNGSBENACHRICHTIGUNG / REJECTION NOTIFICATION ===");
        System.out.println(rejectionNotification);
        System.out.println("============================================================");

        // Log execution details / Ausführungsdetails protokollieren
        System.out.println("=== REJECTION HANDLER DELEGATE EXECUTED ===");
        System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
        System.out.println("Activity ID: " + execution.getCurrentActivityId());
        System.out.println("Application ID: " + applicationId);
        System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
        System.out.println("Email: " + application.getEmail());
        System.out.println("Rejection Reason: " + rejectionReason);
        System.out.println("Final Status: REJECTED");
        System.out.println("Notification Sent: YES");
        System.out.println("Process Completed: YES");
        System.out.println("============================================");
    }

    /**
     * Erstellt eine detaillierte Ablehnungsbenachrichtigung
     * Creates a detailed rejection notification
     */
    private String createRejectionNotification(Application application, String rejectionReason,
                                               String deadlineMessage, String submissionDeadline,
                                               String targetSemester, Long daysLate) {

        StringBuilder notification = new StringBuilder();

        // Header / Kopfzeile
        notification.append("=== BEWERBUNG ABGELEHNT / APPLICATION REJECTED ===\n\n");

        // Personal information / Persönliche Daten
        notification.append("Bewerber / Applicant: ").append(application.getFirstName())
                .append(" ").append(application.getLastName()).append("\n");
        notification.append("E-Mail: ").append(application.getEmail()).append("\n");
        notification.append("Bewerbungs-ID / Application ID: ").append(application.getId()).append("\n");
        notification.append("Studiengang / Study Program: ").append(application.getStudyProgram().getName()).append("\n\n");

        // Timing information / Zeitinformationen
        notification.append("Eingereicht am / Submitted on: ")
                .append(application.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");

        if (submissionDeadline != null) {
            notification.append("Deadline war / Deadline was: ").append(submissionDeadline).append("\n");
        }

        if (targetSemester != null) {
            notification.append("Ziel-Semester / Target Semester: ").append(targetSemester).append("\n");
        }

        if (daysLate != null && daysLate < 0) {
            notification.append("Verspätung / Days Late: ").append(Math.abs(daysLate)).append(" Tage / Days\n");
        }

        notification.append("\n");

        // Rejection reason / Ablehnungsgrund
        notification.append("=== ABLEHNUNGSGRUND / REJECTION REASON ===\n");

        switch (rejectionReason != null ? rejectionReason : "UNKNOWN") {
            case "DEADLINE_EXCEEDED":
                notification.append("Ihre Bewerbung wurde abgelehnt, da sie nach der Einreichungsfrist eingereicht wurde.\n");
                notification.append("Your application was rejected because it was submitted after the deadline.\n\n");
                notification.append("Bewerbungen müssen mindestens 2 Monate vor Semesterbeginn eingereicht werden.\n");
                notification.append("Applications must be submitted at least 2 months before the semester starts.\n");
                break;
            default:
                notification.append("Ihre Bewerbung wurde aus technischen Gründen abgelehnt.\n");
                notification.append("Your application was rejected for technical reasons.\n");
        }

        notification.append("\n");

        // Next steps / Nächste Schritte
        notification.append("=== NÄCHSTE SCHRITTE / NEXT STEPS ===\n");
        notification.append("Sie können sich für das nächste Semester bewerben, wenn Sie die Fristen einhalten.\n");
        notification.append("You can apply for the next semester if you meet the deadlines.\n\n");
        notification.append("Kontakt / Contact: bewerbung@riedtal.de\n");

        // Footer / Fußzeile
        notification.append("\n=== UNIVERSITÄT RIEDTAL - ADMISSIONS OFFICE ===");

        return notification.toString();
    }
}
