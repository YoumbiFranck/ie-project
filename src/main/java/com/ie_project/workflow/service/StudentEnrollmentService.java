package com.ie_project.workflow.service;

import com.ie_project.workflow.entity.Application;
import com.ie_project.workflow.entity.Student;
import com.ie_project.workflow.entity.StudyProgram;
import com.ie_project.workflow.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Service für die Immatrikulation von Studenten
 * Service for student enrollment
 *
 * This service handles the creation of student records from approved applications.
 * It manages the transition from applicant to enrolled student status.
 *
 * Dieser Service behandelt die Erstellung von Studentendatensätzen aus genehmigten Bewerbungen.
 * Er verwaltet den Übergang vom Bewerber zum immatrikulierten Studenten.
 *
 * @author IE Project Team
 */
@Service
@Transactional
public class StudentEnrollmentService {

    @Autowired
    private StudentRepository studentRepository;

    /**
     * Erstellt einen Studentendatensatz aus einer genehmigten Bewerbung
     * Creates a student record from an approved application
     *
     * @param application Die genehmigte Bewerbung / The approved application
     * @param studentNumber Die generierte Matrikelnummer / The generated student number
     * @return Der erstellte Student / The created student
     */
    public Student createStudentFromApplication(Application application, String studentNumber) {

        if (application == null) {
            throw new IllegalArgumentException("Application must not be null / Bewerbung darf nicht null sein");
        }

        if (studentNumber == null || studentNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Student number must not be null or empty / Matrikelnummer darf nicht null oder leer sein");
        }

        // Validate application status / Bewerbungsstatus validieren
        if (!Application.ApplicationStatus.ACCEPTED.equals(application.getStatus())) {
            throw new IllegalStateException("Only accepted applications can be enrolled / Nur angenommene Bewerbungen können immatrikuliert werden. Current status: " + application.getStatus());
        }

        // Validate payment status / Zahlungsstatus validieren
        if (!application.isTuitionFeePaid()) {
            throw new IllegalStateException("Tuition fee must be paid before enrollment / Semesterbeitrag muss vor Immatrikulation bezahlt werden");
        }

        // Check if student already exists for this application
        // Prüfen ob bereits ein Student für diese Bewerbung existiert
        if (studentRepository.existsByApplicationId(application.getId())) {
            throw new IllegalStateException("Student already exists for application / Student existiert bereits für Bewerbung: " + application.getId());
        }

        // Check if student number is already in use
        // Prüfen ob Matrikelnummer bereits verwendet wird
        if (studentRepository.existsByStudentNumber(studentNumber)) {
            throw new IllegalStateException("Student number already exists / Matrikelnummer bereits vorhanden: " + studentNumber);
        }

        // Get study program / Studiengang holen
        StudyProgram studyProgram = application.getStudyProgram();
        if (studyProgram == null) {
            throw new IllegalStateException("Study program not found for application / Studiengang nicht gefunden für Bewerbung: " + application.getId());
        }

        System.out.println("=== CREATING STUDENT RECORD ===");
        System.out.println("Application ID: " + application.getId());
        System.out.println("Student Number: " + studentNumber);
        System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
        System.out.println("Email: " + application.getEmail());
        System.out.println("Study Program: " + studyProgram.getName() + " (" + studyProgram.getCode() + ")");
        System.out.println("==============================");

        // Create new student record / Neuen Studentendatensatz erstellen
        Student student = new Student();

        // Set basic information / Grundinformationen setzen
        student.setStudentNumber(studentNumber);
        student.setFirstName(application.getFirstName());
        student.setLastName(application.getLastName());
        student.setEmail(application.getEmail());

        // Set academic information / Studienbezogene Informationen setzen
        student.setStudyProgram(studyProgram);
        student.setEnrollmentDate(LocalDate.now());
        student.setCurrentSemester(1); // Always start with semester 1

        // Link to original application / Mit ursprünglicher Bewerbung verknüpfen
        student.setApplication(application);

        // Set timestamps / Zeitstempel setzen
        LocalDateTime now = LocalDateTime.now();
        student.setCreatedAt(now);
        student.setUpdatedAt(now);

        // Save student record / Studentendatensatz speichern
        Student savedStudent = studentRepository.save(student);

        System.out.println("=== STUDENT RECORD CREATED SUCCESSFULLY ===");
        System.out.println("Student ID: " + savedStudent.getId());
        System.out.println("Student Number: " + savedStudent.getStudentNumber());
        System.out.println("Full Name: " + savedStudent.getFullName());
        System.out.println("Email: " + savedStudent.getEmail());
        System.out.println("Study Program: " + savedStudent.getStudyProgram().getName());
        System.out.println("Enrollment Date: " + savedStudent.getEnrollmentDate());
        System.out.println("Current Semester: " + savedStudent.getCurrentSemester());
        System.out.println("Academic Year: " + savedStudent.getAcademicYear());
        System.out.println("First Semester Student: " + savedStudent.isFirstSemesterStudent());
        System.out.println("Created At: " + savedStudent.getCreatedAt());
        System.out.println("==========================================");

        return savedStudent;
    }

