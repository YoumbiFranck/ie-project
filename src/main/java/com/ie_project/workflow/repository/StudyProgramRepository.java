package com.ie_project.workflow.repository;

import com.ie_project.workflow.entity.StudyProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository für StudyProgram Entitäten
 * Repository for StudyProgram entities
 *
 * @author IE Project Team
 */
@Repository
public interface StudyProgramRepository extends JpaRepository<StudyProgram, Long> {

    /**
     * Findet Studiengang nach Code
     * Finds study program by code
     */
    Optional<StudyProgram> findByCode(String code);

    /**
     * Prüft ob Studiengang Code bereits existiert
     * Checks if study program code already exists
     */
    boolean existsByCode(String code);
}
