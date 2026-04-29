package com.example.toycontent.app.feedback.repository;

import com.example.toycontent.app.feedback.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
