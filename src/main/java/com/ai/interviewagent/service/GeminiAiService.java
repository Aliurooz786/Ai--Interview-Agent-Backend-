package com.ai.interviewagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiAiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON ko handle karne ke liye

    @Value("${gemini.api.key}")
    private String apiKey;

    private final String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=";

    public GeminiAiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getAnalysis(String question, String answer) {
        log.info("AI se analysis ke liye request bhej rahe hain...");
        return "Error: AI service se connect nahi ho paya.";
    }

    public String[] generateQuestionsFromDocs(String jobDescription, String resumeText) {
        log.info("JD aur Resume se questions generate karne ke liye Gemini AI ko request bhej rahe hain...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String prompt = String.format(
                "You are an expert technical recruiter. Based on the following Job Description and candidate's Resume, " +
                        "generate exactly 5 relevant interview questions. 3 should be technical and 2 should be behavioral. " +
                        "Return the response ONLY as a valid JSON array of strings. Do not add any other text before or after the JSON array. " +
                        "For example: [\"What is your experience with microservices?\", \"Describe a time you handled a conflict.\"]\n\n" +
                        "--- JOB DESCRIPTION ---\n%s\n\n" +
                        "--- CANDIDATE RESUME ---\n%s",
                jobDescription, resumeText
        );

        Map<String, Object> requestBody = Map.of(
                "contents", Collections.singletonList(
                        Map.of("parts", Collections.singletonList(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(apiUrl + apiKey, entity, Map.class);
            log.info("Gemini AI se questions ka response mil gaya hai.");

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                String jsonResponseText = (String) parts.get(0).get("text");

                log.debug("AI se mila raw JSON response: {}", jsonResponseText);

                return objectMapper.readValue(jsonResponseText, String[].class);
            }
            throw new IOException("Failed to parse questions from AI response.");

        } catch (Exception e) {
            log.error("Gemini AI se questions generate karte waqt error aaya", e);
            return new String[]{"Tell me about yourself.", "What are your strengths and weaknesses?"};
        }
    }
}

