package com.example.toycontent.app.config;

import com.example.toycontent.app.common.resolver.CurrentUserIdArgumentResolver;
import com.example.toycontent.app.common.resolver.CurrentUserNameArgumentResolver;
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

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(currentUserIdArgumentResolver);
    resolvers.add(currentUserNameArgumentResolver);

  }
}
