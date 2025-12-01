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

        List<Task> tasks;
        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            log.info("Admin {} listando TODAS as tarefas", currentUser.getUsername());
            tasks = taskRepository.findAll();
        } else {
            log.info("Usuário {} listando SUAS tarefas", currentUser.getUsername());
            tasks = taskRepository.findByUser(currentUser);
        }

        return tasks.stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse findById(Long id) {
        Task task = buscarTaskPorId(id);

        // Verifica permissão (Admin ou Dono)
        checkPermission(task, getLoggedUser());

        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        User currentUser = getLoggedUser();
        User targetUser;

        // Regra: Se for ADMIN, pode criar para qualquer um (usa o ID do JSON).
        // Se for USER, cria obrigatoriamente para si mesmo (ignora o ID do JSON).
        if (currentUser.getRole() == Role.ROLE_ADMIN && request.getUserId() != null) {
            targetUser = buscarUsuario(request.getUserId());
        } else {
            targetUser = currentUser;
        }

        log.info("Criando task: '{}' para o usuário: {}", request.getTitle(), targetUser.getUsername());

        Task task = taskMapper.toEntity(request, targetUser);

        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = buscarTaskPorId(id);
        User currentUser = getLoggedUser();

        // 1. Verifica se pode mexer nessa task
        checkPermission(task, currentUser);

        // 2. Define quem será o dono da tarefa atualizada
        User targetUser;
        if (currentUser.getRole() == Role.ROLE_ADMIN && request.getUserId() != null) {
            targetUser = buscarUsuario(request.getUserId()); // Admin pode trocar o dono
        } else {
            targetUser = task.getUser(); // Usuário comum não pode transferir a tarefa para outro
        }

        log.info("Atualizando task ID: {}", id);

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

        log.info("Atualização parcial (PATCH) task ID: {}", id);

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
                        // Apenas Admin pode trocar o dono via PATCH
                        if (currentUser.getRole() == Role.ROLE_ADMIN) {
                            Long userId = Long.valueOf(value.toString());
                            task.setUser(buscarUsuario(userId));
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
        log.info("Task ID {} deletada", id);
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

        // Se for usuário comum, ele só pode buscar as dele mesmo.
        // Se tentar buscar de outro ID (/user/5 sendo que eu sou 2), leva erro.
        if (currentUser.getRole() != Role.ROLE_ADMIN && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Você não tem permissão para ver tarefas de outro usuário.");
        }

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        return taskRepository.findByUserId(userId).stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    // =================================================================================
    // MÉTODOS AUXILIARES PRIVADOS (A Lógica de Ouro)
    // =================================================================================

    private Task buscarTaskPorId(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
    }

    private User buscarUsuario(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private User getLoggedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     * Valida se o usuário tem permissão para acessar a tarefa.
     * Regra: Deve ser ADMIN ou o DONO da tarefa.
     */
    private void checkPermission(Task task, User user) {
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;
        boolean isOwner = task.getUser().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            log.warn("Acesso negado: Usuário {} tentou acessar task {} de outro dono.", user.getUsername(), task.getId());
            throw new AccessDeniedException("Você não tem permissão para acessar ou modificar esta tarefa.");
        }
    }
}