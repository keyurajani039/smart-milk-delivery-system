package com.example.milkdelivery.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkCategoryDto {

    private Long id;

    private String categoryName;

    private Double pricePerLiter;

    private Boolean active;
}