package com.ie_project.workflow.service;

import com.ie_project.workflow.entity.Application;
import com.ie_project.workflow.entity.Student;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service für das Versenden von Benachrichtigungen
 * Service for sending notifications
 *
 * This service handles all types of notifications sent during the enrollment process.
 * In a production environment, this would integrate with email services, SMS providers,
 * and other communication channels.
 *
 * Dieser Service behandelt alle Arten von Benachrichtigungen, die während des
 * Immatrikulationsprozesses versendet werden. In einer Produktionsumgebung würde
 * dieser Service mit E-Mail-Diensten, SMS-Anbietern und anderen Kommunikationskanälen integriert.
 *
 * @author IE Project Team
 */
@Service
public class NotificationService {

    /**
     * Versendet das Willkommenspaket an einen neuen Studenten
     * Sends the welcome package to a new student
     *
     * @param student Der neue Student / The new student
     * @param welcomePackageContent Der Inhalt des Willkommenspakets / The welcome package content
     * @return true wenn erfolgreich versendet / true if successfully sent
     */
    public boolean sendWelcomePackage(Student student, String welcomePackageContent) {

        if (student == null) {
            System.err.println("ERROR: Cannot send welcome package - student is null");
            return false;
        }

        if (welcomePackageContent == null || welcomePackageContent.trim().isEmpty()) {
            System.err.println("ERROR: Cannot send welcome package - content is null or empty");
            return false;
        }

        try {
            System.out.println("=== SENDING WELCOME PACKAGE ===");
            System.out.println("Recipient: " + student.getFullName());
            System.out.println("Email: " + student.getEmail());
            System.out.println("Student Number: " + student.getStudentNumber());
            System.out.println("Study Program: " + student.getStudyProgram().getName());
            System.out.println("Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            System.out.println("===============================");

            // In production: Send actual email via SMTP service
            // In Produktion: Tatsächliche E-Mail über SMTP-Service senden
            boolean emailSent = sendEmail(student.getEmail(), "Willkommen an der Universität Riedtal / Welcome to University Riedtal", welcomePackageContent);

            // Optional: Send SMS notification
            // Optional: SMS-Benachrichtigung senden
            boolean smsSent = sendSMS(student, "Willkommen an der Uni Riedtal! Ihr Willkommenspaket wurde per E-Mail versendet. / Welcome to Uni Riedtal! Your welcome package has been sent via email.");

            // Log the complete welcome package / Vollständiges Willkommenspaket protokollieren
            System.out.println("=== COMPLETE WELCOME PACKAGE CONTENT ===");
            System.out.println(welcomePackageContent);
            System.out.println("======================================");

            // Create delivery confirmation / Zustellungsbestätigung erstellen
            String deliveryConfirmation = createDeliveryConfirmation(student, emailSent, smsSent);
            System.out.println(deliveryConfirmation);

            return emailSent; // Main success indicator is email delivery

        } catch (Exception e) {
            System.err.println("ERROR sending welcome package to " + student.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sendet eine E-Mail (Simulation für Entwicklung)
     * Sends an email (simulation for development)
     *
     * @param email Die E-Mail-Adresse / The email address
     * @param subject Der Betreff / The subject
     * @param content Der Inhalt / The content
     * @return true wenn erfolgreich / true if successful
     */
    public boolean sendEmail(String email, String subject, String content) {

        try {
            System.out.println("=== EMAIL SIMULATION ===");
            System.out.println("TO: " + email);
            System.out.println("SUBJECT: " + subject);
            System.out.println("CONTENT LENGTH: " + content.length() + " characters");
            System.out.println("TIMESTAMP: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

            // Simulate email sending delay / E-Mail-Versand-Verzögerung simulieren
            Thread.sleep(500);

            System.out.println("STATUS: EMAIL SENT SUCCESSFULLY");
            System.out.println("========================");

            return true;

        } catch (Exception e) {
            System.err.println("ERROR:  EMAIL SENDING FAILED");
            System.err.println("Reason: " + e.getMessage());
            System.err.println("========================");
            return false;
        }
    }

    /**
     * Sendet eine SMS-Benachrichtigung (Simulation für Entwicklung)
     * Sends an SMS notification (simulation for development)
     *
     * @param student Der Student / The student
     * @param message Die Nachricht / The message
     * @return true wenn erfolgreich / true if successful
     */
    public boolean sendSMS(Student student, String message) {

        try {
            // In production: Extract phone number from student profile
            // In Produktion: Telefonnummer aus Studentenprofil extrahieren
            String phoneNumber = extractPhoneNumber(student);

            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                System.out.println("=== SMS NOTIFICATION SKIPPED ===");
                System.out.println("Reason: No phone number available for " + student.getFullName());
                System.out.println("================================");
                return false;
            }

            System.out.println("=== SMS SIMULATION ===");
            System.out.println("TO: " + phoneNumber);
            System.out.println("RECIPIENT: " + student.getFullName());
            System.out.println("MESSAGE: " + message);
            System.out.println("TIMESTAMP: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));

            // Simulate SMS sending delay / SMS-Versand-Verzögerung simulieren
            Thread.sleep(300);

            System.out.println("STATUS: SMS SENT SUCCESSFULLY");
            System.out.println("======================");

            return true;

        } catch (Exception e) {
            System.err.println("ERROR:  SMS SENDING FAILED");
            System.err.println("Reason: " + e.getMessage());
            System.err.println("======================");
            return false;
        }
    }

    /**
     * Sendet eine Admission Letter E-Mail
     * Sends an admission letter email
     *
     * @param application Die Bewerbung / The application
     * @param admissionContent Der Zulassungsinhalt / The admission content
     * @return true wenn erfolgreich / true if successful
     */
    public boolean sendAdmissionLetter(Application application, String admissionContent) {

        if (application == null || admissionContent == null) {
            return false;
        }

        String subject = "Zulassungsbescheid - Universität Riedtal / Admission Letter - University Riedtal";

        System.out.println("=== SENDING ADMISSION LETTER ===");
        System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
        System.out.println("Program: " + application.getStudyProgram().getName());

        return sendEmail(application.getEmail(), subject, admissionContent);
    }

    /**
     * Sendet eine Ablehnungsbenachrichtigung
     * Sends a rejection notification
     *
     * @param application Die Bewerbung / The application
     * @param rejectionContent Der Ablehnungsinhalt / The rejection content
     * @param rejectionReason Der Ablehnungsgrund / The rejection reason
     * @return true wenn erfolgreich / true if successful
     */
    public boolean sendRejectionNotification(Application application, String rejectionContent, String rejectionReason) {

        if (application == null || rejectionContent == null) {
            return false;
        }

        String subject = "Bewerbungsbescheid - Universität Riedtal / Application Decision - University Riedtal";

        System.out.println("=== SENDING REJECTION NOTIFICATION ===");
        System.out.println("Applicant: " + application.getFirstName() + " " + application.getLastName());
        System.out.println("Program: " + application.getStudyProgram().getName());
        System.out.println("Reason: " + rejectionReason);

        return sendEmail(application.getEmail(), subject, rejectionContent);
    }

    /**
     * Sendet eine Zahlungserinnerung
     * Sends a payment reminder
     *
     * @param application Die Bewerbung / The application
     * @param reminderContent Der Erinnerungsinhalt / The reminder content
     * @return true wenn erfolgreich / true if successful
     */
    public boolean sendPaymentReminder(Application application, String reminderContent) {

        if (application == null || reminderContent == null) {
            return false;
        }

        String subject = "Zahlungserinnerung - Semesterbeitrag / Payment Reminder - Semester Fee";

        System.out.println("=== SENDING PAYMENT REMINDER ===");
        System.out.println("Student: " + application.getFirstName() + " " + application.getLastName());
        System.out.println("Program: " + application.getStudyProgram().getName());

        // Also send SMS for urgent payment reminders / Auch SMS für dringende Zahlungserinnerungen
        String smsMessage = "Zahlungserinnerung: Bitte überweisen Sie den Semesterbeitrag bis zum angegebenen Datum. / Payment reminder: Please transfer the semester fee by the specified date.";
        sendSMS(convertApplicationToStudent(application), smsMessage);

        return sendEmail(application.getEmail(), subject, reminderContent);
    }

    /**
     * Sendet eine Prüfungseinladung
     * Sends an exam invitation
     *
     * @param application Die Bewerbung / The application
     * @param examContent Der Prüfungsinhalt / The exam content
     * @return true wenn erfolgreich / true if successful
     */
    public boolean sendExamInvitation(Application application, String examContent) {

        if (application == null || examContent == null) {
            return false;
        }

        String subject = "Einladung zur Aufnahmeprüfung / Invitation to Entrance Exam";

        System.out.println("=== SENDING EXAM INVITATION ===");
        System.out.println("Candidate: " + application.getFirstName() + " " + application.getLastName());
        System.out.println("Program: " + application.getStudyProgram().getName());

        return sendEmail(application.getEmail(), subject, examContent);
    }

    /**
     * Erstellt eine Zustellungsbestätigung
     * Creates a delivery confirmation
     */
    private String createDeliveryConfirmation(Student student, boolean emailSent, boolean smsSent) {

        StringBuilder confirmation = new StringBuilder();

        confirmation.append("=== ZUSTELLUNGSBESTÄTIGUNG / DELIVERY CONFIRMATION ===\n");
        confirmation.append("Empfänger / Recipient: ").append(student.getFullName()).append("\n");
        confirmation.append("Matrikelnummer / Student Number: ").append(student.getStudentNumber()).append("\n");
        confirmation.append("E-Mail: ").append(student.getEmail()).append("\n");
        confirmation.append("Zeitstempel / Timestamp: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))).append("\n\n");

        confirmation.append("Versandstatus / Delivery Status:\n");
        confirmation.append(" E-Mail: ").append(emailSent ? " ERFOLGREICH / SUCCESSFUL" : " FEHLGESCHLAGEN / FAILED").append("\n");
        confirmation.append(" SMS: ").append(smsSent ? " ERFOLGREICH / SUCCESSFUL" : " FEHLGESCHLAGEN / FAILED").append("\n\n");

        if (emailSent) {
            confirmation.append(" Willkommenspaket erfolgreich zugestellt\n");
            confirmation.append(" Welcome package successfully delivered\n");
        } else {
            confirmation.append(" Fehler beim Versand des Willkommenspakets\n");
            confirmation.append(" Error delivering welcome package\n");
        }

        confirmation.append("=====================================================");

        return confirmation.toString();
    }

    /**
     * Extrahiert die Telefonnummer eines Studenten (Simulation)
     * Extracts a student's phone number (simulation)
     */
    private String extractPhoneNumber(Student student) {
        // In production: Get from student profile or application data
        // In Produktion: Aus Studentenprofil oder Bewerbungsdaten holen

        // Simulate phone number based on student ID for demo purposes
        // Telefonnummer basierend auf Student-ID für Demo-Zwecke simulieren
        if (student.getId() != null) {
            return "+49 123 " + String.format("%03d", student.getId() % 1000) + "-" + String.format("%04d", (student.getId() * 7) % 10000);
        }

        return null; // No phone number available
    }

    /**
     * Konvertiert eine Application zu einem temporären Student für SMS-Zwecke
     * Converts an Application to a temporary Student for SMS purposes
     */
    private Student convertApplicationToStudent(Application application) {
        // Create temporary student object for SMS sending
        // Temporäres Student-Objekt für SMS-Versand erstellen
        Student tempStudent = new Student();
        tempStudent.setId(application.getId()); // Use application ID
        tempStudent.setFirstName(application.getFirstName());
        tempStudent.setLastName(application.getLastName());
        tempStudent.setEmail(application.getEmail());
        return tempStudent;
    }

    /**
     * Erstellt eine Benachrichtigungsstatistik
     * Creates notification statistics
     *
     * @return Statistikbericht / Statistics report
     */
    public String generateNotificationStatistics() {

        StringBuilder stats = new StringBuilder();

        stats.append("=== BENACHRICHTIGUNGSSTATISTIK / NOTIFICATION STATISTICS ===\n");
        stats.append("Zeitraum / Period: Aktuelle Sitzung / Current Session\n\n");

        // In production: Query actual statistics from database
        // In Produktion: Tatsächliche Statistiken aus Datenbank abfragen
        stats.append(" VERSENDETE BENACHRICHTIGUNGEN / SENT NOTIFICATIONS:\n");
        stats.append("    E-Mails: Simulation Mode\n");
        stats.append("    SMS: Simulation Mode\n");
        stats.append("    Willkommenspakete: Simulation Mode\n");
        stats.append("    Zulassungsbescheide: Simulation Mode\n");
        stats.append("    Ablehnungen: Simulation Mode\n");
        stats.append("   Zahlungserinnerungen: Simulation Mode\n\n");

        stats.append("  HINWEIS / NOTE:\n");
        stats.append("   Dieses System läuft im Entwicklungsmodus.\n");
        stats.append("   Alle Benachrichtigungen werden nur simuliert.\n");
        stats.append("   This system runs in development mode.\n");
        stats.append("   All notifications are only simulated.\n\n");

        stats.append("Bericht erstellt am / Report generated on: ");
        stats.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        stats.append("\n");
        stats.append("============================================================");

        return stats.toString();
    }
}
