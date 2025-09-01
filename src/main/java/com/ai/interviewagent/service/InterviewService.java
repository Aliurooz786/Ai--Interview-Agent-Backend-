package com.ai.interviewagent.service;

import com.ai.interviewagent.dto.*;
import com.ai.interviewagent.model.*;
import com.ai.interviewagent.model.enums.Difficulty;
import com.ai.interviewagent.model.enums.InterviewStatus;
import com.ai.interviewagent.model.enums.QuestionType;
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
    private final GeminiAiService geminiAiService;

    public InterviewService(InterviewRepository interviewRepository,
                            JobRoleRepository jobRoleRepository,
                            QuestionRepository questionRepository,
                            UserRepository userRepository,
                            AnswerRepository answerRepository,
                            GeminiAiService geminiAiService) {
        this.interviewRepository = interviewRepository;
        this.jobRoleRepository = jobRoleRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.answerRepository = answerRepository;
        this.geminiAiService = geminiAiService;
    }

    public InterviewSessionResponse startInterview(StartInterviewRequest request, String userId) {
        log.info("Attempting to start a new interview for user [{}] for job role id [{}]", userId, request.getJobRoleId());
        JobRole jobRole = jobRoleRepository.findById(request.getJobRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Job Role not found."));
        List<Question> questions = questionRepository.findByJobRoleId(request.getJobRoleId());
        if (questions.isEmpty()) {
            throw new IllegalStateException("There are no questions available for this job role yet.");
        }
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
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found."));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        if (!interview.getUserId().equals(user.getId())) {
            throw new SecurityException("You are not authorized to access this interview.");
        }
        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new IllegalStateException("This interview is not currently in progress.");
        }
        List<Question> allQuestionsForRole = questionRepository.findByJobRoleId(interview.getJobRoleId());
        int nextQuestionIndex = interview.getAnswerIds().size();
        if (nextQuestionIndex >= allQuestionsForRole.size()) {
            log.info("All questions have been answered for interview [{}]. Marking as COMPLETED.", interviewId);
            interview.setStatus(InterviewStatus.COMPLETED);
            interview.setCompletedAt(Instant.now());
            interviewRepository.save(interview);
            throw new IllegalStateException("Interview completed. No more questions available.");
        }
        Question nextQuestion = allQuestionsForRole.get(nextQuestionIndex);
        return QuestionResponse.builder()
                .questionId(nextQuestion.getId())
                .questionText(nextQuestion.getQuestionText())
                .questionType(nextQuestion.getQuestionType())
                .build();
    }

    public Map<String, String> submitAnswer(String interviewId, String userEmail, SubmitAnswerRequest request) {
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
        interview.getAnswerIds().add(savedAnswer.getId());
        interviewRepository.save(interview);
        return Map.of("message", "Answer submitted successfully. You can now request the next question.");
    }

    public InterviewResultDto getInterviewResults(String interviewId, String userEmail) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found."));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        if (!interview.getUserId().equals(user.getId())) {
            throw new SecurityException("You are not authorized to view these results.");
        }
        JobRole jobRole = jobRoleRepository.findById(interview.getJobRoleId())
                .orElse(JobRole.builder().title("Unknown Role").build());
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

    public Map<String, String> submitTextAnswerAndAnalyze(String interviewId, String userEmail, SubmitTextAnswerRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found."));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        if (!interview.getUserId().equals(user.getId())) {
            throw new SecurityException("You are not authorized to modify this interview.");
        }
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("Question not found."));
        String rawAnalysis = geminiAiService.getAnalysis(question.getQuestionText(), request.getAnswerText());
        String feedback = parseFeedback(rawAnalysis);
        Double score = parseScore(rawAnalysis);
        Answer newAnswer = Answer.builder()
                .interviewId(interviewId)
                .questionId(request.getQuestionId())
                .transcribedText(request.getAnswerText())
                .feedback(feedback)
                .score(score)
                .submittedAt(Instant.now())
                .build();
        Answer savedAnswer = answerRepository.save(newAnswer);
        interview.getAnswerIds().add(savedAnswer.getId());
        interviewRepository.save(interview);
        return Map.of("message", "Answer submitted and analyzed successfully.", "feedback", feedback, "score", String.valueOf(score));
    }

    public InterviewSessionResponse generateAndStartInterview(GenerateInterviewRequest request, String userEmail) {
        log.info("AI se dynamic interview generate karne ja rahe hain user [{}] ke liye", userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        String[] generatedQuestionTexts = geminiAiService.generateQuestionsFromDocs(
                request.getJobDescription(),
                request.getResumeText()
        );
        log.info("AI ne {} sawal generate kiye hain.", generatedQuestionTexts.length);
        JobRole tempJobRole = JobRole.builder()
                .title("AI Interview for " + user.getFullName())
                .description("Dynamically generated on " + Instant.now())
                .build();
        JobRole savedJobRole = jobRoleRepository.save(tempJobRole);
        log.info("Naye interview ke liye temporary JobRole ban gaya, ID: {}", savedJobRole.getId());
        for (String qText : generatedQuestionTexts) {
            Question newQuestion = Question.builder()
                    .jobRoleId(savedJobRole.getId())
                    .questionText(qText)
                    .questionType(QuestionType.TECHNICAL)
                    .difficulty(Difficulty.MEDIUM)
                    .build();
            questionRepository.save(newQuestion);
        }
        log.info("Saare AI-generated sawalon ko database mein save kar diya gaya hai.");
        Interview newInterview = Interview.builder()
                .userId(user.getId())
                .jobRoleId(savedJobRole.getId())
                .status(InterviewStatus.IN_PROGRESS)
                .answerIds(new ArrayList<>())
                .createdAt(Instant.now())
                .build();
        Interview savedInterview = interviewRepository.save(newInterview);
        log.info("AI-generated interview session safaltapoorvak shuru ho gaya, ID: {}", savedInterview.getId());
        return InterviewSessionResponse.builder()
                .interviewId(savedInterview.getId())
                .jobRoleTitle(savedJobRole.getTitle())
                .status(savedInterview.getStatus())
                .message("AI-generated interview has started successfully.")
                .build();
    }

    private String parseFeedback(String rawAnalysis) {
        try {
            return rawAnalysis.substring(rawAnalysis.indexOf(":") + 1, rawAnalysis.indexOf("Score:")).trim();
        } catch (Exception e) {
            log.error("Could not parse feedback from AI response: {}", rawAnalysis);
            return rawAnalysis;
        }
    }

    private Double parseScore(String rawAnalysis) {
        try {
            String scorePart = rawAnalysis.substring(rawAnalysis.lastIndexOf(":") + 1).trim();
            return Double.parseDouble(scorePart);
        } catch (Exception e) {
            log.error("Could not parse score from AI response: {}", rawAnalysis);
            return 0.0;
        }
    }
}

