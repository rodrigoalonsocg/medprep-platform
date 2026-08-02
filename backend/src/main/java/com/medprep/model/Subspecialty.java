package com.medprep.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "subspecialties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subspecialty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Specialty specialty;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String code;
}
