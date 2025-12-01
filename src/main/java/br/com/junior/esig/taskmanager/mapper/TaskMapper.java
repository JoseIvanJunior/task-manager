package br.com.junior.esig.taskmanager.mapper;

import br.com.junior.esig.taskmanager.domain.model.Task;
import br.com.junior.esig.taskmanager.domain.model.User;
import br.com.junior.esig.taskmanager.dto.task.TaskRequest;
import br.com.junior.esig.taskmanager.dto.task.TaskResponse;
import br.com.junior.esig.taskmanager.dto.user.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toEntity(TaskRequest request, User user) {
        return Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .responsible(request.getResponsible())
                .priority(request.getPriority())
                .deadline(request.getDeadline())
                .status(request.getStatus())
                .user(user)
                .build();
    }

    public TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .responsible(task.getResponsible())
                .priority(task.getPriority())
                .deadline(task.getDeadline())
                .status(task.getStatus())
                .user(toUserResponse(task.getUser()))
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }
}