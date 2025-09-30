package com.hdfclife.api_gateway.api_gateway.dto;

public class ErrorResponse {
    private String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    // Getter and Setter
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}