package com.ai.interviewagent.controller;

import com.ai.interviewagent.dto.*;
import com.ai.interviewagent.model.User;
import com.ai.interviewagent.repository.UserRepository;
import com.ai.interviewagent.service.InterviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        log.info("Received request to start interview from user [{}]", principal.getName());

        String userEmail = principal.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found in database: " + userEmail));
        String userId = user.getId();

        InterviewSessionResponse response = interviewService.startInterview(request, userId);


        log.info("Successfully initiated interview session [{}] for user [{}]", response.getInterviewId(), userEmail);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{interviewId}/next-question")
    public ResponseEntity<?> getNextQuestion(@PathVariable String interviewId, Principal principal) {
        log.info("User [{}] is requesting the next question for interview [{}]", principal.getName(), interviewId);

        try {
            String userEmail = principal.getName();
            QuestionResponse questionResponse = interviewService.getNextQuestion(interviewId, userEmail);
            return ResponseEntity.ok(questionResponse);

        } catch (IllegalStateException e) {

            log.info("Interview [{}] is complete. Sending completion message.", interviewId);
            return ResponseEntity.ok().body(Map.of("message", e.getMessage()));

        } catch (IllegalArgumentException | SecurityException e) {
            log.error("Client error while fetching next question for interview [{}]: {}", interviewId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("An unexpected error occurred for interview [{}]:", interviewId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An internal server error occurred."));
        }
    }

    @PostMapping("/{interviewId}/submit-answer")
    public ResponseEntity<Map<String, String>> submitAnswer(@PathVariable String interviewId,
                                                            @RequestBody SubmitAnswerRequest request,
                                                            Principal principal) {
        String userEmail = principal.getName();
        log.info("Received answer submission from user [{}] for interview [{}]", userEmail, interviewId);

        try {
            Map<String, String> response = interviewService.submitAnswer(interviewId, userEmail, request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | SecurityException e) {
            log.error("Client error while submitting answer for interview [{}]: {}", interviewId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("An unexpected error occurred while submitting answer for interview [{}]:", interviewId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An internal server error occurred."));
        }
    }

    @GetMapping("/{interviewId}/results")
    public ResponseEntity<?> getInterviewResults(@PathVariable String interviewId, Principal principal) {
        log.info("User [{}] is requesting results for interview [{}]", principal.getName(), interviewId);
        try {
            String userEmail = principal.getName();
            InterviewResultDto resultDto = interviewService.getInterviewResults(interviewId, userEmail);
            return ResponseEntity.ok(resultDto);
        } catch (IllegalArgumentException | SecurityException e) {
            log.error("Client error while fetching results for interview [{}]: {}", interviewId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("An unexpected error occurred while fetching results for interview [{}]:", interviewId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An internal server error occurred."));
        }
    }
}

