package com.example.milkdelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDto {

    private Long id;

    private String customerName;

    private String phoneNumber;

    private String address;

    private Double milkQuantity;

    private Double extraMilk;

    private Integer extraMilkDays;

    private Boolean isPaused;

    private Integer pauseDays;

    private Long milkCategoryId;

    private String milkCategoryName;

    private Double milkPrice;

    private Boolean active;

    private Boolean deliveryCompleted;
}