    /**
     * Aktualisiert das Semester eines Studenten
     * Updates a student's semester
     *
     * @param studentId Die Student-ID / The student ID
     * @param newSemester Das neue Semester / The new semester
     * @return Der aktualisierte Student / The updated student
     */
    public Student updateStudentSemester(Long studentId, int newSemester) {

        if (studentId == null) {
            throw new IllegalArgumentException("Student ID must not be null / Student-ID darf nicht null sein");
        }

        if (newSemester < 1 || newSemester > 20) {
            throw new IllegalArgumentException("Semester must be between 1 and 20 / Semester muss zwischen 1 und 20 liegen");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found / Student nicht gefunden: " + studentId));

        student.setCurrentSemester(newSemester);
        student.setUpdatedAt(LocalDateTime.now());

        Student updatedStudent = studentRepository.save(student);

        System.out.println("=== STUDENT SEMESTER UPDATED ===");
        System.out.println("Student: " + updatedStudent.getFullName());
        System.out.println("Student Number: " + updatedStudent.getStudentNumber());
        System.out.println("New Semester: " + updatedStudent.getCurrentSemester());
        System.out.println("Updated At: " + updatedStudent.getUpdatedAt());
        System.out.println("===============================");

        return updatedStudent;
    }

    /**
     * Befördert einen Studenten ins nächste Semester
     * Advances a student to the next semester
     *
     * @param studentId Die Student-ID / The student ID
     * @return Der aktualisierte Student / The updated student
     */
    public Student advanceStudentToNextSemester(Long studentId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found / Student nicht gefunden: " + studentId));

        int currentSemester = student.getCurrentSemester();
        int nextSemester = currentSemester + 1;

        if (nextSemester > 20) {
            throw new IllegalStateException("Cannot advance beyond semester 20 / Kann nicht über Semester 20 hinaus befördern");
        }

        return updateStudentSemester(studentId, nextSemester);
    }

    /**
     * Findet einen Studenten anhand der ursprünglichen Bewerbung
     * Finds a student based on the original application
     *
     * @param applicationId Die Bewerbungs-ID / The application ID
     * @return Der Student (optional) / The student (optional)
     */
    public Student findStudentByApplicationId(Long applicationId) {

        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID must not be null / Bewerbungs-ID darf nicht null sein");
        }

        return studentRepository.findByApplicationId(applicationId)
                .orElse(null);
    }

    /**
     * Prüft ob für eine Bewerbung bereits ein Student existiert
     * Checks if a student already exists for an application
     *
     * @param applicationId Die Bewerbungs-ID / The application ID
     * @return true wenn Student existiert / true if student exists
     */
    public boolean studentExistsForApplication(Long applicationId) {

        if (applicationId == null) {
            return false;
        }

        return studentRepository.existsByApplicationId(applicationId);
    }

