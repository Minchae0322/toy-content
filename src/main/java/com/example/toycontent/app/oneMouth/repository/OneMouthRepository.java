package com.example.toycontent.app.oneMouth.repository;

import com.example.toycontent.app.oneMouth.domain.SalePost;
import com.example.toycontent.app.oneMouth.repository.queryDsl.OneMouthRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OneMouthRepository extends JpaRepository<SalePost, Long>, OneMouthRepositoryCustom {
}
