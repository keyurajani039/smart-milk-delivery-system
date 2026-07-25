package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.controller.TelegramBotController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class TelegramLongPollingService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramLongPollingService.class);

    @Value("${telegram.bot.token:mock_telegram_token}")
    private String botToken;

    @Autowired
    private TelegramBotController telegramBotController;

    private final RestTemplate restTemplate = new RestTemplate();

    private boolean running = true;
    private long offset = 0;

    @EventListener(ApplicationReadyEvent.class)
    public void startPolling() {
        if (botToken == null || botToken.isBlank() || "mock_telegram_token".equals(botToken) || "mock_telegram_bot_token".equals(botToken)) {
            logger.info("Telegram Long Polling: Disabled (Mock Token active).");
            return;
        }

        logger.info("Telegram Long Polling: Starting service with Bot Token: {}", botToken);

        // Run long polling in a background thread to prevent blocking main thread
        new Thread(this::pollLoop, "telegram-long-polling-thread").start();
    }

    private void pollLoop() {
        // Step 1: Clear existing webhook
        try {
            String deleteUrl = String.format("https://api.telegram.org/bot%s/deleteWebhook", botToken);
            logger.info("Telegram Long Polling: Clearing active webhooks first: {}", deleteUrl);
            restTemplate.getForObject(deleteUrl, String.class);
        } catch (Exception e) {
            logger.warn("Telegram Long Polling: Failed to clear webhook: {}", e.getMessage());
        }

        // Step 2: Poll loop
        logger.info("Telegram Long Polling: Webhook cleared. Polling loop started...");
        while (running) {
            try {
                String getUpdatesUrl = String.format("https://api.telegram.org/bot%s/getUpdates?timeout=30&limit=10", botToken);
                if (offset > 0) {
                    getUpdatesUrl += "&offset=" + offset;
                }

                Map<String, Object> response = restTemplate.getForObject(getUpdatesUrl, Map.class);
                if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                    List<Map<String, Object>> result = (List<Map<String, Object>>) response.get("result");
                    if (result != null && !result.isEmpty()) {
                        logger.info("Telegram Long Polling: Received {} updates", result.size());
                        for (Map<String, Object> update : result) {
                            try {
                                long updateId = ((Number) update.get("update_id")).longValue();
                                offset = updateId + 1;

                                logger.info("Processing update_id: {}", updateId);
                                telegramBotController.handleWebhook(update);
                            } catch (Exception ex) {
                                logger.error("Error dispatching Telegram update: {}", ex.getMessage(), ex);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Telegram Long Polling connection error: {}. Retrying in 10s...", e.getMessage());
                try {
                    Thread.sleep(10000); // Back-off on connection issues
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            try {
                Thread.sleep(500); // Brief pause before polling again
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        this.running = false;
    }
}