    /**
     * Validiert die Datenintegrität eines Studentendatensatzes
     * Validates the data integrity of a student record
     *
     * @param student Der zu validierende Student / The student to validate
     * @return true wenn alle Validierungen bestehen / true if all validations pass
     */
    public boolean validateStudentRecord(Student student) {

        if (student == null) {
            return false;
        }

        // Check required fields / Pflichtfelder prüfen
        if (student.getStudentNumber() == null || student.getStudentNumber().trim().isEmpty()) {
            System.err.println("Validation failed: Student number is missing");
            return false;
        }

        if (student.getFirstName() == null || student.getFirstName().trim().isEmpty()) {
            System.err.println("Validation failed: First name is missing");
            return false;
        }

        if (student.getLastName() == null || student.getLastName().trim().isEmpty()) {
            System.err.println("Validation failed: Last name is missing");
            return false;
        }

        if (student.getEmail() == null || student.getEmail().trim().isEmpty()) {
            System.err.println("Validation failed: Email is missing");
            return false;
        }

        if (student.getStudyProgram() == null) {
            System.err.println("Validation failed: Study program is missing");
            return false;
        }

        if (student.getEnrollmentDate() == null) {
            System.err.println("Validation failed: Enrollment date is missing");
            return false;
        }

        // Check semester range / Semesterbereich prüfen
        if (student.getCurrentSemester() < 1 || student.getCurrentSemester() > 20) {
            System.err.println("Validation failed: Current semester out of range: " + student.getCurrentSemester());
            return false;
        }

        // Check student number format / Matrikelnummer-Format prüfen
        if (!student.hasValidStudentNumberFormat()) {
            System.err.println("Validation failed: Invalid student number format: " + student.getStudentNumber());
            return false;
        }

        // Check email format / E-Mail-Format prüfen
        if (!isValidEmail(student.getEmail())) {
            System.err.println("Validation failed: Invalid email format: " + student.getEmail());
            return false;
        }

        // Check enrollment date is not in the future / Prüfen dass Einschreibungsdatum nicht in der Zukunft liegt
        if (student.getEnrollmentDate().isAfter(LocalDate.now())) {
            System.err.println("Validation failed: Enrollment date is in the future: " + student.getEnrollmentDate());
            return false;
        }

        System.out.println("Student record validation passed for: " + student.getFullName());
        return true;
    }

