package com.ie_project.workflow.delegate;

import com.ie_project.workflow.entity.Application;
import com.ie_project.workflow.repository.ApplicationRepository;
import com.ie_project.workflow.service.ApplicationDeadlineService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Camunda Delegate für die Überprüfung der Bewerbungsfristen
 * Camunda Delegate for checking application deadlines
 *
 * This delegate is used in an exclusive gateway to determine if an application
 * was submitted within the allowed timeframe.
 *
 * Dieser Delegate wird in einem exklusiven Gateway verwendet, um zu bestimmen,
 * ob eine Bewerbung innerhalb der erlaubten Frist eingereicht wurde.
 *
 * @author IE Project Team
 */
@Component("deadlineCheckDelegate")
public class DeadlineCheckDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationDeadlineService deadlineService;

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

        // Check if application was submitted on time
        // Prüfen, ob Bewerbung rechtzeitig eingereicht wurde
        boolean isOnTime = deadlineService.isApplicationOnTime(application.getCreatedAt());

        // Get detailed deadline information / Detaillierte Deadline-Informationen holen
        ApplicationDeadlineService.DeadlineCheckResult deadlineResult =
                deadlineService.getDeadlineCheckResult(application.getCreatedAt());

        // Create deadline message / Deadline-Nachricht erstellen
        String applicantName = application.getFirstName() + " " + application.getLastName();
        String deadlineMessage = deadlineService.createDeadlineMessage(deadlineResult, applicantName);

        // Set process variables for gateway decision / Prozessvariablen für Gateway-Entscheidung setzen
        execution.setVariable("isApplicationOnTime", isOnTime);
        execution.setVariable("deadlineCheckCompleted", true);
        execution.setVariable("deadlineMessage", deadlineMessage);
        execution.setVariable("submissionDeadline", deadlineResult.getSubmissionDeadline().toString());
        execution.setVariable("targetSemester", deadlineResult.getSemesterType());
        execution.setVariable("daysUntilDeadline", deadlineResult.getDaysUntilDeadline());

        // If application is late, update status in database
        // Wenn Bewerbung verspätet, Status in Datenbank aktualisieren
        if (!isOnTime) {
            application.setStatus(Application.ApplicationStatus.REJECTED);
            applicationRepository.save(application);

            execution.setVariable("rejectionReason", "DEADLINE_EXCEEDED");
            execution.setVariable("currentStatus", "REJECTED");

            System.out.println("=== APPLICATION REJECTED - DEADLINE EXCEEDED ===");
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + applicantName);
            System.out.println("Email: " + application.getEmail());
            System.out.println("Submission Date: " + application.getCreatedAt());
            System.out.println("Deadline: " + deadlineResult.getSubmissionDeadline());
            System.out.println("Days Late: " + Math.abs(deadlineResult.getDaysUntilDeadline()));
            System.out.println("Status updated to: REJECTED");
            System.out.println("=================================================");

        } else {
            execution.setVariable("currentStatus", application.getStatus().toString());

            System.out.println("=== APPLICATION DEADLINE CHECK PASSED ===");
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + applicantName);
            System.out.println("Email: " + application.getEmail());
            System.out.println("Submission Date: " + application.getCreatedAt());
            System.out.println("Deadline: " + deadlineResult.getSubmissionDeadline());
            System.out.println("Days Before Deadline: " + deadlineResult.getDaysUntilDeadline());
            System.out.println("Target Semester: " + deadlineResult.getSemesterType());
            System.out.println("Status: ON TIME - Processing continues");
            System.out.println("==========================================");
        }

        // Log the deadline message / Deadline-Nachricht protokollieren
        System.out.println("=== DEADLINE CHECK MESSAGE ===");
        System.out.println(deadlineMessage);
        System.out.println("===============================");

        // Log general execution info / Allgemeine Ausführungsinfo protokollieren
        System.out.println("=== DEADLINE CHECK DELEGATE EXECUTED ===");
        System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
        System.out.println("Activity ID: " + execution.getCurrentActivityId());
        System.out.println("Application ID: " + applicationId);
        System.out.println("Is On Time: " + isOnTime);
        System.out.println("=========================================");
    }
}
