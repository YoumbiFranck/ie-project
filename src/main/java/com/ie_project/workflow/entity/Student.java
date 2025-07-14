package com.ie_project.workflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity für eingeschriebene Studenten
 * JPA Entity for enrolled students
 *
 * Diese Entität repräsentiert einen erfolgreich immatrikulierten Studenten
 * mit seiner Matrikelnummer und allen relevanten Studiendaten.
 *
 * This entity represents a successfully enrolled student
 * with their student number and all relevant study data.
 *
 * @author IE Project Team
 */
@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_students_student_number", columnList = "student_number"),
        @Index(name = "idx_students_email", columnList = "email"),
        @Index(name = "idx_students_study_program", columnList = "study_program_id"),
        @Index(name = "idx_students_enrollment_date", columnList = "enrollment_date")
})
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== STUDENT NUMBER / MATRIKELNUMMER =====

    @Column(name = "student_number", unique = true, nullable = false, length = 20)
    @NotBlank(message = "Student number must not be blank / Matrikelnummer darf nicht leer sein")
    @Size(min = 10, max = 20, message = "Student number must be between 10 and 20 characters / Matrikelnummer muss zwischen 10 und 20 Zeichen lang sein")
    private String studentNumber;

    // ===== PERSONAL INFORMATION / PERSÖNLICHE DATEN =====

    @Column(name = "first_name", nullable = false, length = 100)
    @NotBlank(message = "First name must not be blank / Vorname darf nicht leer sein")
    @Size(max = 100, message = "First name must not exceed 100 characters / Vorname darf nicht länger als 100 Zeichen sein")
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    @NotBlank(message = "Last name must not be blank / Nachname darf nicht leer sein")
    @Size(max = 100, message = "Last name must not exceed 100 characters / Nachname darf nicht länger als 100 Zeichen sein")
    private String lastName;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    @Email(message = "Invalid email format / Ungültiges E-Mail-Format")
    @NotBlank(message = "Email must not be blank / E-Mail darf nicht leer sein")
    @Size(max = 255, message = "Email must not exceed 255 characters / E-Mail darf nicht länger als 255 Zeichen sein")
    private String email;

    // ===== ACADEMIC INFORMATION / STUDIENBEZOGENE DATEN =====

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_program_id", nullable = false, foreignKey = @ForeignKey(name = "fk_students_study_program"))
    @NotNull(message = "Study program must not be null / Studiengang darf nicht null sein")
    private StudyProgram studyProgram;

    @Column(name = "enrollment_date", nullable = false)
    @NotNull(message = "Enrollment date must not be null / Einschreibungsdatum darf nicht null sein")
    private LocalDate enrollmentDate;

    @Column(name = "current_semester", nullable = false)
    @Min(value = 1, message = "Current semester must be at least 1 / Aktuelles Semester muss mindestens 1 sein")
    @Max(value = 20, message = "Current semester must not exceed 20 / Aktuelles Semester darf nicht größer als 20 sein")
    private int currentSemester = 1;

    // ===== APPLICATION LINK / BEWERBUNGSVERKNÜPFUNG =====

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", unique = true, foreignKey = @ForeignKey(name = "fk_students_application"))
    private Application application;

    // ===== TIMESTAMPS / ZEITSTEMPEL =====

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== CONSTRUCTORS / KONSTRUKTOREN =====

    /**
     * Default constructor for JPA
     * Standard-Konstruktor für JPA
     */
    public Student() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.enrollmentDate = LocalDate.now();
        this.currentSemester = 1;
    }

    /**
     * Constructor for creating a student from an application
     * Konstruktor für die Erstellung eines Studenten aus einer Bewerbung
     */
    public Student(String studentNumber, String firstName, String lastName, String email,
                   StudyProgram studyProgram, Application application) {
        this();
        this.studentNumber = studentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.studyProgram = studyProgram;
        this.application = application;
    }

    // ===== JPA LIFECYCLE CALLBACKS / JPA LIFECYCLE CALLBACKS =====

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (enrollmentDate == null) {
            enrollmentDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== GETTERS AND SETTERS / GETTER UND SETTER =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public StudyProgram getStudyProgram() {
        return studyProgram;
    }

    public void setStudyProgram(StudyProgram studyProgram) {
        this.studyProgram = studyProgram;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public int getCurrentSemester() {
        return currentSemester;
    }

    public void setCurrentSemester(int currentSemester) {
        this.currentSemester = currentSemester;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ===== BUSINESS METHODS / GESCHÄFTSMETHODEN =====

    /**
     * Calculates the student's full name
     * Berechnet den vollständigen Namen des Studenten
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Checks if the student is in their first semester
     * Prüft ob der Student im ersten Semester ist
     */
    public boolean isFirstSemesterStudent() {
        return currentSemester == 1;
    }

    /**
     * Advances the student to the next semester
     * Versetzt den Studenten ins nächste Semester
     */
    public void advanceToNextSemester() {
        this.currentSemester++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Gets the academic year based on enrollment date
     * Ermittelt das Studienjahr basierend auf dem Einschreibungsdatum
     */
    public String getAcademicYear() {
        int year = enrollmentDate.getYear();
        // If enrolled in fall semester (August-December), academic year starts that year
        // If enrolled in spring semester (January-July), academic year started previous year
        if (enrollmentDate.getMonthValue() >= 8) {
            return year + "/" + (year + 1);
        } else {
            return (year - 1) + "/" + year;
        }
    }

    /**
     * Calculates years since enrollment
     * Berechnet die Jahre seit der Einschreibung
     */
    public int getYearsSinceEnrollment() {
        return LocalDate.now().getYear() - enrollmentDate.getYear();
    }

    /**
     * Checks if student number has valid format
     * Prüft ob die Matrikelnummer ein gültiges Format hat
     */
    public boolean hasValidStudentNumberFormat() {
        if (studentNumber == null || studentNumber.length() < 10) {
            return false;
        }

        // Check if it matches pattern: LETTERS + 8 DIGITS
        return studentNumber.matches("^[A-Z]+\\d{8}$");
    }

    // ===== OBJECT METHODS / OBJEKT-METHODEN =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;

        Student student = (Student) o;

        if (id != null) {
            return id.equals(student.id);
        }

        // If no ID yet, compare by student number (should be unique)
        return studentNumber != null && studentNumber.equals(student.studentNumber);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return studentNumber != null ? studentNumber.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", studentNumber='" + studentNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", studyProgram=" + (studyProgram != null ? studyProgram.getName() : null) +
                ", enrollmentDate=" + enrollmentDate +
                ", currentSemester=" + currentSemester +
                ", createdAt=" + createdAt +
                '}';
    }
}
