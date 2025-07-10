package com.ie_project.workflow.repository;

import com.ie_project.workflow.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository für Application Entitäten
 * Repository for Application entities
 *
 * @author IE Project Team
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

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
}
