package com.example.toycontent.app.category.repository;

import com.example.toycontent.app.category.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
