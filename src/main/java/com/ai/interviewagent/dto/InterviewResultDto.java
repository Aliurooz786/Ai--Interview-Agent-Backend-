package com.ai.interviewagent.dto;

import com.ai.interviewagent.model.enums.InterviewStatus;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class InterviewResultDto {

    private String interviewId;
    private String jobRoleTitle;
    private InterviewStatus status;
    private Instant completedAt;


    private Double overallScore;
    private String summaryFeedback;

    private List<AnswerResultDto> results;
}
