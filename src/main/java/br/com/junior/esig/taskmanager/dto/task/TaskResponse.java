package br.com.junior.esig.taskmanager.dto.task;

import br.com.junior.esig.taskmanager.domain.enums.Priority;
import br.com.junior.esig.taskmanager.domain.enums.TaskStatus;
import br.com.junior.esig.taskmanager.dto.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Long id;

    private String title;

    private String description;

    private String responsible;

    private Priority priority;

    private LocalDate deadline;

    private TaskStatus status;

    private UserResponse user;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}