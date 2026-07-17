package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.dto.PauseCustomerRequest;
import com.example.milkdelivery.entity.Customer;
import com.example.milkdelivery.entity.Delivery;
import com.example.milkdelivery.entity.MilkCategory;
import com.example.milkdelivery.repository.CustomerRepository;
import com.example.milkdelivery.repository.DeliveryRepository;
import com.example.milkdelivery.repository.MilkCategoryRepository;
import com.example.milkdelivery.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private MilkCategoryRepository milkCategoryRepository;

    @Autowired
    private com.example.milkdelivery.repository.UserRepository userRepository;

    @Autowired
    private com.example.milkdelivery.service.TelegramService telegramService;

    @Autowired
    private com.example.milkdelivery.repository.PlanCancellationRepository planCancellationRepository;

    @Override
    @CacheEvict(value = "customers", allEntries = true)
    public Customer saveCustomer(Customer customer) {
        if (customer.getPhoneNumber() != null) {
            java.util.Optional<Customer> existing = customerRepository.findByPhoneNumber(customer.getPhoneNumber());
            if (existing.isPresent()) {
                throw new IllegalArgumentException("Phone number already registered for another customer: " + customer.getPhoneNumber());
            }
        }

        MilkCategory milkCategory = milkCategoryRepository.findById(
                        customer.getMilkCategory().getId()
                )
                .orElseThrow(() -> new RuntimeException("Milk Category not found"));

        customer.setMilkCategory(milkCategory);

        if (customer.getUser() != null && customer.getUser().getId() != null) {
            com.example.milkdelivery.entity.User user = userRepository.findById(customer.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            customer.setUser(user);
        }
        if (customer.getExtraMilk() == null) {
            customer.setExtraMilk(0.0);
        }
        if (customer.getExtraMilkDays() == null) {
            customer.setExtraMilkDays(0);
        }
        if (customer.getIsPaused() == null) {
            customer.setIsPaused(false);
        }
        if (customer.getPauseDays() == null) {
            customer.setPauseDays(0);
        }
        if (customer.getActive() == null) {
            customer.setActive(true);
        }
        if (customer.getDeliveryCompleted() == null) {
            customer.setDeliveryCompleted(false);
        }
        Customer saved = customerRepository.save(customer);
        if (saved.getUser() != null) {
            populateTodayDeliveryStatus(List.of(saved), saved.getUser().getId());
        }
        if (saved.getTelegramId() != null && !saved.getTelegramId().isBlank()) {
            try {
                telegramService.sendWelcomeNotification(saved.getTelegramId(), saved.getCustomerName());
            } catch (Exception e) {
                // Log and ignore to prevent transaction rollback if Telegram is down
            }
        }
        return saved;
    }

    @Override
    @Cacheable(value = "customers", key = "#userId")
    public List<Customer> getAllCustomers(Long userId) {
        List<Customer> customers = customerRepository.findByUser_Id(userId);
        populateTodayDeliveryStatus(customers, userId);
        return customers;
    }

    @Override
    @CacheEvict(value = "customers", allEntries = true)
    public Customer updateCustomer(Long id, Customer customer) {
        if (customer.getPhoneNumber() != null) {
            java.util.Optional<Customer> existing = customerRepository.findByPhoneNumber(customer.getPhoneNumber());
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("Phone number already registered for another customer: " + customer.getPhoneNumber());
            }
        }

        Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

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
        existingCustomer.setExtraMilkStartDate(customer.getExtraMilkStartDate());

        MilkCategory milkCategory = milkCategoryRepository.findById(
                        customer.getMilkCategory().getId()
                )
                .orElseThrow(() -> new RuntimeException("Milk Category not found"));

        existingCustomer.setMilkCategory(milkCategory);
        existingCustomer.setActive(customer.getActive());

        if (customer.getUser() != null && customer.getUser().getId() != null) {
            com.example.milkdelivery.entity.User user = userRepository.findById(customer.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            existingCustomer.setUser(user);
        }

        Customer saved = customerRepository.save(existingCustomer);
        if (saved.getUser() != null) {
            populateTodayDeliveryStatus(List.of(saved), saved.getUser().getId());
        }
        return saved;
    }

    @Override
    @CacheEvict(value = {"customers", "deliveries", "dashboard"}, allEntries = true)
    public String markDeliveryCompleted(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setDeliveryCompleted(true);

        LocalDate today = LocalDate.now();
        double extraMilkQty = 0.0;
        if (customer.getExtraMilk() != null && customer.getExtraMilk() > 0 &&
                (customer.getExtraMilkStartDate() == null || !today.isBefore(customer.getExtraMilkStartDate()))) {
            extraMilkQty = customer.getExtraMilk();
            if (customer.getExtraMilkDays() != null && customer.getExtraMilkDays() > 0) {
                customer.setExtraMilkDays(customer.getExtraMilkDays() - 1);
                if (customer.getExtraMilkDays() == 0) {
                    customer.setExtraMilk(0.0);
                    customer.setExtraMilkStartDate(null);
                }
            }
        }
        customerRepository.save(customer);

        Delivery delivery = Delivery.builder()
                .customer(customer)
                .user(customer.getUser())
                .deliveryDate(LocalDate.now())
                .deliveryTime(LocalDateTime.now())
                .milkQuantity(customer.getMilkQuantity())
                .extraMilk(extraMilkQty)
                .totalMilk(customer.getMilkQuantity() + extraMilkQty)
                .deliveryStatus("DELIVERED")
                .latitude(customer.getLatitude())
                .longitude(customer.getLongitude())
                .build();

        deliveryRepository.save(delivery);
        return "Delivery completed and history saved successfully";
    }

    @Override
    @Cacheable(value = "customers", key = "#id")
    public Customer getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (customer.getUser() != null) {
            populateTodayDeliveryStatus(List.of(customer), customer.getUser().getId());
        }
        return customer;
    }

    @Override
    @CacheEvict(value = "customers", allEntries = true)
    public String deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setActive(false);
        customer.setIsPaused(false);
        customer.setPauseDays(0);
        customer.setPauseStartDate(null);
        customer.setPauseEndDate(null);
        customer.setExtraMilk(0.0);
        customer.setExtraMilkDays(0);
        customer.setExtraMilkStartDate(null);
        customer.setDeliveryCompleted(false);
        customerRepository.save(customer);

        // Log cancellation record
        com.example.milkdelivery.entity.PlanCancellation cancellation = com.example.milkdelivery.entity.PlanCancellation.builder()
                .customer(customer)
                .cancellationDate(java.time.LocalDateTime.now())
                .reason("Admin deleted/cancelled customer plan via Web App")
                .build();
        planCancellationRepository.save(cancellation);

        return "Customer deleted successfully";
    }

    @Override
    @CacheEvict(value = "customers", allEntries = true)
    public String pauseCustomer(PauseCustomerRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setIsPaused(true);
        customer.setPauseDays(request.getPauseDays());
        customer.setPauseStartDate(request.getPauseStartDate());
        customer.setPauseEndDate(request.getPauseEndDate());

        customerRepository.save(customer);
        return "Customer paused successfully";
    }

    @Override
    @CacheEvict(value = "customers", allEntries = true)
    public String activateCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setActive(true);
        customer.setIsPaused(false);
        customer.setPauseDays(0);
        customer.setPauseStartDate(null);
        customer.setPauseEndDate(null);
        customer.setExtraMilk(0.0);
        customer.setExtraMilkDays(0);
        customer.setExtraMilkStartDate(null);
        customer.setDeliveryCompleted(false);
        customerRepository.save(customer);
        return "Customer activated successfully";
    }

    @Override
    @CacheEvict(value = "customers", allEntries = true)
    public String resumeCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setIsPaused(false);
        customer.setPauseDays(0);
        customer.setPauseStartDate(null);
        customer.setPauseEndDate(null);

        customerRepository.save(customer);
        return "Customer resumed successfully";
    }

    @Override
    @CacheEvict(value = {"customers", "deliveries", "dashboard"}, allEntries = true)
    public String markDeliveryPausedToday(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setDeliveryCompleted(true);
        customerRepository.save(customer);

        Delivery delivery = Delivery.builder()
                .customer(customer)
                .user(customer.getUser())
                .deliveryDate(LocalDate.now())
                .deliveryTime(LocalDateTime.now())
                .milkQuantity(0.0)
                .extraMilk(0.0)
                .totalMilk(0.0)
                .deliveryStatus("PAUSED")
                .latitude(customer.getLatitude())
                .longitude(customer.getLongitude())
                .build();

        deliveryRepository.save(delivery);
        return "Delivery marked as paused today successfully";
    }

    private void populateTodayDeliveryStatus(List<Customer> customers, Long userId) {
        if (customers == null || customers.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        List<Delivery> todayDeliveries = deliveryRepository.findByUser_IdAndDeliveryDate(userId, today);
        java.util.Map<Long, String> statusMap = todayDeliveries.stream()
                .collect(java.util.stream.Collectors.toMap(
                        d -> d.getCustomer().getId(),
                        Delivery::getDeliveryStatus,
                        (s1, s2) -> s1
                ));

        for (Customer c : customers) {
            String status = statusMap.get(c.getId());
            if (status != null) {
                c.setTodayDeliveryStatus(status);
            } else if (Boolean.TRUE.equals(c.getIsPaused())) {
                c.setTodayDeliveryStatus("PAUSED");
            } else {
                c.setTodayDeliveryStatus("PENDING");
            }
        }
    }
}