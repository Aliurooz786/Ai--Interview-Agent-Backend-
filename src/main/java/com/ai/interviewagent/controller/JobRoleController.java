package com.ai.interviewagent.controller;

import com.ai.interviewagent.model.JobRole;
import com.ai.interviewagent.repository.JobRoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/job-roles")
public class JobRoleController {

    private final JobRoleRepository jobRoleRepository;

    public JobRoleController(JobRoleRepository jobRoleRepository) {
        this.jobRoleRepository = jobRoleRepository;
    }

    @PostMapping
    public ResponseEntity<JobRole> createJobRole(@RequestBody JobRole jobRole) {
        JobRole savedRole = jobRoleRepository.save(jobRole);
        return new ResponseEntity<>(savedRole, HttpStatus.CREATED);
    }
}