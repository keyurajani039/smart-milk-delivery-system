package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.dto.PauseCustomerRequest;
import com.example.milkdelivery.entity.Customer;
import com.example.milkdelivery.repository.CustomerRepository;
import com.example.milkdelivery.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import com.example.milkdelivery.entity.Delivery;
import com.example.milkdelivery.repository.DeliveryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;



@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Override
    public Customer saveCustomer(Customer customer) {

        return customerRepository.save(customer);
    }

    @Override
    public List<Customer> getAllCustomers(Long userId) {

        return customerRepository.findByUser_Id(userId);
    }

    @Override
    public Customer updateCustomer(Long id, Customer customer) {

        Customer existingCustomer = customerRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Customer not found"));

        existingCustomer.setCustomerName(customer.getCustomerName());

        existingCustomer.setPhoneNumber(customer.getPhoneNumber());

        existingCustomer.setAddress(customer.getAddress());

        existingCustomer.setLatitude(customer.getLatitude());

        existingCustomer.setLongitude(customer.getLongitude());

        existingCustomer.setMilkQuantity(customer.getMilkQuantity());

        existingCustomer.setExtraMilk(customer.getExtraMilk());

        existingCustomer.setExtraMilkDays(customer.getExtraMilkDays());

        existingCustomer.setIsPaused(customer.getIsPaused());

        existingCustomer.setPauseDays(customer.getPauseDays());

        existingCustomer.setPauseStartDate(customer.getPauseStartDate());

        existingCustomer.setPauseEndDate(customer.getPauseEndDate());

        existingCustomer.setMilkType(customer.getMilkType());

        existingCustomer.setActive(customer.getActive());

        return customerRepository.save(existingCustomer);
    }

    @Override
    public String markDeliveryCompleted(Long customerId) {

        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Customer not found"
                        ));

        // Mark customer delivered
        customer.setDeliveryCompleted(true);

        customerRepository.save(customer);

        // Create delivery history
        Delivery delivery = new Delivery();

        delivery.setCustomer(customer);

        delivery.setUser(customer.getUser());

        delivery.setDeliveryDate(LocalDate.now());

        delivery.setDeliveryTime(LocalDateTime.now());

        delivery.setMilkQuantity(
                customer.getMilkQuantity()
        );

        delivery.setExtraMilk(
                customer.getExtraMilk() == null
                        ? 0.0
                        : customer.getExtraMilk()
        );

        delivery.setTotalMilk(
                customer.getMilkQuantity()
                        +
                        (customer.getExtraMilk() == null
                                ? 0.0
                                : customer.getExtraMilk())
        );

        delivery.setDeliveryStatus(
                "DELIVERED"
        );

        deliveryRepository.save(delivery);

        return "Delivery completed and history saved successfully";
    }

    @Override
    public Customer getCustomerById(Long id) {

        return customerRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Customer not found"
                        ));
    }

    @Override
    public String deleteCustomer(Long id) {

        Customer customer = customerRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Customer not found"
                        ));

        customer.setActive(false);

        customerRepository.save(customer);

        return "Customer deleted successfully";
    }

    @Override
    public String pauseCustomer(
            PauseCustomerRequest request) {

        Customer customer = customerRepository
                .findById(request.getCustomerId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Customer not found"
                        ));

        customer.setIsPaused(true);

        customer.setPauseDays(
                request.getPauseDays());

        customer.setPauseStartDate(
                request.getPauseStartDate());

        customer.setPauseEndDate(
                request.getPauseEndDate());

        customerRepository.save(customer);

        return "Customer paused successfully";
    }
}