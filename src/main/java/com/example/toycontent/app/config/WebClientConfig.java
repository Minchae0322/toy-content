package com.example.toycontent.app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
@Slf4j
public class WebClientConfig {

  @Value("${external.user-service.protocol}")
  private String protocol;

  @Value("${external.user-service.host}")
  private String host;

  @Value("${external.user-service.port}")
  private int port;

  @Value("${external.user-service.api-key}")
  private String apiKey;

  @Bean("userServiceWebClient")
  public WebClient userServiceWebClient() {
    log.info("UserService API Key: {}", apiKey);

    return WebClient.builder()
        .baseUrl(UriComponentsBuilder.newInstance()
            .scheme(protocol)
            .host(host)
            .port(port)
            .toUriString())
        .defaultHeader("X-API-Key", apiKey)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
