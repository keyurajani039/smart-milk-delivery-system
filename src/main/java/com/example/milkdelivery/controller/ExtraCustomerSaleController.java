package com.example.milkdelivery.controller;

import com.example.milkdelivery.entity.ExtraCustomerSale;
import com.example.milkdelivery.entity.User;
import com.example.milkdelivery.exception.ResourceNotFoundException;
import com.example.milkdelivery.repository.ExtraCustomerSaleRepository;
import com.example.milkdelivery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/extra-sales")
public class ExtraCustomerSaleController {

    @Autowired
    private ExtraCustomerSaleRepository extraSaleRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/save")
    public ResponseEntity<ExtraCustomerSale> saveSale(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Double quantity = Double.valueOf(body.get("quantityLiters").toString());
        Double amount = Double.valueOf(body.get("amountCollected").toString());
        String paymentType = body.get("paymentType").toString();
        String notes = body.containsKey("notes") ? body.get("notes").toString() : "";

        User milkman = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Milkman not found"));

        ExtraCustomerSale sale = ExtraCustomerSale.builder()
                .user(milkman)
                .saleDate(LocalDate.now())
                .quantityLiters(quantity)
                .amountCollected(amount)
                .paymentType(paymentType)
                .notes(notes)
                .build();

        ExtraCustomerSale saved = extraSaleRepository.save(sale);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/today/{userId}")
    public ResponseEntity<List<ExtraCustomerSale>> getTodaySales(@PathVariable Long userId) {
        List<ExtraCustomerSale> sales = extraSaleRepository.findByUserIdAndSaleDate(userId, LocalDate.now());
        return ResponseEntity.ok(sales);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<ExtraCustomerSale>> getSalesHistory(
            @PathVariable Long userId,
            @RequestParam String start,
            @RequestParam String end) {
        List<ExtraCustomerSale> sales = extraSaleRepository.findByUserIdAndSaleDateBetween(
                userId, LocalDate.parse(start), LocalDate.parse(end)
        );
        return ResponseEntity.ok(sales);
    }
}
