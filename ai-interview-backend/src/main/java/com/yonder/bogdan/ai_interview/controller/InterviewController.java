package com.yonder.bogdan.ai_interview.controller;

import com.yonder.bogdan.ai_interview.dto.InterviewDto;
import com.yonder.bogdan.ai_interview.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/start")
    public ResponseEntity<InterviewDto.QuestionResponse> startInterview(
            @Valid @RequestBody InterviewDto.StartRequest request) {
        return ResponseEntity.ok(interviewService.startInterview(request.topic()));
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<Object> submitAnswer(
            @PathVariable UUID id,
            @Valid @RequestBody InterviewDto.AnswerRequest request) {
        return ResponseEntity.ok(interviewService.submitAnswer(id, request.answer()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewDto.InterviewSummaryResponse> getInterview(
            @PathVariable UUID id) {
        return ResponseEntity.ok(interviewService.getInterview(id));
    }
}