package com.example.toycontent.app.feed.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "FeedController", description = "피드 API")
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/feeds")
public class FeedController {

}
