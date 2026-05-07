package com.example.toycontent.app.config;

import com.example.toycontent.app.common.resolver.CurrentUserIdArgumentResolver;
import com.example.toycontent.app.common.resolver.CurrentUserIsAdminArgumentResolver;
import com.example.toycontent.app.common.resolver.CurrentUserNameArgumentResolver;
import com.example.toycontent.app.common.resolver.CurrentVoterIdArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

  private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;
  private final CurrentUserNameArgumentResolver currentUserNameArgumentResolver;
  private final CurrentUserIsAdminArgumentResolver currentUserIsAdminArgumentResolver;
  private final CurrentVoterIdArgumentResolver currentVoterIdArgumentResolver;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(currentUserIdArgumentResolver);
    resolvers.add(currentUserNameArgumentResolver);
    resolvers.add(currentUserIsAdminArgumentResolver);
    resolvers.add(currentVoterIdArgumentResolver);
  }
}
