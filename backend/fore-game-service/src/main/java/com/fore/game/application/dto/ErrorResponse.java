package com.fore.game.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String code;
    private String message;
    private List<FieldError> fieldErrors;
    private Instant timestamp;
    private String path;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
