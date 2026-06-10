package com.example.milkdelivery.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDto {

    private Long id;

    private Long customerId;

    private String customerName;

    private Integer month;

    private Integer year;

    private Double amount;

    private String paymentStatus;

    private String paymentType;

    private LocalDate paymentDate;

    private String remarks;
}