package com.example.milkdelivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "route_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private Integer sequenceNumber;
}
