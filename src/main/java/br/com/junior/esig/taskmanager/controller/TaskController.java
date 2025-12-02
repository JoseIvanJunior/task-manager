package br.com.junior.esig.taskmanager.controller;

import br.com.junior.esig.taskmanager.domain.enums.Priority;
import br.com.junior.esig.taskmanager.domain.enums.TaskStatus;
import br.com.junior.esig.taskmanager.dto.task.TaskRequest;
import br.com.junior.esig.taskmanager.dto.task.TaskResponse;
import br.com.junior.esig.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Gerenciamento de Tarefas")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Listar todas", description = "Retorna tarefas do usuário logado (ou todas se for Admin)")
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        return ResponseEntity.ok(taskService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponse> partialUpdate(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(taskService.partialUpdate(id, updates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<TaskResponse> completeTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.completeTask(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TaskResponse>> getTasksByStatus(@PathVariable TaskStatus status) {
        return ResponseEntity.ok(taskService.findByStatus(status));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<TaskResponse>> getTasksByPriority(@PathVariable Priority priority) {
        return ResponseEntity.ok(taskService.findByPriority(priority));
    }

    @Operation(summary = "Buscar tarefas por usuário", description = "Busca tarefas de um usuário específico (apenas para ADMIN)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TaskResponse>> getTasksByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(taskService.findByUserId(userId));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<TaskResponse>> filterTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String responsible,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(taskService.filterTasks(status, priority, responsible, startDate, endDate));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<TaskResponse>> getOverdueTasks() {
        return ResponseEntity.ok(taskService.findOverdueTasks());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<TaskResponse>> getUpcomingTasks() {
        return ResponseEntity.ok(taskService.findUpcomingTasks());
    }
}