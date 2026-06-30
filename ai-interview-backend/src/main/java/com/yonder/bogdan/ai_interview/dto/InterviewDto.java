package com.yonder.bogdan.ai_interview.dto;

import com.yonder.bogdan.ai_interview.model.Interview;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InterviewDto{

    public record StartRequest(@NotBlank String topic) {}

    public record AnswerRequest(@NotBlank String answer) {}

    public record QuestionResponse(
            UUID interviewId,
            int questionNumber,
            int totalQuestions,
            String question,
            boolean isLastQuestion,
            String paraphrase,
            boolean isClarification,
            int clarificationCount
    ) {}

    public record QnAResponse(
            int questionNumber,
            String question,
            String answer,
            String paraphrase,
            String keyInfo
    ) {}

    public record InterviewSummaryResponse(
            UUID id,
            String topic,
            Interview.InterviewStatus status,
            List<QnAResponse> qnaList,
            String summary,
            String sentiment,
            String keywords,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {}
}
