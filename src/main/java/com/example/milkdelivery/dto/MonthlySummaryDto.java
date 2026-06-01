package com.example.milkdelivery.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySummaryDto {

    private Long customerId;

    private String customerName;

    private Integer totalDeliveries;

    private Double totalMilk;

    private Double totalExtraMilk;
}