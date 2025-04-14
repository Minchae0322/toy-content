package com.example.toycontent.app.oneMouth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OneMouthController", description = "한입만 API")
@RestController
@RequiredArgsConstructor
@RequestMapping
public class OneMouthController {
}
