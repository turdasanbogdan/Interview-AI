package com.yonder.bogdan.ai_interview.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.UUID;

@Entity
@Table(name = "interviewScript")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewScript{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    @Column(nullable = false)
    private Integer questionNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String paraphrase;

    @Column(columnDefinition = "TEXT")
    private String keyInfo;

    @Builder.Default
    private Integer clarificationCount = 0;

    @Builder.Default
    private Boolean sufficient = false;

}