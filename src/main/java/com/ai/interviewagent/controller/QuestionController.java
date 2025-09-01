package com.ai.interviewagent.controller;

import com.ai.interviewagent.model.Question;
import com.ai.interviewagent.repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final QuestionRepository questionRepository;

    public QuestionController(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @PostMapping
    public ResponseEntity<Question> createQuestion(@RequestBody Question question) {
        Question savedQuestion = questionRepository.save(question);
        return new ResponseEntity<>(savedQuestion, HttpStatus.CREATED);
    }
}