package com.ai.interviewagent.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "job_roles")
public class JobRole {

    @Id
    private String id;

    private String title;

    private String description;
}