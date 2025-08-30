package com.ai.interviewagent.service;

import com.ai.interviewagent.dto.InterviewSessionResponse;
import com.ai.interviewagent.dto.StartInterviewRequest;
import com.ai.interviewagent.model.Interview;
import com.ai.interviewagent.model.JobRole;
import com.ai.interviewagent.model.Question;
import com.ai.interviewagent.model.enums.InterviewStatus;
import com.ai.interviewagent.repository.InterviewRepository;
import com.ai.interviewagent.repository.JobRoleRepository;
import com.ai.interviewagent.repository.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final JobRoleRepository jobRoleRepository;
    private final QuestionRepository questionRepository;

    public InterviewService(InterviewRepository interviewRepository,
                            JobRoleRepository jobRoleRepository,
                            QuestionRepository questionRepository) {
        this.interviewRepository = interviewRepository;
        this.jobRoleRepository = jobRoleRepository;
        this.questionRepository = questionRepository;
    }

    public InterviewSessionResponse startInterview(StartInterviewRequest request, String userId) {
        log.info("Attempting to start a new interview for user [{}] for job role id [{}]", userId, request.getJobRoleId());


        JobRole jobRole = jobRoleRepository.findById(request.getJobRoleId())
                .orElseThrow(() -> {
                    log.error("Invalid JobRoleId provided: {}", request.getJobRoleId());
                    return new IllegalArgumentException("Job Role not found.");
                });


        List<Question> questions = questionRepository.findByJobRoleId(request.getJobRoleId());
        if (questions.isEmpty()) {
            log.warn("No questions found for job role [{}]. Cannot start interview.", jobRole.getTitle());
            throw new IllegalStateException("There are no questions available for this job role yet.");
        }
        log.info("Found {} questions for job role [{}]", questions.size(), jobRole.getTitle());

        Interview newInterview = Interview.builder()
                .userId(userId)
                .jobRoleId(request.getJobRoleId())
                .status(InterviewStatus.IN_PROGRESS)
                .answerIds(new ArrayList<>()) // Start with an empty list of answers
                .createdAt(Instant.now())
                .build();

        Interview savedInterview = interviewRepository.save(newInterview);
        log.info("Successfully created and saved interview session with ID [{}] for user [{}]", savedInterview.getId(), userId);

        return InterviewSessionResponse.builder()
                .interviewId(savedInterview.getId())
                .jobRoleTitle(jobRole.getTitle())
                .status(savedInterview.getStatus())
                .message("Interview has started. Ready for the first question.")
                .build();
    }
}