package com.yonder.bogdan.ai_interview.repository;

import com.yonder.bogdan.ai_interview.model.InterviewScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterviewScriptRepository extends JpaRepository<InterviewScript, UUID> {
}
