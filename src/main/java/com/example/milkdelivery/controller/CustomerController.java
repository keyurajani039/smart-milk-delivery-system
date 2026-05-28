package com.example.milkdelivery.controller;

import com.example.milkdelivery.dto.PauseCustomerRequest;
import com.example.milkdelivery.entity.Customer;
import com.example.milkdelivery.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    // Save Customer
    @PostMapping("/save")
    public Customer saveCustomer(
            @Valid @RequestBody Customer customer) {

        return customerService.saveCustomer(customer);
    }

    // Get All Customers
    @GetMapping("/all/{userId}")
    public List<Customer> getAllCustomers(
            @PathVariable Long userId) {

        return customerService.getAllCustomers(userId);
    }

    // Update Customer
    @PutMapping("/update/{id}")
    public Customer updateCustomer(
            @PathVariable Long id,
            @RequestBody Customer customer) {

        return customerService.updateCustomer(id, customer);
    }

    @PostMapping("/delivered/{id}")
    public String markDeliveryCompleted(
            @PathVariable Long id) {

        return customerService
                .markDeliveryCompleted(id);
    }

    @GetMapping("/{id}")
    public Customer getCustomerById(
            @PathVariable Long id) {

        return customerService
                .getCustomerById(id);
    }

    @DeleteMapping("/delete/{id}")
    public String deleteCustomer(
            @PathVariable Long id) {

        return customerService
                .deleteCustomer(id);
    }

    @PostMapping("/pause")
    public String pauseCustomer(
            @RequestBody PauseCustomerRequest request) {

        return customerService
                .pauseCustomer(request);
    }
}