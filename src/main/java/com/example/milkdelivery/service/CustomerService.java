package com.example.milkdelivery.service;

import com.example.milkdelivery.dto.PauseCustomerRequest;
import com.example.milkdelivery.entity.Customer;

import java.util.List;

public interface CustomerService {

    Customer saveCustomer(Customer customer);

    List<Customer> getAllCustomers(Long userId);

    Customer updateCustomer(Long id, Customer customer);

    String markDeliveryCompleted(Long customerId);

    Customer getCustomerById(Long id);

    String deleteCustomer(Long id);

    String pauseCustomer(PauseCustomerRequest request);

    String activateCustomer(Long id);

    String resumeCustomer(Long id);
}