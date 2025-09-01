package com.ai.interviewagent.repository;

import com.ai.interviewagent.model.Answer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerRepository extends MongoRepository<Answer, String> {

}
