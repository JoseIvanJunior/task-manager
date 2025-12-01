package br.com.junior.esig.taskmanager.repository;

import br.com.junior.esig.taskmanager.domain.enums.TaskStatus;
import br.com.junior.esig.taskmanager.domain.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findByStatus(TaskStatus status);
}
