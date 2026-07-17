package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.entity.Customer;
import com.example.milkdelivery.entity.Delivery;
import com.example.milkdelivery.entity.ExtraCustomerSale;
import com.example.milkdelivery.entity.Payment;
import com.example.milkdelivery.enums.PaymentStatus;
import com.example.milkdelivery.repository.CustomerRepository;
import com.example.milkdelivery.repository.DeliveryRepository;
import com.example.milkdelivery.repository.ExtraCustomerSaleRepository;
import com.example.milkdelivery.repository.PaymentRepository;
import com.example.milkdelivery.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ExtraCustomerSaleRepository extraSaleRepository;

    @Override
    @Cacheable(value = "dashboard", key = "#milkmanId")
    public Map<String, Object> getDashboardStats(Long milkmanId) {
        Map<String, Object> stats = new HashMap<>();

        List<Customer> allCustomers = customerRepository.findByUser_Id(milkmanId);
        long totalCustomers = allCustomers.size();
        long activeCustomers = allCustomers.stream().filter(Customer::getActive).count();
        long inactiveCustomers = totalCustomers - activeCustomers;

        List<Delivery> todayDeliveries = deliveryRepository.findByUser_IdAndDeliveryDate(milkmanId, LocalDate.now());
        long todayDeliveriesCount = todayDeliveries.stream()
                .filter(d -> d.getDeliveryStatus() != null && "DELIVERED".equalsIgnoreCase(d.getDeliveryStatus().trim()))
                .count();

        double todayMilkQuantity = todayDeliveries.stream()
                .filter(d -> d.getDeliveryStatus() != null && "DELIVERED".equalsIgnoreCase(d.getDeliveryStatus().trim()))
                .mapToDouble(Delivery::getTotalMilk)
                .sum();

        List<Payment> allPayments = paymentRepository.findByCustomer_User_Id(milkmanId);
        double pendingPayments = allPayments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.UNPAID || p.getPaymentStatus() == PaymentStatus.PARTIAL)
                .mapToDouble(Payment::getAmount)
                .sum();

        double paidPayments = allPayments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(Payment::getAmount)
                .sum();

        // Extra customer sales today
        List<ExtraCustomerSale> extraSales = extraSaleRepository.findByUserIdAndSaleDate(milkmanId, LocalDate.now());
        double todayExtraSales = extraSales.stream().mapToDouble(ExtraCustomerSale::getAmountCollected).sum();

        double totalCollections = paidPayments + todayExtraSales;

        stats.put("totalCustomers", totalCustomers);
        stats.put("activeCustomers", activeCustomers);
        stats.put("inactiveCustomers", inactiveCustomers);
        stats.put("todayDeliveries", todayDeliveriesCount);
        stats.put("todayMilkQuantity", todayMilkQuantity);
        stats.put("pendingPayments", pendingPayments);
        stats.put("paidPayments", paidPayments);
        stats.put("totalCollections", totalCollections);
        stats.put("todayExtraSalesAmount", todayExtraSales);

        return stats;
    }

    @Override
    public List<Map<String, Object>> getWeeklyStats(Long milkmanId) {
        List<Map<String, Object>> weeklyData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);

        List<Delivery> deliveries = deliveryRepository.findByUser_IdAndDeliveryDateBetween(milkmanId, startDate, today);
        List<ExtraCustomerSale> extraSales = extraSaleRepository.findByUserIdAndSaleDateBetween(milkmanId, startDate, today);
        List<Payment> payments = paymentRepository.findByCustomer_User_Id(milkmanId);

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);

            // Filter deliveries for the day
            double milkQty = deliveries.stream()
                    .filter(d -> d.getDeliveryDate().equals(date) && d.getDeliveryStatus() != null && "DELIVERED".equalsIgnoreCase(d.getDeliveryStatus().trim()))
                    .mapToDouble(Delivery::getTotalMilk)
                    .sum();

            // Filter extra sales for the day
            double extraSaleAmt = extraSales.stream()
                    .filter(s -> s.getSaleDate().equals(date))
                    .mapToDouble(ExtraCustomerSale::getAmountCollected)
                    .sum();

            // Filter paid payments on this date
            double paidAmt = payments.stream()
                    .filter(p -> p.getPaymentDate() != null && p.getPaymentDate().equals(date) && p.getPaymentStatus() == PaymentStatus.PAID)
                    .mapToDouble(Payment::getAmount)
                    .sum();

            double collections = paidAmt + extraSaleAmt;

            Map<String, Object> dayStats = new HashMap<>();
            dayStats.put("date", date.toString());
            dayStats.put("milkQuantity", milkQty);
            dayStats.put("collections", collections);

            weeklyData.add(dayStats);
        }

        return weeklyData;
    }
}
