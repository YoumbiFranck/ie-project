package com.ie_project.workflow.delegate;

import com.ie_project.workflow.entity.Application;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Camunda Delegate für das NC-Auswahlverfahren mit Geschlechterquoten
 * Camunda Delegate for NC selection process with gender quotas
 *
 * This delegate applies the final selection logic for Numerus Clausus programs,
 * considering both ranking and gender quota requirements.
 *
 * Dieser Delegate wendet die finale Auswahllogik für NC-Studiengänge an,
 * unter Berücksichtigung von Rangfolge und Geschlechterquoten-Anforderungen.
 *
 * @author IE Project Team
 */
@Component("ncSelectionDelegate")
public class NCSelectionDelegate implements JavaDelegate {

    @Value("${application.nc.gender-quota.enabled:true}")
    private boolean genderQuotaEnabled;

    @Value("${application.nc.gender-quota.minimum-per-gender:1}")
    private int minimumPerGender;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== NC SELECTION DELEGATE EXECUTED ===");

        try {
            // Get ranking information from previous step
            // Ranking-Informationen aus vorherigem Schritt holen
            Long applicationId = getApplicationId(execution);
            Integer currentRank = getIntegerVariable(execution, "currentApplicationRank");
            Double currentGrade = getDoubleVariable(execution, "currentApplicationGrade");
            Integer totalApplications = getIntegerVariable(execution, "totalApplicationsWithGrades");
            Integer maxStudents = getIntegerVariable(execution, "maxStudents");
            String studyProgramName = getStringVariable(execution, "studyProgramName");

            // Get current application information
            // Aktuelle Bewerbungs-Informationen holen
            String applicantName = getStringVariable(execution, "firstName") + " " + getStringVariable(execution, "lastName");
            String applicantEmail = getStringVariable(execution, "email");
            String applicantSex = getStringVariable(execution, "sex");

            System.out.println("=== NC SELECTION INPUT DATA ===");
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + applicantName + " (" + applicantSex + ")");
            System.out.println("Current Rank: " + currentRank + " / " + totalApplications);
            System.out.println("Current Grade: " + currentGrade);
            System.out.println("Available Seats: " + maxStudents);
            System.out.println("Gender Quota Enabled: " + genderQuotaEnabled);
            System.out.println("Minimum Per Gender: " + minimumPerGender);
            System.out.println("==============================");

            // Basic ranking check / Grundlegende Ranking-Prüfung
            boolean admittedByRank = currentRank <= maxStudents;

            // Apply gender quota logic if enabled
            // Geschlechterquoten-Logik anwenden falls aktiviert
            boolean admittedByQuota = false;
            String admissionReason;

            if (genderQuotaEnabled && !admittedByRank) {
                // Check if applicant can be admitted due to gender quota
                // Prüfen ob Bewerber aufgrund Geschlechterquote zugelassen werden kann
                admittedByQuota = applyGenderQuotaLogic(applicantSex, currentRank, maxStudents);
            }

            // Final admission decision / Finale Zulassungsentscheidung
            boolean isAdmitted = admittedByRank || admittedByQuota;

            if (isAdmitted) {
                if (admittedByRank) {
                    admissionReason = "RANK_BASED";
                } else {
                    admissionReason = "GENDER_QUOTA";
                }
            } else {
                admissionReason = "INSUFFICIENT_RANK";
            }

