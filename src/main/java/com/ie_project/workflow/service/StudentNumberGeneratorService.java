package com.ie_project.workflow.service;

import com.ie_project.workflow.entity.StudyProgram;
import com.ie_project.workflow.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Service für die Generierung von Matrikelnummern
 * Service for generating student numbers (Matrikelnummer)
 *
 * Generates unique student numbers following the pattern:
 * PROGRAM_CODE + YEAR + 4-digit sequential number
 * Example: INF20250001, BWL20250002, etc.
 *
 * Generiert eindeutige Matrikelnummern nach dem Muster:
 * STUDIENGANG_CODE + JAHR + 4-stellige Folgenummer
 * Beispiel: INF20250001, BWL20250002, etc.
 *
 * @author IE Project Team
 */
@Service
@Transactional
public class StudentNumberGeneratorService {

    @Autowired
    private StudentRepository studentRepository;

    /**
     * Generiert eine neue Matrikelnummer für den angegebenen Studiengang
     * Generates a new student number for the specified study program
     *
     * @param studyProgram Der Studiengang / The study program
     * @return Neue eindeutige Matrikelnummer / New unique student number
     */
    public String generateStudentNumber(StudyProgram studyProgram) {

        if (studyProgram == null || studyProgram.getCode() == null) {
            throw new IllegalArgumentException("Study program and code must not be null / Studiengang und Code dürfen nicht null sein");
        }

        String programCode = studyProgram.getCode().toUpperCase();
        int currentYear = LocalDate.now().getYear();

        // Pattern: PROGRAMCODE + YEAR + 4-digit sequential number
        String basePattern = programCode + currentYear;

        // Finde die höchste bestehende Nummer für dieses Programm und Jahr
        // Find the highest existing number for this program and year
        Optional<String> lastNumberOpt = studentRepository.findLastStudentNumberForPattern(basePattern);

        int nextSequentialNumber = 1;

        if (lastNumberOpt.isPresent()) {
            String lastNumber = lastNumberOpt.get();
            try {
                // Extrahiere die letzten 4 Ziffern und erhöhe um 1
                // Extract the last 4 digits and increment by 1
                String sequentialPart = lastNumber.substring(lastNumber.length() - 4);
                nextSequentialNumber = Integer.parseInt(sequentialPart) + 1;
            } catch (Exception e) {
                System.err.println("Error parsing last student number: " + lastNumber);
                // Fallback zu 1 wenn Parsing fehlschlägt
                nextSequentialNumber = 1;
            }
        }

        // Formatiere die Nummer mit führenden Nullen (4 Stellen)
        // Format the number with leading zeros (4 digits)
        String formattedSequentialNumber = String.format("%04d", nextSequentialNumber);

        String newStudentNumber = basePattern + formattedSequentialNumber;

        // Sicherheitscheck: Prüfe ob die Nummer bereits existiert
        // Safety check: Verify the number doesn't already exist
        while (studentRepository.existsByStudentNumber(newStudentNumber)) {
            nextSequentialNumber++;
            formattedSequentialNumber = String.format("%04d", nextSequentialNumber);
            newStudentNumber = basePattern + formattedSequentialNumber;
        }

        System.out.println("=== STUDENT NUMBER GENERATED ===");
        System.out.println("Study Program: " + studyProgram.getName() + " (" + programCode + ")");
        System.out.println("Year: " + currentYear);
        System.out.println("Sequential Number: " + nextSequentialNumber);
        System.out.println("Generated Student Number: " + newStudentNumber);
        System.out.println("===============================");

        return newStudentNumber;
    }

    /**
     * Validiert eine Matrikelnummer auf korrektes Format
     * Validates a student number for correct format
     *
     * @param studentNumber Die zu validierende Matrikelnummer / The student number to validate
     * @return true wenn Format korrekt / true if format is correct
     */
    public boolean isValidStudentNumberFormat(String studentNumber) {

        if (studentNumber == null || studentNumber.length() < 8) {
            return false;
        }

        // Format: PROGRAMCODE (min 2 chars) + YEAR (4 digits) + SEQUENTIAL (4 digits)
        // Mindestlänge: 2 + 4 + 4 = 10 Zeichen
        if (studentNumber.length() < 10) {
            return false;
        }

        try {
            // Prüfe ob die letzten 8 Zeichen numerisch sind (Jahr + Folgenummer)
            // Check if the last 8 characters are numeric (year + sequential number)
            String numericPart = studentNumber.substring(studentNumber.length() - 8);
            Integer.parseInt(numericPart);

            // Prüfe Jahr (sollte realistisch sein)
            // Check year (should be realistic)
            String yearPart = studentNumber.substring(studentNumber.length() - 8, studentNumber.length() - 4);
            int year = Integer.parseInt(yearPart);
            int currentYear = LocalDate.now().getYear();

            return year >= 2000 && year <= currentYear + 5; // Erlaubt bis zu 5 Jahre in die Zukunft

        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Extrahiert das Jahr aus einer Matrikelnummer
     * Extracts the year from a student number
     *
     * @param studentNumber Die Matrikelnummer / The student number
     * @return Das Jahr / The year
     */
    public int extractYearFromStudentNumber(String studentNumber) {

        if (!isValidStudentNumberFormat(studentNumber)) {
            throw new IllegalArgumentException("Invalid student number format / Ungültiges Matrikelnummer-Format: " + studentNumber);
        }

        String yearPart = studentNumber.substring(studentNumber.length() - 8, studentNumber.length() - 4);
        return Integer.parseInt(yearPart);
    }

    /**
     * Extrahiert den Programmcode aus einer Matrikelnummer
     * Extracts the program code from a student number
     *
     * @param studentNumber Die Matrikelnummer / The student number
     * @return Der Programmcode / The program code
     */
    public String extractProgramCodeFromStudentNumber(String studentNumber) {

        if (!isValidStudentNumberFormat(studentNumber)) {
            throw new IllegalArgumentException("Invalid student number format / Ungültiges Matrikelnummer-Format: " + studentNumber);
        }

        return studentNumber.substring(0, studentNumber.length() - 8);
    }
}
