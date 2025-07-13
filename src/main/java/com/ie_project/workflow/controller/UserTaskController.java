package com.ie_project.workflow.controller;

import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller für User Tasks in Camunda (Simplified Version)
 * REST Controller for User Tasks in Camunda (Simplified Version)
 *
 * This simplified controller provides basic endpoints to manage user tasks
 * without complex form handling that may cause compatibility issues.
 *
 * Dieser vereinfachte Controller bietet grundlegende Endpoints für User Tasks
 * ohne komplexe Formularverarbeitung, die Kompatibilitätsprobleme verursachen könnte.
 *
 * @author IE Project Team
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class UserTaskController {

    @Autowired
    private TaskService taskService;

    /**
     * Holt alle offenen User Tasks
     * Gets all open user tasks
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllTasks() {

        try {
            List<Task> tasks = taskService.createTaskQuery()
                    .active()
                    .orderByTaskCreateTime()
                    .desc()
                    .list();

            List<Map<String, Object>> taskList = tasks.stream()
                    .map(this::convertTaskToMap)
                    .collect(Collectors.toList());

            System.out.println("=== USER TASKS RETRIEVED ===");
            System.out.println("Number of tasks: " + taskList.size());
            System.out.println("=============================");

            return ResponseEntity.ok(taskList);

        } catch (Exception e) {
            System.err.println("Error retrieving tasks: " + e.getMessage());
            return ResponseEntity.internalServerError().body(List.of());
        }
    }

    /**
     * Holt eine spezifische Task nach ID
     * Gets a specific task by ID
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {

        try {
            Task task = taskService.createTaskQuery()
                    .taskId(taskId)
                    .singleResult();

            if (task == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> taskData = convertTaskToMap(task);

            System.out.println("=== TASK RETRIEVED ===");
            System.out.println("Task ID: " + taskId);
            System.out.println("Task Name: " + task.getName());
            System.out.println("Process Instance: " + task.getProcessInstanceId());
            System.out.println("======================");

            return ResponseEntity.ok(taskData);

        } catch (Exception e) {
            System.err.println("Error retrieving task " + taskId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Vervollständigt eine User Task
     * Completes a user task
     */
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<Map<String, Object>> completeTask(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> variables) {

        try {
            // Verify task exists / Prüfen ob Task existiert
            Task task = taskService.createTaskQuery()
                    .taskId(taskId)
                    .singleResult();

            if (task == null) {
                return ResponseEntity.notFound().build();
            }

            // Complete the task with provided variables / Task mit bereitgestellten Variablen abschließen
            taskService.complete(taskId, variables);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Task completed successfully / Task erfolgreich abgeschlossen");
            response.put("taskId", taskId);
            response.put("taskName", task.getName());
            response.put("processInstanceId", task.getProcessInstanceId());
            response.put("completedAt", java.time.LocalDateTime.now());
            response.put("variables", variables);

            System.out.println("=== TASK COMPLETED ===");
            System.out.println("Task ID: " + taskId);
            System.out.println("Task Name: " + task.getName());
            System.out.println("Process Instance: " + task.getProcessInstanceId());
            System.out.println("Variables: " + variables);
            System.out.println("======================");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error completing task " + taskId + ": " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error completing task / Fehler beim Abschließen der Task");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("taskId", taskId);
            errorResponse.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Holt Tasks für Dokumentenprüfung
     * Gets tasks for document verification
     */
    @GetMapping("/document-verification")
    public ResponseEntity<List<Map<String, Object>>> getDocumentVerificationTasks() {

        try {
            List<Task> tasks = taskService.createTaskQuery()
                    .taskDefinitionKey("Task_DocumentVerification")
                    .active()
                    .orderByTaskCreateTime()
                    .desc()
                    .list();

            List<Map<String, Object>> taskList = tasks.stream()
                    .map(this::convertTaskToMap)
                    .collect(Collectors.toList());

            System.out.println("=== DOCUMENT VERIFICATION TASKS RETRIEVED ===");
            System.out.println("Number of tasks: " + taskList.size());
            System.out.println("===============================================");

            return ResponseEntity.ok(taskList);

        } catch (Exception e) {
            System.err.println("Error retrieving document verification tasks: " + e.getMessage());
            return ResponseEntity.internalServerError().body(List.of());
        }
    }

    /**
     * Simuliert die Vervollständigung einer Dokumentenprüfung für Tests
     * Simulates completing a document verification for testing
     */
    @PostMapping("/document-verification/{taskId}/simulate")
    public ResponseEntity<Map<String, Object>> simulateDocumentVerification(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "true") boolean documentsComplete,
            @RequestParam(defaultValue = "") String missingDocuments,
            @RequestParam(defaultValue = "Automatische Simulation") String verificationNotes,
            @RequestParam(defaultValue = "System Test") String verifiedBy) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("documentsComplete", documentsComplete);
        variables.put("missingDocuments", missingDocuments);
        variables.put("verificationNotes", verificationNotes);
        variables.put("verifiedBy", verifiedBy);

        return completeTask(taskId, variables);
    }

    /**
     * Erstellt eine einfache Übersicht der verfügbaren Formulardaten
     * Creates a simple overview of available form data
     */
    @GetMapping("/{taskId}/form-info")
    public ResponseEntity<Map<String, Object>> getTaskFormInfo(@PathVariable String taskId) {

        try {
            Task task = taskService.createTaskQuery()
                    .taskId(taskId)
                    .singleResult();

            if (task == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> formInfo = new HashMap<>();
            formInfo.put("taskId", taskId);
            formInfo.put("taskName", task.getName());
            formInfo.put("processInstanceId", task.getProcessInstanceId());

            // Get process variables / Prozessvariablen holen
            Map<String, Object> variables = taskService.getVariables(taskId);
            formInfo.put("availableVariables", variables);

            // Provide expected form fields for document verification
            // Erwartete Formularfelder für Dokumentenprüfung bereitstellen
            if ("Task_DocumentVerification".equals(task.getTaskDefinitionKey())) {
                Map<String, Object> expectedFields = new HashMap<>();
                expectedFields.put("documentsComplete", "boolean - Sind alle Dokumente vollständig?");
                expectedFields.put("missingDocuments", "string - Fehlende Dokumente (falls unvollständig)");
                expectedFields.put("verificationNotes", "string - Anmerkungen zur Dokumentenprüfung");
                expectedFields.put("verifiedBy", "string - Geprüft von");

                formInfo.put("expectedFields", expectedFields);
            }

            return ResponseEntity.ok(formInfo);

        } catch (Exception e) {
            System.err.println("Error getting form info for task " + taskId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Konvertiert eine Camunda Task zu einer Map (vereinfachte Version)
     * Converts a Camunda Task to a Map (simplified version)
     */
    private Map<String, Object> convertTaskToMap(Task task) {
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", task.getId());
        taskMap.put("name", task.getName());
        taskMap.put("description", task.getDescription());
        taskMap.put("assignee", task.getAssignee());
        taskMap.put("created", task.getCreateTime());
        taskMap.put("processInstanceId", task.getProcessInstanceId());
        taskMap.put("processDefinitionId", task.getProcessDefinitionId());
        taskMap.put("taskDefinitionKey", task.getTaskDefinitionKey());
        taskMap.put("priority", task.getPriority());

        // Add process variables if available / Prozessvariablen hinzufügen falls verfügbar
        try {
            Map<String, Object> variables = taskService.getVariables(task.getId());
            taskMap.put("variables", variables);
        } catch (Exception e) {
            taskMap.put("variables", Map.of());
        }

        return taskMap;
    }
}
