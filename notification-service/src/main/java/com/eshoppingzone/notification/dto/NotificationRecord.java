package com.eshoppingzone.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRecord {
    private String id;
    private String toEmail;
    private String subject;
    private String type; // WELCOME, ORDER_CONFIRMATION, GENERAL
    private String snippet;
    private String htmlContent;
    private LocalDateTime timestamp;
    private String status; // SENT, SIMULATED
}
