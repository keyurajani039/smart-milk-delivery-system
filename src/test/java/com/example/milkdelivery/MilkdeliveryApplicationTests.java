package com.example.milkdelivery;

import com.example.milkdelivery.entity.Customer;
import com.example.milkdelivery.entity.MilkCategory;
import com.example.milkdelivery.entity.PlanCancellation;
import com.example.milkdelivery.repository.CustomerRepository;
import com.example.milkdelivery.repository.MilkCategoryRepository;
import com.example.milkdelivery.repository.PlanCancellationRepository;
import com.example.milkdelivery.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MilkdeliveryApplicationTests {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MilkCategoryRepository milkCategoryRepository;

    @Autowired
    private PlanCancellationRepository planCancellationRepository;

    @Autowired
    private com.example.milkdelivery.service.PaymentService paymentService;

    @Test
    void testCancelAndReactivatePlan() {
        // Create category if not present
        MilkCategory cat = milkCategoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> milkCategoryRepository.save(new MilkCategory(null, "Test Category", 50.0, true)));

        Customer customer = Customer.builder()
                .customerName("Integration Test Customer")
                .phoneNumber("9000000009")
                .address("Test Address")
                .milkQuantity(2.0)
                .extraMilk(1.5)
                .extraMilkDays(3)
                .isPaused(true)
                .pauseDays(5)
                .active(true)
                .deliveryCompleted(false)
                .milkCategory(cat)
                .build();

        Customer saved = customerRepository.save(customer);
        assertNotNull(saved.getId());

        // Cancel plan (Deactivate)
        customerService.deleteCustomer(saved.getId());

        // Reload customer
        Customer deactivated = customerRepository.findById(saved.getId()).orElseThrow();
        assertFalse(deactivated.getActive());
        assertEquals(0.0, deactivated.getExtraMilk());
        assertEquals(0, deactivated.getExtraMilkDays());
        assertFalse(deactivated.getIsPaused());
        assertEquals(0, deactivated.getPauseDays());

        // Check plan cancellation saved
        List<PlanCancellation> cancellations = planCancellationRepository.findByCustomerId(saved.getId());
        assertEquals(1, cancellations.size());
        assertEquals("Admin deleted/cancelled customer plan via Web App", cancellations.get(0).getReason());

        // Reactivate plan (Activate)
        customerService.activateCustomer(saved.getId());

        // Reload customer
        Customer reactivated = customerRepository.findById(saved.getId()).orElseThrow();
        assertTrue(reactivated.getActive());
        assertEquals(0.0, reactivated.getExtraMilk());
        assertFalse(reactivated.getIsPaused());
    }

    @Autowired
    private com.example.milkdelivery.repository.UserRepository userRepository;

    @Autowired
    private com.example.milkdelivery.repository.PaymentRepository paymentRepository;

    @Autowired
    private com.example.milkdelivery.repository.SubscriptionPlanRepository planRepository;

    @Autowired
    private com.example.milkdelivery.controller.SubscriptionController subscriptionController;

    @Autowired
    private com.example.milkdelivery.repository.SubscriptionPaymentLogRepository paymentLogRepository;

    @Test
    void testGenerateUpiQrCodeAutofill() throws Exception {
        // Create a Milkman User
        com.example.milkdelivery.entity.User milkman = com.example.milkdelivery.entity.User.builder()
                .firstName("Test")
                .lastName("Milkman")
                .milkCompanyName("Surat Fresh Milk Cooperative")
                .phoneNumber("9900990099")
                .email("testqr@freshmilk.com")
                .upiId("testvpa@paytm")
                .build();
        com.example.milkdelivery.entity.User savedMilkman = userRepository.save(milkman);

        // Create a Customer
        MilkCategory cat = milkCategoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> milkCategoryRepository.save(new MilkCategory(null, "Test Category", 50.0, true)));
        Customer customer = Customer.builder()
                .customerName("G-Pay Customer")
                .phoneNumber("8877665544")
                .address("Surat")
                .milkQuantity(1.5)
                .deliveryCompleted(false)
                .active(true)
                .milkCategory(cat)
                .user(savedMilkman)
                .build();
        Customer savedCustomer = customerRepository.save(customer);

        // Create a Payment
        com.example.milkdelivery.entity.Payment payment = com.example.milkdelivery.entity.Payment.builder()
                .customer(savedCustomer)
                .amount(250.75)
                .month(6)
                .year(2026)
                .paymentStatus(com.example.milkdelivery.enums.PaymentStatus.UNPAID)
                .build();
        com.example.milkdelivery.entity.Payment savedPayment = paymentRepository.save(payment);

        // Generate QR code
        String qrBase64 = paymentService.generateUpiQrCode(savedPayment.getId());
        assertNotNull(qrBase64);
        assertFalse(qrBase64.isBlank());

        // Decode QR code back to text URL using ZXing reader
        byte[] qrBytes = java.util.Base64.getDecoder().decode(qrBase64.trim());
        java.awt.image.BufferedImage bufferedImage = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(qrBytes));
        com.google.zxing.LuminanceSource source = new com.google.zxing.client.j2se.BufferedImageLuminanceSource(bufferedImage);
        com.google.zxing.BinaryBitmap bitmap = new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));
        com.google.zxing.Result qrResult = new com.google.zxing.qrcode.QRCodeReader().decode(bitmap);

        String decodedUrl = qrResult.getText();
        System.out.println("DECODED UPI URL: " + decodedUrl);

        // Verify parameters
        assertTrue(decodedUrl.startsWith("upi://pay?"), "URL should start with upi://pay?");
        assertTrue(decodedUrl.contains("pa=testvpa@paytm"), "Should contain valid VPA suffix for payee address");
        assertTrue(decodedUrl.contains("pn=Surat%20Fresh%20Milk%20Cooperative"), "Should contain URL-encoded merchant name");
        assertTrue(decodedUrl.contains("am=250.75"), "Should contain correctly formatted transaction amount");
        assertTrue(decodedUrl.contains("cu=INR"), "Should contain currency code INR");
    }

    @Test
    void testSubscriptionCheckoutAndVerification() {
        // Create user with unique phone number to avoid database constraint violations
        com.example.milkdelivery.entity.User milkman = com.example.milkdelivery.entity.User.builder()
                .firstName("Sub")
                .lastName("Test")
                .milkCompanyName("Sub Co")
                .phoneNumber("9090909090")
                .email("subtest@freshmilk.com")
                .build();
        com.example.milkdelivery.entity.User savedUser = userRepository.save(milkman);

        // Create plan
        com.example.milkdelivery.entity.SubscriptionPlan plan = com.example.milkdelivery.entity.SubscriptionPlan.builder()
                .name("Premium Pro Plan")
                .price(999.00)
                .durationDays(30)
                .maxCustomers(100)
                .build();
        com.example.milkdelivery.entity.SubscriptionPlan savedPlan = planRepository.save(plan);

        // 1. Checkout Test
        org.springframework.http.ResponseEntity<?> checkoutResponse = subscriptionController.createCheckoutOrder(savedUser.getId(), savedPlan.getId());
        assertEquals(org.springframework.http.HttpStatus.OK, checkoutResponse.getStatusCode());
        
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) checkoutResponse.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("razorpayOrderId"));
        assertEquals("INR", body.get("currency"));

        // 2. Verification Test
        String mockOrderId = (String) body.get("razorpayOrderId");
        com.example.milkdelivery.dto.SubscriptionPaymentRequest verificationRequest = com.example.milkdelivery.dto.SubscriptionPaymentRequest.builder()
                .razorpayOrderId(mockOrderId)
                .razorpayPaymentId("pay_mock_123456")
                .razorpaySignature("mock_signature")
                .userId(savedUser.getId())
                .planId(savedPlan.getId())
                .build();

        org.springframework.http.ResponseEntity<?> verificationResponse = subscriptionController.verifyPayment(verificationRequest);
        assertEquals(org.springframework.http.HttpStatus.OK, verificationResponse.getStatusCode());

        java.util.Map<String, Object> verifyBody = (java.util.Map<String, Object>) verificationResponse.getBody();
        assertNotNull(verifyBody);
        assertEquals("success", verifyBody.get("status"));

        // Check user subscription extended
        com.example.milkdelivery.entity.User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertNotNull(updatedUser.getSubscriptionEndDate());
        assertTrue(updatedUser.getSubscriptionEndDate().isAfter(java.time.LocalDateTime.now()));
        assertEquals(savedPlan.getId(), updatedUser.getSubscriptionPlanId());

        // Check subscription payment log is saved
        List<com.example.milkdelivery.entity.SubscriptionPaymentLog> logs = paymentLogRepository.findByUser_Id(savedUser.getId());
        assertEquals(1, logs.size());
        com.example.milkdelivery.entity.SubscriptionPaymentLog log = logs.get(0);
        assertEquals(mockOrderId, log.getRazorpayOrderId());
        assertEquals("pay_mock_123456", log.getRazorpayPaymentId());
        assertEquals("SUCCESS", log.getPaymentStatus());
        assertEquals(savedPlan.getPrice(), log.getAmount());
    }

    @Autowired
    private com.example.milkdelivery.service.UserService userService;

    @Test
    void testFirebaseTokenAuthentication() {
        // Create user with phone number 9595959595
        com.example.milkdelivery.entity.User milkman = com.example.milkdelivery.entity.User.builder()
                .firstName("Firebase")
                .lastName("User")
                .milkCompanyName("Firebase Company")
                .phoneNumber("9595959595")
                .email("firebase@example.com")
                .role(com.example.milkdelivery.enums.Role.DELIVERY_MAN)
                .status(com.example.milkdelivery.enums.UserStatus.ACTIVE)
                .build();
        userRepository.save(milkman);

        // Initiate login with mock Firebase ID Token matching phone suffix
        com.example.milkdelivery.dto.FirebaseLoginRequest request = new com.example.milkdelivery.dto.FirebaseLoginRequest();
        request.setIdToken("mock_firebase_token_9595959595");
        request.setDeviceId("device_integration_test_123");

        com.example.milkdelivery.dto.LoginResponse response = userService.loginWithFirebase(request);
        assertNotNull(response);
        assertEquals("Login successful via Firebase", response.getMessage());
        assertNotNull(response.getToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("9595959595", response.getUser().getPhoneNumber());

        // Test with invalid mock token/phone that is not registered
        com.example.milkdelivery.dto.FirebaseLoginRequest invalidRequest = new com.example.milkdelivery.dto.FirebaseLoginRequest();
        invalidRequest.setIdToken("mock_firebase_token_0000000000");

        assertThrows(RuntimeException.class, () -> {
            userService.loginWithFirebase(invalidRequest);
        });
    }
}

