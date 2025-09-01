package com.ai.interviewagent.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@Document(collection = "answers")
public class Answer {

    @Id
    private String id;

    private String interviewId;

    private String questionId;


    private String answerUrl;

    private String transcribedText;
    private String feedback;
    private Double score;

    private Instant submittedAt;
}
