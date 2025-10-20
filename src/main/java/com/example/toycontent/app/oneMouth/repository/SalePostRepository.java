package com.example.toycontent.app.oneMouth.repository;

import com.example.toycontent.app.oneMouth.domain.SalePost;
import com.example.toycontent.app.oneMouth.repository.queryDsl.OneMouthRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalePostRepository extends JpaRepository<SalePost, Long>, OneMouthRepositoryCustom {
}
