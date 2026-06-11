package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.entity.*;
import com.example.milkdelivery.enums.*;
import com.example.milkdelivery.repository.*;
import com.example.milkdelivery.service.DummyDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DummyDataServiceImpl implements DummyDataService, CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MilkCategoryRepository milkCategoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ExtraCustomerSaleRepository extraSaleRepository;

    @Autowired
    private TrackingRepository trackingRepository;

    @Autowired
    private DeliverySessionRepository sessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Run on startup if database has no users
        if (userRepository.count() == 0) {
            System.out.println("No users found in database. Seeding dummy data on startup...");
            seedData();
        }
    }

    @Override
    public void seedData() {
        System.out.println("Starting database seeding process...");

        // 1. Seed Milkman (User)
        User milkman = userRepository.findByPhoneNumber("9327304535").orElseGet(() -> {
            User u = User.builder()
                    .firstName("Keyur")
                    .lastName("Ajani")
                    .milkCompanyName("Surat Fresh Milk Cooperative")
                    .phoneNumber("9327304535")
                    .email("keyur@freshmilk.com")
                    .password(passwordEncoder.encode("coopPassword123"))
                    .role(Role.DELIVERY_MAN)
                    .status(UserStatus.ACTIVE)
                    .deviceId("mobile_android_uuid_12345")
                    .upiId("merchantvpa@ybl")
                    .blocked(false)
                    .trialStartDate(LocalDateTime.now().minusDays(10))
                    .trialEndDate(LocalDateTime.now().plusDays(57))
                    .subscriptionPlanId(1L)
                    .authProvider(AuthProvider.LOCAL)
                    .build();
            return userRepository.save(u);
        });

        // 2. Seed Milk Categories
        List<MilkCategory> cowMilkList = milkCategoryRepository.findByCategoryName("Cow Milk");
        MilkCategory cowMilk = cowMilkList.isEmpty() ? milkCategoryRepository.save(
                MilkCategory.builder()
                        .categoryName("Cow Milk")
                        .pricePerLiter(60.0)
                        .active(true)
                        .build()
        ) : cowMilkList.get(0);

        List<MilkCategory> buffaloMilkList = milkCategoryRepository.findByCategoryName("Buffalo Milk");
        MilkCategory buffaloMilk = buffaloMilkList.isEmpty() ? milkCategoryRepository.save(
                MilkCategory.builder()
                        .categoryName("Buffalo Milk")
                        .pricePerLiter(70.0)
                        .active(true)
                        .build()
        ) : buffaloMilkList.get(0);

        // 3. Seed Customers
        Customer aarav = customerRepository.findByPhoneNumber("9988776655").orElseGet(() -> {
            Customer c = Customer.builder()
                    .customerName("Aarav Mehta")
                    .phoneNumber("9988776655")
                    .address("G-402 Shanti Niketan, Ahmedabad")
                    .latitude(23.0225)
                    .longitude(72.5714)
                    .milkQuantity(1.5)
                    .active(true)
                    .telegramId("12345678")
                    .milkCategory(cowMilk)
                    .user(milkman)
                    .extraMilk(0.0)
                    .extraMilkDays(0)
                    .isPaused(false)
                    .pauseDays(0)
                    .deliveryCompleted(false)
                    .build();
            return customerRepository.save(c);
        });

        Customer khushi = customerRepository.findByPhoneNumber("8401516824").orElseGet(() -> {
            Customer c = Customer.builder()
                    .customerName("khushi")
                    .phoneNumber("8401516824")
                    .address("B-201 Green Valley, Ahmedabad")
                    .latitude(23.0300)
                    .longitude(72.5800)
                    .milkQuantity(2.0)
                    .active(true)
                    .telegramId("98765432")
                    .milkCategory(cowMilk)
                    .user(milkman)
                    .extraMilk(0.0)
                    .extraMilkDays(0)
                    .isPaused(false)
                    .pauseDays(0)
                    .deliveryCompleted(false)
                    .build();
            return customerRepository.save(c);
        });

        // 4. Seed Deliveries (Last 45 days up to yesterday)
        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(45);

        // Fetch existing deliveries count
        long existingDeliveries = deliveryRepository.count();
        if (existingDeliveries < 10) {
            System.out.println("Seeding daily delivery records...");
            List<Delivery> deliveriesToSave = new ArrayList<>();

            for (LocalDate date = startDay; date.isBefore(today); date = date.plusDays(1)) {
                // Aarav Mehta (1.5 qty regular)
                String aaravStatus = "DELIVERED";
                double aaravReg = 1.5;
                double aaravExtra = 0.0;

                // Simulate skipped and paused days
                if (date.getDayOfMonth() == 10) {
                    aaravStatus = "SKIPPED";
                    aaravReg = 0.0;
                } else if (date.getDayOfMonth() == 25) {
                    aaravStatus = "SKIPPED";
                    aaravReg = 0.0;
                } else if (date.getMonthValue() == today.getMonthValue() && date.getDayOfMonth() == 2) {
                    aaravStatus = "PAUSED";
                    aaravReg = 0.0;
                }

                // Simulate extra milk requests
                if ("DELIVERED".equals(aaravStatus) && date.getDayOfMonth() % 7 == 0) {
                    aaravExtra = 1.0;
                }

                Delivery d1 = Delivery.builder()
                        .customer(aarav)
                        .user(milkman)
                        .deliveryDate(date)
                        .deliveryTime(date.atTime(7, 30))
                        .milkQuantity(aaravReg)
                        .extraMilk(aaravExtra)
                        .totalMilk(aaravReg + aaravExtra)
                        .deliveryStatus(aaravStatus)
                        .latitude(aarav.getLatitude())
                        .longitude(aarav.getLongitude())
                        .createdAt(date.atTime(7, 30))
                        .build();
                deliveriesToSave.add(d1);

                // Khushi (2.0 qty regular)
                String khushiStatus = "DELIVERED";
                double khushiReg = 2.0;

                if (date.getDayOfMonth() == 15) {
                    khushiStatus = "SKIPPED";
                    khushiReg = 0.0;
                } else if (date.getMonthValue() == today.getMonthValue() && date.getDayOfMonth() == 5) {
                    khushiStatus = "PAUSED";
                    khushiReg = 0.0;
                }

                Delivery d2 = Delivery.builder()
                        .customer(khushi)
                        .user(milkman)
                        .deliveryDate(date)
                        .deliveryTime(date.atTime(7, 45))
                        .milkQuantity(khushiReg)
                        .extraMilk(0.0)
                        .totalMilk(khushiReg)
                        .deliveryStatus(khushiStatus)
                        .latitude(khushi.getLatitude())
                        .longitude(khushi.getLongitude())
                        .createdAt(date.atTime(7, 45))
                        .build();
                deliveriesToSave.add(d2);
            }
            deliveryRepository.saveAll(deliveriesToSave);
        }

        // 5. Seed Payments (Monthly Bills)
        // Let's generate a bill for the previous month (May 2026 if today is June 2026)
        LocalDate prevMonthDate = today.minusMonths(1);
        int billMonth = prevMonthDate.getMonthValue();
        int billYear = prevMonthDate.getYear();

        // Check if Aarav Mehta has a bill for previous month
        Optional<Payment> aaravBill = paymentRepository.findByCustomerIdAndMonthAndYear(aarav.getId(), billMonth, billYear);
        if (aaravBill.isEmpty()) {
            System.out.println("Generating May bill for Aarav...");
            LocalDate start = LocalDate.of(billYear, billMonth, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            List<Delivery> aaravDeliveries = deliveryRepository.findByCustomer_IdAndDeliveryDateBetween(aarav.getId(), start, end);
            double totalAaravMilk = aaravDeliveries.stream()
                    .filter(d -> "DELIVERED".equalsIgnoreCase(d.getDeliveryStatus()))
                    .mapToDouble(Delivery::getTotalMilk)
                    .sum();
            double aaravAmount = totalAaravMilk * cowMilk.getPricePerLiter();

            Payment p = Payment.builder()
                    .customer(aarav)
                    .month(billMonth)
                    .year(billYear)
                    .amount(aaravAmount)
                    .paymentStatus(PaymentStatus.UNPAID)
                    .remarks("Monthly invoice generated dynamically")
                    .createdAt(LocalDateTime.now())
                    .build();
            paymentRepository.save(p);
        }

        // Check if Khushi has a bill for previous month
        Optional<Payment> khushiBill = paymentRepository.findByCustomerIdAndMonthAndYear(khushi.getId(), billMonth, billYear);
        if (khushiBill.isEmpty()) {
            System.out.println("Generating May bill for Khushi...");
            LocalDate start = LocalDate.of(billYear, billMonth, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            List<Delivery> khushiDeliveries = deliveryRepository.findByCustomer_IdAndDeliveryDateBetween(khushi.getId(), start, end);
            double totalKhushiMilk = khushiDeliveries.stream()
                    .filter(d -> "DELIVERED".equalsIgnoreCase(d.getDeliveryStatus()))
                    .mapToDouble(Delivery::getTotalMilk)
                    .sum();
            double khushiAmount = totalKhushiMilk * cowMilk.getPricePerLiter();

            Payment p = Payment.builder()
                    .customer(khushi)
                    .month(billMonth)
                    .year(billYear)
                    .amount(khushiAmount)
                    .paymentStatus(PaymentStatus.PAID)
                    .paymentType(PaymentType.CASH)
                    .paymentDate(LocalDate.now().minusDays(5))
                    .remarks("Paid cash to delivery man")
                    .createdAt(LocalDateTime.now())
                    .build();
            paymentRepository.save(p);
        }

        // 6. Seed Extra Customer Sales
        if (extraSaleRepository.count() == 0) {
            System.out.println("Seeding extra walk-up customer sales...");
            ExtraCustomerSale s1 = ExtraCustomerSale.builder()
                    .user(milkman)
                    .quantityLiters(3.0)
                    .amountCollected(180.0)
                    .paymentType("CASH")
                    .saleDate(today.minusDays(2))
                    .notes("Walk-up customer cash sale")
                    .build();

            ExtraCustomerSale s2 = ExtraCustomerSale.builder()
                    .user(milkman)
                    .quantityLiters(5.0)
                    .amountCollected(300.0)
                    .paymentType("UPI")
                    .saleDate(today.minusDays(5))
                    .notes("Direct sale via QR")
                    .build();

            extraSaleRepository.save(s1);
            extraSaleRepository.save(s2);
        }

        // 7. Seed Tracking
        if (trackingRepository.count() == 0) {
            System.out.println("Seeding location tracking logs...");
            Tracking t1 = Tracking.builder()
                    .userId(milkman.getId())
                    .latitude(21.1702)
                    .longitude(72.8311)
                    .speed(12.5)
                    .timestamp(LocalDateTime.now().minusMinutes(30))
                    .build();

            Tracking t2 = Tracking.builder()
                    .userId(milkman.getId())
                    .latitude(21.1824)
                    .longitude(72.8402)
                    .speed(15.0)
                    .timestamp(LocalDateTime.now().minusMinutes(10))
                    .build();

            trackingRepository.save(t1);
            trackingRepository.save(t2);
        }

        // 8. Seed Delivery Sessions
        if (sessionRepository.count() == 0) {
            System.out.println("Seeding delivery sessions...");
            DeliverySession sess = DeliverySession.builder()
                    .user(milkman)
                    .loadedMilkQuantity(100.0)
                    .startedAt(LocalDateTime.now().minusDays(1).withHour(7).withMinute(0))
                    .endedAt(LocalDateTime.now().minusDays(1).withHour(9).withMinute(30))
                    .active(false)
                    .createdAt(LocalDateTime.now().minusDays(1).withHour(7).withMinute(0))
                    .build();
            sessionRepository.save(sess);
        }

        System.out.println("Database seeding process completed successfully.");
    }
}
