package com.ai.interviewagent.model;

import com.ai.interviewagent.model.enums.InterviewStatus;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@Document(collection = "interviews")
public class Interview {

    @Id
    private String id;

    private String userId;

    private String jobRoleId;

    @Field(targetType = FieldType.STRING)
    private InterviewStatus status;


    private List<String> answerIds;


    private Double overallScore;
    private String summaryFeedback;

    private Instant createdAt;
    private Instant completedAt;
}