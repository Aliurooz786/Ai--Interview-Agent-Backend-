package com.ai.interviewagent.dto;

import lombok.Data;


@Data
public class SubmitTextAnswerRequest {
    private String questionId;
    private String answerText;
}

