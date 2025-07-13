package com.ie_project.workflow.delegate;

import com.ie_project.workflow.entity.Application;
import com.ie_project.workflow.repository.ApplicationRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Camunda Delegate für das Speichern von Bewerbungen (Type-Safe)
 * Camunda Delegate for saving applications (Type-Safe)
 *
 * @author IE Project Team
 */
@Component("saveApplicationDelegate")
public class SaveApplicationDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== SAVE APPLICATION DELEGATE EXECUTED ===");

        try {
            // Get application ID from process variables with type safety
            // Bewerbungs-ID aus Prozessvariablen mit Type Safety holen
            Long applicationId = getApplicationId(execution);

            System.out.println("Application ID: " + applicationId);

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

            System.out.println("=== SAVE APPLICATION COMPLETED SUCCESSFULLY ===");

        } catch (Exception e) {
            System.err.println("=== ERROR IN SAVE APPLICATION DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==========================================");
            throw e;
        }
    }

    /**
     * Helper method for type-safe applicationId retrieval
     */
    private Long getApplicationId(DelegateExecution execution) {
        Object applicationIdObj = execution.getVariable("applicationId");

        System.out.println("DEBUG - applicationId raw value: " + applicationIdObj);
        System.out.println("DEBUG - applicationId type: " + (applicationIdObj != null ? applicationIdObj.getClass() : "null"));

        if (applicationIdObj == null) {
            throw new IllegalArgumentException("Application ID nicht in Prozessvariablen gefunden / Application ID not found in process variables");
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
}