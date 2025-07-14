package com.ie_project.workflow.delegate;

import com.ie_project.workflow.entity.Application;
import com.ie_project.workflow.entity.Student;
import com.ie_project.workflow.entity.StudyProgram;
import com.ie_project.workflow.repository.ApplicationRepository;
import com.ie_project.workflow.repository.StudentRepository;
import com.ie_project.workflow.service.NotificationService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Camunda Delegate für das Versenden des Willkommenspakets
 * Camunda Delegate for sending the welcome package
 *
 * This delegate represents the final step in the enrollment process.
 * It sends a comprehensive welcome package to newly enrolled students
 * containing all necessary information for starting their studies.
 *
 * Dieser Delegate stellt den finalen Schritt im Immatrikulationsprozess dar.
 * Er versendet ein umfassendes Willkommenspaket an neu eingeschriebene Studenten
 * mit allen notwendigen Informationen für den Studienbeginn.
 *
 * @author IE Project Team
 */
@Component("welcomePackageDelegate")
public class WelcomePackageDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== WELCOME PACKAGE DELEGATE EXECUTED ===");

        try {
            // Get required data from process variables
            // Erforderliche Daten aus Prozessvariablen holen
            Long applicationId = getApplicationId(execution);
            Long studentId = getLongVariable(execution, "studentId");
            String finalStudentNumber = getStringVariable(execution, "finalStudentNumber");

            System.out.println("Application ID: " + applicationId);
            System.out.println("Student ID: " + studentId);
            System.out.println("Final Student Number: " + finalStudentNumber);

            // Validate that student record was created
            // Validieren dass Studentendatensatz erstellt wurde
            Boolean studentRecordCreated = getBooleanVariable(execution, "studentRecordCreated");
            if (!Boolean.TRUE.equals(studentRecordCreated)) {
                throw new IllegalStateException("Student record creation not completed / Studentendatensatz-Erstellung nicht abgeschlossen");
            }

            // Find application and student in database
            // Bewerbung und Student in Datenbank finden
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found / Student nicht gefunden: " + studentId));

            // Verify application is enrolled / Verifizieren dass Bewerbung immatrikuliert ist
            if (!Application.ApplicationStatus.ENROLLED.equals(application.getStatus())) {
                throw new IllegalStateException("Welcome package only for enrolled applications / Willkommenspaket nur für immatrikulierte Bewerbungen. Current status: " + application.getStatus());
            }

            // Verify student data consistency / Studentendaten-Konsistenz verifizieren
            if (!student.getStudentNumber().equals(finalStudentNumber)) {
                throw new IllegalStateException("Student number mismatch / Matrikelnummer-Abweichung. Expected: " + finalStudentNumber + ", Found: " + student.getStudentNumber());
            }

            // Get study program information
            // Studiengang-Informationen holen
            StudyProgram studyProgram = student.getStudyProgram();
            if (studyProgram == null) {
                throw new IllegalStateException("Study program not found for student / Studiengang nicht gefunden für Student: " + studentId);
            }

            // Create comprehensive welcome package
            // Umfassendes Willkommenspaket erstellen
            String welcomePackageContent = createWelcomePackage(student, application, studyProgram);

            // Send welcome package via notification service
            // Willkommenspaket über Benachrichtigungsservice versenden
            boolean packageSent = notificationService.sendWelcomePackage(student, welcomePackageContent);

            // Set final process variables / Finale Prozessvariablen setzen
            execution.setVariable("welcomePackageSent", packageSent);
            execution.setVariable("welcomePackageSentAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            execution.setVariable("processCompleted", true);
            execution.setVariable("finalStatus", "ENROLLED");
            execution.setVariable("processEndReason", "SUCCESSFUL_ENROLLMENT");
            execution.setVariable("enrollmentCompletedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

            // Create process completion summary / Prozessabschluss-Zusammenfassung erstellen
            String processCompletionSummary = createProcessCompletionSummary(
                    student, application, studyProgram, packageSent
            );

            execution.setVariable("processCompletionSummary", processCompletionSummary);
            execution.setVariable("welcomePackageContent", welcomePackageContent);

            // Log welcome package delivery / Willkommenspaket-Versand protokollieren
            System.out.println("=== WILLKOMMENSPAKET VERSENDET / WELCOME PACKAGE SENT ===");
            System.out.println(processCompletionSummary);
            System.out.println("========================================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== WELCOME PACKAGE PROCESSING COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Student ID: " + studentId);
            System.out.println("Student Name: " + student.getFullName());
            System.out.println("Student Number: " + student.getStudentNumber());
            System.out.println("Email: " + student.getEmail());
            System.out.println("Study Program: " + studyProgram.getName() + " (" + studyProgram.getCode() + ")");
            System.out.println("Enrollment Date: " + student.getEnrollmentDate());
            System.out.println("Welcome Package Sent: " + (packageSent ? "YES" : "NO"));
            System.out.println("Process Status: COMPLETED");
            System.out.println("Final Status: ENROLLED");
            System.out.println("Processing Completed: YES");
            System.out.println("==========================================");

            // Log successful process completion / Erfolgreichen Prozessabschluss protokollieren
            System.out.println("ENROLLMENT PROCESS SUCCESSFULLY COMPLETED! ");
            System.out.println("Welcome to University Riedtal, " + student.getFullName() + "!");
            System.out.println("Welcome package sent to: " + student.getEmail());
            System.out.println("======================================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN WELCOME PACKAGE DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=========================================");
            throw e;
        }
    }

    /**
     * Erstellt das umfassende Willkommenspaket für den neuen Studenten
     * Creates the comprehensive welcome package for the new student
     */
    private String createWelcomePackage(Student student, Application application, StudyProgram studyProgram) {

        StringBuilder welcomePackage = new StringBuilder();


        // Personal welcome message / Persönliche Willkommensnachricht
        welcomePackage.append("=== HERZLICH WILLKOMMEN! / WELCOME! ===\n\n");
        welcomePackage.append("Liebe/r ").append(student.getFirstName()).append(",\n\n");
        welcomePackage.append("herzlichen Glückwunsch! Sie haben erfolgreich Ihr Studium an der Universität Riedtal begonnen.\n");
        welcomePackage.append("Wir freuen uns sehr, Sie als neuen Studenten in unserem ").append(studyProgram.getName()).append("-Programm begrüßen zu dürfen!\n\n");


        // Student information summary / Studenteninformations-Zusammenfassung
        welcomePackage.append("=== IHRE STUDENTENDATEN / YOUR STUDENT DATA ===\n");
        welcomePackage.append(" Name / Name: ").append(student.getFullName()).append("\n");
        welcomePackage.append(" Matrikelnummer / Student Number: ").append(student.getStudentNumber()).append("\n");
        welcomePackage.append(" E-Mail: ").append(student.getEmail()).append("\n");
        welcomePackage.append(" Studiengang / Study Program: ").append(studyProgram.getName()).append(" (").append(studyProgram.getCode()).append(")\n");
        welcomePackage.append(" Immatrikulationsdatum / Enrollment Date: ").append(student.getEnrollmentDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("\n");
        welcomePackage.append(" Semester: ").append(student.getCurrentSemester()).append("\n");
        welcomePackage.append(" Studienjahr / Academic Year: ").append(student.getAcademicYear()).append("\n\n");



        // Important first steps / Wichtige erste Schritte
        welcomePackage.append("   • Benutzername: ").append(student.getStudentNumber()).append("\n\n");




        // Study program specific information / Studiengangspezifische Informationen
        welcomePackage.append("=== INFORMATIONEN ZU IHREM STUDIENGANG / STUDY PROGRAM INFORMATION ===\n");
        welcomePackage.append(" Studiengang / Program: ").append(studyProgram.getName()).append("\n");
        welcomePackage.append(" Programmcode / Program Code: ").append(studyProgram.getCode()).append("\n");
        welcomePackage.append(" Zulassungsart / Admission Type: ");

        switch (studyProgram.getAdmissionType()) {
            case OPEN:
                welcomePackage.append("Zulassungsfrei / Open Admission\n");
                break;
            case NUMERUS_CLAUSUS:
                welcomePackage.append("Numerus Clausus\n");
                break;
            case ENTRANCE_EXAM:
                welcomePackage.append("Aufnahmeprüfung / Entrance Exam\n");
                break;
        }

        if (studyProgram.getMaxStudents() != null) {
            welcomePackage.append(" Maximale Studierendenzahl / Max Students: ").append(studyProgram.getMaxStudents()).append("\n");
        }


        return welcomePackage.toString();
    }

    /**
     * Erstellt eine Zusammenfassung des abgeschlossenen Prozesses
     * Creates a summary of the completed process
     */
    private String createProcessCompletionSummary(Student student, Application application,
                                                  StudyProgram studyProgram, boolean packageSent) {

        StringBuilder summary = new StringBuilder();

        summary.append("=== IMMATRIKULATIONSPROZESS ERFOLGREICH ABGESCHLOSSEN ===\n");
        summary.append("=== ENROLLMENT PROCESS SUCCESSFULLY COMPLETED ===\n\n");

        // Student summary / Studenten-Zusammenfassung
        summary.append(" NEUER STUDENT / NEW STUDENT:\n");
        summary.append("   Name: ").append(student.getFullName()).append("\n");
        summary.append("   Matrikelnummer: ").append(student.getStudentNumber()).append("\n");
        summary.append("   E-Mail: ").append(student.getEmail()).append("\n");
        summary.append("   Studiengang: ").append(studyProgram.getName()).append(" (").append(studyProgram.getCode()).append(")\n");
        summary.append("   Immatrikulation: ").append(student.getEnrollmentDate()).append("\n\n");





        return summary.toString();
    }

    /**
     * Berechnet die Dauer des gesamten Prozesses
     * Calculates the duration of the entire process
     */
    private String calculateProcessDuration(Application application, Student student) {

        LocalDateTime start = application.getCreatedAt();
        LocalDateTime end = student.getCreatedAt();

        java.time.Duration duration = java.time.Duration.between(start, end);

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        return String.format("%d Tage, %d Stunden, %d Minuten", days, hours, minutes);
    }

    /**
     * Helper methods for type-safe variable retrieval
     * Hilfsmethoden für typsichere Variablen-Abfrage
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

    private Long getLongVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);

        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else {
            return null;
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
