package com.example.milkdelivery.controller;

import com.example.milkdelivery.entity.Customer;
import com.example.milkdelivery.entity.MilkCategory;
import com.example.milkdelivery.entity.Payment;
import com.example.milkdelivery.entity.User;
import com.example.milkdelivery.repository.CustomerRepository;
import com.example.milkdelivery.repository.PaymentRepository;
import com.example.milkdelivery.repository.UserRepository;
import com.example.milkdelivery.entity.Tracking;
import com.example.milkdelivery.service.CustomerService;
import com.example.milkdelivery.service.OllamaAiService;
import com.example.milkdelivery.service.PaymentService;
import com.example.milkdelivery.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/telegram")
public class TelegramBotController {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotController.class);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OllamaAiService aiService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private com.example.milkdelivery.repository.PlanCancellationRepository planCancellationRepository;

    @Value("${telegram.bot.token:mock_telegram_token}")
    private String botToken;

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String CUSTOM_KEYBOARD_JSON = "{\"keyboard\":[[" +
            "{\"text\":\"📊 બિલ (Bill)\"},{\"text\":\"💳 ચુકવણી (Payment)\"}]," +
            "[{\"text\":\"🥛 વધારે દૂધ (Extra Milk)\"},{\"text\":\"❌ વધારે દૂધ રદ (Cancel Extra)\"}]," +
            "[{\"text\":\"📍 ટ્રેક લોકેશન (Track)\"},{\"text\":\"⏸ બંધ કરો (Pause)\"},{\"text\":\"▶ ફરી શરૂ કરો (Resume)\"}]," +
            "[{\"text\":\"❌ કાયમી બંધ (Cancel Plan)\"}" +
            "]],\"resize_keyboard\":true,\"one_time_keyboard\":false}";

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> update) {
        logger.info("Received Telegram Update payload: {}", update);

        if (!update.containsKey("message")) {
            return ResponseEntity.ok("No message found");
        }

        Map<String, Object> message = (Map<String, Object>) update.get("message");
        Map<String, Object> from = (Map<String, Object>) message.get("from");
        Map<String, Object> chat = (Map<String, Object>) message.get("chat");

        if (from == null || chat == null) {
            return ResponseEntity.ok("Invalid message components");
        }

        String telegramId = from.get("id").toString();
        String chatInstanceId = chat.get("id").toString();
        String text = message.containsKey("text") ? message.get("text").toString().trim() : "";
        boolean isVoice = message.containsKey("voice");

        // Handle Gujarati voice parsing (Whisper integration)
        if (isVoice) {
            Map<String, Object> voice = (Map<String, Object>) message.get("voice");
            String fileId = voice != null ? (String) voice.get("file_id") : null;
            int duration = 0;
            if (voice != null && voice.containsKey("duration")) {
                duration = ((Number) voice.get("duration")).intValue();
            }

            String transcription = null;
            if (fileId != null && openAiApiKey != null && !openAiApiKey.isBlank()) {
                transcription = transcribeVoiceMessage(fileId);
            }

            if (transcription != null && !transcription.isBlank()) {
                text = transcription;
                logger.info("Parsed Voice Message via OpenAI Whisper transcript: {}", text);
            } else {
                // Duration-based fallback
                if (duration > 0 && duration <= 3) {
                    text = "મિલ્કમેન ક્યાં છે?"; // Track Location
                } else if (duration > 3 && duration <= 6) {
                    text = "મને કાલે ૨ લીટર દૂધ વધારે આપજો"; // Extra Milk (2.0 liters)
                } else if (duration > 6 && duration <= 9) {
                    text = "દૂધ ૫ દિવસ માટે બંધ કરજો"; // Pause Delivery (5 days)
                } else if (duration > 9 && duration <= 12) {
                    text = "મારું ચાલુ મહિનાનું બિલ જણાવો"; // Check Bill
                } else if (duration > 12 && duration <= 15) {
                    text = "વધારે દૂધ કેન્સલ કરો"; // Cancel Extra Milk
                } else if (duration > 15) {
                    text = "દૂધ ફરીથી ચાલુ કરો"; // Resume Delivery
                } else {
                    text = "મને કાલે ૨ લીટર દૂધ વધારે આપજો"; // Default fallback
                }
                logger.info("Parsed Voice Message of duration {}s via Whisper transcript mock: {}", duration, text);

                if (openAiApiKey == null || openAiApiKey.isBlank()) {
                    String voiceTip = "💡 [Tip] વોઈસ મેસેજ ટ્રાન્સક્રિપ્શન ચાલુ કરવા માટે OpenAI API Key કન્ફિગર કરો.\n" +
                                      "(To enable real voice message transcription, please configure your OpenAI API Key.)";
                    sendTelegramMessage(chatInstanceId, voiceTip);
                } else {
                    String voiceErrorTip = "⚠️ [OpenAI Whisper Error] Transcription failed (possibly due to an expired or out-of-quota OpenAI API Key).\n" +
                                           "Falling back to duration-based mock parsing (Duration: " + duration + "s).";
                    sendTelegramMessage(chatInstanceId, voiceErrorTip);
                }
            }
        }

        // 1. Check if the message contains a contact object
        if (message.containsKey("contact")) {
            Map<String, Object> contact = (Map<String, Object>) message.get("contact");
            if (contact != null && contact.containsKey("phone_number")) {
                String rawPhone = contact.get("phone_number").toString().replaceAll("\\D", "");
                String phone = rawPhone.length() >= 10 ? rawPhone.substring(rawPhone.length() - 10) : rawPhone;

                Optional<Customer> customerOpt = customerRepository.findByPhoneNumber(phone);
                if (customerOpt.isPresent()) {
                    Customer customer = customerOpt.get();
                    customer.setTelegramId(telegramId);
                    customerRepository.save(customer);

                    String welcomeSuccess = "\u0AA8\u0AAE\u0AB8\u0ACD\u0AA4\u0AC7! " + customer.getCustomerName() + "\n" +
                            "\u0AA4\u0AAE\u0ABE\u0AB0\u0AC1\u0A82 \u0A9F\u0AC7\u0AB2\u0ABF\u0A97\u0ACD\u0AB0\u0ABE\u0AAE \u0A8F\u0A95\u0ABE\u0A89\u0AA8\u0ACD\u0A9F \u0AB8\u0AAB\u0AB3\u0AA4\u0AAA\u0AC2\u0AB0\u0ACD\u0AB5\u0A95 \u0AB2\u0AC0\u0A82\u0A95 \u0AA5\u0A88 \u0A97\u0AAF\u0AC1\u0A82 \u0A9B\u0AC7.\n\n" +
                            "તમે નીચેના કમાન્ડનો ઉપયોગ કરી શકો છો:\n" +
                            "📊 /bill - તમારું ચાલુ મહિનાનું બિલ જોવા માટે\n" +
                            "💳 /payment - ઓનલાઈન ચુકવણી QR કોડ મેળવવા\n" +
                            "⏸ /pause - દૂધ બંધ (Pause) કરવાની વિનંતી\n" +
                            "▶ /resume - દૂધ ફરીથી ચાલુ કરવાની વિનંતી\n" +
                            "🥛 /extra - કાલે વધારે દૂધ (Extra Milk) મેળવવા માટે (દા.ત. /extra 2)\n" +
                            "❌ /extra cancel - વધારે દૂધ રદ કરવા માટે\n" +
                            "📍 /track - મિલ્કમેનનું લોકેશન ટ્રેક કરવા માટે\n" +
                            "💬 અથવા તમે ગુજરાતીમાં ટાઈપ કરીને અથવા વોઈસ મેસેજ મોકલીને પણ ઓર્ડર બદલી શકો છો.";
                    sendTelegramMessageWithReplyMarkup(chatInstanceId, welcomeSuccess, CUSTOM_KEYBOARD_JSON);

                    // Notify the Milkman (User) if they have a mapped Telegram ID
                    if (customer.getUser() != null && customer.getUser().getTelegramId() != null && !customer.getUser().getTelegramId().isBlank()) {
                        String milkmanNotify = "\u0A97\u0ACD\u0AB0\u0ABE\u0AB9\u0A95 " + customer.getCustomerName() + " (ફોન: " + customer.getPhoneNumber() + ") એ સફળતાપૂર્વક ટેલિગ્રામ પર જોડાણ કર્યું છે.";
                        sendTelegramMessage(customer.getUser().getTelegramId(), milkmanNotify);
                    }

                    return ResponseEntity.ok("Successfully mapped via contact");
                } else {
                    // Check if it matches a User (Milkman)
                    Optional<User> userOpt = userRepository.findByPhoneNumber(phone);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        user.setTelegramId(telegramId);
                        userRepository.save(user);

                        String welcomeUser = "\u0AA8\u0AAE\u0AB8\u0ACD\u0AA4\u0AC7! " + user.getFirstName() + " " + user.getLastName() + "\n" +
                                "તમે સ્માર્ટ મિલ્ક ડિલિવરી સિસ્ટમના મિલ્કમેન તરીકે સફળતાપૂર્વક લીંક થઈ ગયા છો.";
                        sendTelegramMessage(chatInstanceId, welcomeUser);
                        return ResponseEntity.ok("Successfully mapped user via contact");
                    } else {
                        String notFoundMsg = "\u0AA6\u0ABF\u0AB2\u0A97\u0AC0\u0AB0 \u0A9B\u0AC0\u0A8F, \u0A86 \u0AAB\u0ACB\u0AA8 \u0AA8\u0A82\u0AAC\u0AB0 (" + phone + ") \u0A85\u0AAE\u0ABE\u0AB0\u0AC0 \u0AB8\u0ABF\u0AB8\u0ACD\u0A9F\u0AAE\u0AAE\u0ABE\u0A82 \u0AA8\u0ACB\u0A82\u0AA7\u0ABE\u0AAF\u0AC7\u0AB2 \u0AA8\u0AA5\u0AC0. \u0A95\u0AC3\u0AAA\u0ABE \u0A95\u0AB0\u0AC0\u0AA8\u0AC7 \u0AA4\u0AAE\u0ABE\u0AB0\u0ABE \u0AAE\u0ABF\u0AB2\u0ACD\u0A95\u0AAE\u0AC7\u0AA8\u0AA8\u0ACB \u0AB8\u0A82\u0AAA\u0AB0\u0ACD\u0A95 \u0A95\u0AB0\u0ACB.";
                        sendTelegramMessage(chatInstanceId, notFoundMsg);
                        return ResponseEntity.ok("Phone not registered");
                    }
                }
            }
        }

        // Find customer mapped to this Telegram ID
        List<Customer> customers = customerRepository.findAll().stream()
                .filter(c -> telegramId.equals(c.getTelegramId()))
                .toList();

        if (customers.isEmpty()) {
            // Check if it matches a User (Milkman)
            Optional<User> userOpt = userRepository.findByTelegramId(telegramId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String welcomeMsg = "\u0AA8\u0AAE\u0AB8\u0ACD\u0AA4\u0AC7! " + user.getFirstName() + " (" + user.getMilkCompanyName() + ")\n" +
                        "તમે મિલ્કમેન તરીકે લીંક છો. તમે વેબ એપ્લિકેશન દ્વારા ગ્રાહકોનું સંચાલન કરી શકો છો.";
                sendTelegramMessage(chatInstanceId, welcomeMsg);
                return ResponseEntity.ok("Success for Milkman");
            }

            // Ask to share contact if neither customer nor user
            String welcomeMsg = "\u0AA8\u0AAE\u0AB8\u0ACD\u0AA4\u0AC7! \u0AB8\u0ACD\u0AAE\u0ABE\u0AB0\u0ACD\u0A9F \u0AAE\u0ABF\u0AB2\u0ACD\u0A95 \u0AA1\u0ABF\u0AB2\u0ABF\u0AB5\u0AB0\u0AC0 \u0AB8\u0ABF\u0AB8\u0ACD\u0A9F\u0AAE\u0AAE\u0ABE\u0A82 \u0AA4\u0AAE\u0ABE\u0AB0\u0AC1\u0A82 \u0AB8\u0ACD\u0AB5\u0ABE\u0A97\u0AA4 \u0A9B\u0AC7. " +
                    "\u0AA4\u0AAE\u0ABE\u0AB0\u0AC1\u0A82 \u0A8F\u0A95\u0ABE\u0A89\u0AA8\u0ACD\u0A9F \u0AB2\u0AC0\u0A82\u0A95 \u0A95\u0AB0\u0AB5\u0ABE \u0AAE\u0ABE\u0A9F\u0AC7 \u0A95\u0AC3\u0AAA\u0ABE \u0A95\u0AB0\u0AC0\u0AA8\u0AC7 \u0AA8\u0AC0\u0A9A\u0AC7 \u0A86\u0AAA\u0AC7\u0AB2\u0ABE \u0AAC\u0A9F\u0AA8 \u0AAA\u0AB0 \u0A95\u0ACD\u0AB2\u0ABF\u0A95 \u0A95\u0AB0\u0AC0\u0AA8\u0AC7 \u0AA4\u0AAE\u0ABE\u0AB0\u0ACB \u0AAB\u0ACB\u0AA8 \u0AA8\u0A82\u0AAC\u0AB0 \u0AB6\u0AC7\u0AB0 \u0A95\u0AB0\u0ACB.\n\n" +
                    "Welcome! Please click the button below to share your contact number to link your account.";

            String replyMarkupJson = "{\"keyboard\":[[{\"text\":\"\uD83D\uDCF1 \u0AAB\u0ACB\u0AA8 \u0AA8\u0A82\u0AAC\u0AB0 \u0AB6\u0AC7\u0AB0 \u0A95\u0AB0\u0ACB (Share Contact)\",\"request_contact\":true}]],\"one_time_keyboard\":true,\"resize_keyboard\":true}";

            sendTelegramMessageWithReplyMarkup(chatInstanceId, welcomeMsg, replyMarkupJson);
            return ResponseEntity.ok("Not registered");
        }

        Customer customer = customers.get(0);

        // Map Telegram custom keyboard buttons to standard commands
        if (text.contains("બિલ (Bill)")) {
            text = "/bill";
        } else if (text.contains("ચુકવણી (Payment)")) {
            text = "/payment";
        } else if (text.contains("વધારે દૂધ રદ (Cancel Extra)")) {
            text = "/extra cancel";
        } else if (text.contains("વધારે દૂધ (Extra Milk)")) {
            text = "/extra";
        } else if (text.contains("ટ્રેક લોકેશન (Track)")) {
            text = "/track";
        } else if (text.contains("બંધ કરો (Pause)")) {
            text = "/pause";
        } else if (text.contains("ફરી શરૂ કરો (Resume)")) {
            text = "/resume";
        } else if (text.contains("કાયમી બંધ (Cancel Plan)") || text.contains("કાયમી બંધ કરો")) {
            text = "/cancel_plan";
        }

        // Handle permanently inactive (cancelled) customers
        if (!Boolean.TRUE.equals(customer.getActive())) {
            boolean isStartIntent = text.startsWith("/start_plan") || text.startsWith("/start") ||
                    text.equalsIgnoreCase("start") || text.equalsIgnoreCase("start plan") ||
                    text.equalsIgnoreCase("start_plan") || text.contains("ચાલુ કરો") ||
                    text.contains("શરૂ કરો");
            
            if (!isStartIntent) {
                // Also check AI parsing if OpenAI / local parser returns RESUME_DELIVERY or starts plan
                try {
                    Map<String, Object> intent = aiService.parseGujaratiRequest(text);
                    if ("RESUME_DELIVERY".equals(intent.get("action"))) {
                        isStartIntent = true;
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }

            if (isStartIntent) {
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

                String reactivateMsg = "નમસ્તે " + customer.getCustomerName() + ",\n" +
                        "તમારો મિલ્ક ડિલિવરી પ્લાન સફળતાપૂર્વક ફરીથી શરૂ (Reactivate) કરવામાં આવ્યો છે!\n\n" +
                        "તમે નીચેના કમાન્ડનો ઉપયોગ કરી શકો છો:\n" +
                        "📊 /bill - તમારું ચાલુ મહિનાનું બિલ જોવા માટે\n" +
                        "💳 /payment - ઓનલાઈન ચુકવણી QR કોડ મેળવવા\n" +
                        "⏸ /pause - દૂધ બંધ (Pause) કરવાની વિનંતી\n" +
                        "▶ /resume - દૂધ ફરીથી ચાલુ કરવાની વિનંતી\n" +
                        "🥛 /extra - કાલે વધારે દૂધ (Extra Milk) મેળવવા માટે (દા.ત. /extra 2)\n" +
                        "❌ /extra cancel - વધારે દૂધ રદ કરવા માટે\n" +
                        "❌ /cancel_plan - મિલ્ક ડિલિવરી પ્લાન કાયમી બંધ કરવા માટે\n" +
                        "📍 /track - મિલ્કમેનનું લોકેશન ટ્રેક કરવા માટે\n" +
                        "💬 અથવા તમે ગુજરાતીમાં ટાઈપ કરીને અથવા વોઈસ મેસેજ મોકલીને પણ ઓર્ડર બદલી શકો છો.";
                sendTelegramMessageWithReplyMarkup(chatInstanceId, reactivateMsg, CUSTOM_KEYBOARD_JSON);
                return ResponseEntity.ok("Plan reactivated successfully");
            } else {
                String inactiveMsg = "તમારો મિલ્ક ડિલિવરી પ્લાન હાલમાં કાયમી બંધ (Permanently Cancelled) છે.\n\n" +
                        "નવો પ્લાન શરૂ કરવા માટે કૃપા કરીને /start_plan અથવા /start મોકલો.";
                sendTelegramMessage(chatInstanceId, inactiveMsg);
                return ResponseEntity.ok("Customer is inactive");
            }
        }

        if (text.startsWith("/start") || text.startsWith("/help")) {
            String helpMsg = "નમસ્તે " + customer.getCustomerName() + ",\n" +
                    "તમે નીચેના કમાન્ડનો ઉપયોગ કરી શકો છો:\n" +
                    "📊 /bill - તમારું ચાલુ મહિનાનું બિલ જોવા માટે\n" +
                    "💳 /payment - ઓનલાઈન ચુકવણી QR કોડ મેળવવા\n" +
                    "⏸ /pause - દૂધ બંધ (Pause) કરવાની વિનંતી\n" +
                    "▶ /resume - દૂધ ફરીથી ચાલુ કરવાની વિનંતી\n" +
                    "🥛 /extra - કાલે વધારે દૂધ (Extra Milk) મેળવવા માટે (દા.ત. /extra 2)\n" +
                    "❌ /extra cancel - વધારે દૂધ રદ કરવા માટે\n" +
                    "❌ /cancel_plan - મિલ્ક ડિલિવરી પ્લાન કાયમી બંધ કરવા માટે\n" +
                    "📍 /track - મિલ્કમેનનું લોકેશન ટ્રેક કરવા માટે\n" +
                    "💬 અથવા તમે ગુજરાતીમાં ટાઈપ કરીને અથવા વોઈસ મેસેજ મોકલીને પણ ઓર્ડર બદલી શકો છો.";
            sendTelegramMessageWithReplyMarkup(chatInstanceId, helpMsg, CUSTOM_KEYBOARD_JSON);
        } else if (text.startsWith("/bill")) {
            LocalDate now = LocalDate.now();
            Payment bill = paymentService.generateBill(customer.getId(), now.getMonthValue(), now.getYear());
            String response = String.format("તમારું ચાલુ મહિનાનું બિલ: ₹%.2f (Status: %s).\nઇન્વોઇસ ડાઉનલોડ કરવા ક્લિક કરો: http://localhost:8081/api/payments/invoice/%d",
                    bill.getAmount(), bill.getPaymentStatus().name(), bill.getId());
            sendTelegramMessageWithReplyMarkup(chatInstanceId, response, CUSTOM_KEYBOARD_JSON);
            try {
                byte[] pdfBytes = paymentService.generateInvoicePdf(bill.getId());
                if (pdfBytes != null && pdfBytes.length > 0) {
                    sendTelegramDocument(chatInstanceId, pdfBytes, "invoice-" + bill.getId() + ".pdf");
                }
            } catch (Exception e) {
                logger.error("Failed to generate or send PDF invoice document", e);
            }
        } else if (text.startsWith("/payment")) {
            LocalDate now = LocalDate.now();
            Optional<Payment> billOpt = paymentRepository.findByCustomerIdAndMonthAndYear(customer.getId(), now.getMonthValue(), now.getYear());
            if (billOpt.isEmpty()) {
                sendTelegramMessageWithReplyMarkup(chatInstanceId, "કોઈ બાકી બિલ મળ્યું નથી. (No pending bills found.)", CUSTOM_KEYBOARD_JSON);
            } else {
                String response = "ચુકવણી કરવા માટે નીચેની લિંક પરથી UPI QR કોડ સ્કેન કરો:\n" +
                        "QR Code Link: http://localhost:8081/api/payments/qr/" + billOpt.get().getId();
                sendTelegramMessageWithReplyMarkup(chatInstanceId, response, CUSTOM_KEYBOARD_JSON);
                try {
                    byte[] pdfBytes = paymentService.generateInvoicePdf(billOpt.get().getId());
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        sendTelegramDocument(chatInstanceId, pdfBytes, "invoice-" + billOpt.get().getId() + ".pdf");
                    }
                } catch (Exception e) {
                    logger.error("Failed to generate or send PDF invoice document", e);
                }
                try {
                    String qrBase64 = paymentService.generateUpiQrCode(billOpt.get().getId());
                    if (qrBase64 != null && !qrBase64.isBlank()) {
                        byte[] qrBytes = java.util.Base64.getDecoder().decode(qrBase64.trim());
                        sendTelegramDocument(chatInstanceId, qrBytes, "payment-qr-" + billOpt.get().getId() + ".png");
                    }
                } catch (Exception e) {
                    logger.error("Failed to generate or send QR code image", e);
                }
            }
        } else if (text.startsWith("/pause")) {
            int days = 5;
            String[] parts = text.split("\\s+");
            if (parts.length > 1) {
                String daysStr = parts[1];
                StringBuilder cleanStr = new StringBuilder();
                for (char c : daysStr.toCharArray()) {
                    if (c >= '\u0AE6' && c <= '\u0AEF') {
                        cleanStr.append((char) (c - '\u0AE6' + '0'));
                    } else if (Character.isDigit(c)) {
                        cleanStr.append(c);
                    }
                }
                if (cleanStr.length() > 0) {
                    try {
                        days = Integer.parseInt(cleanStr.toString());
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
            customer.setIsPaused(true);
            customer.setPauseDays(days);
            customer.setPauseStartDate(LocalDate.now());
            customer.setPauseEndDate(LocalDate.now().plusDays(days));
            customerRepository.save(customer);
            String pauseMsg = String.format(
                    "દૂધ વિતરણ %d દિવસ માટે બંધ (Pause) કરવામાં આવ્યું છે.\n\n" +
                    "Milk delivery has been paused for %d days.",
                    days, days
            );
            sendTelegramMessageWithReplyMarkup(chatInstanceId, pauseMsg, CUSTOM_KEYBOARD_JSON);
        } else if (text.startsWith("/resume")) {
            customerService.resumeCustomer(customer.getId());
            sendTelegramMessageWithReplyMarkup(chatInstanceId, "દૂધ વિતરણ સફળતાપૂર્વક ફરીથી શરૂ કરવામાં આવ્યું છે.", CUSTOM_KEYBOARD_JSON);
        } else if (text.startsWith("/cancel_plan")) {
            // Log cancellation record
            com.example.milkdelivery.entity.PlanCancellation cancellation = com.example.milkdelivery.entity.PlanCancellation.builder()
                    .customer(customer)
                    .cancellationDate(java.time.LocalDateTime.now())
                    .reason("Customer requested permanent cancellation via Telegram command")
                    .build();
            planCancellationRepository.save(cancellation);

            // Deactivate customer and clear plans state
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

            String cancelPlanMsg = "તમારો મિલ્ક ડિલિવરી પ્લાન કાયમી ધોરણે બંધ કરવામાં આવ્યો છે અને કન્ફર્મેશન સેવ કરવામાં આવ્યું છે.\n\n" +
                    "ભવિષ્યમાં ફરીથી શરૂ કરવા માટે તમે ગમે ત્યારે /start_plan મોકલી શકો છો.\n\n" +
                    "Your milk delivery plan has been permanently cancelled. You can send /start_plan anytime to start a new plan.";
            sendTelegramMessage(chatInstanceId, cancelPlanMsg);
        } else if (text.startsWith("/track") || text.startsWith("/where") || text.startsWith("/location")) {
            handleTrackingRequest(customer, chatInstanceId);
        } else if (text.startsWith("/extra")) {
            double qty = 1.0;
            int days = 1;
            boolean isCancel = false;
            String[] parts = text.split("\\s+");
            if (parts.length > 1) {
                String qtyStr = parts[1].toLowerCase();
                if ("cancel".equals(qtyStr) || "કેન્સલ".equals(qtyStr) || "રદ".equals(qtyStr)) {
                    isCancel = true;
                } else {
                    qty = parseDoubleSupportGujarati(qtyStr);
                    if (qty <= 0) {
                        isCancel = true;
                    }
                }
            }
            if (isCancel) {
                customer.setExtraMilk(0.0);
                customer.setExtraMilkDays(0);
                customer.setExtraMilkStartDate(null);
                customerRepository.save(customer);
                
                String cancelMsg = "વધારાનું દૂધ કેન્સલ કરવામાં આવ્યું છે.\n\nExtra milk request has been cancelled.";
                sendTelegramMessageWithReplyMarkup(chatInstanceId, cancelMsg, CUSTOM_KEYBOARD_JSON);
            } else {
                if (parts.length > 2) {
                    days = parseIntSupportGujarati(parts[2]);
                    if (days <= 0) days = 1;
                }
                customer.setExtraMilk(qty);
                customer.setExtraMilkDays(days);
                customer.setExtraMilkStartDate(LocalDate.now().plusDays(1));
                customerRepository.save(customer);
                
                String extraMsg;
                if (days > 1) {
                    extraMsg = String.format(
                            "તમારી વિનંતી સ્વીકારવામાં આવી છે. તમને %d દિવસ માટે %.1f લીટર વધારે દૂધ આપવામાં આવશે.\n\n" +
                            "Your request is accepted. You will be delivered %.1f liters of extra milk for %d days.",
                            days, qty, qty, days
                    );
                } else {
                    extraMsg = String.format(
                            "તમારી વિનંતી સ્વીકારવામાં આવી છે. કાલે તમને %.1f લીટર વધારે દૂધ આપવામાં આવશે.\n\n" +
                            "Your request is accepted. Tomorrow you will be delivered %.1f liters of extra milk.",
                            qty, qty
                    );
                }
                sendTelegramMessageWithReplyMarkup(chatInstanceId, extraMsg, CUSTOM_KEYBOARD_JSON);
            }
        } else {
            // Conversational input using Llama 3 parser
            Map<String, Object> intent = aiService.parseGujaratiRequest(text);
            String action = intent.get("action").toString();

            if ("EXTRA_MILK".equals(action)) {
                double qty = Double.parseDouble(intent.get("quantity").toString());
                int days = intent.containsKey("days") ? Integer.parseInt(intent.get("days").toString()) : 1;
                if (days <= 0) days = 1;
                customer.setExtraMilk(qty);
                customer.setExtraMilkDays(days);
                customer.setExtraMilkStartDate(LocalDate.now().plusDays(1));
                customerRepository.save(customer);

                String msg;
                if (days > 1) {
                    msg = String.format(
                            "તમારી વિનંતી સ્વીકારવામાં આવી છે. તમને %d દિવસ માટે %.1f લીટર વધારે દૂધ આપવામાં આવશે.\n\n" +
                            "Your request is accepted. You will be delivered %.1f liters of extra milk for %d days.",
                            days, qty, qty, days
                    );
                } else {
                    msg = String.format(
                            "તમારી વિનંતી સ્વીકારવામાં આવી છે. કાલે તમને %.1f લીટર વધારે દૂધ આપવામાં આવશે.\n\n" +
                            "Your request is accepted. Tomorrow you will be delivered %.1f liters of extra milk.",
                            qty, qty
                    );
                }
                sendTelegramMessageWithReplyMarkup(chatInstanceId, msg, CUSTOM_KEYBOARD_JSON);
            } else if ("CANCEL_EXTRA_MILK".equals(action)) {
                customer.setExtraMilk(0.0);
                customer.setExtraMilkDays(0);
                customer.setExtraMilkStartDate(null);
                customerRepository.save(customer);
                
                String msg = "વધારાનું દૂધ કેન્સલ કરવામાં આવ્યું છે.\n\nExtra milk request has been cancelled.";
                sendTelegramMessageWithReplyMarkup(chatInstanceId, msg, CUSTOM_KEYBOARD_JSON);
            } else if ("PAUSE_DELIVERY".equals(action)) {
                int days = Integer.parseInt(intent.get("days").toString());
                customer.setIsPaused(true);
                customer.setPauseDays(days);
                customer.setPauseStartDate(LocalDate.now());
                customer.setPauseEndDate(LocalDate.now().plusDays(days));
                customerRepository.save(customer);

                String msg = String.format(
                        "દૂધ વિતરણ %d દિવસ માટે બંધ કરવામાં આવ્યું છે.\n\n" +
                        "Milk delivery has been paused for %d days.",
                        days, days
                );
                sendTelegramMessageWithReplyMarkup(chatInstanceId, msg, CUSTOM_KEYBOARD_JSON);
            } else if ("RESUME_DELIVERY".equals(action)) {
                customerService.resumeCustomer(customer.getId());
                sendTelegramMessageWithReplyMarkup(chatInstanceId, "દૂધ વિતરણ સફળતાપૂર્વક ફરીથી શરૂ કરવામાં આવ્યું છે.", CUSTOM_KEYBOARD_JSON);
            } else if ("CHECK_BILL".equals(action)) {
                LocalDate now = LocalDate.now();
                Payment bill = paymentService.generateBill(customer.getId(), now.getMonthValue(), now.getYear());
                String response = String.format("તમારું ચાલુ મહિનાનું બિલ: ₹%.2f (Status: %s).", bill.getAmount(), bill.getPaymentStatus().name());
                sendTelegramMessageWithReplyMarkup(chatInstanceId, response, CUSTOM_KEYBOARD_JSON);
                try {
                    byte[] pdfBytes = paymentService.generateInvoicePdf(bill.getId());
                    if (pdfBytes != null && pdfBytes.length > 0) {
                        sendTelegramDocument(chatInstanceId, pdfBytes, "invoice-" + bill.getId() + ".pdf");
                    }
                } catch (Exception e) {
                    logger.error("Failed to generate or send PDF invoice document", e);
                }
            } else if ("CHECK_PAYMENT".equals(action)) {
                LocalDate now = LocalDate.now();
                Optional<Payment> billOpt = paymentRepository.findByCustomerIdAndMonthAndYear(customer.getId(), now.getMonthValue(), now.getYear());
                if (billOpt.isEmpty()) {
                    sendTelegramMessageWithReplyMarkup(chatInstanceId, "કોઈ બાકી બિલ મળ્યું નથી. (No pending bills found.)", CUSTOM_KEYBOARD_JSON);
                } else {
                    String response = "ચુકવણી કરવા માટે નીચેની લિંક પરથી UPI QR કોડ સ્કેન કરો:\n" +
                            "QR Code Link: http://localhost:8081/api/payments/qr/" + billOpt.get().getId();
                    sendTelegramMessageWithReplyMarkup(chatInstanceId, response, CUSTOM_KEYBOARD_JSON);
                    try {
                        byte[] pdfBytes = paymentService.generateInvoicePdf(billOpt.get().getId());
                        if (pdfBytes != null && pdfBytes.length > 0) {
                            sendTelegramDocument(chatInstanceId, pdfBytes, "invoice-" + billOpt.get().getId() + ".pdf");
                        }
                    } catch (Exception e) {
                        logger.error("Failed to generate or send PDF invoice document", e);
                    }
                    try {
                        String qrBase64 = paymentService.generateUpiQrCode(billOpt.get().getId());
                        if (qrBase64 != null && !qrBase64.isBlank()) {
                            byte[] qrBytes = java.util.Base64.getDecoder().decode(qrBase64.trim());
                            sendTelegramDocument(chatInstanceId, qrBytes, "payment-qr-" + billOpt.get().getId() + ".png");
                        }
                    } catch (Exception e) {
                        logger.error("Failed to generate or send QR code image", e);
                    }
                }
            } else if ("TRACK_LOCATION".equals(action)) {
                handleTrackingRequest(customer, chatInstanceId);
            } else if ("CANCEL_PLAN".equals(action)) {
                // Log cancellation record
                com.example.milkdelivery.entity.PlanCancellation cancellation = com.example.milkdelivery.entity.PlanCancellation.builder()
                        .customer(customer)
                        .cancellationDate(java.time.LocalDateTime.now())
                        .reason("Customer requested permanent cancellation via conversational message")
                        .build();
                planCancellationRepository.save(cancellation);

                // Deactivate customer and clear plans state
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

                String cancelPlanMsg = "તમારો મિલ્ક ડિલિવરી પ્લાન કાયમી ધોરણે બંધ કરવામાં આવ્યો છે અને કન્ફર્મેશન સેવ કરવામાં આવ્યું છે.\n\n" +
                        "ભવિષ્યમાં ફરીથી શરૂ કરવા માટે તમે ગમે ત્યારે /start_plan મોકલી શકો છો.\n\n" +
                        "Your milk delivery plan has been permanently cancelled. You can send /start_plan anytime to start a new plan.";
                sendTelegramMessage(chatInstanceId, cancelPlanMsg);
            } else {
                sendTelegramMessageWithReplyMarkup(chatInstanceId, "સમજવામાં ભૂલ થઈ. કૃપા કરીને સ્પષ્ટ કહી શકો છો? (Could not understand request. Please say it clearly.)", CUSTOM_KEYBOARD_JSON);
            }
        }

        return ResponseEntity.ok("Success");
    }

    private void sendTelegramMessage(String chatId, String msg) {
        if ("mock_telegram_token".equals(botToken)) {
            logger.info("[MOCK TELEGRAM BOT REPLY] To Chat ID: {}, Message: \n{}", chatId, msg);
            return;
        }

        try {
            String url = String.format("https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                    botToken, chatId, URLEncoder.encode(msg, StandardCharsets.UTF_8));
            java.net.URI uri = new java.net.URI(url);
            restTemplate.getForObject(uri, String.class);
        } catch (Exception e) {
            logger.error("Failed to send telegram webhook response: {}", e.getMessage());
        }
    }

    private void sendTelegramMessageWithReplyMarkup(String chatId, String msg, String replyMarkupJson) {
        if ("mock_telegram_token".equals(botToken)) {
            logger.info("[MOCK TELEGRAM BOT REPLY WITH MARKUP] To Chat ID: {}, Message: \n{}, Markup: {}", chatId, msg, replyMarkupJson);
            return;
        }

        try {
            String url = String.format("https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&reply_markup=%s",
                    botToken, chatId,
                    URLEncoder.encode(msg, StandardCharsets.UTF_8),
                    URLEncoder.encode(replyMarkupJson, StandardCharsets.UTF_8));
            java.net.URI uri = new java.net.URI(url);
            restTemplate.getForObject(uri, String.class);
        } catch (Exception e) {
            logger.error("Failed to send telegram webhook response with markup: {}", e.getMessage());
        }
    }

    private void handleTrackingRequest(Customer customer, String chatInstanceId) {
        User milkman = customer.getUser();
        if (milkman != null) {
            Tracking tracking = trackingService.getCurrentLocation(milkman.getId());
            if (tracking != null && customer.getLatitude() != null && customer.getLongitude() != null) {
                double distance = calculateDistance(
                        tracking.getLatitude(), tracking.getLongitude(),
                        customer.getLatitude(), customer.getLongitude()
                );
                int etaMinutes = (int) Math.ceil(distance * 4); // assume 4 mins per km (15 km/h)
                if (etaMinutes < 1) etaMinutes = 1;

                String milkmanName = milkman.getFirstName() + " " + milkman.getLastName();

                String trackMsg;
                if (distance < 0.1) {
                    trackMsg = String.format(
                            "📍 મિલ્કમેન %s તમારા ઘરની ખૂબ નજીક છે!\n\n" +
                            "📍 Milkman %s is very close to your location!",
                            milkmanName, milkmanName
                    );
                } else {
                    trackMsg = String.format(
                            "📍 મિલ્કમેન %s તમારાથી %.2f કિમી દૂર છે અને અંદાજે %d મિનિટમાં પહોંચશે.\n\n" +
                            "📍 Milkman %s is %.2f km away from you and will arrive in approximately %d minutes.",
                            milkmanName, distance, etaMinutes,
                            milkmanName, distance, etaMinutes
                    );
                }
                sendTelegramMessageWithReplyMarkup(chatInstanceId, trackMsg, CUSTOM_KEYBOARD_JSON);
            } else {
                String milkmanName = milkman.getFirstName() + " " + milkman.getLastName();
                String noLocationMsg = String.format(
                        "📍 મિલ્કમેન %s નું લાઈવ લોકેશન હાલમાં ઉપલબ્ધ નથી.\n\n" +
                        "📍 Live location for Milkman %s is currently not available.",
                        milkmanName, milkmanName
                );
                sendTelegramMessageWithReplyMarkup(chatInstanceId, noLocationMsg, CUSTOM_KEYBOARD_JSON);
            }
        } else {
            sendTelegramMessageWithReplyMarkup(chatInstanceId, "તમારા માટે કોઈ મિલ્કમેન મેપ થયેલ નથી. (No milkman mapped to your account.)", CUSTOM_KEYBOARD_JSON);
        }
    }

    private double parseDoubleSupportGujarati(String s) {
        StringBuilder sb = new StringBuilder();
        boolean hasDot = false;
        for (char c : s.toCharArray()) {
            if (c >= '\u0AE6' && c <= '\u0AEF') {
                sb.append((char) (c - '\u0AE6' + '0'));
            } else if (Character.isDigit(c)) {
                sb.append(c);
            } else if (c == '.' && !hasDot) {
                sb.append(c);
                hasDot = true;
            }
        }
        try {
            return sb.length() > 0 ? Double.parseDouble(sb.toString()) : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int parseIntSupportGujarati(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= '\u0AE6' && c <= '\u0AEF') {
                sb.append((char) (c - '\u0AE6' + '0'));
            } else if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        try {
            return sb.length() > 0 ? Integer.parseInt(sb.toString()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String transcribeVoiceMessage(String fileId) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return null;
        }
        try {
            String getFileUrl = String.format("https://api.telegram.org/bot%s/getFile?file_id=%s", botToken, fileId);
            Map<String, Object> fileResponse = restTemplate.getForObject(getFileUrl, Map.class);
            if (fileResponse == null || !Boolean.TRUE.equals(fileResponse.get("ok"))) {
                return null;
            }
            Map<String, Object> result = (Map<String, Object>) fileResponse.get("result");
            String filePath = (String) result.get("file_path");

            String downloadUrl = String.format("https://api.telegram.org/file/bot%s/%s", botToken, filePath);
            byte[] audioBytes = restTemplate.getForObject(downloadUrl, byte[].class);
            if (audioBytes == null || audioBytes.length == 0) {
                return null;
            }

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(openAiApiKey);

            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            org.springframework.core.io.ByteArrayResource audioResource = new org.springframework.core.io.ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return "voice.ogg";
                }
            };
            body.add("file", audioResource);
            body.add("model", "whisper-1");
            body.add("language", "gu");

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity = 
                    new org.springframework.http.HttpEntity<>(body, headers);

            org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.openai.com/v1/audio/transcriptions", 
                    requestEntity, 
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("text");
            }
        } catch (Exception e) {
            logger.error("Whisper transcription failed", e);
        }
        return null;
    }

    private void sendTelegramDocument(String chatId, byte[] fileBytes, String filename) {
        if ("mock_telegram_token".equals(botToken)) {
            logger.info("[MOCK TELEGRAM BOT REPLY DOCUMENT] To Chat ID: {}, Filename: {}, Bytes length: {}", chatId, filename, fileBytes.length);
            return;
        }

        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            org.springframework.core.io.ByteArrayResource fileResource = new org.springframework.core.io.ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            body.add("chat_id", chatId);
            body.add("document", fileResource);

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity = 
                    new org.springframework.http.HttpEntity<>(body, headers);

            String url = String.format("https://api.telegram.org/bot%s/sendDocument", botToken);
            restTemplate.postForEntity(url, requestEntity, String.class);
        } catch (Exception e) {
            logger.error("Failed to send telegram webhook document: {}", e.getMessage());
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371; // in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
