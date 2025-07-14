package com.ie_project.workflow.dto;

import java.time.LocalDateTime;

/**
 * DTO für Bewerbungsantworten über REST API
 * DTO for application responses via REST API
 *
 * @author IE Project Team
 */
public class ApplicationResponseDTO {

    private Long applicationId;
    private String message;
    private String processInstanceId;
    private String status;
    private LocalDateTime submissionTime;
    private String sex;

    // Constructors
    public ApplicationResponseDTO() {}

    public ApplicationResponseDTO(Long applicationId, String message, String processInstanceId) {
        this.applicationId = applicationId;
        this.message = message;
        this.processInstanceId = processInstanceId;
        this.status = "SUBMITTED";
        this.submissionTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(String processInstanceId) { this.processInstanceId = processInstanceId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSubmissionTime() { return submissionTime; }
    public void setSubmissionTime(LocalDateTime submissionTime) { this.submissionTime = submissionTime; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
}