package com.yonder.bogdan.ai_interview.repository;

import com.yonder.bogdan.ai_interview.model.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {
}
