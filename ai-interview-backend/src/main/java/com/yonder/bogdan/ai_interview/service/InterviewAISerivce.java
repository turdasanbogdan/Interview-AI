package com.yonder.bogdan.ai_interview.service;

import com.yonder.bogdan.ai_interview.model.InterviewScript;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAISerivce {

    private final ChatClient chatClient;

    @Value("${interviewer.questions.max:5}")
    private int maxQuestions;

    public String generateFirstQuestion(String topic){

        String prompt = """
                You are a professional interviewer conducting a structured interview.
                The topic is: "{topic}"
                
                Generate the FIRST interview question for this topic.
                Requirements:
                - Open-ended (cannot be answered with yes/no)
                - Thought-provoking and relevant to the topic
                - Invites the interviewee to share their perspective
                
                Return ONLY the question text, nothing else.
               """;
        return call(prompt, Map.of("topic", topic));
    }

    private String call(String promptTemplate, Map<String, Object> variables) {
        String filledPrompt = new PromptTemplate(promptTemplate)
                .render(variables);
        return chatClient.prompt()
                .user(filledPrompt)
                .call()
                .content();
    }

    public String generateNextQuestion(String topic, List<InterviewScript> previousScripts, int questionNumber) {

        String history = previousScripts.stream()
                .map(s -> "Q" + s.getQuestionNumber() + ": " + s.getQuestion()
                        + "\nA" + s.getQuestionNumber() + ": " + (
                        s.getKeyInfo() != null ? s.getKeyInfo() : s.getAnswer()
                ))
                .collect(Collectors.joining("\n\n"));

        String prompt = """
            You are a professional interviewer conducting a structured interview.
            Topic: "{topic}"
            
            Interview history so far:
            {history}
            
            Generate question number {questionNumber} out of {totalQuestions}.
            Requirements:
            - Build naturally on the previous answers
            - Go deeper into a theme that emerged, or explore a related angle
            - Open-ended and thought-provoking
            
            Return ONLY the question text, nothing else.
            """;

        return call(prompt, Map.of(
                "topic", topic,
                "history", history,
                "questionNumber", String.valueOf(questionNumber),
                "totalQuestions", String.valueOf(maxQuestions)
        ));
    }

    public boolean checkSufficiency(String question, String answer) {
        String prompt = """
            You are evaluating whether an interview answer sufficiently addresses the question.
            
            Question: "{question}"
            Answer: "{answer}"
            
            Evaluate if the answer:
            1. Is relevant to the question (not off-topic or gibberish)
            2. Provides some substance (not just "I don't know" or one word)
            3. Makes a genuine attempt to respond
            
            Respond with ONLY one word: SUFFICIENT or INSUFFICIENT
            """;

        String result = call(prompt, Map.of(
                "question", question,
                "answer", answer
        ));

        boolean sufficient = result.trim().toUpperCase().contains("SUFFICIENT")
                && !result.trim().toUpperCase().contains("INSUFFICIENT");

        log.debug("Sufficiency check for Q='{}': {} → {}", question, answer, sufficient);
        return sufficient;
    }

    public String generateClarificationRequest(String question, String answer, int clarificationCount) {
        String prompt = """
            You are a professional interviewer. The interviewee gave an answer that needs clarification.
            
            Original question: "{question}"
            Their answer: "{answer}"
            This is clarification attempt number: {count} out of 2 maximum.
            
            Generate a SHORT, empathetic clarification request that:
            - Acknowledges what they said (active listening)
            - Politely asks them to elaborate or be more specific
            - Does NOT repeat the original question verbatim
            - Is warm and encouraging, not pushy
            
            Return ONLY the clarification request text, nothing else.
            """;

        return call(prompt, Map.of(
                "question", question,
                "answer", answer,
                "count", String.valueOf(clarificationCount)
        ));
    }

    public String generateParaphrase(String question, String answer) {
        String prompt = """
            You are a professional interviewer practicing active listening.
            
            The interviewee answered this question:
            Question: "{question}"
            Answer: "{answer}"
            
            Generate a SHORT paraphrase (1-2 sentences) that:
            - Restates the key idea of their answer in your own words
            - Shows you understood them (e.g. "I see that...", "It sounds like...", "So for you...")
            - Is warm and validates their perspective
            - Does NOT add new questions
            
            Return ONLY the paraphrase text, nothing else.
            """;

        return call(prompt, Map.of(
                "question", question,
                "answer", answer
        ));
    }

    public String extractKeyInfo(String question, String answer) {
        String prompt = """
            Extract the key information from this interview answer in 1-2 concise sentences.
            Focus only on the essential facts, opinions, or themes expressed.
            This will be used as context for follow-up questions.
            
            Question: "{question}"
            Answer: "{answer}"
            
            Return ONLY the extracted key information, nothing else.
            """;

        return call(prompt, Map.of(
                "question", question,
                "answer", answer
        ));
    }

    public AnalysisResult generateAnalysis(String topic, List<InterviewScript> scripts) {

        String transcript = scripts.stream()
                .map(s -> "Q" + s.getQuestionNumber() + ": " + s.getQuestion()
                        + "\nA: " + s.getAnswer())
                .collect(Collectors.joining("\n\n"));

        String prompt = """
            You are analyzing a complete interview transcript.
            Topic: "{topic}"
            
            Transcript:
            {transcript}
            
            Provide a structured analysis in EXACTLY this format (no extra text, no markdown):
            
            SUMMARY:
            [2-3 sentences summarizing the key themes and insights from the interviewee]
            
            SENTIMENT:
            [One of: POSITIVE, NEGATIVE, NEUTRAL, MIXED — followed by one sentence explanation]
            
            KEYWORDS:
            [5-8 comma-separated keywords capturing the main concepts discussed]
            """;

        String raw = call(prompt, Map.of(
                "topic", topic,
                "transcript", transcript
        ));

        return parseAnalysis(raw);
    }

    private AnalysisResult parseAnalysis(String raw) {
        String summary = extractSection(raw, "SUMMARY:", "SENTIMENT:");
        String sentiment = extractSection(raw, "SENTIMENT:", "KEYWORDS:");
        String keywords = extractSection(raw, "KEYWORDS:", null);
        return new AnalysisResult(summary.trim(), sentiment.trim(), keywords.trim());
    }

    private String extractSection(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        if (start == -1) return "";
        start += startMarker.length();
        if (endMarker != null) {
            int end = text.indexOf(endMarker, start);
            return end == -1 ? text.substring(start) : text.substring(start, end);
        }
        return text.substring(start);
    }

    public record AnalysisResult(String summary, String sentiment, String keywords) {}


}