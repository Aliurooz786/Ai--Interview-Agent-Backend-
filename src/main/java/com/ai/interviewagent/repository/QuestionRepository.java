package com.ai.interviewagent.repository;

import com.ai.interviewagent.model.Question;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends MongoRepository<Question, String> {

    List<Question> findByJobRoleId(String jobRoleId);
}