package br.com.junior.esig.taskmanager.repository;

import br.com.junior.esig.taskmanager.domain.model.Task;
import br.com.junior.esig.taskmanager.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser(User user);

    List<Task> findByUserId(Long userId);
}