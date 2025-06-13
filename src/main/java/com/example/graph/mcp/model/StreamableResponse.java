package com.example.graph.mcp.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StreamableResponse {
    private String status; // 状态：STARTED, IN_PROGRESS, COMPLETED, ERROR
    private int progress; // 进度：0-100
    private String data; // 实际数据
    private String error; // 错误信息
    private String message; // 状态消息

    public static StreamableResponse started(String message) {
        return StreamableResponse.builder()
                .status("STARTED")
                .progress(0)
                .message(message)
                .build();
    }

    public static StreamableResponse inProgress(int progress, String data) {
        return StreamableResponse.builder()
                .status("IN_PROGRESS")
                .progress(progress)
                .data(data)
                .build();
    }

    public static StreamableResponse completed(String data) {
        return StreamableResponse.builder()
                .status("COMPLETED")
                .progress(100)
                .data(data)
                .build();
    }

    public static StreamableResponse error(String error) {
        return StreamableResponse.builder()
                .status("ERROR")
                .error(error)
                .build();
    }
}