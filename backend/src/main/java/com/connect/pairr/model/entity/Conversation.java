package com.connect.pairr.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "conversations",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"participant_1_id", "participant_2_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "participant_1_id")
    private User participant1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "participant_2_id")
    private User participant2;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }
}
