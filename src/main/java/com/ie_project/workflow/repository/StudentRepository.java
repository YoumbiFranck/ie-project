package com.ie_project.workflow.repository;

import com.ie_project.workflow.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository für Student Entitäten
 * Repository for Student entities
 *
 * Bietet umfassende Methoden für die Verwaltung von Studentendaten:
 * - Basis CRUD Operationen
 * - Matrikelnummer-Management
 * - Studiengang-basierte Abfragen
 * - Semester- und Jahrgangsverwaltung
 *
 * @author IE Project Team
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // ===== BASIC QUERIES / BASIS ABFRAGEN =====

    /**
     * Findet Student nach Matrikelnummer
     * Finds student by student number
     */
    Optional<Student> findByStudentNumber(String studentNumber);

    /**
     * Findet Student nach E-Mail
     * Finds student by email
     */
    Optional<Student> findByEmail(String email);

    /**
     * Prüft ob Matrikelnummer bereits existiert
     * Checks if student number already exists
     */
    boolean existsByStudentNumber(String studentNumber);

    /**
     * Prüft ob E-Mail bereits für Student verwendet wird
     * Checks if email is already used for a student
     */
    boolean existsByEmail(String email);

    // ===== STUDENT NUMBER GENERATION QUERIES / MATRIKELNUMMER GENERIERUNGS ABFRAGEN =====

    /**
     * Findet die letzte (höchste) Matrikelnummer für ein bestimmtes Pattern
     * Finds the last (highest) student number for a specific pattern
     *
     * @param pattern Das Pattern (z.B. "INF2025") / The pattern (e.g. "INF2025")
     * @return Die höchste gefundene Matrikelnummer / The highest found student number
     */
    @Query("SELECT s.studentNumber FROM Student s WHERE s.studentNumber LIKE CONCAT(:pattern, '%') ORDER BY s.studentNumber DESC LIMIT 1")
    Optional<String> findLastStudentNumberForPattern(@Param("pattern") String pattern);

    /**
     * Zählt Studenten für ein bestimmtes Pattern (Jahr und Studiengang)
     * Counts students for a specific pattern (year and study program)
     */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.studentNumber LIKE CONCAT(:pattern, '%')")
    Long countStudentsForPattern(@Param("pattern") String pattern);

    // ===== STUDY PROGRAM QUERIES / STUDIENGANG ABFRAGEN =====

    /**
     * Findet alle Studenten für einen bestimmten Studiengang
     * Finds all students for a specific study program
     */
    List<Student> findByStudyProgramId(Long studyProgramId);

    /**
     * Zählt Studenten für einen bestimmten Studiengang
     * Counts students for a specific study program
     */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.studyProgram.id = :studyProgramId")
    Long countByStudyProgramId(@Param("studyProgramId") Long studyProgramId);

    /**
     * Findet Studenten für einen Studiengang in einem bestimmten Jahr
     * Finds students for a study program in a specific year
     */
    @Query("SELECT s FROM Student s WHERE s.studyProgram.id = :studyProgramId AND YEAR(s.enrollmentDate) = :year ORDER BY s.studentNumber ASC")
    List<Student> findByStudyProgramIdAndEnrollmentYear(@Param("studyProgramId") Long studyProgramId, @Param("year") int year);

    // ===== SEMESTER MANAGEMENT QUERIES / SEMESTER VERWALTUNG ABFRAGEN =====

    /**
     * Findet Studenten nach aktuellem Semester
     * Finds students by current semester
     */
    List<Student> findByCurrentSemester(int currentSemester);

    /**
     * Findet Studenten die in einem bestimmten Jahr eingeschrieben wurden
     * Finds students enrolled in a specific year
     */
    @Query("SELECT s FROM Student s WHERE YEAR(s.enrollmentDate) = :year ORDER BY s.enrollmentDate ASC")
    List<Student> findByEnrollmentYear(@Param("year") int year);

    /**
     * Findet Studenten die zwischen zwei Daten eingeschrieben wurden
     * Finds students enrolled between two dates
     */
    @Query("SELECT s FROM Student s WHERE s.enrollmentDate BETWEEN :startDate AND :endDate ORDER BY s.enrollmentDate ASC")
    List<Student> findByEnrollmentDateBetween(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

    // ===== APPLICATION LINK QUERIES / BEWERBUNGSVERKNÜPFUNGS ABFRAGEN =====

    /**
     * Findet Student basierend auf der ursprünglichen Bewerbung
     * Finds student based on original application
     */
    Optional<Student> findByApplicationId(Long applicationId);

    /**
     * Prüft ob für eine Bewerbung bereits ein Student erstellt wurde
     * Checks if a student was already created for an application
     */
    boolean existsByApplicationId(Long applicationId);

    // ===== STATISTICS AND REPORTING QUERIES / STATISTIK UND REPORTING ABFRAGEN =====

    /**
     * Zählt Studenten nach Studiengang
     * Counts students by study program
     */
    @Query("SELECT s.studyProgram.name, COUNT(s) FROM Student s GROUP BY s.studyProgram.name ORDER BY COUNT(s) DESC")
    List<Object[]> countStudentsByStudyProgram();

    /**
     * Zählt Studenten nach Einschreibungsjahr
     * Counts students by enrollment year
     */
    @Query("SELECT YEAR(s.enrollmentDate), COUNT(s) FROM Student s GROUP BY YEAR(s.enrollmentDate) ORDER BY YEAR(s.enrollmentDate) DESC")
    List<Object[]> countStudentsByEnrollmentYear();

    /**
     * Findet die neuesten eingeschriebenen Studenten
     * Finds the most recently enrolled students
     */
    @Query("SELECT s FROM Student s ORDER BY s.enrollmentDate DESC, s.createdAt DESC LIMIT :limit")
    List<Student> findRecentlyEnrolledStudents(@Param("limit") int limit);

    /**
     * Berechnet die durchschnittliche Anzahl von Studenten pro Studiengang
     * Calculates average number of students per study program
     */
    @Query("SELECT AVG(subquery.studentCount) FROM (SELECT COUNT(s) as studentCount FROM Student s GROUP BY s.studyProgram.id) as subquery")
    Double calculateAverageStudentsPerProgram();

    // ===== VALIDATION QUERIES / VALIDIERUNGS ABFRAGEN =====

    /**
     * Findet Studenten mit doppelten E-Mail-Adressen (sollte nicht vorkommen)
     * Finds students with duplicate email addresses (should not occur)
     */
    @Query("SELECT s.email, COUNT(s) FROM Student s GROUP BY s.email HAVING COUNT(s) > 1")
    List<Object[]> findDuplicateEmails();

    /**
     * Findet Studenten ohne verknüpfte Bewerbung
     * Finds students without linked application
     */
    @Query("SELECT s FROM Student s WHERE s.application IS NULL")
    List<Student> findStudentsWithoutApplication();

    /**
     * Findet Studenten mit ungültigen Matrikelnummer-Formaten (vereinfachte Prüfung)
     * Finds students with invalid student number formats (simplified check)
     * Prüft nur die Länge - für komplexere Validierung sollte Service-Layer verwendet werden
     */
    @Query("SELECT s FROM Student s WHERE LENGTH(s.studentNumber) <> 10")
    List<Student> findStudentsWithInvalidNumbers();
}