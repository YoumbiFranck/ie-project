package com.ie_project.workflow.delegate;

import com.ie_project.workflow.entity.Application;
import com.ie_project.workflow.entity.StudyProgram;
import com.ie_project.workflow.repository.ApplicationRepository;
import com.ie_project.workflow.service.StudentNumberGeneratorService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Camunda Delegate für die Generierung der Matrikelnummer
 * Camunda Delegate for generating student numbers (Matrikelnummer)
 *
 * This delegate generates a unique student number for admitted applications.
 * The number format follows the pattern: PROGRAM_CODE + YEAR + SEQUENTIAL_NUMBER
 * Example: INF20250001 for first Computer Science student in 2025
 *
 * Dieser Delegate generiert eine eindeutige Matrikelnummer für zugelassene Bewerbungen.
 * Das Nummernformat folgt dem Muster: STUDIENGANG_CODE + JAHR + FORTLAUFENDE_NUMMER
 * Beispiel: INF20250001 für den ersten Informatik-Studenten in 2025
 *
 * @author IE Project Team
 */
@Component("generateStudentNumberDelegate")
public class GenerateStudentNumberDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private StudentNumberGeneratorService studentNumberGeneratorService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== GENERATE STUDENT NUMBER DELEGATE EXECUTED ===");

        try {
            // Get application ID from process variables
            // Bewerbungs-ID aus Prozessvariablen holen
            Long applicationId = getApplicationId(execution);

            System.out.println("Application ID: " + applicationId);

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Verify application is accepted / Verifizieren dass Bewerbung angenommen wurde
            if (!Application.ApplicationStatus.ACCEPTED.equals(application.getStatus())) {
                throw new IllegalStateException("Student number generation only allowed for accepted applications / Matrikelnummer-Generierung nur für angenommene Bewerbungen erlaubt. Current status: " + application.getStatus());
            }

            // Verify payment has been made / Verifizieren dass Zahlung eingegangen ist
            if (!application.isTuitionFeePaid()) {
                throw new IllegalStateException("Student number generation requires completed payment / Matrikelnummer-Generierung erfordert abgeschlossene Zahlung");
            }

            // Get study program information
            // Studiengang-Informationen holen
            StudyProgram studyProgram = application.getStudyProgram();
            if (studyProgram == null) {
                throw new IllegalStateException("Study program not found for application / Studiengang nicht gefunden für Bewerbung: " + applicationId);
            }

            String studyProgramName = studyProgram.getName();
            String studyProgramCode = studyProgram.getCode();

            // Generate unique student number / Eindeutige Matrikelnummer generieren
            String studentNumber = studentNumberGeneratorService.generateStudentNumber(studyProgram);

            // Set process variables for next steps / Prozessvariablen für nächste Schritte setzen
            execution.setVariable("studentNumberGenerated", true);
            execution.setVariable("generatedStudentNumber", studentNumber);
            execution.setVariable("studentNumberGeneratedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            execution.setVariable("nextProcessStep", "CREATE_STUDENT_RECORD");

            // Create detailed student number notification / Detaillierte Matrikelnummer-Benachrichtigung erstellen
            String studentNumberNotification = createStudentNumberNotification(
                    application, studentNumber, studyProgramName, studyProgramCode
            );

            execution.setVariable("studentNumberNotification", studentNumberNotification);

            // Log student number generation / Matrikelnummer-Generierung protokollieren
            System.out.println("=== MATRIKELNUMMER GENERIERT / STUDENT NUMBER GENERATED ===");
            System.out.println(studentNumberNotification);
            System.out.println("==========================================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== STUDENT NUMBER GENERATION COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Study Program: " + studyProgramName + " (" + studyProgramCode + ")");
            System.out.println("Generated Student Number: " + studentNumber);
            System.out.println("Payment Status: " + (application.isTuitionFeePaid() ? "PAID" : "PENDING"));
            System.out.println("Application Status: " + application.getStatus());
            System.out.println("Next Step: CREATE_STUDENT_RECORD");
            System.out.println("Generation Completed: YES");
            System.out.println("==========================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN GENERATE STUDENT NUMBER DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("================================================");
            throw e;
        }
    }

    /**
     * Erstellt eine detaillierte Benachrichtigung für die Matrikelnummer-Generierung
     * Creates a detailed notification for student number generation
     */
    private String createStudentNumberNotification(Application application, String studentNumber,
                                                   String studyProgramName, String studyProgramCode) {

        StringBuilder notification = new StringBuilder();

        // Header / Kopfzeile
        notification.append("=== MATRIKELNUMMER GENERIERT / STUDENT NUMBER GENERATED ===\n\n");

        // Personal information / Persönliche Daten
        notification.append("Zukünftiger Student / Future Student: ").append(application.getFirstName())
                .append(" ").append(application.getLastName()).append("\n");
        notification.append("E-Mail: ").append(application.getEmail()).append("\n");
        notification.append("Bewerbungs-ID / Application ID: ").append(application.getId()).append("\n");
        notification.append("Studiengang / Study Program: ").append(studyProgramName)
                .append(" (").append(studyProgramCode).append(")\n\n");

        // Student number information / Matrikelnummer-Informationen
        notification.append("=== MATRIKELNUMMER / STUDENT NUMBER ===\n");
        notification.append("Ihre neue Matrikelnummer / Your new student number: ").append(studentNumber).append("\n\n");

        // Explain number format / Nummernformat erklären
        notification.append("Format-Erklärung / Format explanation:\n");
        notification.append("• ").append(studyProgramCode).append(" = Studiengang-Code / Study program code\n");
        notification.append("• ").append(studentNumberGeneratorService.extractYearFromStudentNumber(studentNumber))
                .append(" = Einschreibungsjahr / Enrollment year\n");
        notification.append("• ").append(studentNumber.substring(studentNumber.length() - 4))
                .append(" = Fortlaufende Nummer / Sequential number\n\n");

        // Timing information / Zeitinformationen
        notification.append("Bewerbung eingereicht / Application submitted: ")
                .append(application.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n");
        notification.append("Matrikelnummer generiert / Student number generated: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n\n");





        return notification.toString();
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
