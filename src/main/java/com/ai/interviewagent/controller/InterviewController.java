package com.ai.interviewagent.controller;

import com.ai.interviewagent.dto.*;
import com.ai.interviewagent.model.User;
import com.ai.interviewagent.repository.UserRepository;
import com.ai.interviewagent.service.InterviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/interviews")
public class InterviewController {

    private final InterviewService interviewService;
    private final UserRepository userRepository;

    public InterviewController(InterviewService interviewService, UserRepository userRepository) {
        this.interviewService = interviewService;
        this.userRepository = userRepository;
    }

    @PostMapping("/start")
    public ResponseEntity<InterviewSessionResponse> startInterview(@RequestBody StartInterviewRequest request, Principal principal) {
        String userEmail = principal.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found in database: " + userEmail));
        InterviewSessionResponse response = interviewService.startInterview(request, user.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{interviewId}/next-question")
    public ResponseEntity<?> getNextQuestion(@PathVariable String interviewId, Principal principal) {
        try {
            String userEmail = principal.getName();
            QuestionResponse questionResponse = interviewService.getNextQuestion(interviewId, userEmail);
            return ResponseEntity.ok(questionResponse);
        } catch (IllegalStateException e) {
            return ResponseEntity.ok().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An internal server error occurred."));
        }
    }

    @PostMapping("/{interviewId}/submit-answer")
    public ResponseEntity<Map<String, String>> submitAnswer(@PathVariable String interviewId,
                                                            @RequestBody SubmitAnswerRequest request,
                                                            Principal principal) {
        String userEmail = principal.getName();
        Map<String, String> response = interviewService.submitAnswer(interviewId, userEmail, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{interviewId}/results")
    public ResponseEntity<?> getInterviewResults(@PathVariable String interviewId, Principal principal) {
        String userEmail = principal.getName();
        InterviewResultDto resultDto = interviewService.getInterviewResults(interviewId, userEmail);
        return ResponseEntity.ok(resultDto);
    }

    @PostMapping("/{interviewId}/analyze-text-answer")
    public ResponseEntity<Map<String, String>> submitTextAnswer(
            @PathVariable String interviewId,
            @RequestBody SubmitTextAnswerRequest request,
            Principal principal) {

        String userEmail = principal.getName();
        log.info("User [{}] se text jawab mila hai interview [{}] ke liye", userEmail, interviewId);
        try {
            Map<String, String> response = interviewService.submitTextAnswerAndAnalyze(interviewId, userEmail, request);
            log.info("AI analysis safaltapoorvak poora hua interview [{}] ke liye.", interviewId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Text jawab submit karte waqt error aaya interview [{}]:", interviewId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate-and-start")
    public ResponseEntity<InterviewSessionResponse> generateAndStartInterview(@RequestBody GenerateInterviewRequest request, Principal principal) {
        log.info("User [{}] se dynamic interview generate karne ki request aayi hai", principal.getName());
        try {
            String userEmail = principal.getName();
            InterviewSessionResponse response = interviewService.generateAndStartInterview(request, userEmail);
            log.info("AI-generated interview safaltapoorvak shuru ho gaya, ID: {}", response.getInterviewId());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("AI interview generate karte waqt error aaya:", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}

