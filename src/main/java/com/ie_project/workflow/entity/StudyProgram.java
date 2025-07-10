package com.ie_project.workflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a study program (Studiengang)
 * @author IE Project Team
 */
@Entity
@Table(name = "study_programs")
public class StudyProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "admission_type", nullable = false)
    private AdmissionType admissionType;

    @Column(name = "max_students")
    private Integer maxStudents;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public StudyProgram() {}

    public StudyProgram(String name, String code, AdmissionType admissionType) {
        this.name = name;
        this.code = code;
        this.admissionType = admissionType;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public AdmissionType getAdmissionType() { return admissionType; }
    public void setAdmissionType(AdmissionType admissionType) { this.admissionType = admissionType; }

    public Integer getMaxStudents() { return maxStudents; }
    public void setMaxStudents(Integer maxStudents) { this.maxStudents = maxStudents; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * Enum für Zulassungsarten / Enum for admission types
     */
    public enum AdmissionType {
        OPEN,               // Zulassungsfrei
        NUMERUS_CLAUSUS,    // Numerus Clausus
        ENTRANCE_EXAM       // Aufnahmeprüfung
    }
}