            // Set process variables for gateway decision
            // Prozessvariablen für Gateway-Entscheidung setzen
            execution.setVariable("ncAdmissionDecision", isAdmitted ? "ACCEPTED" : "REJECTED");
            execution.setVariable("ncAdmissionReason", admissionReason);
            execution.setVariable("ncSelectionCompleted", true);
            execution.setVariable("finalRank", currentRank);
            execution.setVariable("finalGrade", currentGrade);
            execution.setVariable("admittedByQuota", admittedByQuota);
            execution.setVariable("selectionProcessedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

            if (isAdmitted) {
                execution.setVariable("nextProcessStep", "ADMISSION_LETTER");
            } else {
                execution.setVariable("nextProcessStep", "NC_REJECTION");
            }

            // Create detailed selection report / Detaillierten Auswahlbericht erstellen
            String selectionReport = createSelectionReport(
                    applicantName, applicantEmail, applicantSex, studyProgramName,
                    currentRank, currentGrade, totalApplications, maxStudents,
                    isAdmitted, admissionReason, admittedByQuota
            );

            execution.setVariable("ncSelectionReport", selectionReport);

            // Log selection decision / Auswahlentscheidung protokollieren
            System.out.println("=== NC SELECTION DECISION ===");
            System.out.println(selectionReport);
            System.out.println("=============================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== NC SELECTION PROCESSING COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + applicantName);
            System.out.println("Final Decision: " + (isAdmitted ? "ACCEPTED" : "REJECTED"));
            System.out.println("Admission Reason: " + admissionReason);
            System.out.println("Admitted by Quota: " + admittedByQuota);
            System.out.println("Next Step: " + (isAdmitted ? "ADMISSION_LETTER" : "NC_REJECTION"));
            System.out.println("=========================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN NC SELECTION DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("======================================");
            throw e;
        }
    }

    /**
     * Wendet die Geschlechterquoten-Logik an
     * Applies gender quota logic
     *
     * Simplified logic for demonstration:
     * - If rank is close to limit and gender diversity is needed, admit
     * - In real implementation, this would check actual gender distribution
     */
    private boolean applyGenderQuotaLogic(String applicantSex, int currentRank, int maxStudents) {

        System.out.println("=== APPLYING GENDER QUOTA LOGIC ===");
        System.out.println("Applicant Sex: " + applicantSex);
        System.out.println("Current Rank: " + currentRank);
        System.out.println("Max Students: " + maxStudents);

        // Simplified quota logic for demonstration
        // In reality, this would check the actual gender distribution of already admitted students

        boolean quotaAdmission = false;

        // If applicant is within a reasonable range beyond the seat limit (e.g., +2 positions)
        // and quota is needed, admit them
        if (currentRank <= maxStudents + 2) {

            // Simplified logic: assume quota is needed for underrepresented gender
            // In real implementation, check actual statistics
            if ("F".equals(applicantSex) || "D".equals(applicantSex)) {
                quotaAdmission = true;
                System.out.println(" Gender quota applied: Female/Diverse candidate admitted");
            } else if ("M".equals(applicantSex) && minimumPerGender > 0) {
                // This would need more complex logic in reality
                quotaAdmission = false;
                System.out.println(" Gender quota not applicable for male candidate");
            }
        }

        System.out.println("Quota Admission Result: " + quotaAdmission);
        System.out.println("==================================");

        return quotaAdmission;
    }

    /**
     * Erstellt einen detaillierten Auswahlbericht
     * Creates a detailed selection report
     */
    private String createSelectionReport(String applicantName, String applicantEmail, String applicantSex,
                                         String studyProgramName, int currentRank, double currentGrade,
                                         int totalApplications, int maxStudents, boolean isAdmitted,
                                         String admissionReason, boolean admittedByQuota) {

        StringBuilder report = new StringBuilder();

        // Header / Kopfzeile
        report.append("=== NC AUSWAHLVERFAHREN ERGEBNIS / NC SELECTION RESULT ===\n\n");

        // Applicant information / Bewerber-Informationen
        report.append("Bewerber / Applicant: ").append(applicantName).append("\n");
        report.append("E-Mail: ").append(applicantEmail).append("\n");
        report.append("Geschlecht / Gender: ").append(applicantSex).append("\n");
        report.append("Studiengang / Study Program: ").append(studyProgramName).append("\n\n");

        // Selection data / Auswahldaten
        report.append("=== AUSWAHLDATEN / SELECTION DATA ===\n");
        report.append("Abiturnote / High School Grade: ").append(String.format("%.1f", currentGrade)).append("\n");
        report.append("Rangplatz / Rank: ").append(currentRank).append(" von / of ").append(totalApplications).append("\n");
        report.append("Verfügbare Plätze / Available Seats: ").append(maxStudents).append("\n");
        report.append("Geschlechterquote aktiv / Gender Quota Active: ").append(genderQuotaEnabled ? "Ja/Yes" : "Nein/No").append("\n\n");

        // Decision / Entscheidung
        report.append("=== ZULASSUNGSENTSCHEIDUNG / ADMISSION DECISION ===\n");
        if (isAdmitted) {
            report.append(" ZUGELASSEN / ADMITTED\n\n");

            switch (admissionReason) {
                case "RANK_BASED":
                    report.append("Grund / Reason: Zulassung aufgrund Rangplatz\n");
                    break;
                case "GENDER_QUOTA":
                    report.append("Grund / Reason: Zulassung aufgrund Geschlechterquote\n");
                    report.append(" Diversitätsförderung / Diversity promotion\n");
                    break;
            }
        } else {
            report.append(" ABGELEHNT / REJECTED\n\n");
            report.append("Grund / Reason: Rangplatz reicht nicht aus\n");
            report.append("Rangplatz ").append(currentRank).append(" > ").append(maxStudents).append(" verfügbare Plätze\n");
            report.append("Rank ").append(currentRank).append(" > ").append(maxStudents).append(" available seats\n");

            if (genderQuotaEnabled) {
                report.append("Geschlechterquote konnte nicht angewendet werden\n");
            }
        }

        // Next steps / Nächste Schritte
        report.append("\n=== NÄCHSTE SCHRITTE / NEXT STEPS ===\n");
        if (isAdmitted) {
            report.append("1. Zulassungsbescheid wird versendet\n");
        } else {
            report.append("1. Ablehnungsbescheid wird versendet\n");
        }

        // Footer / Fußzeile
        report.append("\n=== UNIVERSITÄT RIEDTAL - NC AUSWAHLVERFAHREN ===");

        return report.toString();
    }

    /**
     * Helper methods for type-safe variable retrieval
     */
    private Long getApplicationId(DelegateExecution execution) {
        Object applicationIdObj = execution.getVariable("applicationId");

        if (applicationIdObj == null) {
            throw new IllegalArgumentException("Application ID not found in process variables");
        }

        if (applicationIdObj instanceof Long) {
            return (Long) applicationIdObj;
        } else if (applicationIdObj instanceof String) {
            try {
                return Long.parseLong((String) applicationIdObj);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert applicationId to Long: " + applicationIdObj);
            }
        } else if (applicationIdObj instanceof Integer) {
            return ((Integer) applicationIdObj).longValue();
        } else {
            throw new IllegalArgumentException("Unexpected type for applicationId: " + applicationIdObj.getClass());
        }
    }

    private Integer getIntegerVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);

        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private Double getDoubleVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);

        if (value == null) {
            return null;
        }

        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Float) {
            return ((Float) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private String getStringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);
        return value != null ? value.toString() : null;
    }
}
