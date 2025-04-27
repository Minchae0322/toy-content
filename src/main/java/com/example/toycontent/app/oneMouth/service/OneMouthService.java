package com.example.toycontent.app.oneMouth.service;

import com.example.toycontent.app.oneMouth.controller.dto.OneMouthCreateDto;
import com.example.toycontent.app.oneMouth.repository.OneMouthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OneMouthService {

    private final OneMouthRepository oneMouthRepository;

    public Long createOneMouth(OneMouthCreateDto oneMouthCreateDto) {

    }
}
