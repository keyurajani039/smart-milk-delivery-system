package com.example.milkdelivery.dto;

import com.example.milkdelivery.enums.PaymentType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReceivedRequest {

    private PaymentType paymentType;
}