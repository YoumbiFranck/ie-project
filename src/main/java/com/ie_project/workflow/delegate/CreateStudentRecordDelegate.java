package com.ie_project.workflow.delegate;

import com.ie_project.workflow.entity.Application;
import com.ie_project.workflow.entity.Student;
import com.ie_project.workflow.entity.StudyProgram;
import com.ie_project.workflow.repository.ApplicationRepository;
import com.ie_project.workflow.repository.StudentRepository;
import com.ie_project.workflow.service.StudentEnrollmentService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Camunda Delegate für die Erstellung des Studentendatensatzes
 * Camunda Delegate for creating the student record
 *
 * This delegate creates the final student record in the database after
 * a student number has been generated. It marks the completion of the
 * enrollment process and prepares for the welcome package.
 *
 * Dieser Delegate erstellt den finalen Studentendatensatz in der Datenbank
 * nachdem eine Matrikelnummer generiert wurde. Er markiert den Abschluss des
 * Immatrikulationsprozesses und bereitet das Willkommenspaket vor.
 *
 * @author IE Project Team
 */
@Component("createStudentRecordDelegate")
public class CreateStudentRecordDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentEnrollmentService studentEnrollmentService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== CREATE STUDENT RECORD DELEGATE EXECUTED ===");

        try {
            // Get required data from process variables
            // Erforderliche Daten aus Prozessvariablen holen
            Long applicationId = getApplicationId(execution);
            String generatedStudentNumber = getStringVariable(execution, "generatedStudentNumber");

            System.out.println("Application ID: " + applicationId);
            System.out.println("Generated Student Number: " + generatedStudentNumber);

            // Validate that student number was generated
            // Validieren dass Matrikelnummer generiert wurde
            if (generatedStudentNumber == null || generatedStudentNumber.trim().isEmpty()) {
                throw new IllegalStateException("Student number not found in process variables / Matrikelnummer nicht in Prozessvariablen gefunden");
            }

            // Verify student number generation was completed
            // Verifizieren dass Matrikelnummer-Generierung abgeschlossen wurde
            Boolean studentNumberGenerated = getBooleanVariable(execution, "studentNumberGenerated");
            if (!Boolean.TRUE.equals(studentNumberGenerated)) {
                throw new IllegalStateException("Student number generation not completed / Matrikelnummer-Generierung nicht abgeschlossen");
            }

            // Find application in database / Bewerbung in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Verify application status / Bewerbungsstatus verifizieren
            if (!Application.ApplicationStatus.ACCEPTED.equals(application.getStatus())) {
                throw new IllegalStateException("Student record creation only allowed for accepted applications / Studentendatensatz-Erstellung nur für angenommene Bewerbungen erlaubt. Current status: " + application.getStatus());
            }

            // Verify payment has been made / Verifizieren dass Zahlung eingegangen ist
            if (!application.isTuitionFeePaid()) {
                throw new IllegalStateException("Student record creation requires completed payment / Studentendatensatz-Erstellung erfordert abgeschlossene Zahlung");
            }

            // Check if student record already exists for this application
            // Prüfen ob bereits ein Studentendatensatz für diese Bewerbung existiert
            if (studentRepository.existsByApplicationId(applicationId)) {
                System.out.println("WARNING: Student record already exists for application " + applicationId);
                Student existingStudent = studentRepository.findByApplicationId(applicationId)
                        .orElseThrow(() -> new IllegalStateException("Student exists but could not be found"));

                // Use existing student data / Bestehende Studentendaten verwenden
                handleExistingStudent(execution, existingStudent, application);
                return;
            }

            // Check if student number is already in use
            // Prüfen ob Matrikelnummer bereits verwendet wird
            if (studentRepository.existsByStudentNumber(generatedStudentNumber)) {
                throw new IllegalStateException("Student number already exists / Matrikelnummer bereits vorhanden: " + generatedStudentNumber);
            }

            // Get study program information
            // Studiengang-Informationen holen
            StudyProgram studyProgram = application.getStudyProgram();
            if (studyProgram == null) {
                throw new IllegalStateException("Study program not found for application / Studiengang nicht gefunden für Bewerbung: " + applicationId);
            }

            // Create student record using enrollment service
            // Studentendatensatz mit Enrollment-Service erstellen
            Student student = studentEnrollmentService.createStudentFromApplication(
                    application, generatedStudentNumber
            );

            // Update application status to ENROLLED
            // Bewerbungsstatus auf ENROLLED aktualisieren
            application.setStatus(Application.ApplicationStatus.ENROLLED);
            applicationRepository.save(application);

            // Set process variables for next steps / Prozessvariablen für nächste Schritte setzen
            execution.setVariable("studentRecordCreated", true);
            execution.setVariable("studentId", student.getId());
            execution.setVariable("finalStudentNumber", student.getStudentNumber());
            execution.setVariable("enrollmentDate", student.getEnrollmentDate().toString());
            execution.setVariable("currentSemester", student.getCurrentSemester());
            execution.setVariable("studentRecordCreatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            execution.setVariable("nextProcessStep", "WELCOME_PACKAGE");

            // Create detailed student record notification / Detaillierte Studentendatensatz-Benachrichtigung erstellen
            String studentRecordNotification = createStudentRecordNotification(
                    student, application, studyProgram
            );

            execution.setVariable("studentRecordNotification", studentRecordNotification);

            // Log student record creation / Studentendatensatz-Erstellung protokollieren
            System.out.println("=== STUDENTENDATENSATZ ERSTELLT / STUDENT RECORD CREATED ===");
            System.out.println(studentRecordNotification);
            System.out.println("===========================================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== STUDENT RECORD CREATION COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Student ID: " + student.getId());
            System.out.println("Student Name: " + student.getFullName());
            System.out.println("Student Number: " + student.getStudentNumber());
            System.out.println("Email: " + student.getEmail());
            System.out.println("Study Program: " + studyProgram.getName() + " (" + studyProgram.getCode() + ")");
            System.out.println("Enrollment Date: " + student.getEnrollmentDate());
            System.out.println("Current Semester: " + student.getCurrentSemester());
            System.out.println("Application Status Updated: " + application.getStatus());
            System.out.println("Next Step: WELCOME_PACKAGE");
            System.out.println("Record Creation Completed: YES");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN CREATE STUDENT RECORD DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("===============================================");
            throw e;
        }
    }

    /**
     * Behandelt den Fall, dass bereits ein Studentendatensatz existiert
     * Handles the case where a student record already exists
     */
    private void handleExistingStudent(DelegateExecution execution, Student existingStudent, Application application) {

        System.out.println("=== HANDLING EXISTING STUDENT RECORD ===");
        System.out.println("Student ID: " + existingStudent.getId());
        System.out.println("Student Number: " + existingStudent.getStudentNumber());
        System.out.println("Student Name: " + existingStudent.getFullName());

        // Update process variables with existing student data
        // Prozessvariablen mit bestehenden Studentendaten aktualisieren
        execution.setVariable("studentRecordCreated", true);
        execution.setVariable("studentId", existingStudent.getId());
        execution.setVariable("finalStudentNumber", existingStudent.getStudentNumber());
        execution.setVariable("enrollmentDate", existingStudent.getEnrollmentDate().toString());
        execution.setVariable("currentSemester", existingStudent.getCurrentSemester());
        execution.setVariable("studentRecordCreatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        execution.setVariable("nextProcessStep", "WELCOME_PACKAGE");
        execution.setVariable("studentRecordAlreadyExisted", true);

        // Ensure application status is ENROLLED
        // Sicherstellen dass Bewerbungsstatus ENROLLED ist
        if (!Application.ApplicationStatus.ENROLLED.equals(application.getStatus())) {
            application.setStatus(Application.ApplicationStatus.ENROLLED);
            applicationRepository.save(application);
        }

        System.out.println("Existing student record handled successfully");
        System.out.println("=======================================");
    }

    /**
     * Erstellt eine detaillierte Benachrichtigung für die Studentendatensatz-Erstellung
     * Creates a detailed notification for student record creation
     */
    private String createStudentRecordNotification(Student student, Application application, StudyProgram studyProgram) {

        StringBuilder notification = new StringBuilder();

        // Header / Kopfzeile
        notification.append("=== STUDENTENDATENSATZ ERSTELLT / STUDENT RECORD CREATED ===\n\n");

        // Student information / Studenteninformationen
        notification.append(" HERZLICHEN GLÜCKWUNSCH! Sie sind nun offiziell immatrikuliert!\n");

        notification.append("Student / Student: ").append(student.getFullName()).append("\n");
        notification.append("Matrikelnummer / Student Number: ").append(student.getStudentNumber()).append("\n");
        notification.append("E-Mail: ").append(student.getEmail()).append("\n");
        notification.append("Studiengang / Study Program: ").append(studyProgram.getName())
                .append(" (").append(studyProgram.getCode()).append(")\n");
        notification.append("Immatrikulationsdatum / Enrollment Date: ")
                .append(student.getEnrollmentDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("\n");
        notification.append("Aktuelles Semester / Current Semester: ").append(student.getCurrentSemester()).append("\n\n");

        // Academic year information / Studienjahr-Informationen






        // Important information / Wichtige Informationen
        notification.append("=== WICHTIGE INFORMATIONEN / IMPORTANT INFORMATION ===\n");
        notification.append(" Ihre Matrikelnummer: ").append(student.getStudentNumber()).append("\n");
        notification.append(" Your student number: ").append(student.getStudentNumber()).append("\n\n");



        // Database information / Datenbank-Informationen
        notification.append("=== SYSTEM-INFORMATIONEN / SYSTEM INFORMATION ===\n");
        notification.append("Student ID (intern): ").append(student.getId()).append("\n");
        notification.append("Bewerbungs-ID / Application ID: ").append(application.getId()).append("\n");
        notification.append("Studiengang-ID / Study Program ID: ").append(studyProgram.getId()).append("\n");
        notification.append("Erstellt am / Created at: ")
                .append(student.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))).append("\n\n");








        return notification.toString();
    }

    /**
     * Helper methods for type-safe variable retrieval
     * Hilfsmethoden für typsichere Variablen-Abfrage
     */
    private Long getApplicationId(DelegateExecution execution) {
        Object applicationIdObj = execution.getVariable("applicationId");

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
            return Boolean.FALSE;
        }
    }
}
