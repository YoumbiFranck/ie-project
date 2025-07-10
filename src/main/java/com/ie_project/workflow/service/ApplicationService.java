package com.ie_project.workflow.service;

import com.ie_project.workflow.dto.ApplicationRequestDTO;
import com.ie_project.workflow.dto.ApplicationResponseDTO;
import com.ie_project.workflow.entity.Application;
import com.ie_project.workflow.entity.StudyProgram;
import com.ie_project.workflow.repository.ApplicationRepository;
import com.ie_project.workflow.repository.StudyProgramRepository;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service für Bewerbungslogik
 * Service for application logic
 *
 * @author IE Project Team
 */
@Service
@Transactional
public class ApplicationService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private StudyProgramRepository studyProgramRepository;

    @Autowired
    private RuntimeService runtimeService;

    /**
     * Verarbeitet eine neue Bewerbung
     * Processes a new application
     */
    public ApplicationResponseDTO submitApplication(ApplicationRequestDTO requestDTO) {

        // Validate that email is not already used / Prüfen ob E-Mail bereits verwendet wird
        if (applicationRepository.existsByEmail(requestDTO.getEmail())) {
            throw new IllegalArgumentException("E-Mail bereits verwendet / Email already in use: " + requestDTO.getEmail());
        }

        // Find study program / Studiengang finden
        StudyProgram studyProgram = studyProgramRepository.findById(requestDTO.getStudyProgramId())
                .orElseThrow(() -> new IllegalArgumentException("Studiengang nicht gefunden / Study program not found: " + requestDTO.getStudyProgramId()));

        // Create application entity / Bewerbung erstellen
        Application application = new Application();
        application.setFirstName(requestDTO.getFirstName());
        application.setLastName(requestDTO.getLastName());
        application.setEmail(requestDTO.getEmail());
        application.setPhone(requestDTO.getPhone());
        application.setDateOfBirth(requestDTO.getDateOfBirth());
        application.setStreet(requestDTO.getStreet());
        application.setCity(requestDTO.getCity());
        application.setPostalCode(requestDTO.getPostalCode());
        application.setCountry(requestDTO.getCountry());
        application.setStudyProgram(studyProgram);
        application.setHighSchoolGrade(requestDTO.getHighSchoolGrade());

        // Save application / Bewerbung speichern
        Application savedApplication = applicationRepository.save(application);

        // Start Camunda process / Camunda-Prozess starten
        String processInstanceId = startApplicationProcess(savedApplication);

        // Update application with process instance ID / Bewerbung mit Prozess-ID aktualisieren
        savedApplication.setCamundaProcessInstanceId(processInstanceId);
        applicationRepository.save(savedApplication);

        // Create response / Antwort erstellen
        String message = String.format(
                "Bewerbung erfolgreich eingereicht! Application ID: %d, Studiengang: %s",
                savedApplication.getId(),
                studyProgram.getName()
        );

        return new ApplicationResponseDTO(savedApplication.getId(), message, processInstanceId);
    }

    /**
     * Startet den Camunda-Prozess für eine Bewerbung
     * Starts the Camunda process for an application
     */
    private String startApplicationProcess(Application application) {

        // Prepare process variables / Prozessvariablen vorbereiten
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicationId", application.getId());
        variables.put("firstName", application.getFirstName());
        variables.put("lastName", application.getLastName());
        variables.put("email", application.getEmail());
        variables.put("studyProgramId", application.getStudyProgram().getId());
        variables.put("studyProgramName", application.getStudyProgram().getName());
        variables.put("studyProgramCode", application.getStudyProgram().getCode());
        variables.put("applicationDate", application.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

        if (application.getHighSchoolGrade() != null) {
            variables.put("highSchoolGrade", application.getHighSchoolGrade());
        }

        // Start process instance / Prozessinstanz starten
        org.camunda.bpm.engine.runtime.ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "student-application-process",
                variables
        );

        return processInstance.getId();
    }
}