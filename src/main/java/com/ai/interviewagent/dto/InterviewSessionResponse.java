package com.ai.interviewagent.dto;

import com.ai.interviewagent.model.enums.InterviewStatus;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class InterviewSessionResponse {
    private String interviewId;
    private String jobRoleTitle;
    private InterviewStatus status;
    private String message;
}