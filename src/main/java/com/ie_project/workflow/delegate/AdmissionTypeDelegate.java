package com.ie_project.workflow.delegate;

import com.ie_project.workflow.entity.Application;
import com.ie_project.workflow.entity.StudyProgram;
import com.ie_project.workflow.repository.ApplicationRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Camunda Delegate für die Bestimmung des Zulassungstyps
 * Camunda Delegate for determining the admission type
 *
 * This delegate checks the admission type of the study program
 * and sets the appropriate process variable for gateway routing.
 *
 * Dieser Delegate prüft den Zulassungstyp des Studiengangs
 * und setzt die entsprechende Prozessvariable für Gateway-Routing.
 *
 * @author IE Project Team
 */
@Component("admissionTypeDelegate")
public class AdmissionTypeDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== ADMISSION TYPE DELEGATE EXECUTED ===");

        try {
            // Get application ID from process variables with type safety
            // Bewerbungs-ID aus Prozessvariablen mit Type Safety holen
            Long applicationId = getApplicationId(execution);

            System.out.println("Application ID: " + applicationId);

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Get study program and admission type / Studiengang und Zulassungstyp holen
            StudyProgram studyProgram = application.getStudyProgram();
            StudyProgram.AdmissionType admissionType = studyProgram.getAdmissionType();

            // Set admission type for gateway decision / Zulassungstyp für Gateway-Entscheidung setzen
            execution.setVariable("admissionType", admissionType.name());
            execution.setVariable("studyProgramId", studyProgram.getId());
            execution.setVariable("studyProgramName", studyProgram.getName());
            execution.setVariable("studyProgramCode", studyProgram.getCode());

            // Get additional program information if available
            // Zusätzliche Programminformationen falls verfügbar
            if (studyProgram.getMaxStudents() != null) {
                execution.setVariable("maxStudents", studyProgram.getMaxStudents());
            }

            // Log admission type determination / Zulassungstyp-Bestimmung protokollieren
            System.out.println("=== ADMISSION TYPE DETERMINED ===");
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Study Program: " + studyProgram.getName() + " (" + studyProgram.getCode() + ")");
            System.out.println("Admission Type: " + admissionType.name());

            // Log routing information / Routing-Informationen protokollieren
            switch (admissionType) {
                case OPEN:
                    System.out.println("Routing: Direct admission path (Zulassungsfrei)");
                    System.out.println("Next Step: Direct admission processing");
                    break;
                case NUMERUS_CLAUSUS:
                    System.out.println("Routing: Numerus Clausus path");
                    System.out.println("Next Step: Wait for all applications and calculate NC ranking");
                    if (studyProgram.getMaxStudents() != null) {
                        System.out.println("Available Seats: " + studyProgram.getMaxStudents());
                    }
                    break;
                case ENTRANCE_EXAM:
                    System.out.println("Routing: Entrance exam path (Aufnahmeprüfung)");
                    System.out.println("Next Step: Schedule entrance examination");
                    break;
                default:
                    System.out.println("Warning: Unknown admission type: " + admissionType);
            }

            System.out.println("==============================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Admission Type Variable Set: " + admissionType.name());
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN ADMISSION TYPE DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================");
            throw e;
        }
    }

    /**
     * Helper method for type-safe applicationId retrieval
     * Hilfsmethode für typsichere applicationId-Abfrage
     */
    private Long getApplicationId(DelegateExecution execution) {
        Object applicationIdObj = execution.getVariable("applicationId");

        System.out.println("DEBUG - applicationId raw value: " + applicationIdObj);
        System.out.println("DEBUG - applicationId type: " + (applicationIdObj != null ? applicationIdObj.getClass() : "null"));

        if (applicationIdObj == null) {
            throw new IllegalArgumentException("Application ID not found in process variables / Bewerbungs-ID nicht in Prozessvariablen gefunden");
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
