package com.example.toycontent.app.post.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

@MappedSuperclass
public abstract class PostBase {


    @Column(name = "title", nullable = false, length = 200)
    @Comment("게시글 제목")
    private String title;

    @Column(name = "content", columnDefinition = "CLOB", nullable = false)
    @Comment("게시글 내용")
    private String content;


}
