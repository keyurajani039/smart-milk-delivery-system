package com.example.milkdelivery.controller;

import com.example.milkdelivery.dto.SubscriptionPaymentRequest;
import com.example.milkdelivery.entity.SubscriptionPaymentLog;
import com.example.milkdelivery.entity.SubscriptionPlan;
import com.example.milkdelivery.entity.User;
import com.example.milkdelivery.exception.ResourceNotFoundException;
import com.example.milkdelivery.repository.SubscriptionPaymentLogRepository;
import com.example.milkdelivery.repository.SubscriptionPlanRepository;
import com.example.milkdelivery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionPlanRepository planRepository;

    @Autowired
    private SubscriptionPaymentLogRepository paymentLogRepository;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckoutOrder(@RequestParam Long userId, @RequestParam Long planId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found"));

        try {
            // If standard API credentials are mock or network is offline during local tests, simulate order creation
            if (keyId.startsWith("rzp_test_mock")) {
                Map<String, Object> response = new HashMap<>();
                response.put("razorpayOrderId", "order_mock_" + System.currentTimeMillis());
                response.put("amount", (int)(plan.getPrice() * 100));
                response.put("currency", "INR");
                response.put("keyId", keyId);
                response.put("status", "created");
                return ResponseEntity.ok(response);
            }

            com.razorpay.RazorpayClient razorpay = new com.razorpay.RazorpayClient(keyId, keySecret);
            
            org.json.JSONObject orderRequest = new org.json.JSONObject();
            orderRequest.put("amount", (int)(plan.getPrice() * 100)); // amount in the smallest currency unit (paise)
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_sub_" + userId + "_" + planId);

            org.json.JSONObject notes = new org.json.JSONObject();
            notes.put("userId", String.valueOf(userId));
            notes.put("planId", String.valueOf(planId));
            orderRequest.put("notes", notes);

            com.razorpay.Order order = razorpay.orders.create(orderRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("razorpayOrderId", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("currency", order.get("currency"));
            response.put("keyId", keyId);
            response.put("status", order.get("status"));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Razorpay Order creation failed: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody SubscriptionPaymentRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found"));

        boolean isSignatureValid = false;
        try {
            // Validate Razorpay Payment Signature
            if (keyId.startsWith("rzp_test_mock") && "mock_signature".equals(request.getRazorpaySignature())) {
                isSignatureValid = true;
            } else {
                org.json.JSONObject attributes = new org.json.JSONObject();
                attributes.put("razorpay_order_id", request.getRazorpayOrderId());
                attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
                attributes.put("razorpay_signature", request.getRazorpaySignature());
                isSignatureValid = com.razorpay.Utils.verifyPaymentSignature(attributes, keySecret);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Signature verification failed: " + e.getMessage());
        }

        if (isSignatureValid) {
            // Extend user subscription
            LocalDateTime base = user.getSubscriptionEndDate() != null && user.getSubscriptionEndDate().isAfter(LocalDateTime.now())
                    ? user.getSubscriptionEndDate()
                    : LocalDateTime.now();
            
            user.setSubscriptionEndDate(base.plusDays(plan.getDurationDays()));
            user.setSubscriptionPlanId(plan.getId());
            userRepository.save(user);

            // Log subscription payment transaction
            SubscriptionPaymentLog paymentLog = SubscriptionPaymentLog.builder()
                    .user(user)
                    .subscriptionPlan(plan)
                    .razorpayOrderId(request.getRazorpayOrderId())
                    .razorpayPaymentId(request.getRazorpayPaymentId())
                    .amount(plan.getPrice())
                    .paymentStatus("SUCCESS")
                    .createdAt(LocalDateTime.now())
                    .build();
            paymentLogRepository.save(paymentLog);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Subscription updated successfully");
            response.put("subscriptionEndDate", user.getSubscriptionEndDate());
            response.put("planName", plan.getName());

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body("Invalid payment signature");
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody String payload, @RequestHeader("X-Razorpay-Signature") String signature) {
        try {
            // Verify Webhook Signature if webhookSecret is configured
            if (!keyId.startsWith("rzp_test_mock")) {
                boolean isValid = com.razorpay.Utils.verifyWebhookSignature(payload, signature, webhookSecret);
                if (!isValid) {
                    return ResponseEntity.badRequest().body("Invalid webhook signature");
                }
            }

            // Parse webhook payload
            org.json.JSONObject json = new org.json.JSONObject(payload);
            String event = json.optString("event");

            if ("payment.captured".equals(event)) {
                org.json.JSONObject paymentEntity = json.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");
                
                String orderId = paymentEntity.optString("order_id");
                
                org.json.JSONObject notes = paymentEntity.has("notes") && !paymentEntity.isNull("notes")
                        ? paymentEntity.optJSONObject("notes") 
                        : null;
                
                if (notes != null && notes.has("userId") && notes.has("planId")) {
                    Long userId = Long.parseLong(notes.getString("userId"));
                    Long planId = Long.parseLong(notes.getString("planId"));

                    User user = userRepository.findById(userId).orElse(null);
                    SubscriptionPlan plan = planRepository.findById(planId).orElse(null);

                    if (user != null && plan != null) {
                        LocalDateTime base = user.getSubscriptionEndDate() != null && user.getSubscriptionEndDate().isAfter(LocalDateTime.now())
                                ? user.getSubscriptionEndDate()
                                : LocalDateTime.now();
                        
                        user.setSubscriptionEndDate(base.plusDays(plan.getDurationDays()));
                        user.setSubscriptionPlanId(plan.getId());
                        userRepository.save(user);

                        // Log subscription payment transaction
                        SubscriptionPaymentLog paymentLog = SubscriptionPaymentLog.builder()
                                .user(user)
                                .subscriptionPlan(plan)
                                .razorpayOrderId(orderId)
                                .razorpayPaymentId(paymentEntity.optString("id"))
                                .amount(plan.getPrice())
                                .paymentStatus("CAPTURED_WEBHOOK")
                                .createdAt(LocalDateTime.now())
                                .build();
                        paymentLogRepository.save(paymentLog);
                    }
                }
            }

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Webhook processing failed: " + e.getMessage());
        }
    }
}
