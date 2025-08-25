package com.example.toycontent.app.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Schema(title = "성공 여부")
    private boolean success;

    @Schema(title = "응답 데이터")
    private T data;

    @Schema(title = "메시지")
    private String message;

    @Schema(title = "에러 코드")
    private String errorCode;

    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .success(true)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
