package com.ai.interviewagent.model;

import com.ai.interviewagent.model.enums.Difficulty;
import com.ai.interviewagent.model.enums.QuestionType;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Data
@Builder
@Document(collection = "questions")
public class Question {

    @Id
    private String id;


    private String jobRoleId;

    private String questionText;

    @Field(targetType = FieldType.STRING)
    private QuestionType questionType;

    @Field(targetType = FieldType.STRING)
    private Difficulty difficulty;
}