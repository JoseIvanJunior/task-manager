package br.com.junior.esig.taskmanager.dto.task;

import br.com.junior.esig.taskmanager.domain.enums.Priority;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequest {
    private String title;

    private String description;

    private String responsible;

    private Priority priority;

    private LocalDate deadline;
}
