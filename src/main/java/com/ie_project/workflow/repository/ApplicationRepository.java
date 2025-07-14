package com.ie_project.workflow.repository;

import com.ie_project.workflow.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository für Application Entitäten - Erweiterte Version
 * Repository for Application entities - Extended version
 *
 * Bietet umfassende Methoden für alle Workflow-Schritte:
 * - Basis CRUD Operationen
 * - NC (Numerus Clausus) Ranking und Selektion
 * - Geschlechterquoten-Berechnungen
 * - Status- und Deadline-Management
 * - Zahlungsstatus-Verfolgung
 *
 * @author IE Project Team
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // ===== BASIC QUERIES / BASIS ABFRAGEN =====

    /**
     * Findet Bewerbung nach E-Mail
     * Finds application by email
     */
    Optional<Application> findByEmail(String email);

    /**
     * Findet Bewerbung nach Camunda Process Instance ID
     * Finds application by Camunda process instance ID
     */
    Optional<Application> findByCamundaProcessInstanceId(String processInstanceId);

    /**
     * Prüft ob E-Mail bereits für Bewerbung verwendet wurde
     * Checks if email is already used for application
     */
    boolean existsByEmail(String email);

    // ===== STUDY PROGRAM QUERIES / STUDIENGANG ABFRAGEN =====

    /**
     * Findet alle Bewerbungen für einen bestimmten Studiengang
     * Finds all applications for a specific study program
     */
    List<Application> findByStudyProgramId(Long studyProgramId);

    /**
     * Findet alle Bewerbungen für einen bestimmten Studiengang mit einem anderen Status als dem angegebenen
     * Finds all applications for a specific study program with a status different from the specified one
     */
    List<Application> findByStudyProgramIdAndStatusNot(Long studyProgramId, Application.ApplicationStatus status);

    /**
     * Findet alle Bewerbungen für einen bestimmten Studiengang mit einem bestimmten Status
     * Finds all applications for a specific study program with a specific status
     */
    List<Application> findByStudyProgramIdAndStatus(Long studyProgramId, Application.ApplicationStatus status);

    // ===== STATUS-BASED QUERIES / STATUS-BASIERTE ABFRAGEN =====

    /**
     * Findet alle Bewerbungen mit einem bestimmten Status
     * Finds all applications with a specific status
     */
    List<Application> findByStatus(Application.ApplicationStatus status);

    /**
     * Zählt Bewerbungen für einen bestimmten Studiengang ohne bestimmten Status
     * Counts applications for a specific study program excluding certain status
     */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.studyProgram.id = :studyProgramId AND a.status != :excludeStatus")
    Long countByStudyProgramIdAndStatusNot(@Param("studyProgramId") Long studyProgramId, @Param("excludeStatus") Application.ApplicationStatus excludeStatus);

    // ===== NC RANKING QUERIES / NC RANKING ABFRAGEN =====

    /**
     * Findet alle Bewerbungen für NC-Ranking (mit Noten, nicht abgelehnt, bestimmter Studiengang)
     * Sortiert nach Note (aufsteigend: 1,0 = beste) und dann nach Eingangsdatum
     * Finds all applications for NC ranking (with grades, not rejected, specific study program)
     * Sorted by grade (ascending: 1.0 = best) and then by submission date
     */
    @Query("SELECT a FROM Application a WHERE a.studyProgram.id = :studyProgramId AND a.highSchoolGrade IS NOT NULL AND a.status != 'REJECTED' ORDER BY a.highSchoolGrade ASC, a.createdAt ASC")
    List<Application> findForNCRanking(@Param("studyProgramId") Long studyProgramId);

    /**
     * Findet alle Bewerbungen für einen bestimmten Studiengang, die Abiturnoten haben
     * Finds all applications for a specific study program that have high school grades
     */
    @Query("SELECT a FROM Application a WHERE a.studyProgram.id = :studyProgramId AND a.highSchoolGrade IS NOT NULL AND a.status != :excludeStatus ORDER BY a.highSchoolGrade ASC")
    List<Application> findByStudyProgramIdWithGradesExcludingStatus(@Param("studyProgramId") Long studyProgramId, @Param("excludeStatus") Application.ApplicationStatus excludeStatus);

    /**
     * Findet Bewerbungen mit besserer oder gleicher Note für NC-Konkurrenzanalyse
     * Finds applications with better or equal grade for NC competition analysis
     */
    @Query("SELECT a FROM Application a WHERE a.studyProgram.id = :studyProgramId AND a.highSchoolGrade <= :grade AND a.status != 'REJECTED' ORDER BY a.highSchoolGrade ASC, a.createdAt ASC")
    List<Application> findBetterOrEqualGradeApplications(@Param("studyProgramId") Long studyProgramId, @Param("grade") Double grade);

    // ===== GENDER QUOTA QUERIES / GESCHLECHTERQUOTEN ABFRAGEN =====

    /**
     * Zählt Bewerbungen nach Geschlecht für einen bestimmten Studiengang (für Quotenberechnungen)
     * Counts applications by gender for a specific study program (for quota calculations)
     */
    @Query("SELECT a.sex, COUNT(a) FROM Application a WHERE a.studyProgram.id = :studyProgramId AND a.status != 'REJECTED' GROUP BY a.sex")
    List<Object[]> countByGenderForStudyProgram(@Param("studyProgramId") Long studyProgramId);

    /**
     * Findet alle Bewerbungen eines bestimmten Geschlechts für einen Studiengang (nicht abgelehnt)
     * Finds all applications of a specific gender for a study program (not rejected)
     */
    @Query("SELECT a FROM Application a WHERE a.studyProgram.id = :studyProgramId AND a.sex = :sex AND a.status != 'REJECTED' ORDER BY a.highSchoolGrade ASC, a.createdAt ASC")
    List<Application> findByStudyProgramIdAndGender(@Param("studyProgramId") Long studyProgramId, @Param("sex") Application.Sex sex);

    /**
     * Zählt bereits zugelassene Bewerbungen nach Geschlecht für einen Studiengang
     * Counts already admitted applications by gender for a study program
     */
    @Query("SELECT a.sex, COUNT(a) FROM Application a WHERE a.studyProgram.id = :studyProgramId AND a.status = 'ACCEPTED' GROUP BY a.sex")
    List<Object[]> countAdmittedByGenderForStudyProgram(@Param("studyProgramId") Long studyProgramId);

    // ===== PAYMENT QUERIES / ZAHLUNGS ABFRAGEN =====

    /**
     * Findet alle Bewerbungen mit ausstehenden Zahlungen
     * Finds all applications with pending payments
     */
    @Query("SELECT a FROM Application a WHERE a.status = 'ACCEPTED' AND a.tuitionFeePaid = false")
    List<Application> findWithPendingPayments();

    /**
     * Findet überfällige Zahlungen (nach bestimmtem Datum)
     * Finds overdue payments (after certain date)
     */
    @Query("SELECT a FROM Application a WHERE a.status = 'ACCEPTED' AND a.tuitionFeePaid = false AND a.updatedAt < :deadline")
    List<Application> findOverduePayments(@Param("deadline") LocalDateTime deadline);

    /**
     * Zählt Bewerbungen mit erfolgten Zahlungen für einen Studiengang
     * Counts applications with completed payments for a study program
     */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.studyProgram.id = :studyProgramId AND a.tuitionFeePaid = true")
    Long countPaidApplicationsForStudyProgram(@Param("studyProgramId") Long studyProgramId);

    // ===== DEADLINE AND TIMING QUERIES / DEADLINE UND TIMING ABFRAGEN =====

    /**
     * Findet Bewerbungen eingereicht vor einem bestimmten Datum
     * Finds applications submitted before a certain date
     */
    @Query("SELECT a FROM Application a WHERE a.createdAt < :deadline ORDER BY a.createdAt ASC")
    List<Application> findSubmittedBefore(@Param("deadline") LocalDateTime deadline);

    /**
     * Findet Bewerbungen eingereicht nach einem bestimmten Datum
     * Finds applications submitted after a certain date
     */
    @Query("SELECT a FROM Application a WHERE a.createdAt > :deadline ORDER BY a.createdAt ASC")
    List<Application> findSubmittedAfter(@Param("deadline") LocalDateTime deadline);

    /**
     * Findet Bewerbungen eingereicht in einem bestimmten Zeitraum
     * Finds applications submitted within a certain time period
     */
    @Query("SELECT a FROM Application a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt ASC")
    List<Application> findSubmittedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ===== STATISTICS AND REPORTING QUERIES / STATISTIK UND REPORTING ABFRAGEN =====

    /**
     * Zählt Bewerbungen nach Status für einen Studiengang
     * Counts applications by status for a study program
     */
    @Query("SELECT a.status, COUNT(a) FROM Application a WHERE a.studyProgram.id = :studyProgramId GROUP BY a.status")
    List<Object[]> countByStatusForStudyProgram(@Param("studyProgramId") Long studyProgramId);

    /**
     * Berechnet Durchschnittsnote für einen Studiengang
     * Calculates average grade for a study program
     */
    @Query("SELECT AVG(a.highSchoolGrade) FROM Application a WHERE a.studyProgram.id = :studyProgramId AND a.highSchoolGrade IS NOT NULL AND a.status != 'REJECTED'")
    Double calculateAverageGradeForStudyProgram(@Param("studyProgramId") Long studyProgramId);

    /**
     * Findet beste Note für einen Studiengang
     * Finds best grade for a study program
     */
    @Query("SELECT MIN(a.highSchoolGrade) FROM Application a WHERE a.studyProgram.id = :studyProgramId AND a.highSchoolGrade IS NOT NULL AND a.status != 'REJECTED'")
    Double findBestGradeForStudyProgram(@Param("studyProgramId") Long studyProgramId);

    /**
     * Findet schlechteste Note für einen Studiengang
     * Finds worst grade for a study program
     */
    @Query("SELECT MAX(a.highSchoolGrade) FROM Application a WHERE a.studyProgram.id = :studyProgramId AND a.highSchoolGrade IS NOT NULL AND a.status != 'REJECTED'")
    Double findWorstGradeForStudyProgram(@Param("studyProgramId") Long studyProgramId);

    // ===== ADMISSION PROCESS QUERIES / ZULASSUNGSVERFAHREN ABFRAGEN =====

    /**
     * Findet alle Bewerbungen bereit für NC-Verfahren
     * Finds all applications ready for NC process
     */
    @Query("SELECT a FROM Application a WHERE a.studyProgram.admissionType = 'NUMERUS_CLAUSUS' AND a.status = 'DOCUMENT_CHECK' AND a.highSchoolGrade IS NOT NULL")
    List<Application> findReadyForNCProcess();

    /**
     * Findet alle Bewerbungen bereit für Aufnahmeprüfungen
     * Finds all applications ready for entrance exams
     */
    @Query("SELECT a FROM Application a WHERE a.studyProgram.admissionType = 'ENTRANCE_EXAM' AND a.status = 'DOCUMENT_CHECK'")
    List<Application> findReadyForEntranceExam();

    /**
     * Findet alle Bewerbungen für zulassungsfreie Studiengänge
     * Finds all applications for open admission programs
     */
    @Query("SELECT a FROM Application a WHERE a.studyProgram.admissionType = 'OPEN' AND a.status = 'DOCUMENT_CHECK'")
    List<Application> findForDirectAdmission();

    // ===== DUPLICATE AND VALIDATION QUERIES / DUPLIKAT UND VALIDIERUNGS ABFRAGEN =====

    /**
     * Findet Duplikate basierend auf E-Mail und Studiengang
     * Finds duplicates based on email and study program
     */
    @Query("SELECT a FROM Application a WHERE a.email = :email AND a.studyProgram.id = :studyProgramId")
    List<Application> findDuplicateApplications(@Param("email") String email, @Param("studyProgramId") Long studyProgramId);

    /**
     * Prüft ob Bewerber bereits für diesen Studiengang zugelassen wurde
     * Checks if applicant is already admitted for this study program
     */
    @Query("SELECT COUNT(a) > 0 FROM Application a WHERE a.email = :email AND a.studyProgram.id = :studyProgramId AND a.status IN ('ACCEPTED', 'ENROLLED')")
    boolean isAlreadyAdmittedForProgram(@Param("email") String email, @Param("studyProgramId") Long studyProgramId);
}
