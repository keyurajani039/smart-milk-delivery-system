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
}
