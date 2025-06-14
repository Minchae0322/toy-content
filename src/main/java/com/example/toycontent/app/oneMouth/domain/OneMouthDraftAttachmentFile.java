package com.example.toycontent.app.oneMouth.domain;

import com.example.toycontent.app.file.domain.AttachmentFile;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_one_mouth_draft_attachment_file", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"one_mouth_draft_id", "attachment_file_id"})
})
public class OneMouthDraftAttachmentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "one_mouth_draft_attachment_file_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "one_mouth_draft_id")
    private OneMouthDraft oneMouthDraft;

    @ManyToOne
    @JoinColumn(name = "attachment_file_id")
    private AttachmentFile attachmentFile;

    private Integer sortOrder;
}
