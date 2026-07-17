package com.example.milkdelivery.controller;

import com.example.milkdelivery.dto.PauseCustomerRequest;
import com.example.milkdelivery.entity.Customer;
import com.example.milkdelivery.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/save")
    public Customer saveCustomer(@Valid @RequestBody Customer customer) {
        return customerService.saveCustomer(customer);
    }

    @GetMapping("/all/{userId}")
    public List<Customer> getAllCustomers(@PathVariable Long userId) {
        return customerService.getAllCustomers(userId);
    }

    @PutMapping("/update/{id}")
    public Customer updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        return customerService.updateCustomer(id, customer);
    }

    @PostMapping("/delivered/{id}")
    public String markDeliveryCompleted(@PathVariable Long id) {
        return customerService.markDeliveryCompleted(id);
    }

    @GetMapping("/{id}")
    public Customer getCustomerById(@PathVariable Long id) {
        return customerService.getCustomerById(id);
    }

    @DeleteMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Long id) {
        return customerService.deleteCustomer(id);
    }

    @PutMapping("/activate/{id}")
    public String activateCustomer(@PathVariable Long id) {
        return customerService.activateCustomer(id);
    }

    @PostMapping("/pause")
    public String pauseCustomer(@RequestBody PauseCustomerRequest request) {
        return customerService.pauseCustomer(request);
    }

    @PostMapping("/resume")
    public String resumeCustomer(@RequestBody Map<String, Long> body) {
        Long id = body.get("customerId");
        if (id == null) {
            throw new RuntimeException("customerId is required in body");
        }
        return customerService.resumeCustomer(id);
    }

    @PostMapping("/paused-today/{id}")
    public String markDeliveryPausedToday(@PathVariable Long id) {
        return customerService.markDeliveryPausedToday(id);
    }
}