package com.ai.interviewagent.repository;

import com.ai.interviewagent.model.JobRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface JobRoleRepository extends MongoRepository<JobRole, String> {

}