    /**
     * Hilfsmethode zur E-Mail-Validierung
     * Helper method for email validation
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Basic email pattern validation
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailPattern);
    }

    /**
     * Erstellt einen Statistikbericht für Immatrikulationen
     * Creates a statistics report for enrollments
     *
     * @param studyProgramId Die Studiengang-ID (optional) / The study program ID (optional)
     * @return Statistikbericht / Statistics report
     */
    public String generateEnrollmentStatistics(Long studyProgramId) {

        StringBuilder stats = new StringBuilder();

        stats.append("=== IMMATRIKULATIONSSTATISTIK / ENROLLMENT STATISTICS ===\n\n");

        if (studyProgramId != null) {
            // Statistics for specific study program / Statistiken für bestimmten Studiengang
            long totalStudents = studentRepository.countByStudyProgramId(studyProgramId);
            stats.append("Studiengang-ID / Study Program ID: ").append(studyProgramId).append("\n");
            stats.append("Gesamtzahl Studenten / Total Students: ").append(totalStudents).append("\n\n");

            // Students by enrollment year / Studenten nach Einschreibungsjahr
            stats.append("Studenten nach Jahren / Students by Year:\n");
            int currentYear = LocalDate.now().getYear();
            for (int year = currentYear; year >= currentYear - 5; year--) {
                long studentsInYear = studentRepository.findByStudyProgramIdAndEnrollmentYear(studyProgramId, year).size();
                stats.append("  ").append(year).append(": ").append(studentsInYear).append(" Studenten\n");
            }
        } else {
            // Overall statistics / Gesamtstatistiken
            long totalStudents = studentRepository.count();
            stats.append("Gesamtzahl Studenten / Total Students: ").append(totalStudents).append("\n\n");

            // Students by study program / Studenten nach Studiengang
            stats.append("Studenten nach Studiengang / Students by Study Program:\n");
            studentRepository.countStudentsByStudyProgram().forEach(result -> {
                String programName = (String) result[0];
                Long count = (Long) result[1];
                stats.append("  ").append(programName).append(": ").append(count).append(" Studenten\n");
            });

            stats.append("\n");

            // Students by enrollment year / Studenten nach Einschreibungsjahr
            stats.append("Studenten nach Einschreibungsjahr / Students by Enrollment Year:\n");
            studentRepository.countStudentsByEnrollmentYear().forEach(result -> {
                Integer year = (Integer) result[0];
                Long count = (Long) result[1];
                stats.append("  ").append(year).append(": ").append(count).append(" Studenten\n");
            });
        }

        stats.append("\n");
        stats.append("Bericht erstellt am / Report generated on: ");
        stats.append(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        stats.append("\n");
        stats.append("======================================================");

        return stats.toString();
    }

    /**
     * Führt eine Datenintegritätsprüfung für alle Studenten durch
     * Performs a data integrity check for all students
     *
     * @return Bericht über gefundene Probleme / Report of found issues
     */
    public String performDataIntegrityCheck() {

        StringBuilder report = new StringBuilder();
        report.append("=== DATENINTEGRITÄTSPRÜFUNG / DATA INTEGRITY CHECK ===\n\n");

        int issuesFound = 0;

        // Check for duplicate emails / Prüfe auf doppelte E-Mails
        var duplicateEmails = studentRepository.findDuplicateEmails();
        if (!duplicateEmails.isEmpty()) {
            report.append("WARNUNG: Doppelte E-Mail-Adressen gefunden / WARNING: Duplicate emails found:\n");
            final int[] issuesFoundRef = {issuesFound}; // Create effectively final reference
            duplicateEmails.forEach(result -> {
                String email = (String) result[0];
                Long count = (Long) result[1];
                report.append("  ").append(email).append(" (").append(count).append(" mal)\n");
                issuesFoundRef[0]++;
            });
            issuesFound = issuesFoundRef[0]; // Update the counter
            report.append("\n");
        }

        // Check for students without applications / Prüfe auf Studenten ohne Bewerbungen
        var studentsWithoutApplication = studentRepository.findStudentsWithoutApplication();
        if (!studentsWithoutApplication.isEmpty()) {
            report.append("WARNUNG: Studenten ohne verknüpfte Bewerbung / WARNING: Students without linked application:\n");
            final int[] issuesFoundRef2 = {issuesFound}; // Create effectively final reference
            studentsWithoutApplication.forEach(student -> {
                report.append("  ").append(student.getStudentNumber()).append(" - ").append(student.getFullName()).append("\n");
                issuesFoundRef2[0]++;
            });
            issuesFound = issuesFoundRef2[0]; // Update the counter
            report.append("\n");
        }

        // Check for invalid student numbers / Prüfe auf ungültige Matrikelnummern
        var studentsWithInvalidNumbers = studentRepository.findStudentsWithInvalidNumbers();
        if (!studentsWithInvalidNumbers.isEmpty()) {
            report.append("WARNUNG: Studenten mit ungültigen Matrikelnummern / WARNING: Students with invalid numbers:\n");
            final int[] issuesFoundRef3 = {issuesFound}; // Create effectively final reference
            studentsWithInvalidNumbers.forEach(student -> {
                report.append("  ").append(student.getStudentNumber()).append(" - ").append(student.getFullName()).append("\n");
                issuesFoundRef3[0]++;
            });
            issuesFound = issuesFoundRef3[0]; // Update the counter
            report.append("\n");
        }

        if (issuesFound == 0) {
            report.append(" Keine Datenintegritätsprobleme gefunden\n");
            report.append("No data integrity issues found\n");
        } else {
            report.append("Insgesamt ").append(issuesFound).append(" Probleme gefunden\n");
            report.append("Total of ").append(issuesFound).append(" issues found\n");
        }

        report.append("\n");
        report.append("Prüfung durchgeführt am / Check performed on: ");
        report.append(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        report.append("\n");
        report.append("=====================================================");

        return report.toString();
    }
}