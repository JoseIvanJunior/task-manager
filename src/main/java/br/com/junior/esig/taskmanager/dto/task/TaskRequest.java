package br.com.junior.esig.taskmanager.dto.task;

import br.com.junior.esig.taskmanager.domain.enums.Priority;
import br.com.junior.esig.taskmanager.domain.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

    @NotBlank(message = "Title é obrigatório")
    private String title;

    private String description;

    private String responsible;

    private Priority priority;

    private LocalDate deadline;

    private TaskStatus status;

    @NotNull(message = "User ID é obrigatório")
    private Long userId;
}