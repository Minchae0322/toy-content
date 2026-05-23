package com.example.toycontent.app.notification.hotcontent;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotContentNotificationSentRepository
    extends JpaRepository<HotContentNotificationSent, Long> {

  List<HotContentNotificationSent> findByContentTypeAndContentIdIn(
      HotContentType contentType, Collection<Long> contentIds);
}
