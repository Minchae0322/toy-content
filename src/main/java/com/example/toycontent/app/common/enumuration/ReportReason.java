package com.example.toycontent.app.common.enumuration;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ReportReason {

    SPAM("SPAM", "광고/스팸"),
    OFFENSIVE("OFFENSIVE", "욕설/비방"),
    SEXUAL("SEXUAL", "음란/선정성"),
    VIOLENCE("VIOLENCE", "폭력/혐오"),
    MISINFORMATION("MISINFORMATION", "거짓 정보"),
    COPYRIGHT("COPYRIGHT", "저작권 침해"),
    ETC("ETC", "기타"),
    ;

    private String title;
    private String description;
}
