package com.example.toycontent.app.feedback.service;

import com.example.toycontent.app.feedback.controller.dto.FeedbackRequest;
import com.example.toycontent.app.feedback.controller.dto.FeedbackResponse.ListView;
import com.example.toycontent.app.feedback.domain.Feedback;
import com.example.toycontent.app.feedback.repository.FeedbackRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackService {

  private final FeedbackRepository feedbackRepository;

  public void create(FeedbackRequest.Create request) {
    Feedback feedback = Feedback.create(request.getTitle(), request.getContent());
    feedbackRepository.save(feedback);
  }

  public Page<ListView> getFeedbacks(Pageable pageable) {
    return feedbackRepository.findAll(pageable).map(ListView::from);
  }
}
