-- =====================================================
-- Database Initialization Script
-- File: database/init.sql
-- This script will be executed automatically when MySQL container starts
-- =====================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the camunda database
USE camunda;

-- =====================================================
-- 1. STUDY PROGRAMS (Studiengänge)
-- =====================================================
CREATE TABLE IF NOT EXISTS study_programs (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    admission_type ENUM('OPEN', 'NUMERUS_CLAUSUS', 'ENTRANCE_EXAM') NOT NULL,
    max_students INT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;;

-- =====================================================
-- 2. APPLICATIONS (Bewerbungen)
-- =====================================================
CREATE TABLE IF NOT EXISTS applications (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Personal Information
                                            first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    sex ENUM('M', 'F', 'D') NOT NULL DEFAULT 'M',
    phone VARCHAR(50),
    date_of_birth DATE NOT NULL,

    -- Address Information
    street VARCHAR(255),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),

    -- Academic Information
    study_program_id BIGINT NOT NULL,
    high_school_grade DECIMAL(3,2),

    -- Application Status
    status ENUM('SUBMITTED', 'DOCUMENT_CHECK', 'ACCEPTED', 'REJECTED', 'ENROLLED') DEFAULT 'SUBMITTED',

    -- Payment Status
    tuition_fee_paid BOOLEAN DEFAULT FALSE,

    -- Process Information
    camunda_process_instance_id VARCHAR(255),

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign Keys
    FOREIGN KEY (study_program_id) REFERENCES study_programs(id)
    )ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 3. STUDENTS (Enrolled Students)
-- =====================================================
CREATE TABLE IF NOT EXISTS students (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        student_number VARCHAR(20) UNIQUE NOT NULL,

    -- Personal Information
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,

    -- Academic Information
    study_program_id BIGINT NOT NULL,
    enrollment_date DATE NOT NULL,
    current_semester INT DEFAULT 1,


    -- Link to original application
    application_id BIGINT UNIQUE,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign Keys
    FOREIGN KEY (study_program_id) REFERENCES study_programs(id),
    FOREIGN KEY (application_id) REFERENCES applications(id)
    )ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- FUNCTION for Student Number Generation
-- =====================================================
DELIMITER //

DROP FUNCTION IF EXISTS generate_student_number//

CREATE FUNCTION generate_student_number(study_program_code VARCHAR(50))
    RETURNS VARCHAR(20)
    READS SQL DATA
    DETERMINISTIC
BEGIN
    DECLARE next_number INT;
    DECLARE student_number VARCHAR(20);
    DECLARE current_year INT;

    SET current_year = YEAR(CURDATE());

    -- Get the next sequential number for this program and year
SELECT COALESCE(MAX(CAST(SUBSTRING(student_number, -4) AS UNSIGNED)), 0) + 1
INTO next_number
FROM students
WHERE student_number LIKE CONCAT(study_program_code, current_year, '%');

-- Format: PROGRAMCODE + YEAR + 4-digit sequential number
SET student_number = CONCAT(study_program_code, current_year, LPAD(next_number, 4, '0'));

RETURN student_number;
END//

DELIMITER ;

-- =====================================================
-- TRIGGER for Automatic Student Number Generation
-- =====================================================
DELIMITER //

DROP TRIGGER IF EXISTS tr_students_generate_number//

CREATE TRIGGER tr_students_generate_number
    BEFORE INSERT ON students
    FOR EACH ROW
BEGIN
    DECLARE program_code VARCHAR(50);

    -- Get the study program code
    SELECT code INTO program_code
    FROM study_programs
    WHERE id = NEW.study_program_id;

    -- Generate student number if not provided
    IF NEW.student_number IS NULL OR NEW.student_number = '' THEN
        SET NEW.student_number = generate_student_number(program_code);
END IF;
END//

DELIMITER ;

-- =====================================================
-- INSERT SAMPLE DATA
-- =====================================================

-- Insert study programs (only if table is empty)
INSERT IGNORE INTO study_programs (name, code, admission_type, max_students) VALUES
('Informatik', 'INF', 'NUMERUS_CLAUSUS', 3),
('Betriebswirtschaftslehre', 'BWL', 'NUMERUS_CLAUSUS', 3),
('Maschinenbau', 'MB', 'OPEN', NULL),
('Medizin', 'MED', 'ENTRANCE_EXAM', NULL),
('Philosophie', 'PHIL', 'OPEN', NULL);

-- =====================================================
-- CREATE INDEXES (without IF NOT EXISTS)
-- =====================================================
CREATE INDEX idx_applications_email ON applications(email);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_process_instance ON applications(camunda_process_instance_id);
CREATE INDEX idx_students_student_number ON students(student_number);
CREATE INDEX idx_students_email ON students(email);

-- =====================================================
-- Show confirmation
-- =====================================================
SELECT 'Database initialized successfully' AS status;
SELECT COUNT(*) AS study_programs_count FROM study_programs;