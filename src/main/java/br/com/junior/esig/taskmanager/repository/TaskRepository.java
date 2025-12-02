package br.com.junior.esig.taskmanager.repository;

import br.com.junior.esig.taskmanager.domain.enums.Priority;
import br.com.junior.esig.taskmanager.domain.enums.TaskStatus;
import br.com.junior.esig.taskmanager.domain.model.Task;
import br.com.junior.esig.taskmanager.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Métodos para ADMIN (Busca global)
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByPriority(Priority priority);
    List<Task> findByDeadlineBetween(LocalDate start, LocalDate end);
    List<Task> findByDeadlineBeforeAndStatusNot(LocalDate date, TaskStatus status);

    // Métodos para USER (Busca restrita ao dono)
    List<Task> findByUser(User user);
    List<Task> findByUserId(Long userId); // Alternativa útil
    List<Task> findByUserAndStatus(User user, TaskStatus status);
    List<Task> findByUserAndPriority(User user, Priority priority);
    List<Task> findByUserAndDeadlineBetween(User user, LocalDate start, LocalDate end);
    List<Task> findByUserAndDeadlineBeforeAndStatusNot(User user, LocalDate date, TaskStatus status);
}