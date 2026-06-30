package com.yonder.bogdan.ai_interview.service;

import com.yonder.bogdan.ai_interview.dto.InterviewDto;
import com.yonder.bogdan.ai_interview.model.Interview;
import com.yonder.bogdan.ai_interview.model.InterviewScript;
import com.yonder.bogdan.ai_interview.repository.InterviewRepository;
import com.yonder.bogdan.ai_interview.repository.InterviewScriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService{

    private final InterviewRepository interviewRepository;
    private final InterviewScriptRepository interviewScriptRepository;
    private final InterviewAISerivce aiService;

    @Value("${interviewer.questions.max:5}")
    private int maxQuestions;

    @Value("${interviewer.clarification.max:2}")
    private int maxClarifications;

    @Transactional
    public InterviewDto.QuestionResponse startInterview(String topic) {
        log.info("Starting interview on topic: {}", topic);

        String firstQuestion = aiService.generateFirstQuestion(topic);

        Interview interview = Interview.builder()
                .topic(topic)
                .status(Interview.InterviewStatus.IN_PROGRESS)
                .build();
        interview = interviewRepository.save(interview);

        InterviewScript script = InterviewScript.builder()
                .interview(interview)
                .questionNumber(1)
                .question(firstQuestion)
                .clarificationCount(0)
                .sufficient(false)
                .build();
        interviewScriptRepository.save(script);

        log.info("Interview {} started, first question generated", interview.getId());

        return new InterviewDto.QuestionResponse(
                interview.getId(),
                1,
                maxQuestions,
                firstQuestion,
                false,
                null,
                false,
                0
        );
    }

    @Transactional
    public Object submitAnswer(UUID interviewId, String answer) {
        Interview interview = getInterviewOrThrow(interviewId);
        validateInProgress(interview);

        List<InterviewScript> scripts = interview.getScriptList();
        InterviewScript currentScript = scripts.get(scripts.size() - 1);

        // ── Step 1: Salveaza raspunsul ────────────────────────────────────────
        if (currentScript.getAnswer() != null && !currentScript.getAnswer().isBlank()) {
            currentScript.setAnswer(currentScript.getAnswer() + "\n[Clarification]: " + answer);
        } else {
            currentScript.setAnswer(answer);
        }
        interviewScriptRepository.save(currentScript);

        // ── Step 2: Sufficiency Check ─────────────────────────────────────────
        boolean sufficient = aiService.checkSufficiency(
                currentScript.getQuestion(),
                currentScript.getAnswer()
        );
        int clarificationCount = currentScript.getClarificationCount();

        if (!sufficient && clarificationCount < maxClarifications) {
            return handleClarification(interviewId, currentScript, clarificationCount);
        }

        // ── Step 3: Sufficient — extrage keyInfo + paraphrase ────────────────
        currentScript.setSufficient(true);

        String keyInfo = aiService.extractKeyInfo(
                currentScript.getQuestion(),
                currentScript.getAnswer()
        );
        currentScript.setKeyInfo(keyInfo);

        String paraphrase = aiService.generateParaphrase(
                currentScript.getQuestion(),
                currentScript.getAnswer()
        );
        currentScript.setParaphrase(paraphrase);
        interviewScriptRepository.save(currentScript);

        log.debug("Q{} answered sufficiently. KeyInfo and paraphrase generated.",
                currentScript.getQuestionNumber());

        // ── Step 4: Verifica daca interviul e complet ─────────────────────────
        int nextQuestionNumber = scripts.size() + 1;

        if (nextQuestionNumber > maxQuestions) {
            return completeInterview(interview, scripts);
        }

        // ── Step 5: Genereaza urmatoarea intrebare ────────────────────────────
        String nextQuestion = aiService.generateNextQuestion(
                interview.getTopic(),
                scripts,
                nextQuestionNumber
        );

        InterviewScript nextScript = InterviewScript.builder()
                .interview(interview)
                .questionNumber(nextQuestionNumber)
                .question(nextQuestion)
                .clarificationCount(0)
                .sufficient(false)
                .build();
        interviewScriptRepository.save(nextScript);

        boolean isLast = nextQuestionNumber == maxQuestions;

        log.info("Interview {} — Q{} answered, Q{} generated",
                interviewId, currentScript.getQuestionNumber(), nextQuestionNumber);

        return new InterviewDto.QuestionResponse(
                interviewId,
                nextQuestionNumber,
                maxQuestions,
                nextQuestion,
                isLast,
                paraphrase,
                false,
                0
        );
    }

    @Transactional
    public InterviewDto.InterviewSummaryResponse completeInterview(UUID interviewId) {
        Interview interview = getInterviewOrThrow(interviewId);
        validateInProgress(interview);
        return completeInterview(interview, interview.getScriptList());
    }

    private InterviewDto.InterviewSummaryResponse completeInterview(
            Interview interview,
            List<InterviewScript> scripts) {

        log.info("Completing interview: {}", interview.getId());

        InterviewAISerivce.AnalysisResult analysis =
                aiService.generateAnalysis(interview.getTopic(), scripts);

        interview.setSummary(analysis.summary());
        interview.setSentiment(analysis.sentiment());
        interview.setKeywords(analysis.keywords());
        interview.setStatus(Interview.InterviewStatus.COMPLETED);
        interview.setCompletedAt(LocalDateTime.now());
        interviewRepository.save(interview);

        return toSummaryResponse(interview);
    }

    @Transactional(readOnly = true)
    public InterviewDto.InterviewSummaryResponse getInterview(UUID interviewId) {
        return toSummaryResponse(getInterviewOrThrow(interviewId));
    }

    private Interview getInterviewOrThrow(UUID id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Interview not found: " + id
                ));
    }

    private void validateInProgress(Interview interview) {
        if (interview.getStatus() != Interview.InterviewStatus.IN_PROGRESS) {
            throw new IllegalStateException("Interview is already completed");
        }
    }

    private InterviewDto.QuestionResponse handleClarification(
            UUID interviewId,
            InterviewScript currentScript,
            int currentCount) {

        int newCount = currentCount + 1;
        currentScript.setClarificationCount(newCount);
        interviewScriptRepository.save(currentScript);

        String clarification = aiService.generateClarificationRequest(
                currentScript.getQuestion(),
                currentScript.getAnswer(),
                newCount
        );

        log.debug("Interview {} — Clarification {}/{} for Q{}",
                interviewId, newCount, maxClarifications,
                currentScript.getQuestionNumber());

        return new InterviewDto.QuestionResponse(
                interviewId,
                currentScript.getQuestionNumber(),
                maxQuestions,
                clarification,
                false,
                null,
                true,
                newCount
        );
    }

    private InterviewDto.InterviewSummaryResponse toSummaryResponse(Interview interview) {
        List<InterviewDto.QnAResponse> responses = interview.getScriptList().stream()
                .filter(s -> s.getAnswer() != null)
                .map(s -> new InterviewDto.QnAResponse(
                        s.getQuestionNumber(),
                        s.getQuestion(),
                        s.getAnswer(),
                        s.getParaphrase(),
                        s.getKeyInfo()
                ))
                .toList();

        return new InterviewDto.InterviewSummaryResponse(
                interview.getId(),
                interview.getTopic(),
                interview.getStatus(),
                responses,
                interview.getSummary(),
                interview.getSentiment(),
                interview.getKeywords(),
                interview.getCreatedAt(),
                interview.getCompletedAt()
        );
    }
}