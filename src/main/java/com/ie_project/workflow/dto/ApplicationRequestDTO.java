package com.ie_project.workflow.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * DTO für Bewerbungsanfragen über REST API
 * DTO for application requests via REST API
 *
 * @author IE Project Team
 */
public class ApplicationRequestDTO {

    @NotBlank(message = "Vorname ist erforderlich / First name is required")
    @Size(max = 100, message = "Vorname darf maximal 100 Zeichen haben / First name max 100 characters")
    private String firstName;

    @NotBlank(message = "Nachname ist erforderlich / Last name is required")
    @Size(max = 100, message = "Nachname darf maximal 100 Zeichen haben / Last name max 100 characters")
    private String lastName;

    @NotBlank(message = "E-Mail ist erforderlich / Email is required")
    @Email(message = "Ungültige E-Mail-Adresse / Invalid email address")
    private String email;

    @Size(max = 50, message = "Telefon darf maximal 50 Zeichen haben / Phone max 50 characters")
    private String phone;

    @NotNull(message = "Geburtsdatum ist erforderlich / Date of birth is required")
    @Past(message = "Geburtsdatum muss in der Vergangenheit liegen / Date of birth must be in the past")
    private LocalDate dateOfBirth;

    // Address Information / Adressinformationen
    private String street;
    private String city;
    private String postalCode;
    private String country;

    @NotNull(message = "Studiengang ID ist erforderlich / Study program ID is required")
    private Long studyProgramId;

    @DecimalMin(value = "1.0", message = "Abiturnote muss zwischen 1.0 und 4.0 liegen / High school grade must be between 1.0 and 4.0")
    @DecimalMax(value = "4.0", message = "Abiturnote muss zwischen 1.0 und 4.0 liegen / High school grade must be between 1.0 and 4.0")
    @Digits(integer = 1, fraction = 2, message = "Abiturnote muss das Format X.XX haben / High school grade must have format X.XX")
    private BigDecimal highSchoolGrade;

    // Constructors
    public ApplicationRequestDTO() {}

    // Getters and Setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

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

    public Long getStudyProgramId() { return studyProgramId; }
    public void setStudyProgramId(Long studyProgramId) { this.studyProgramId = studyProgramId; }

    public BigDecimal getHighSchoolGrade() { return highSchoolGrade; }
    public void setHighSchoolGrade(BigDecimal highSchoolGrade) { this.highSchoolGrade = highSchoolGrade; }

    @Override
    public String toString() {
        return "ApplicationRequestDTO{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", studyProgramId=" + studyProgramId +
                '}';
    }
}


