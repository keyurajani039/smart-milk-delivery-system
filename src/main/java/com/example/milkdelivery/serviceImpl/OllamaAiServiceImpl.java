package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.service.OllamaAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OllamaAiServiceImpl implements OllamaAiService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaAiServiceImpl.class);

    @Value("${ollama.api.url}")
    private String ollamaUrl;

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Map<String, Object> parseGujaratiRequest(String text) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "NONE");
        result.put("quantity", 0.0);
        result.put("days", 0);

        if (text == null || text.isBlank()) {
            return result;
        }

        // Try OpenAI GPT parsing first if key is configured
        if (openAiApiKey != null && !openAiApiKey.isBlank()) {
            try {
                String prompt = "You are an AI assistant for a smart milk delivery system.\n" +
                        "Parse the user request (which may be in Gujarati, English, or mixed) and map it to one of these actions:\n" +
                        "- EXTRA_MILK (when user requests extra milk. For this action, extract double quantity and int days if specified. If not specified, default to quantity 1.0 and days 1. If user cancels extra milk, action should be CANCEL_EXTRA_MILK).\n" +
                        "- CANCEL_EXTRA_MILK (when user cancels their extra milk request, e.g. \"cancel extra\", \"વધારે દૂધ કેન્સલ કરો\", \"extra milk 0\", \"0 extra\").\n" +
                        "- PAUSE_DELIVERY (when user wants to pause/stop milk delivery temporarily. Extract int days if specified, default to 3).\n" +
                        "- RESUME_DELIVERY (when user wants to resume/start milk delivery again, e.g. \"resume\", \"ચાલુ કરો\").\n" +
                        "- CHECK_BILL (when user asks for their bill or invoice, e.g. \"bill\", \"બિલ કેટલું થયું\").\n" +
                        "- CHECK_PAYMENT (when user asks about payment or how to pay).\n" +
                        "- TRACK_LOCATION (when user asks to track the milkman or where he is, e.g. \"where\", \"લોકેશન\", \"ક્યાં છે\").\n" +
                        "- CANCEL_PLAN (when user wants to permanently cancel, stop, or delete their milk delivery plan/subscription, e.g. \"cancel plan\", \"કાયમી બંધ કરો\", \"કાયમી બંધ\").\n\n" +
                        "Respond ONLY with a valid JSON block containing:\n" +
                        "{\n" +
                        "  \"action\": \"EXTRA_MILK\" | \"CANCEL_EXTRA_MILK\" | \"PAUSE_DELIVERY\" | \"RESUME_DELIVERY\" | \"CHECK_BILL\" | \"CHECK_PAYMENT\" | \"TRACK_LOCATION\" | \"CANCEL_PLAN\" | \"NONE\",\n" +
                        "  \"quantity\": double (only for EXTRA_MILK, else 0.0),\n" +
                        "  \"days\": int (for EXTRA_MILK or PAUSE_DELIVERY, else 0)\n" +
                        "}\n" +
                        "Do not write any explanation or markdown code block wrapper. Return ONLY the raw JSON string.\n\n" +
                        "User Message: \"" + text + "\"";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(openAiApiKey);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", "gpt-3.5-turbo");
                requestBody.put("temperature", 0.0);

                List<Map<String, String>> messages = new ArrayList<>();
                Map<String, String> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", prompt);
                messages.add(userMessage);

                requestBody.put("messages", messages);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                logger.info("Sending request to OpenAI API GPT-3.5-turbo for text: {}", text);
                Map<String, Object> response = restTemplate.postForObject("https://api.openai.com/v1/chat/completions", entity, Map.class);
                if (response != null && response.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        if (message != null && message.containsKey("content")) {
                            String content = (String) message.get("content");
                            if (content != null) {
                                content = content.trim();
                                if (content.startsWith("```")) {
                                    content = content.replaceAll("^```(?:json)?\\s*", "");
                                    content = content.replaceAll("\\s*```$", "");
                                    content = content.trim();
                                }
                                logger.info("GPT-3.5 response content: {}", content);
                                ObjectMapper mapper = new ObjectMapper();
                                Map<String, Object> parsedMap = mapper.readValue(content, Map.class);
                                if (parsedMap != null && parsedMap.containsKey("action")) {
                                    result.put("action", parsedMap.get("action"));
                                    result.put("quantity", parsedMap.containsKey("quantity") ? Double.parseDouble(parsedMap.get("quantity").toString()) : 0.0);
                                    result.put("days", parsedMap.containsKey("days") ? Integer.parseInt(parsedMap.get("days").toString()) : 0);
                                    return result;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("OpenAI GPT parser failed: {}. Falling back to local Regex parser.", e.getMessage());
            }
        }

        // Highly robust Local Regex Parser (Gujarati & English)
        String lowerText = text.toLowerCase();
        logger.info("Incoming text char codes: {}", text.chars().mapToObj(c -> String.format("U+%04X", c)).collect(java.util.stream.Collectors.toList()));
        logger.info("Literal char codes: {}", "\u0AB5\u0AA7\u0ABE\u0AB0\u0AC7".chars().mapToObj(c -> String.format("U+%04X", c)).collect(java.util.stream.Collectors.toList()));



        // 1c. Cancel Plan (કાયમી બંધ કરો / કાયમી બંધ / cancel plan / stop plan / cancel subscription / permanent cancel)
        boolean hasCancelAction = lowerText.contains("cancel") || 
                                  lowerText.contains("cancle") || 
                                  lowerText.contains("stop") || 
                                  lowerText.contains("delete");
        boolean hasPlanNoun = lowerText.contains("plan") || 
                              lowerText.contains("subscription") ||
                              lowerText.contains("palm") ||
                              lowerText.contains("plam");
        boolean hasCancelPlanKeyword = (hasCancelAction && hasPlanNoun) ||
                                       lowerText.contains("permanently cancel") ||
                                       lowerText.contains("permanently cancle") ||
                                       lowerText.contains("permanent cancel") ||
                                       lowerText.contains("permanent cancle") ||
                                       lowerText.contains("cancellation") ||
                                       lowerText.contains("cancelation") ||
                                       lowerText.contains("canclelation") ||
                                       lowerText.contains("\u0A95\u0ABE\u0AAF\u0AAE\u0AC0 \u0AAC\u0A82\u0AA7") || // કાયમી બંધ
                                       lowerText.contains("\u0A95\u0ABE\u0AAF\u0AAE\u0AC0 \u0AAC\u0A82\u0AA7 \u0A95\u0AB0\u0ACB") || // કાયમી બંધ કરો
                                       lowerText.contains("\u0AAA\u0ACD\u0AB2\u0ABE\u0AA8 \u0AAC\u0A82\u0AA7") || // પ્લાન બંધ
                                       lowerText.contains("પ્લાન કેન્સલ");

        if (hasCancelPlanKeyword) {
            result.put("action", "CANCEL_PLAN");
            return result;
        }

        // 1a. Cancel Extra Milk (કેન્સલ વધારે દૂધ / વધારે દૂધ કેન્સલ કરો / રદ કરો વધારાનું / cancel extra / 0 extra / extra milk 0)
        boolean hasCancelKeyword = lowerText.contains("cancel") || 
                                   lowerText.contains("\u0A95\u0AC7\u0AA8\u0ACD\u0AB8\u0AB2") || // કેન્સલ
                                   lowerText.contains("\u0AB0\u0AA6") || // રદ
                                   lowerText.contains("\u0AB0\u0AA6\u0ACD\u0AA6") || // રદ્દ
                                   lowerText.contains("\u0AA8\u0AA5\u0AC0 \u0A9C\u0ACB\u0A88\u0AA4\u0AC1\u0A82"); // નથી જોઈતું
        
        boolean hasExtraKeyword = lowerText.contains("\u0AB5\u0AA7\u0ABE\u0AB0\u0AC7") || 
                                  lowerText.contains("\u0AB5\u0AA7\u0ABE\u0AB0\u0ACB") || 
                                  lowerText.contains("\u0AB5\u0AA7\u0AC1") || 
                                  lowerText.contains("\u0A89\u0AAE\u0AC7\u0AB0\u0ACB") || 
                                  lowerText.contains("\u0A89\u0AAE\u0AC7\u0AB0\u0AC7") || 
                                  lowerText.contains("\u0AB5\u0AA7\u0ABE\u0AB0\u0ABE\u0AA8\u0AC1\u0A82") || 
                                  lowerText.contains("extra") || 
                                  lowerText.contains("more") || 
                                  lowerText.contains("add");

        double parsedQty = parseQuantity(lowerText);

        boolean hasDigit = false;
        String translated = translateGujaratiDigits(lowerText);
        for (int i = 0; i < translated.length(); i++) {
            if (Character.isDigit(translated.charAt(i))) {
                hasDigit = true;
                break;
            }
        }

        if (hasExtraKeyword && (hasCancelKeyword || (hasDigit && parsedQty == 0.0) || lowerText.contains(" 0 ") || lowerText.contains(" ૦ ") || lowerText.startsWith("0 ") || lowerText.startsWith("૦ "))) {
            result.put("action", "CANCEL_EXTRA_MILK");
            return result;
        }

        // 1b. Extra Milk (વધારે દૂધ / વધારે આપો / વધુ / ઉમેરો)
        if (hasExtraKeyword) {
            result.put("action", "EXTRA_MILK");
            double qty = parsedQty;
            int days = parseDays(lowerText);
            result.put("quantity", qty > 0 ? qty : 1.0); // Default 1 liter if not specified
            result.put("days", days > 0 ? days : 1); // Default 1 day if not specified
            return result;
        }

        // 2. Pause Delivery (બંધ કરો / નથી જોઈતું / રજા)
        if (lowerText.contains("\u0AAC\u0A82\u0AA7") || lowerText.contains("\u0AB0\u0A9C\u0ABE") || lowerText.contains("\u0AA8\u0AA5\u0AC0 \u0A9C\u0ACB\u0A88\u0AA4\u0AC1\u0A82") || lowerText.contains("pause") || lowerText.contains("stop")) {
            result.put("action", "PAUSE_DELIVERY");
            int days = parseDays(lowerText);
            result.put("days", days > 0 ? days : 3); // Default 3 days if not specified
            return result;
        }

        // 3. Resume Delivery (ચાલુ કરો / ફરીથી ચાલુ)
        if (lowerText.contains("\u0A9A\u0ABE\u0AB2\u0AC1") || lowerText.contains("\u0AB6\u0AB0\u0AC2") || lowerText.contains("resume") || lowerText.contains("start")) {
            result.put("action", "RESUME_DELIVERY");
            return result;
        }

        // 4. Check Bill (બિલ કેટલું થયું / હિસાબ / bill)
        if (lowerText.contains("\u0AAC\u0ABF\u0AB2") || lowerText.contains("\u0AB9\u0ABF\u0AB8\u0ABE\u0AAC") || lowerText.contains("\u0AB0\u0AC2\u0AAA\u0ABF\u0AAF\u0ABE") || lowerText.contains("bill") || lowerText.contains("amount")) {
            result.put("action", "CHECK_BILL");
            return result;
        }

        // 5. Payment check (પેમેન્ટ / ચુકવણી)
        if (lowerText.contains("payment") || lowerText.contains("pay")) {
            result.put("action", "CHECK_PAYMENT");
            return result;
        }

        // 6. Track location (ક્યાં છે / ક્યારે આવશે / લોકેશન / ટ્રેક / where / location / track / arrive)
        if (lowerText.contains("\u0A95\u0ACD\u0AAF\u0ABE\u0A82 \u0A9B\u0AC7") || 
            lowerText.contains("\u0A95\u0ACD\u0AAF\u0ABE \u0A9B\u0AC7") || 
            lowerText.contains("\u0A95\u0ACD\u0AAF\u0ABE\u0A82") || 
            lowerText.contains("\u0A95\u0ACD\u0AAF\u0ABE") || 
            lowerText.contains("\u0A95\u0ACD\u0AAF\u0ABE\u0AB0\u0AC7") || 
            lowerText.contains("\u0AB2\u0ACB\u0A95\u0AC7\u0AB6\u0AA8") || 
            lowerText.contains("\u0A9F\u0ACD\u0AB0\u0AC7\u0A95") || 
            lowerText.contains("where") || 
            lowerText.contains("location") || 
            lowerText.contains("track") || 
            lowerText.contains("arrive") || 
            lowerText.contains("when") || 
            lowerText.contains("status")) {
            result.put("action", "TRACK_LOCATION");
            return result;
        }

        return result;
    }

    private double parseQuantity(String text) {
        String translated = translateGujaratiDigits(text);
        
        // 1. Explicit keyword match
        Pattern qtyPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:liter|liters|l|લીટર|લી)");
        Matcher qtyMatcher = qtyPattern.matcher(translated);
        if (qtyMatcher.find()) {
            return Double.parseDouble(qtyMatcher.group(1));
        }
        
        // 2. Fallback: find all numbers in text
        List<String> allNumbers = findAllNumbers(translated);
        for (String numStr : allNumbers) {
            // Check if this number is followed by day keywords in the original text
            if (isFollowedByDayKeyword(translated, numStr)) {
                continue; // Skip, this is for days
            }
            return Double.parseDouble(numStr);
        }
        
        return 0.0;
    }

    private int parseDays(String text) {
        String translated = translateGujaratiDigits(text);
        
        // 1. Explicit keyword match
        Pattern daysPattern = Pattern.compile("(\\d+)\\s*(?:day|days|દિવસ|દિવસો|દિ)");
        Matcher daysMatcher = daysPattern.matcher(translated);
        if (daysMatcher.find()) {
            return Integer.parseInt(daysMatcher.group(1));
        }
        
        // 2. Fallback: find all numbers in text
        List<String> allNumbers = findAllNumbers(translated);
        
        // Identify which number is the quantity to filter it out
        String quantityNumStr = null;
        for (String numStr : allNumbers) {
            if (!isFollowedByDayKeyword(translated, numStr)) {
                quantityNumStr = numStr;
                break;
            }
        }
        
        for (String numStr : allNumbers) {
            // Skip the quantity number
            if (numStr.equals(quantityNumStr)) {
                continue;
            }
            // Skip numbers followed by liter keywords
            if (isFollowedByLiterKeyword(translated, numStr)) {
                continue;
            }
            return Integer.parseInt(numStr);
        }
        
        return 0;
    }

    private List<String> findAllNumbers(String text) {
        List<String> numbers = new ArrayList<>();
        Pattern p = Pattern.compile("\\d+(?:\\.\\d+)?");
        Matcher m = p.matcher(text);
        while (m.find()) {
            numbers.add(m.group());
        }
        return numbers;
    }

    private boolean isFollowedByDayKeyword(String text, String number) {
        Pattern p = Pattern.compile(Pattern.quote(number) + "\\s*(?:day|days|દિવસ|દિવસો|દિ)");
        return p.matcher(text).find();
    }

    private boolean isFollowedByLiterKeyword(String text, String number) {
        Pattern p = Pattern.compile(Pattern.quote(number) + "\\s*(?:liter|liters|l|લીટર|લી)");
        return p.matcher(text).find();
    }

    private String translateGujaratiDigits(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= '\u0AE6' && c <= '\u0AEF') {
                sb.append((char) (c - '\u0AE6' + '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

