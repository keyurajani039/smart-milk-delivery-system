package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.dto.DeliveryDto;
import com.example.milkdelivery.dto.MonthlySummaryDto;
import com.example.milkdelivery.entity.Customer;
import com.example.milkdelivery.entity.Delivery;
import com.example.milkdelivery.entity.DeliverySession;
import com.example.milkdelivery.entity.ExtraCustomerSale;
import com.example.milkdelivery.entity.User;
import com.example.milkdelivery.exception.ResourceNotFoundException;
import com.example.milkdelivery.repository.CustomerRepository;
import com.example.milkdelivery.repository.DeliveryRepository;
import com.example.milkdelivery.repository.DeliverySessionRepository;
import com.example.milkdelivery.repository.ExtraCustomerSaleRepository;
import com.example.milkdelivery.repository.UserRepository;
import com.example.milkdelivery.service.DeliveryService;
import com.example.milkdelivery.service.TelegramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeliverySessionRepository sessionRepository;

    @Autowired
    private ExtraCustomerSaleRepository extraSaleRepository;

    @Autowired
    private TelegramService telegramService;

    @Override
    @CacheEvict(value = "deliveries", allEntries = true)
    public Delivery saveDelivery(Delivery delivery) {
        return deliveryRepository.save(delivery);
    }

    @Override
    @Cacheable(value = "deliveries")
    public List<Delivery> getAllDeliveries() {
        return deliveryRepository.findAll();
    }

    @Override
    @Cacheable(value = "deliveries", key = "#id")
    public Delivery getDeliveryById(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
    }

    @Override
    public List<DeliveryDto> getDeliveriesByCustomer(Long customerId) {
        List<Delivery> deliveries = deliveryRepository.findByCustomer_Id(customerId);
        return deliveries.stream().map(this::convertToDto).toList();
    }

    @Override
    public List<DeliveryDto> getTodayDeliveries() {
        List<Delivery> deliveries = deliveryRepository.findByDeliveryDate(LocalDate.now());
        return deliveries.stream().map(this::convertToDto).toList();
    }

    @Override
    public List<DeliveryDto> getTodayDeliveriesByMilkman(Long milkmanId) {
        List<Delivery> deliveries = deliveryRepository.findByUser_IdAndDeliveryDate(milkmanId, LocalDate.now());
        return deliveries.stream().map(this::convertToDto).toList();
    }

    @Override
    @CacheEvict(value = {"deliveries", "customers", "dashboard"}, allEntries = true)
    public String skipTodayDeliveries(Long milkmanId) {
        List<Customer> activeCustomers = customerRepository.findByUser_IdAndActiveTrue(milkmanId);
        int count = 0;
        User milkman = userRepository.findById(milkmanId)
                .orElseThrow(() -> new ResourceNotFoundException("Milkman not found"));

        for (Customer customer : activeCustomers) {
            // Check if already completed delivery today
            List<Delivery> todayDeliveries = deliveryRepository.findByUser_IdAndDeliveryDate(milkmanId, LocalDate.now());
            boolean alreadyDelivered = todayDeliveries.stream()
                    .anyMatch(d -> d.getCustomer().getId().equals(customer.getId()));

            if (!alreadyDelivered) {
                customer.setDeliveryCompleted(true);
                customerRepository.save(customer);

                Delivery delivery = Delivery.builder()
                        .customer(customer)
                        .user(milkman)
                        .deliveryDate(LocalDate.now())
                        .deliveryTime(LocalDateTime.now())
                        .milkQuantity(0.0)
                        .extraMilk(0.0)
                        .totalMilk(0.0)
                        .deliveryStatus("SKIPPED")
                        .latitude(customer.getLatitude())
                        .longitude(customer.getLongitude())
                        .build();

                deliveryRepository.save(delivery);

                // Notify via Telegram Bot
                telegramService.sendAbsenceNotification(customer.getTelegramId(), customer.getCustomerName(), milkman.getMilkCompanyName());
                count++;
            } else {
                // If already completed or skipped today, still broadcast the Telegram message for test/retry
                telegramService.sendAbsenceNotification(customer.getTelegramId(), customer.getCustomerName(), milkman.getMilkCompanyName());
                count++;
            }
        }
        return "Broadcasting absence complete. Skipped deliveries for " + count + " customers.";
    }

    @Override
    @CacheEvict(value = {"deliveries", "customers", "dashboard"}, allEntries = true)
    public DeliverySession startSession(Long milkmanId, Double loadedMilk) {
        User milkman = userRepository.findById(milkmanId)
                .orElseThrow(() -> new ResourceNotFoundException("Milkman not found"));

        // Check once-per-day restriction with 2-minute reset for testing
        java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        java.util.Optional<DeliverySession> lastSessionOpt = sessionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(milkmanId) && s.getStartedAt().isAfter(startOfDay))
                .max(java.util.Comparator.comparing(DeliverySession::getStartedAt));

        if (lastSessionOpt.isPresent()) {
            DeliverySession lastSession = lastSessionOpt.get();
            if (Boolean.FALSE.equals(lastSession.getActive())) {
                java.time.LocalDateTime endedAt = lastSession.getEndedAt();
                if (endedAt != null) {
                    long secondsSinceEnd = java.time.Duration.between(endedAt, java.time.LocalDateTime.now()).toSeconds();
                    if (secondsSinceEnd < 120) {
                        throw new RuntimeException("Shift can only be started once per day. Please wait 2 minutes to reset for testing (Time remaining: " 
                            + (120 - secondsSinceEnd) + " seconds).");
                    }
                }
            }
        }

        // Close any existing open session
        Optional<DeliverySession> activeSessionOpt = sessionRepository.findByUser_IdAndActiveTrue(milkmanId);
        activeSessionOpt.ifPresent(session -> {
            session.setActive(false);
            session.setEndedAt(LocalDateTime.now());
            sessionRepository.save(session);
        });

        // Reset customer delivery completed status for today's deliveries
        List<Customer> customers = customerRepository.findByUser_Id(milkmanId);
        for (Customer c : customers) {
            if (Boolean.TRUE.equals(c.getIsPaused())) {
                c.setDeliveryCompleted(true);
                if (c.getPauseDays() != null && c.getPauseDays() > 0) {
                    c.setPauseDays(c.getPauseDays() - 1);
                    if (c.getPauseDays() == 0) {
                        c.setIsPaused(false);
                        c.setPauseStartDate(null);
                        c.setPauseEndDate(null);
                    }
                }
                customerRepository.save(c);

                // Record a PAUSED delivery record if not already recorded today
                boolean alreadyRecorded = deliveryRepository.findByUser_IdAndDeliveryDate(milkmanId, LocalDate.now())
                        .stream().anyMatch(d -> d.getCustomer().getId().equals(c.getId()));
                if (!alreadyRecorded) {
                    Delivery delivery = Delivery.builder()
                            .customer(c)
                            .user(milkman)
                            .deliveryDate(LocalDate.now())
                            .deliveryTime(LocalDateTime.now())
                            .milkQuantity(0.0)
                            .extraMilk(0.0)
                            .totalMilk(0.0)
                            .deliveryStatus("PAUSED")
                            .latitude(c.getLatitude())
                            .longitude(c.getLongitude())
                            .build();
                    deliveryRepository.save(delivery);
                }
            } else {
                c.setDeliveryCompleted(false);
                customerRepository.save(c);
            }
        }

        DeliverySession newSession = DeliverySession.builder()
                .user(milkman)
                .startedAt(LocalDateTime.now())
                .active(true)
                .loadedMilkQuantity(loadedMilk)
                .build();

        return sessionRepository.save(newSession);
    }

    @Override
    public Map<String, Object> getSessionSummary(Long milkmanId) {
        Map<String, Object> summary = new HashMap<>();
        Optional<DeliverySession> activeSessionOpt = sessionRepository.findByUser_IdAndActiveTrue(milkmanId);

        if (activeSessionOpt.isEmpty()) {
            summary.put("activeSession", false);
            summary.put("loadedMilk", 0.0);
            summary.put("totalDelivered", 0.0);
            summary.put("remainingMilk", 0.0);
            summary.put("deliveryProgress", 0.0);
            summary.put("message", "No active delivery session. Please start delivery first.");
            return summary;
        }

        DeliverySession session = activeSessionOpt.get();
        double loadedMilk = session.getLoadedMilkQuantity();

        // Calculate total milk delivered today
        List<Delivery> todayDeliveries = deliveryRepository.findByUser_IdAndDeliveryDate(milkmanId, LocalDate.now());
        double totalDelivered = todayDeliveries.stream()
                .filter(d -> d.getDeliveryStatus() != null && "DELIVERED".equalsIgnoreCase(d.getDeliveryStatus().trim()))
                .mapToDouble(Delivery::getTotalMilk)
                .sum();

        // Calculate total extra customer sales today
        List<ExtraCustomerSale> extraSales = extraSaleRepository.findByUserIdAndSaleDate(milkmanId, LocalDate.now());
        double totalExtraSales = extraSales.stream()
                .mapToDouble(ExtraCustomerSale::getQuantityLiters)
                .sum();

        double remainingMilk = loadedMilk - totalDelivered - totalExtraSales;

        // Calculate progress
        List<Customer> activeCustomers = customerRepository.findByUser_IdAndActiveTrue(milkmanId);
        long totalCustomersCount = activeCustomers.size();
        long completedCustomersCount = activeCustomers.stream()
                .filter(Customer::getDeliveryCompleted)
                .count();

        double progressPercent = totalCustomersCount == 0 ? 0.0 : ((double) completedCustomersCount / totalCustomersCount) * 100;

        summary.put("activeSession", true);
        summary.put("loadedMilk", loadedMilk);
        summary.put("totalDelivered", totalDelivered);
        summary.put("totalExtraSales", totalExtraSales);
        summary.put("remainingMilk", Math.max(0.0, remainingMilk));
        summary.put("totalCustomers", totalCustomersCount);
        summary.put("completedCustomers", completedCustomersCount);
        summary.put("deliveryProgress", progressPercent);

        return summary;
    }

    @Override
    public List<DeliveryDto> getMonthlyDeliveries(Long customerId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        List<Delivery> deliveries = deliveryRepository.findByCustomer_IdAndDeliveryDateBetween(customerId, startDate, endDate);
        return deliveries.stream().map(this::convertToDto).toList();
    }

    @Override
    public MonthlySummaryDto getMonthlySummary(Long customerId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        List<Delivery> deliveries = deliveryRepository.findByCustomer_IdAndDeliveryDateBetween(customerId, startDate, endDate);

        if (deliveries.isEmpty()) {
            throw new ResourceNotFoundException("No deliveries found for this month");
        }

        double totalMilk = deliveries.stream().mapToDouble(Delivery::getTotalMilk).sum();
        double totalExtraMilk = deliveries.stream().mapToDouble(Delivery::getExtraMilk).sum();

        return MonthlySummaryDto.builder()
                .customerId(customerId)
                .customerName(deliveries.get(0).getCustomer().getCustomerName())
                .totalDeliveries(deliveries.size())
                .totalMilk(totalMilk)
                .totalExtraMilk(totalExtraMilk)
                .build();
    }

    private DeliveryDto convertToDto(Delivery delivery) {
        return DeliveryDto.builder()
                .id(delivery.getId())
                .customerId(delivery.getCustomer().getId())
                .customerName(delivery.getCustomer().getCustomerName())
                .userId(delivery.getUser().getId())
                .milkmanName(delivery.getUser().getFirstName() + " " + delivery.getUser().getLastName())
                .deliveryDate(delivery.getDeliveryDate())
                .deliveryTime(delivery.getDeliveryTime())
                .milkQuantity(delivery.getMilkQuantity())
                .extraMilk(delivery.getExtraMilk())
                .totalMilk(delivery.getTotalMilk())
                .deliveryStatus(delivery.getDeliveryStatus())
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}