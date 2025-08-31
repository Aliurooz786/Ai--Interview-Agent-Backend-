package com.ai.interviewagent.service;

import com.ai.interviewagent.dto.*;
import com.ai.interviewagent.model.*;
import com.ai.interviewagent.model.enums.InterviewStatus;
import com.ai.interviewagent.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final JobRoleRepository jobRoleRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;

    public InterviewService(InterviewRepository interviewRepository,
                            JobRoleRepository jobRoleRepository,
                            QuestionRepository questionRepository,
                            UserRepository userRepository,
                            AnswerRepository answerRepository) {
        this.interviewRepository = interviewRepository;
        this.jobRoleRepository = jobRoleRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.answerRepository = answerRepository;
    }

    // ... (startInterview, getNextQuestion, submitAnswer methods waise hi rahenge)
    // ... (Code for previous methods is omitted for brevity)
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
                .answerIds(new ArrayList<>())
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


    public QuestionResponse getNextQuestion(String interviewId, String userEmail) {
        log.info("Fetching next question for interview [{}] for user [{}]", interviewId, userEmail);

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> {
                    log.error("Interview session not found with ID: {}", interviewId);
                    return new IllegalArgumentException("Interview not found.");
                });

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        if (!interview.getUserId().equals(user.getId())) {
            log.warn("SECURITY ALERT: User [{}] attempted to access interview [{}] which belongs to user [{}]",
                    userEmail, interviewId, interview.getUserId());
            throw new SecurityException("You are not authorized to access this interview.");
        }

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            log.warn("Attempted to get next question for an interview that is not in progress. Status: {}", interview.getStatus());
            throw new IllegalStateException("This interview is not currently in progress.");
        }

        List<Question> allQuestionsForRole = questionRepository.findByJobRoleId(interview.getJobRoleId());

        int currentAnswerCount = interview.getAnswerIds().size();
        int nextQuestionIndex = currentAnswerCount;

        if (nextQuestionIndex >= allQuestionsForRole.size()) {
            log.info("All questions have been answered for interview [{}]. Marking as COMPLETED.", interviewId);
            interview.setStatus(InterviewStatus.COMPLETED);
            interview.setCompletedAt(Instant.now());
            interviewRepository.save(interview);
            throw new IllegalStateException("Interview completed. No more questions available.");
        }

        Question nextQuestion = allQuestionsForRole.get(nextQuestionIndex);
        log.info("Serving question ID [{}] (index {}) for interview [{}]", nextQuestion.getId(), nextQuestionIndex, interviewId);

        return QuestionResponse.builder()
                .questionId(nextQuestion.getId())
                .questionText(nextQuestion.getQuestionText())
                .questionType(nextQuestion.getQuestionType())
                .build();
    }

    public Map<String, String> submitAnswer(String interviewId, String userEmail, SubmitAnswerRequest request) {
        log.info("User [{}] is submitting an answer for question [{}] in interview [{}]", userEmail, request.getQuestionId(), interviewId);

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found."));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        if (!interview.getUserId().equals(user.getId())) {
            throw new SecurityException("You are not authorized to modify this interview.");
        }

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new IllegalStateException("This interview is not currently in progress.");
        }

        String placeholderUrl = String.format("uploads/%s/%s/%s", interviewId, request.getQuestionId(), request.getFileName());

        Answer newAnswer = Answer.builder()
                .interviewId(interviewId)
                .questionId(request.getQuestionId())
                .answerUrl(placeholderUrl)
                .submittedAt(Instant.now())
                .build();

        Answer savedAnswer = answerRepository.save(newAnswer);
        log.info("Successfully saved new answer with ID [{}]", savedAnswer.getId());

        interview.getAnswerIds().add(savedAnswer.getId());
        interviewRepository.save(interview);
        log.info("Updated interview session [{}] with new answer ID.", interviewId);

        return Map.of("message", "Answer submitted successfully. You can now request the next question.");
    }



    public InterviewResultDto getInterviewResults(String interviewId, String userEmail) {
        log.info("Fetching results for interview [{}] for user [{}]", interviewId, userEmail);


        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found."));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        if (!interview.getUserId().equals(user.getId())) {
            throw new SecurityException("You are not authorized to view these results.");
        }


        JobRole jobRole = jobRoleRepository.findById(interview.getJobRoleId())
                .orElse(JobRole.builder().title("Unknown Role").build()); // Graceful handling


        List<AnswerResultDto> answerResults = interview.getAnswerIds().stream()
                .map(answerId -> {

                    Answer answer = answerRepository.findById(answerId).orElse(null);
                    if (answer == null) return null;


                    Question question = questionRepository.findById(answer.getQuestionId()).orElse(null);
                    if (question == null) return null;


                    return AnswerResultDto.builder()
                            .questionText(question.getQuestionText())
                            .answerUrl(answer.getAnswerUrl())
                            .feedback(answer.getFeedback())
                            .score(answer.getScore())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());


        return InterviewResultDto.builder()
                .interviewId(interview.getId())
                .jobRoleTitle(jobRole.getTitle())
                .status(interview.getStatus())
                .completedAt(interview.getCompletedAt())
                .overallScore(interview.getOverallScore())
                .summaryFeedback(interview.getSummaryFeedback())
                .results(answerResults)
                .build();
    }
}

