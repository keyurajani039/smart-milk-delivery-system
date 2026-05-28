package com.example.milkdelivery.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PauseCustomerRequest {

    private Long customerId;

    private Integer pauseDays;

    private LocalDate pauseStartDate;

    private LocalDate pauseEndDate;
}