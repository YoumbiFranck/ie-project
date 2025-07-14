package com.ie_project.workflow.delegate;

import com.ie_project.workflow.entity.Application;
import com.ie_project.workflow.entity.StudyProgram;
import com.ie_project.workflow.repository.ApplicationRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Camunda Delegate für die Berechnung der NC-Rangfolge
 * Camunda Delegate for calculating NC (Numerus Clausus) ranking
 *
 * This delegate calculates the ranking of all applications for a specific
 * study program based on high school grades (Abiturnote).
 *
 * Dieser Delegate berechnet die Rangfolge aller Bewerbungen für einen
 * bestimmten Studiengang basierend auf der Abiturnote.
 *
 * @author IE Project Team
 */
@Component("ncRankingDelegate")
public class NCRankingDelegate implements JavaDelegate {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        System.out.println("=== NC RANKING DELEGATE EXECUTED ===");

        try {
            // Get current application and study program information
            // Aktuelle Bewerbung und Studiengang-Informationen holen
            Long applicationId = getApplicationId(execution);
            Long studyProgramId = getLongVariable(execution, "studyProgramId");
            String studyProgramName = getStringVariable(execution, "studyProgramName");
            String studyProgramCode = getStringVariable(execution, "studyProgramCode");

            System.out.println("Current Application ID: " + applicationId);
            System.out.println("Study Program ID: " + studyProgramId);

            // Get current application / Aktuelle Bewerbung holen
            Application currentApplication = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found / Bewerbung nicht gefunden: " + applicationId));

            // Get all applications for this study program that are not rejected
            // Alle Bewerbungen für diesen Studiengang holen, die nicht abgelehnt wurden
            List<Application> allApplications = applicationRepository.findByStudyProgramIdAndStatusNot(
                    studyProgramId, Application.ApplicationStatus.REJECTED);

            // Filter applications that have high school grades
            // Bewerbungen filtern, die Abiturnoten haben
            List<Application> applicationsWithGrades = allApplications.stream()
                    .filter(app -> app.getHighSchoolGrade() != null)
                    .collect(Collectors.toList());

            // Sort applications by grade (ascending: 1.0 is best, 4.0 is worst)
            // Bewerbungen nach Note sortieren (aufsteigend: 1,0 ist beste, 4,0 ist schlechteste Note)
            applicationsWithGrades.sort((app1, app2) ->
                    app1.getHighSchoolGrade().compareTo(app2.getHighSchoolGrade()));

            // Calculate ranking for current application / Rangplatz für aktuelle Bewerbung berechnen
            int currentApplicationRank = -1;
            BigDecimal currentApplicationGrade = currentApplication.getHighSchoolGrade();

            if (currentApplicationGrade == null) {
                throw new IllegalStateException("Current application has no high school grade / Aktuelle Bewerbung hat keine Abiturnote");
            }

            // Find rank of current application / Rangplatz der aktuellen Bewerbung finden
            for (int i = 0; i < applicationsWithGrades.size(); i++) {
                if (applicationsWithGrades.get(i).getId().equals(applicationId)) {
                    currentApplicationRank = i + 1; // Rank starts from 1
                    break;
                }
            }

            if (currentApplicationRank == -1) {
                throw new IllegalStateException("Current application not found in ranking / Aktuelle Bewerbung nicht in Rangfolge gefunden");
            }

            // Get study program information for available seats
            // Studiengang-Informationen für verfügbare Plätze holen
            StudyProgram studyProgram = currentApplication.getStudyProgram();
            Integer maxStudents = studyProgram.getMaxStudents();

            if (maxStudents == null) {
                throw new IllegalStateException("Study program has no seat limit defined / Studiengang hat keine Platzgrenze definiert");
            }

            // Set process variables for NC selection / Prozessvariablen für NC-Auswahl setzen
            execution.setVariable("ncRankingCompleted", true);
            execution.setVariable("currentApplicationRank", currentApplicationRank);
            execution.setVariable("currentApplicationGrade", currentApplicationGrade.doubleValue());
            execution.setVariable("totalApplicationsWithGrades", applicationsWithGrades.size());
            execution.setVariable("maxStudents", maxStudents);
            execution.setVariable("rankingCalculatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            execution.setVariable("nextProcessStep", "NC_SELECTION");

            // Create detailed ranking report / Detaillierten Ranking-Bericht erstellen
            String rankingReport = createRankingReport(
                    currentApplication, applicationsWithGrades, currentApplicationRank, maxStudents,
                    studyProgramName, studyProgramCode
            );

            execution.setVariable("ncRankingReport", rankingReport);

            // Log ranking calculation / Ranking-Berechnung protokollieren
            System.out.println("=== NC RANKING CALCULATION COMPLETED ===");
            System.out.println(rankingReport);
            System.out.println("========================================");

            // Log execution details / Ausführungsdetails protokollieren
            System.out.println("=== NC RANKING PROCESSING COMPLETED ===");
            System.out.println("Process Instance ID: " + execution.getProcessInstanceId());
            System.out.println("Activity ID: " + execution.getCurrentActivityId());
            System.out.println("Application ID: " + applicationId);
            System.out.println("Applicant: " + currentApplication.getFirstName() + " " + currentApplication.getLastName());
            System.out.println("Study Program: " + studyProgramName + " (" + studyProgramCode + ")");
            System.out.println("Current Rank: " + currentApplicationRank + " / " + applicationsWithGrades.size());
            System.out.println("Current Grade: " + currentApplicationGrade);
            System.out.println("Available Seats: " + maxStudents);
            System.out.println("Next Step: NC_SELECTION");
            System.out.println("=======================================");

        } catch (Exception e) {
            System.err.println("=== ERROR IN NC RANKING DELEGATE ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("====================================");
            throw e;
        }
    }

    /**
     * Erstellt einen detaillierten Ranking-Bericht
     * Creates a detailed ranking report
     */
    private String createRankingReport(Application currentApplication, List<Application> applicationsWithGrades,
                                       int currentRank, int maxStudents, String studyProgramName, String studyProgramCode) {

        StringBuilder report = new StringBuilder();

        // Header / Kopfzeile
        report.append("=== NC RANKING BERECHNUNG / NC RANKING CALCULATION ===\n\n");

        // Current application info / Info zur aktuellen Bewerbung
        report.append("Bewerber / Applicant: ").append(currentApplication.getFirstName())
                .append(" ").append(currentApplication.getLastName()).append("\n");
        report.append("E-Mail: ").append(currentApplication.getEmail()).append("\n");
        report.append("Bewerbungs-ID / Application ID: ").append(currentApplication.getId()).append("\n");
        report.append("Studiengang / Study Program: ").append(studyProgramName)
                .append(" (").append(studyProgramCode).append(")\n\n");

        // Ranking information / Ranking-Informationen
        report.append("=== RANKING ERGEBNIS / RANKING RESULT ===\n");
        report.append("Abiturnote / High School Grade: ").append(currentApplication.getHighSchoolGrade()).append("\n");
        report.append("Rangplatz / Rank: ").append(currentRank).append(" von / of ").append(applicationsWithGrades.size()).append("\n");
        report.append("Verfügbare Plätze / Available Seats: ").append(maxStudents).append("\n");

        // Preliminary assessment / Vorläufige Einschätzung
        report.append("\n=== VORLÄUFIGE EINSCHÄTZUNG / PRELIMINARY ASSESSMENT ===\n");
        if (currentRank <= maxStudents) {
            report.append(" Gute Chance auf Zulassung / Good chance for admission\n");
            report.append(" Rangplatz innerhalb der verfügbaren Plätze\n");
        } else {
            report.append(" Zulassung unwahrscheinlich / Admission unlikely\n");
            report.append(" Rangplatz außerhalb der verfügbaren Plätze\n");

        }

        // Note about quota system / Hinweis zum Quotensystem
        report.append("\n=== WICHTIGER HINWEIS / IMPORTANT NOTE ===\n");
        report.append("Die endgültige Zulassung berücksichtigt auch Geschlechterquoten.\n");
        report.append("Das finale Auswahlverfahren folgt im nächsten Schritt.\n");

        // Top 10 ranking overview / Top 10 Ranking-Übersicht
        report.append("\n=== TOP 10 RANKING ÜBERSICHT / TOP 10 RANKING OVERVIEW ===\n");
        int displayCount = Math.min(10, applicationsWithGrades.size());
        for (int i = 0; i < displayCount; i++) {
            Application app = applicationsWithGrades.get(i);
            String marker = app.getId().equals(currentApplication.getId()) ? " ← SIE/YOU" : "";
            report.append(String.format("%2d. %s %s (%.1f)%s\n",
                    i + 1,
                    app.getFirstName(),
                    app.getLastName(),
                    app.getHighSchoolGrade(),
                    marker));
        }

        if (applicationsWithGrades.size() > 10) {
            report.append("... (").append(applicationsWithGrades.size() - 10).append(" weitere Bewerbungen / additional applications)\n");
        }



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

    private Long getLongVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);

        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else {
            return null;
        }
    }

    private String getStringVariable(DelegateExecution execution, String variableName) {
        Object value = execution.getVariable(variableName);
        return value != null ? value.toString() : null;
    }
}
