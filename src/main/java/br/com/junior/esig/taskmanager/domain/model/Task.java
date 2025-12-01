package br.com.junior.esig.taskmanager.domain.model;

import br.com.junior.esig.taskmanager.domain.enums.Priority;
import br.com.junior.esig.taskmanager.domain.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private String responsible;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @ManyToOne
    private User user;
}
