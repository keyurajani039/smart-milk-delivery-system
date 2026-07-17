package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.dto.PaymentDto;
import com.example.milkdelivery.entity.Customer;
import com.example.milkdelivery.entity.Delivery;
import com.example.milkdelivery.entity.Payment;
import com.example.milkdelivery.entity.User;
import com.example.milkdelivery.enums.PaymentStatus;
import com.example.milkdelivery.enums.PaymentType;
import com.example.milkdelivery.exception.ResourceNotFoundException;
import com.example.milkdelivery.repository.CustomerRepository;
import com.example.milkdelivery.repository.DeliveryRepository;
import com.example.milkdelivery.repository.PaymentRepository;
import com.example.milkdelivery.service.PaymentService;
import com.example.milkdelivery.service.TelegramService;
import com.example.milkdelivery.util.QrCodeGeneratorUtil;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private TelegramService telegramService;

    @Override
    @CacheEvict(value = "payments", allEntries = true)
    public Payment savePayment(Payment payment) {
        Customer customer = customerRepository.findById(payment.getCustomer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        payment.setCustomer(customer);
        return paymentRepository.save(payment);
    }

    @Override
    @Cacheable(value = "payments", key = "#customerId")
    public List<PaymentDto> getPaymentsByCustomer(Long customerId) {
        List<Payment> payments = paymentRepository.findByCustomer_Id(customerId);
        return payments.stream().map(this::convertToDto).toList();
    }

    @Override
    @CacheEvict(value = {"payments", "dashboard"}, allEntries = true)
    public String markPaymentPaid(Long paymentId, PaymentDto paymentDto) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaymentType(PaymentType.valueOf(paymentDto.getPaymentType().toUpperCase()));
        payment.setPaymentDate(LocalDate.now());
        payment.setRemarks(paymentDto.getRemarks() != null ? paymentDto.getRemarks() : "Paid");
        paymentRepository.save(payment);

        // Notify customer via Telegram
        Customer customer = payment.getCustomer();
        telegramService.sendPaymentReceivedNotification(
                customer.getTelegramId(),
                customer.getCustomerName(),
                payment.getAmount()
        );

        return "Payment marked as paid successfully";
    }

    @Override
    @CacheEvict(value = {"payments", "dashboard"}, allEntries = true)
    public Payment generateBill(Long customerId, int month, int year) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        // Sum delivered milk volumes
        List<Delivery> deliveries = deliveryRepository.findByCustomer_IdAndDeliveryDateBetween(customerId, startDate, endDate);
        double totalDeliveredMilk = deliveries.stream()
                .filter(d -> d.getDeliveryStatus() != null && "DELIVERED".equalsIgnoreCase(d.getDeliveryStatus().trim()))
                .mapToDouble(Delivery::getTotalMilk)
                .sum();

        double pricePerLiter = customer.getMilkCategory().getPricePerLiter();
        double totalAmount = totalDeliveredMilk * pricePerLiter;

        // Check duplicate bill
        Optional<Payment> existingBill = paymentRepository.findByCustomerIdAndMonthAndYear(customerId, month, year);
        if (existingBill.isPresent()) {
            Payment bill = existingBill.get();
            if (bill.getPaymentStatus() == PaymentStatus.UNPAID) {
                bill.setAmount(totalAmount);
                bill = paymentRepository.save(bill);
            }
            // Notify customer via Telegram even if bill already exists
            telegramService.sendBillGeneratedNotification(
                    customer.getTelegramId(),
                    customer.getCustomerName(),
                    bill.getAmount(),
                    month,
                    year
            );
            return bill;
        }

        Payment payment = Payment.builder()
                .customer(customer)
                .month(month)
                .year(year)
                .amount(totalAmount)
                .paymentStatus(PaymentStatus.UNPAID)
                .remarks("Monthly invoice generated dynamically")
                .createdAt(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(payment);

        // Notify customer via Telegram
        telegramService.sendBillGeneratedNotification(
                customer.getTelegramId(),
                customer.getCustomerName(),
                totalAmount,
                month,
                year
        );

        return saved;
    }

    @Override
    public byte[] generateInvoicePdf(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Recalculate and update amount if unpaid to ensure database and PDF remain in sync
        if (payment.getPaymentStatus() == PaymentStatus.UNPAID) {
            double latestAmount = calculateLatestAmount(payment);
            payment.setAmount(latestAmount);
            payment = paymentRepository.save(payment);
        }

        Customer customer = payment.getCustomer();
        User milkman = customer.getUser();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // Invoice Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("SMART MILK DELIVERY SYSTEM INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Metadata info
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLACK);

            document.add(new Paragraph("Invoice No: INV-" + payment.getId() + "-" + payment.getYear() + payment.getMonth(), boldFont));
            document.add(new Paragraph("Date: " + LocalDate.now(), regularFont));
            document.add(new Paragraph("Billing Cycle: " + payment.getMonth() + "/" + payment.getYear(), regularFont));
            document.add(new Paragraph("Milkman: " + (milkman != null ? milkman.getFirstName() + " " + milkman.getLastName() : "N/A"), regularFont));
            document.add(new Paragraph("\n"));

            // 2 column table for Customer and Merchant details
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);

            PdfPCell cellLeft = new PdfPCell(new Paragraph("BILLED TO:\n" +
                    customer.getCustomerName() + "\n" +
                    customer.getAddress() + "\n" +
                    "Phone: " + customer.getPhoneNumber(), regularFont));
            cellLeft.setBorder(Rectangle.NO_BORDER);

            PdfPCell cellRight = new PdfPCell(new Paragraph("MERCHANT DETAILS:\n" +
                    (milkman != null ? milkman.getMilkCompanyName() : "Smart Milk Inc.") + "\n" +
                    (milkman != null ? milkman.getFirstName() + " " + milkman.getLastName() : "Milkman Admin") + "\n" +
                    "Phone: " + (milkman != null ? milkman.getPhoneNumber() : "N/A"), regularFont));
            cellRight.setBorder(Rectangle.NO_BORDER);
            cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);

            infoTable.addCell(cellLeft);
            infoTable.addCell(cellRight);
            document.add(infoTable);
            document.add(new Paragraph("\n"));

            // Daily Delivery Breakdown Table
            PdfPTable itemsTable = new PdfPTable(6);
            itemsTable.setWidthPercentage(100);
            itemsTable.setSpacingBefore(10);
            itemsTable.setSpacingAfter(10);

            // Header cells
            PdfPCell h1 = new PdfPCell(new Paragraph("Date", boldFont));
            PdfPCell h2 = new PdfPCell(new Paragraph("Status", boldFont));
            PdfPCell h3 = new PdfPCell(new Paragraph("Regular (Ltrs)", boldFont));
            PdfPCell h4 = new PdfPCell(new Paragraph("Extra (Ltrs)", boldFont));
            PdfPCell h5 = new PdfPCell(new Paragraph("Total (Ltrs)", boldFont));
            PdfPCell h6 = new PdfPCell(new Paragraph("Amount", boldFont));
            BaseColor headerBg = new BaseColor(230, 230, 230);
            h1.setBackgroundColor(headerBg);
            h2.setBackgroundColor(headerBg);
            h3.setBackgroundColor(headerBg);
            h4.setBackgroundColor(headerBg);
            h5.setBackgroundColor(headerBg);
            h6.setBackgroundColor(headerBg);

            itemsTable.addCell(h1);
            itemsTable.addCell(h2);
            itemsTable.addCell(h3);
            itemsTable.addCell(h4);
            itemsTable.addCell(h5);
            itemsTable.addCell(h6);

            // Fetch actual daily deliveries
            LocalDate startDate = LocalDate.of(payment.getYear(), payment.getMonth(), 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            List<Delivery> deliveries = deliveryRepository.findByCustomer_IdAndDeliveryDateBetween(customer.getId(), startDate, endDate);
            
            // Sort chronologically
            List<Delivery> sortedDeliveries = new java.util.ArrayList<>(deliveries);
            sortedDeliveries.sort((d1, d2) -> d1.getDeliveryDate().compareTo(d2.getDeliveryDate()));

            double pricePerLiter = customer.getMilkCategory().getPricePerLiter();
            double sumRegular = 0.0;
            double sumExtra = 0.0;
            double sumTotal = 0.0;
            double sumAmount = 0.0;

            for (Delivery d : sortedDeliveries) {
                String status = d.getDeliveryStatus() != null ? d.getDeliveryStatus() : "N/A";
                double regQty = d.getMilkQuantity() != null ? d.getMilkQuantity() : 0.0;
                double extQty = d.getExtraMilk() != null ? d.getExtraMilk() : 0.0;
                double totQty = d.getTotalMilk() != null ? d.getTotalMilk() : 0.0;

                double amount = 0.0;
                if (status != null && "DELIVERED".equalsIgnoreCase(status.trim())) {
                    amount = totQty * pricePerLiter;
                } else {
                    regQty = 0.0;
                    extQty = 0.0;
                    totQty = 0.0;
                }

                sumRegular += regQty;
                sumExtra += extQty;
                sumTotal += totQty;
                sumAmount += amount;

                itemsTable.addCell(d.getDeliveryDate().toString());
                itemsTable.addCell(status);
                itemsTable.addCell(String.format("%.2f", regQty));
                itemsTable.addCell(String.format("%.2f", extQty));
                itemsTable.addCell(String.format("%.2f", totQty));
                itemsTable.addCell(String.format("₹%.2f", amount));
            }

            // Total row
            PdfPCell totalLabelCell = new PdfPCell(new Paragraph("TOTAL", boldFont));
            totalLabelCell.setColspan(2);
            totalLabelCell.setBackgroundColor(headerBg);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            itemsTable.addCell(totalLabelCell);

            PdfPCell totalRegularCell = new PdfPCell(new Paragraph(String.format("%.2f", sumRegular), boldFont));
            totalRegularCell.setBackgroundColor(headerBg);
            itemsTable.addCell(totalRegularCell);

            PdfPCell totalExtraCell = new PdfPCell(new Paragraph(String.format("%.2f", sumExtra), boldFont));
            totalExtraCell.setBackgroundColor(headerBg);
            itemsTable.addCell(totalExtraCell);

            PdfPCell totalQtyCell = new PdfPCell(new Paragraph(String.format("%.2f", sumTotal), boldFont));
            totalQtyCell.setBackgroundColor(headerBg);
            itemsTable.addCell(totalQtyCell);

            PdfPCell totalAmountCell = new PdfPCell(new Paragraph(String.format("₹%.2f", sumAmount), boldFont));
            totalAmountCell.setBackgroundColor(headerBg);
            itemsTable.addCell(totalAmountCell);

            document.add(itemsTable);
            document.add(new Paragraph("\n"));

            // Status details
            document.add(new Paragraph("Payment Status: " + payment.getPaymentStatus().name(), boldFont));
            document.add(new Paragraph("Payment Method: " + (payment.getPaymentType() != null ? payment.getPaymentType().name() : "PENDING"), regularFont));
            if (payment.getPaymentDate() != null) {
                document.add(new Paragraph("Payment Date: " + payment.getPaymentDate(), regularFont));
            }

            // Embedded QR Code for scan and pay
            if (payment.getPaymentStatus() == PaymentStatus.UNPAID) {
                document.add(new Paragraph("\n"));
                String qrBase64 = generateUpiQrCode(payment.getId());
                if (qrBase64 != null && !qrBase64.isBlank()) {
                    try {
                        byte[] qrBytes = java.util.Base64.getDecoder().decode(qrBase64.trim());
                        Image qrImage = Image.getInstance(qrBytes);
                        qrImage.scaleAbsolute(120f, 120f);
                        qrImage.setAlignment(Element.ALIGN_CENTER);

                        Paragraph scanLabel = new Paragraph("SCAN & PAY WITH ANY UPI APP", boldFont);
                        scanLabel.setAlignment(Element.ALIGN_CENTER);
                        scanLabel.setSpacingAfter(5);

                        document.add(scanLabel);
                        document.add(qrImage);
                    } catch (Exception e) {
                        logger.error("Failed to embed QR code in PDF", e);
                    }
                }
            }

            document.add(new Paragraph("\nThank you for your business!", boldFont));

            document.close();
        } catch (Exception e) {
            logger.error("Error creating PDF invoice: {}", e.getMessage());
        }

        return out.toByteArray();
    }

    @Override
    public String generateUpiQrCode(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Recalculate and update amount if unpaid to ensure QR code has latest amount
        if (payment.getPaymentStatus() == PaymentStatus.UNPAID) {
            double latestAmount = calculateLatestAmount(payment);
            payment.setAmount(latestAmount);
            payment = paymentRepository.save(payment);
        }

        Customer customer = payment.getCustomer();
        User milkman = customer.getUser();

        // Structure a valid payee address (VPA) handle (ending with @upi) for payment apps
        String upiId = "merchant@upi";
        if (milkman != null) {
            if (milkman.getUpiId() != null && !milkman.getUpiId().isBlank()) {
                upiId = milkman.getUpiId();
            } else if (milkman.getPhoneNumber() != null && !milkman.getPhoneNumber().isBlank()) {
                upiId = milkman.getPhoneNumber() + "@upi";
            } else if (milkman.getEmail() != null && !milkman.getEmail().isBlank()) {
                String email = milkman.getEmail();
                if (email.contains("@")) {
                    upiId = email.substring(0, email.indexOf("@")) + "@upi";
                } else {
                    upiId = email + "@upi";
                }
            }
        }

        String merchantName = (milkman != null) ? milkman.getMilkCompanyName() : "MilkmanMerchant";
        // Fully URL-encode merchant name to prevent format corruption, replacing "+" with "%20" for UPI compatibility
        String encodedMerchantName;
        try {
            encodedMerchantName = java.net.URLEncoder.encode(merchantName, java.nio.charset.StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
        } catch (Exception e) {
            encodedMerchantName = merchantName.replace(" ", "%20");
        }

        // Construct standard UPI URL with formatted amount autofill
        String upiUrl = String.format("upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=Milk%20Bill%20%d-%d",
                upiId, encodedMerchantName, payment.getAmount(), payment.getMonth(), payment.getYear());

        return QrCodeGeneratorUtil.generateUpiQrCodeBase64(upiUrl, 250, 250);
    }

    @Override
    public void runAutoMonthlyBilling() {
        LocalDate prevMonthDate = LocalDate.now().minusMonths(1);
        int month = prevMonthDate.getMonthValue();
        int year = prevMonthDate.getYear();

        logger.info("Executing automated bulk monthly billing for {}/{}", month, year);

        List<Customer> activeCustomers = customerRepository.findAll().stream()
                .filter(Customer::getActive)
                .toList();

        for (Customer customer : activeCustomers) {
            try {
                generateBill(customer.getId(), month, year);
                logger.info("Monthly bill generated for customer id: {}", customer.getId());
            } catch (Exception e) {
                logger.error("Failed to generate monthly bill for customer id {}: {}", customer.getId(), e.getMessage());
            }
        }
        logger.info("Automated bulk billing complete.");
    }

    private double calculateLatestAmount(Payment payment) {
        Customer customer = payment.getCustomer();
        LocalDate startDate = LocalDate.of(payment.getYear(), payment.getMonth(), 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        List<Delivery> deliveries = deliveryRepository.findByCustomer_IdAndDeliveryDateBetween(customer.getId(), startDate, endDate);
        double totalDeliveredMilk = deliveries.stream()
                .filter(d -> d.getDeliveryStatus() != null && "DELIVERED".equalsIgnoreCase(d.getDeliveryStatus().trim()))
                .mapToDouble(Delivery::getTotalMilk)
                .sum();
        double pricePerLiter = customer.getMilkCategory().getPricePerLiter();
        return totalDeliveredMilk * pricePerLiter;
    }

    private PaymentDto convertToDto(Payment payment) {
        return PaymentDto.builder()
                .id(payment.getId())
                .customerId(payment.getCustomer().getId())
                .customerName(payment.getCustomer().getCustomerName())
                .month(payment.getMonth())
                .year(payment.getYear())
                .amount(payment.getAmount())
                .paymentStatus(payment.getPaymentStatus().name())
                .paymentType(payment.getPaymentType() != null ? payment.getPaymentType().name() : null)
                .paymentDate(payment.getPaymentDate())
                .remarks(payment.getRemarks())
                .build();
    }
}