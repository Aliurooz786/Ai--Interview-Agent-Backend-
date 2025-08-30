package com.ai.interviewagent.repository;

import com.ai.interviewagent.model.Interview;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface InterviewRepository extends MongoRepository<Interview, String> {

    List<Interview> findByUserId(String userId);
}