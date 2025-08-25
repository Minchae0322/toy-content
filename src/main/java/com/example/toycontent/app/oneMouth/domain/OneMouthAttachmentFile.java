package com.example.toycontent.app.oneMouth.domain;


import com.example.toycontent.app.file.domain.AttachmentFile;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_one_mouth_attachment_file", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"one_mouth_id", "attachment_file_id"})
})
public class OneMouthAttachmentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "one_mouth_id")
    private OneMouth oneMouth;

    @ManyToOne
    @JoinColumn(name = "attachment_file_id")
    private AttachmentFile attachmentFile;

    private Integer sortOrder;
}
