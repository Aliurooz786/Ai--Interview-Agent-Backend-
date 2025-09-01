package com.ai.interviewagent.dto;

import lombok.Data;

@Data
public class GenerateInterviewRequest {
    private String jobDescription;
    private String resumeText;
}

