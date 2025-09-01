package com.ai.interviewagent.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class AnswerResultDto {

    private String questionText;
    private String answerUrl;


    private String feedback;
    private Double score;
}
