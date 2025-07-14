package com.ie_project.workflow.controller;

import com.ie_project.workflow.entity.Application;
import com.ie_project.workflow.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * REST API Controller for simulating tuition fee payments
 * REST API Controller für die Simulation von Studiengebührenzahlungen
 *
 * This controller provides endpoints to update the tuition_fee_paid status
 * in the applications table for testing payment workflows.
 *
 * Dieser Controller stellt Endpunkte zur Verfügung, um den tuition_fee_paid Status
 * in der applications Tabelle für Test-Zahlungsworkflows zu aktualisieren.
 *
 * @author IE Project Team
 */
@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*") // Enable CORS for frontend access / CORS für Frontend-Zugriff aktivieren
public class PaymentSimulationController {

    @Autowired
    private ApplicationRepository applicationRepository;

    /**
     * Updates the tuition fee payment status for an application
     * Aktualisiert den Studiengebühren-Zahlungsstatus für eine Bewerbung
     *
     * @param request Payment update request / Zahlungsaktualisierungsanfrage
     * @return Payment update response / Zahlungsaktualisierungsantwort
     */
    @PostMapping("/update-status")
    public ResponseEntity<PaymentUpdateResponse> updatePaymentStatus(@RequestBody PaymentUpdateRequest request) {

        System.out.println("=== PAYMENT SIMULATION API CALLED ===");
        System.out.println("Application ID: " + request.getApplicationId());
        System.out.println("Payment Status: " + request.isPaid());
        System.out.println("====================================");

        try {
            // Validate request / Anfrage validieren
            if (request.getApplicationId() == null) {
                return ResponseEntity.badRequest()
                        .body(new PaymentUpdateResponse(false, "Application ID is required / Bewerbungs-ID ist erforderlich", null));
            }

            // Find application / Bewerbung finden
            Optional<Application> applicationOpt = applicationRepository.findById(request.getApplicationId());

            if (applicationOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new PaymentUpdateResponse(false,
                                "Application not found / Bewerbung nicht gefunden: " + request.getApplicationId(),
                                null));
            }

            Application application = applicationOpt.get();

            // Check if application is in correct status for payment
            // Prüfen ob Bewerbung im korrekten Status für Zahlung ist
            if (!isValidForPayment(application)) {
                return ResponseEntity.badRequest()
                        .body(new PaymentUpdateResponse(false,
                                "Application is not in a valid status for payment. Current status: " + application.getStatus() +
                                        " / Bewerbung ist nicht im gültigen Status für Zahlung. Aktueller Status: " + application.getStatus(),
                                createPaymentInfo(application)));
            }

            // Store previous payment status / Vorherigen Zahlungsstatus speichern
            boolean previousStatus = application.isTuitionFeePaid();

            // Update payment status / Zahlungsstatus aktualisieren
            application.setTuitionFeePaid(request.isPaid());
            application.setUpdatedAt(LocalDateTime.now());

            // Save to database / In Datenbank speichern
            Application savedApplication = applicationRepository.save(application);

            // Create success response / Erfolgsantwort erstellen
            PaymentUpdateResponse response = new PaymentUpdateResponse(
                    true,
                    createSuccessMessage(savedApplication, previousStatus, request.isPaid()),
                    createPaymentInfo(savedApplication)
            );

            // Log payment update / Zahlungsaktualisierung protokollieren
            logPaymentUpdate(savedApplication, previousStatus, request.isPaid());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("=== ERROR IN PAYMENT SIMULATION API ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("======================================");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PaymentUpdateResponse(false,
                            "Internal server error / Interner Serverfehler: " + e.getMessage(),
                            null));
        }
    }

    /**
     * Gets the current payment status for an application
     * Holt den aktuellen Zahlungsstatus für eine Bewerbung
     */
    @GetMapping("/status/{applicationId}")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable Long applicationId) {

        System.out.println("=== PAYMENT STATUS CHECK ===");
        System.out.println("Application ID: " + applicationId);
        System.out.println("============================");

        try {
            Optional<Application> applicationOpt = applicationRepository.findById(applicationId);

            if (applicationOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new PaymentStatusResponse(false,
                                "Application not found / Bewerbung nicht gefunden: " + applicationId,
                                null));
            }

            Application application = applicationOpt.get();
            PaymentInfo paymentInfo = createPaymentInfo(application);

            return ResponseEntity.ok(new PaymentStatusResponse(true, "Success / Erfolgreich", paymentInfo));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PaymentStatusResponse(false,
                            "Error retrieving payment status / Fehler beim Abrufen des Zahlungsstatus: " + e.getMessage(),
                            null));
        }
    }

    /**
     * Simulates payment for all accepted applications (bulk operation)
     * Simuliert Zahlung für alle angenommenen Bewerbungen (Massenoperation)
     */
    @PostMapping("/simulate-bulk-payment")
    public ResponseEntity<BulkPaymentResponse> simulateBulkPayment(@RequestBody BulkPaymentRequest request) {

        System.out.println("=== BULK PAYMENT SIMULATION ===");
        System.out.println("Payment Status: " + request.isPaid());
        System.out.println("===============================");

        try {
            // Find all accepted applications / Alle angenommenen Bewerbungen finden
            var acceptedApplications = applicationRepository.findByStatus(Application.ApplicationStatus.ACCEPTED);

            int updatedCount = 0;
            StringBuilder results = new StringBuilder();

            for (Application application : acceptedApplications) {
                boolean previousStatus = application.isTuitionFeePaid();
                application.setTuitionFeePaid(request.isPaid());
                application.setUpdatedAt(LocalDateTime.now());

                applicationRepository.save(application);
                updatedCount++;

                results.append(String.format("ID %d: %s -> %s\n",
                        application.getId(),
                        previousStatus ? "PAID" : "UNPAID",
                        request.isPaid() ? "PAID" : "UNPAID"));
            }

            String message = String.format(
                    "Bulk payment simulation completed. Updated %d applications.\n" +
                            "Massenzahlungssimulation abgeschlossen. %d Bewerbungen aktualisiert.\n\n%s",
                    updatedCount, updatedCount, results.toString()
            );

            return ResponseEntity.ok(new BulkPaymentResponse(true, message, updatedCount));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BulkPaymentResponse(false,
                            "Error in bulk payment simulation / Fehler bei Massenzahlungssimulation: " + e.getMessage(),
                            0));
        }
    }

    // ===== HELPER METHODS / HILFSMETHODEN =====

    /**
     * Checks if application is in valid status for payment
     * Prüft ob Bewerbung im gültigen Status für Zahlung ist
     */
    private boolean isValidForPayment(Application application) {
        return application.getStatus() == Application.ApplicationStatus.ACCEPTED ||
                application.getStatus() == Application.ApplicationStatus.ENROLLED;
    }

    /**
     * Creates success message / Erstellt Erfolgsnachricht
     */
    private String createSuccessMessage(Application application, boolean previousStatus, boolean newStatus) {
        String statusChange = String.format("%s -> %s",
                previousStatus ? "PAID/BEZAHLT" : "UNPAID/UNBEZAHLT",
                newStatus ? "PAID/BEZAHLT" : "UNPAID/UNBEZAHLT");

        return String.format(
                "Payment status updated successfully for %s %s (ID: %d). Status: %s\n" +
                        "Zahlungsstatus erfolgreich aktualisiert für %s %s (ID: %d). Status: %s",
                application.getFirstName(), application.getLastName(), application.getId(), statusChange,
                application.getFirstName(), application.getLastName(), application.getId(), statusChange
        );
    }

    /**
     * Creates payment info object / Erstellt Zahlungsinfo-Objekt
     */
    private PaymentInfo createPaymentInfo(Application application) {
        return new PaymentInfo(
                application.getId(),
                application.getFirstName() + " " + application.getLastName(),
                application.getEmail(),
                application.getStudyProgram().getName(),
                application.getStatus().toString(),
                application.isTuitionFeePaid(),
                application.getUpdatedAt() != null ?
                        application.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")) :
                        "N/A"
        );
    }

    /**
     * Logs payment update / Protokolliert Zahlungsaktualisierung
     */
    private void logPaymentUpdate(Application application, boolean previousStatus, boolean newStatus) {
        System.out.println("=== PAYMENT STATUS UPDATED ===");
        System.out.println("Application ID: " + application.getId());
        System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
        System.out.println("Email: " + application.getEmail());
        System.out.println("Study Program: " + application.getStudyProgram().getName());
        System.out.println("Previous Payment Status: " + (previousStatus ? "PAID" : "UNPAID"));
        System.out.println("New Payment Status: " + (newStatus ? "PAID" : "UNPAID"));
        System.out.println("Application Status: " + application.getStatus());
        System.out.println("Updated At: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        System.out.println("==============================");
    }

    // ===== DTO CLASSES / DTO KLASSEN =====

    /**
     * Request DTO for payment update / Anfrage-DTO für Zahlungsaktualisierung
     */
    public static class PaymentUpdateRequest {
        private Long applicationId;
        private boolean paid;

        // Constructors
        public PaymentUpdateRequest() {}

        public PaymentUpdateRequest(Long applicationId, boolean paid) {
            this.applicationId = applicationId;
            this.paid = paid;
        }

        // Getters and Setters
        public Long getApplicationId() { return applicationId; }
        public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
        public boolean isPaid() { return paid; }
        public void setPaid(boolean paid) { this.paid = paid; }
    }

    /**
     * Response DTO for payment update / Antwort-DTO für Zahlungsaktualisierung
     */
    public static class PaymentUpdateResponse {
        private boolean success;
        private String message;
        private PaymentInfo paymentInfo;

        public PaymentUpdateResponse(boolean success, String message, PaymentInfo paymentInfo) {
            this.success = success;
            this.message = message;
            this.paymentInfo = paymentInfo;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public PaymentInfo getPaymentInfo() { return paymentInfo; }
        public void setPaymentInfo(PaymentInfo paymentInfo) { this.paymentInfo = paymentInfo; }
    }

    /**
     * Response DTO for payment status check / Antwort-DTO für Zahlungsstatusprüfung
     */
    public static class PaymentStatusResponse {
        private boolean success;
        private String message;
        private PaymentInfo paymentInfo;

        public PaymentStatusResponse(boolean success, String message, PaymentInfo paymentInfo) {
            this.success = success;
            this.message = message;
            this.paymentInfo = paymentInfo;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public PaymentInfo getPaymentInfo() { return paymentInfo; }
        public void setPaymentInfo(PaymentInfo paymentInfo) { this.paymentInfo = paymentInfo; }
    }

    /**
     * Request DTO for bulk payment / Anfrage-DTO für Massenzahlung
     */
    public static class BulkPaymentRequest {
        private boolean paid;

        public BulkPaymentRequest() {}
        public BulkPaymentRequest(boolean paid) { this.paid = paid; }

        public boolean isPaid() { return paid; }
        public void setPaid(boolean paid) { this.paid = paid; }
    }

    /**
     * Response DTO for bulk payment / Antwort-DTO für Massenzahlung
     */
    public static class BulkPaymentResponse {
        private boolean success;
        private String message;
        private int updatedCount;

        public BulkPaymentResponse(boolean success, String message, int updatedCount) {
            this.success = success;
            this.message = message;
            this.updatedCount = updatedCount;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getUpdatedCount() { return updatedCount; }
        public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }
    }

    /**
     * Payment information DTO / Zahlungsinformations-DTO
     */
    public static class PaymentInfo {
        private Long applicationId;
        private String applicantName;
        private String email;
        private String studyProgram;
        private String applicationStatus;
        private boolean tuitionFeePaid;
        private String lastUpdated;

        public PaymentInfo(Long applicationId, String applicantName, String email,
                           String studyProgram, String applicationStatus,
                           boolean tuitionFeePaid, String lastUpdated) {
            this.applicationId = applicationId;
            this.applicantName = applicantName;
            this.email = email;
            this.studyProgram = studyProgram;
            this.applicationStatus = applicationStatus;
            this.tuitionFeePaid = tuitionFeePaid;
            this.lastUpdated = lastUpdated;
        }

        // Getters and Setters
        public Long getApplicationId() { return applicationId; }
        public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
        public String getApplicantName() { return applicantName; }
        public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getStudyProgram() { return studyProgram; }
        public void setStudyProgram(String studyProgram) { this.studyProgram = studyProgram; }
        public String getApplicationStatus() { return applicationStatus; }
        public void setApplicationStatus(String applicationStatus) { this.applicationStatus = applicationStatus; }
        public boolean isTuitionFeePaid() { return tuitionFeePaid; }
        public void setTuitionFeePaid(boolean tuitionFeePaid) { this.tuitionFeePaid = tuitionFeePaid; }
        public String getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
    }
}
