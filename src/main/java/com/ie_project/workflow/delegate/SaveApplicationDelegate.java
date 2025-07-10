package com.ie_project.workflow.delegate;

import com.ie_project.workflow.entity.Application;
import com.ie_project.workflow.repository.ApplicationRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Camunda Delegate für das Speichern von Bewerbungen
 * Camunda Delegate for saving applications
 *
 * @author IE Project Team
 */
@Component("saveApplicationDelegate")
public class SaveApplicationDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        // Get application ID from process variables / Bewerbungs-ID aus Prozessvariablen holen
        Long applicationId = (Long) execution.getVariable("applicationId");

        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID nicht in Prozessvariablen gefunden / Application ID not found in process variables");
        }

        // Find application / Bewerbung finden
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Bewerbung nicht gefunden / Application not found: " + applicationId));

        // Update status / Status aktualisieren
        application.setStatus(Application.ApplicationStatus.DOCUMENT_CHECK);
        applicationRepository.save(application);

        // Log success / Erfolg protokollieren
        System.out.println("=== BEWERBUNG GESPEICHERT ===");
        System.out.println("Application ID: " + applicationId);
        System.out.println("Name: " + application.getFirstName() + " " + application.getLastName());
        System.out.println("E-Mail: " + application.getEmail());
        System.out.println("Studiengang: " + application.getStudyProgram().getName());
        System.out.println("Status: " + application.getStatus());
        System.out.println("Prozess Instance ID: " + execution.getProcessInstanceId());
        System.out.println("============================");

        // Set additional process variables / Zusätzliche Prozessvariablen setzen
        execution.setVariable("applicationSaved", true);
        execution.setVariable("currentStatus", application.getStatus().toString());
    }
}
