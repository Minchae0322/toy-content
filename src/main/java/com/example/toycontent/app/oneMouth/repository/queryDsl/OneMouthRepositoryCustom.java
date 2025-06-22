package com.example.toycontent.app.oneMouth.repository.queryDsl;

import com.example.toycontent.app.oneMouth.controller.dto.OneMouthListView;
import com.example.toycontent.app.oneMouth.controller.dto.condition.OneMouthSearchCondition;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OneMouthRepositoryCustom {
    List<OneMouthListView> searchByCondition(OneMouthSearchCondition condition, Pageable pageable);
    long countByCondition(OneMouthSearchCondition condition);
}
