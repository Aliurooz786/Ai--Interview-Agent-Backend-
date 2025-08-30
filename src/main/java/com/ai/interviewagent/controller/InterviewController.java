package com.ai.interviewagent.controller;

import com.ai.interviewagent.dto.InterviewSessionResponse;
import com.ai.interviewagent.dto.StartInterviewRequest;
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
}
