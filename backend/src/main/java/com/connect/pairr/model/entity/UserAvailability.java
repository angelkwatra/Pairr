package com.connect.pairr.model.entity;

import com.connect.pairr.model.enums.DayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_availability",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "day_type", "start_time", "end_time"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAvailability {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false)
    private DayType dayType;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @PrePersist
    void prePersist() {
        this.id = UUID.randomUUID();
    }
}

