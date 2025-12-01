package br.com.junior.esig.taskmanager.dto.task;

import br.com.junior.esig.taskmanager.domain.enums.Priority;
import br.com.junior.esig.taskmanager.domain.enums.TaskStatus;
import lombok.Data;

@Data
public class TaskFilter {
    private String title;

    private Priority priority;
    
    private TaskStatus status;
}
