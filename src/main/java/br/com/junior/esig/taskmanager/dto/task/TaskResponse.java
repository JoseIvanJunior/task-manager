package br.com.junior.esig.taskmanager.dto.task;

import br.com.junior.esig.taskmanager.domain.enums.Priority;
import br.com.junior.esig.taskmanager.domain.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TaskResponse {
    private Long id;

    private String title;

    private String description;

    private String responsible;

    private Priority priority;

    private LocalDate deadline;

    private TaskStatus status;
}
