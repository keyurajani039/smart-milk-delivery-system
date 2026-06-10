package com.example.milkdelivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "milk_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String categoryName;

    private Double pricePerLiter;

    @Column(nullable = false)
    private Boolean active = true;


}