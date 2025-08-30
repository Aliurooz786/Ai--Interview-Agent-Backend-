package com.ai.interviewagent.dto;

import com.ai.interviewagent.model.enums.QuestionType;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class QuestionResponse {
    private String questionId;
    private String questionText;
    private QuestionType questionType;
}