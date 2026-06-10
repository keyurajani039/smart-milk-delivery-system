package com.example.milkdelivery.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate routeDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User milkman;

    private Double totalDistance;

    private Integer totalCustomers;
}
