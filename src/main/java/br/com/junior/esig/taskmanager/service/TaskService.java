package br.com.junior.esig.taskmanager.service;

import br.com.junior.esig.taskmanager.domain.enums.Priority;
import br.com.junior.esig.taskmanager.domain.enums.Role;
import br.com.junior.esig.taskmanager.domain.enums.TaskStatus;
import br.com.junior.esig.taskmanager.domain.model.Task;
import br.com.junior.esig.taskmanager.domain.model.User;
import br.com.junior.esig.taskmanager.dto.task.TaskRequest;
import br.com.junior.esig.taskmanager.dto.task.TaskResponse;
import br.com.junior.esig.taskmanager.exception.ResourceNotFoundException;
import br.com.junior.esig.taskmanager.mapper.TaskMapper;
import br.com.junior.esig.taskmanager.repository.TaskRepository;
import br.com.junior.esig.taskmanager.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public List<TaskResponse> findAll() {
        User currentUser = getLoggedUser();
        List<Task> tasks = (currentUser.getRole() == Role.ROLE_ADMIN)
                ? taskRepository.findAll()
                : taskRepository.findByUser(currentUser);

        return tasks.stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse findById(Long id) {
        Task task = buscarTaskPorId(id);
        checkPermission(task, getLoggedUser());
        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        User currentUser = getLoggedUser();
        User targetUser = (currentUser.getRole() == Role.ROLE_ADMIN && request.getUserId() != null)
                ? buscarUsuario(request.getUserId())
                : currentUser;

        log.info("Criando task: '{}' para o usuário: {}", request.getTitle(), targetUser.getUsername());

        Task task = taskMapper.toEntity(request, targetUser);
        if (task.getStatus() == null) task.setStatus(TaskStatus.TODO);

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = buscarTaskPorId(id);
        User currentUser = getLoggedUser();
        checkPermission(task, currentUser);

        User targetUser = (currentUser.getRole() == Role.ROLE_ADMIN && request.getUserId() != null)
                ? buscarUsuario(request.getUserId())
                : task.getUser();

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setResponsible(request.getResponsible());
        task.setPriority(request.getPriority());
        task.setDeadline(request.getDeadline());
        task.setStatus(request.getStatus());
        task.setUser(targetUser);

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse partialUpdate(Long id, Map<String, Object> updates) {
        Task task = buscarTaskPorId(id);
        User currentUser = getLoggedUser();
        checkPermission(task, currentUser);

        updates.forEach((key, value) -> {
            if (value != null) {
                switch (key) {
                    case "title" -> task.setTitle((String) value);
                    case "description" -> task.setDescription((String) value);
                    case "responsible" -> task.setResponsible((String) value);
                    case "priority" -> task.setPriority(Priority.valueOf(((String) value).toUpperCase()));
                    case "deadline" -> task.setDeadline(LocalDate.parse((String) value));
                    case "status" -> task.setStatus(TaskStatus.valueOf(((String) value).toUpperCase()));
                    case "userId" -> {
                        if (currentUser.getRole() == Role.ROLE_ADMIN) {
                            task.setUser(buscarUsuario(Long.valueOf(value.toString())));
                        }
                    }
                }
            }
        });
        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        Task task = buscarTaskPorId(id);
        checkPermission(task, getLoggedUser());
        taskRepository.deleteById(id);
    }

    @Transactional
    public TaskResponse completeTask(Long id) {
        Task task = buscarTaskPorId(id);
        checkPermission(task, getLoggedUser());
        task.setStatus(TaskStatus.DONE);
        return taskMapper.toResponse(taskRepository.save(task));
    }

    public List<TaskResponse> findByUserId(Long userId) {
        User currentUser = getLoggedUser();
        if (currentUser.getRole() != Role.ROLE_ADMIN && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Acesso negado.");
        }
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return taskRepository.findByUserId(userId).stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ============ MÉTODOS DE FILTRO E BUSCA ============

    public List<TaskResponse> filterTasks(TaskStatus status, Priority priority, String responsible, LocalDate startDate, LocalDate endDate) {
        User currentUser = getLoggedUser();
        List<Task> tasks = (currentUser.getRole() == Role.ROLE_ADMIN)
                ? taskRepository.findAll()
                : taskRepository.findByUser(currentUser);

        return tasks.stream()
                .filter(t -> (status == null || t.getStatus() == status))
                .filter(t -> (priority == null || t.getPriority() == priority))
                .filter(t -> (responsible == null || (t.getResponsible() != null && t.getResponsible().toLowerCase().contains(responsible.toLowerCase()))))
                .filter(t -> (startDate == null || (t.getDeadline() != null && !t.getDeadline().isBefore(startDate))))
                .filter(t -> (endDate == null || (t.getDeadline() != null && !t.getDeadline().isAfter(endDate))))
                .sorted(Comparator.comparing(Task::getPriority).reversed()) // Alta prioridade primeiro
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> findByStatus(TaskStatus status) {
        User user = getLoggedUser();
        List<Task> tasks = (user.getRole() == Role.ROLE_ADMIN)
                ? taskRepository.findByStatus(status)
                : taskRepository.findByUserAndStatus(user, status);
        return convertList(tasks);
    }

    public List<TaskResponse> findByPriority(Priority priority) {
        User user = getLoggedUser();
        List<Task> tasks = (user.getRole() == Role.ROLE_ADMIN)
                ? taskRepository.findByPriority(priority)
                : taskRepository.findByUserAndPriority(user, priority);
        return convertList(tasks);
    }

    public List<TaskResponse> findUpcomingTasks() {
        User user = getLoggedUser();
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        List<Task> tasks = (user.getRole() == Role.ROLE_ADMIN)
                ? taskRepository.findByDeadlineBetween(today, nextWeek)
                : taskRepository.findByUserAndDeadlineBetween(user, today, nextWeek);
        return convertList(tasks);
    }

    public List<TaskResponse> findOverdueTasks() {
        User user = getLoggedUser();
        LocalDate today = LocalDate.now();
        List<Task> tasks = (user.getRole() == Role.ROLE_ADMIN)
                ? taskRepository.findByDeadlineBeforeAndStatusNot(today, TaskStatus.DONE)
                : taskRepository.findByUserAndDeadlineBeforeAndStatusNot(user, today, TaskStatus.DONE);
        return convertList(tasks);
    }

    // ============ HELPER METHODS ============

    private List<TaskResponse> convertList(List<Task> tasks) {
        return tasks.stream().map(taskMapper::toResponse).collect(Collectors.toList());
    }

    private Task buscarTaskPorId(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
    }

    private User buscarUsuario(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private User getLoggedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    private void checkPermission(Task task, User user) {
        if (user.getRole() != Role.ROLE_ADMIN && !task.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Acesso negado.");
        }
    }
}