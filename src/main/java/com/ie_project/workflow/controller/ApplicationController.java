package com.ie_project.workflow.controller;

import com.ie_project.workflow.dto.ApplicationRequestDTO;
import com.ie_project.workflow.dto.ApplicationResponseDTO;
import com.ie_project.workflow.entity.StudyProgram;
import com.ie_project.workflow.repository.StudyProgramRepository;
import com.ie_project.workflow.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller für Bewerbungen
 * REST Controller for applications
 *
 * @author IE Project Team
 */
@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "*") // For development - in production, specify allowed origins
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private StudyProgramRepository studyProgramRepository;

    /**
     * Neue Bewerbung einreichen
     * Submit new application
     *
     * @param requestDTO Bewerbungsdaten / Application data
     * @return Antwort mit Bewerbungs-ID und Prozess-ID / Response with application ID and process ID
     */
    @PostMapping
    public ResponseEntity<?> submitApplication(@Valid @RequestBody ApplicationRequestDTO requestDTO) {

        try {
            ApplicationResponseDTO response = applicationService.submitApplication(requestDTO);

            System.out.println("=== NEUE BEWERBUNG ÜBER API ===");
            System.out.println("Request: " + requestDTO.toString());
            System.out.println("Response: Application ID " + response.getApplicationId());
            System.out.println("Process Instance ID: " + response.getProcessInstanceId());
            System.out.println("===============================");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {

            System.err.println("=== FEHLER BEI BEWERBUNG ===");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Request: " + requestDTO.toString());
            System.err.println("============================");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Ungültige Bewerbungsdaten / Invalid application data",
                            "message", e.getMessage(),
                            "timestamp", java.time.LocalDateTime.now()
                    ));

        } catch (Exception e) {

            System.err.println("=== SYSTEMFEHLER ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("===================");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Interner Serverfehler / Internal server error",
                            "message", "Bitte versuchen Sie es später erneut / Please try again later",
                            "timestamp", java.time.LocalDateTime.now()
                    ));
        }
    }

    /**
     * Alle verfügbaren Studiengänge abrufen
     * Get all available study programs
     *
     * @return Liste der Studiengänge / List of study programs
     */
    @GetMapping("/study-programs")
    public ResponseEntity<List<StudyProgram>> getStudyPrograms() {

        try {
            List<StudyProgram> studyPrograms = studyProgramRepository.findAll();

            System.out.println("=== STUDIENGÄNGE ABGERUFEN ===");
            System.out.println("Anzahl: " + studyPrograms.size());
            System.out.println("==============================");

            return ResponseEntity.ok(studyPrograms);

        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen der Studiengänge: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Gesundheitscheck für die API
     * Health check for the API
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {

        Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "Application Service",
                "timestamp", java.time.LocalDateTime.now(),
                "version", "1.0.0"
        );

        return ResponseEntity.ok(health);
    }
}
