package com.example.toycontent.app.oneMouth.repository;

import com.example.toycontent.app.oneMouth.domain.OneMouth;
import com.example.toycontent.app.oneMouth.repository.queryDsl.OneMouthRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OneMouthRepository extends JpaRepository<OneMouth, Long>, OneMouthRepositoryCustom {
}
