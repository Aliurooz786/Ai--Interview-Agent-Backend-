package com.ai.interviewagent.dto;

import lombok.Data;

@Data
public class SubmitAnswerRequest {
    private String questionId;
    private String fileName;
}
