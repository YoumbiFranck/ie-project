package com.ie_project.workflow.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * Entity representing a student application (Bewerbung)
 * @author IE Project Team
 */
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Personal Information / Persönliche Daten
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    // AJOUTÉ : Sex enum et propriété
    @Column(name = "sex", nullable = false)
    @Enumerated(EnumType.STRING)
    private Sex sex;

    @Column(length = 50)
    private String phone;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    // Address Information / Adressdaten
    private String street;
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    private String country;

    // Academic Information / Akademische Daten
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "study_program_id", nullable = false)
    private StudyProgram studyProgram;

    @Column(name = "high_school_grade", precision = 3, scale = 2)
    private BigDecimal highSchoolGrade;

    // Application Status / Bewerbungsstatus
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    // AJOUTÉ : Payment Status / Zahlungsstatus
    @Column(name = "tuition_fee_paid", nullable = false)
    private boolean tuitionFeePaid = false;

    // Process Information / Prozessinformationen
    @Column(name = "camunda_process_instance_id")
    private String camundaProcessInstanceId;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Application() {}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // AJOUTÉ : Sex getter/setter
    public Sex getSex() { return sex; }
    public void setSex(Sex sex) { this.sex = sex; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public StudyProgram getStudyProgram() { return studyProgram; }
    public void setStudyProgram(StudyProgram studyProgram) { this.studyProgram = studyProgram; }

    public BigDecimal getHighSchoolGrade() { return highSchoolGrade; }
    public void setHighSchoolGrade(BigDecimal highSchoolGrade) { this.highSchoolGrade = highSchoolGrade; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    // AJOUTÉ : Payment getter/setter
    public boolean isTuitionFeePaid() { return tuitionFeePaid; }
    public void setTuitionFeePaid(boolean tuitionFeePaid) { this.tuitionFeePaid = tuitionFeePaid; }

    public String getCamundaProcessInstanceId() { return camundaProcessInstanceId; }
    public void setCamundaProcessInstanceId(String camundaProcessInstanceId) {
        this.camundaProcessInstanceId = camundaProcessInstanceId;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // AJOUTÉ : Business methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isPending() {
        return status == ApplicationStatus.SUBMITTED || status == ApplicationStatus.DOCUMENT_CHECK;
    }

    public boolean isDecided() {
        return status == ApplicationStatus.ACCEPTED || status == ApplicationStatus.REJECTED || status == ApplicationStatus.ENROLLED;
    }

    public boolean isEnrolled() {
        return status == ApplicationStatus.ENROLLED;
    }

    public boolean isRejected() {
        return status == ApplicationStatus.REJECTED;
    }

    /**
     * AJOUTÉ : Sex enum für Geschlecht / Sex enum for gender
     */
    public enum Sex {
        M("Male", "Männlich"),
        F("Female", "Weiblich"),
        D("Diverse", "Divers");

        private final String englishLabel;
        private final String germanLabel;

        Sex(String englishLabel, String germanLabel) {
            this.englishLabel = englishLabel;
            this.germanLabel = germanLabel;
        }

        public String getEnglishLabel() {
            return englishLabel;
        }

        public String getGermanLabel() {
            return germanLabel;
        }
    }

    /**
     * Status enum für Bewerbungen / Status enum for applications
     */
    public enum ApplicationStatus {
        SUBMITTED,          // Eingereicht
        DOCUMENT_CHECK,     // Dokumentenprüfung
        ACCEPTED,          // Angenommen
        REJECTED,          // Abgelehnt
        ENROLLED           // Eingeschrieben
    }

    @Override
    public String toString() {
        return "Application{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", sex=" + sex +
                ", studyProgram=" + (studyProgram != null ? studyProgram.getName() : null) +
                ", highSchoolGrade=" + highSchoolGrade +
                ", status=" + status +
                ", tuitionFeePaid=" + tuitionFeePaid +
                ", createdAt=" + createdAt +
                '}';
    }
}
