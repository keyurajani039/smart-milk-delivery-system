package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.service.TelegramService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TelegramServiceImpl implements TelegramService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramServiceImpl.class);

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendAbsenceNotification(String telegramId, String customerName, String companyName) {
        String msg = String.format(
                "નમસ્તે %s, દિલગીરી સાથે જણાવવાનું કે આજે %s તરફથી દૂધ વિતરણ બંધ રહેશે.\n\n" +
                "Dear %s, we apologize but there will be no milk delivery today from %s.",
                customerName, companyName, customerName, companyName
        );
        send(telegramId, msg);
    }

    @Override
    public void sendBillGeneratedNotification(String telegramId, String customerName, double amount, int month, int year) {
        String msg = String.format(
                "નમસ્તે %s, %d/%d મહિનાનું બિલ જનરેટ થઈ ગયું છે. કુલ ચૂકવવાપાત્ર રકમ: ₹%.2f.\n\n" +
                "Dear %s, your bill for %d/%d is generated. Total payable: ₹%.2f.",
                customerName, month, year, amount, customerName, month, year, amount
        );
        send(telegramId, msg);
    }

    @Override
    public void sendPaymentReceivedNotification(String telegramId, String customerName, double amount) {
        String msg = String.format(
                "નમસ્તે %s, ₹%.2f ની ચુકવણી સફળતાપૂર્વક મળી ગઈ છે. આભાર!\n\n" +
                "Dear %s, payment of ₹%.2f received successfully. Thank you!",
                customerName, amount, customerName, amount
        );
        send(telegramId, msg);
    }

    @Override
    public void sendPaymentReminderNotification(String telegramId, String customerName, double amount) {
        String msg = String.format(
                "નમસ્તે %s, ₹%.2f ની બાકી ચુકવણી માટેનું રીમાઇન્ડર. કૃપા કરીને વહેલી તકે ચૂકવો.\n\n" +
                "Dear %s, this is a reminder for pending payment of ₹%.2f. Please pay as soon as possible.",
                customerName, amount, customerName, amount
        );
        send(telegramId, msg);
    }

    @Override
    public void sendWelcomeNotification(String telegramId, String customerName) {
        String msg = String.format(
                "\u0AA8\u0AAE\u0AB8\u0ACD\u0AA4\u0AC7 %s! \u0AB8\u0ACD\u0AAE\u0ABE\u0AB0\u0ACD\u0A9F \u0AAE\u0ABF\u0AB2\u0ACD\u0A95 \u0AA1\u0ABF\u0AB2\u0ABF\u0AB5\u0AB0\u0AC0 \u0AB8\u0ABF\u0AB8\u0ACD\u0A9F\u0AAE\u0AAE\u0ABE\u0A82 \u0AA4\u0AAE\u0ABE\u0AB0\u0AC1\u0A82 \u0AB8\u0ACD\u0AB5\u0ABE\u0A97\u0AA4 \u0A9B\u0AC7! \u0AA4\u0AAE\u0ABE\u0AB0\u0AC1\u0A82 \u0A8F\u0A95\u0ABE\u0A89\u0AA8\u0ACD\u0A9F \u0AA8\u0ACB\u0A82\u0AA7\u0ABE\u0AAF\u0AC7\u0AB2 \u0A9B\u0AC7.\n\n" +
                "Dear %s, welcome to Smart Milk Delivery System! Your account has been registered.",
                customerName, customerName
        );
        send(telegramId, msg);
    }

    private void send(String telegramId, String text) {
        if (telegramId == null || telegramId.isBlank()) {
            logger.warn("Skipping telegram message. telegramId is empty.");
            return;
        }

        if ("mock_telegram_token".equals(botToken)) {
            logger.info("[MOCK TELEGRAM BOT] Sending to {}: \n{}", telegramId, text);
            return;
        }

        try {
            String url = String.format(
                    "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                    botToken, telegramId, URLEncoder.encode(text, StandardCharsets.UTF_8)
            );
            java.net.URI uri = new java.net.URI(url);
            restTemplate.getForObject(uri, String.class);
            logger.info("Successfully sent telegram notification to {}", telegramId);
        } catch (Exception e) {
            logger.error("Failed to send telegram message to {}: {}", telegramId, e.getMessage());
        }
    }
}
