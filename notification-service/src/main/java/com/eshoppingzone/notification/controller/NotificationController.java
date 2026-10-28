package com.eshoppingzone.notification.controller;

import com.eshoppingzone.notification.dto.DeliveryAssignmentRequest;
import com.eshoppingzone.notification.dto.EmailRequest;
import com.eshoppingzone.notification.dto.NotificationRecord;
import com.eshoppingzone.notification.dto.OrderCancellationRequest;
import com.eshoppingzone.notification.dto.OrderConfirmationRequest;
import com.eshoppingzone.notification.dto.OrderStatusUpdateRequest;
import com.eshoppingzone.notification.dto.WelcomeEmailRequest;
import com.eshoppingzone.notification.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notify")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private EmailService emailService;

    // 1. Generic Email
    @PostMapping("/email")
    public ResponseEntity<String> sendNotification(@RequestBody EmailRequest request) {
        if (request.isHtml()) {
            emailService.sendHtmlEmail(request.getToEmail(), request.getSubject(), request.getBody(), "GENERAL");
        } else {
            emailService.sendEmail(request.getToEmail(), request.getSubject(), request.getBody());
        }
        return ResponseEntity.ok("Email processed/dispatched successfully");
    }

    // 2. Welcome Email
    @PostMapping("/welcome")
    public ResponseEntity<String> sendWelcomeEmail(@RequestBody WelcomeEmailRequest request) {
        emailService.sendWelcomeEmail(request);
        return ResponseEntity.ok("Welcome notification dispatched to " + request.getToEmail());
    }

    // 3. Order Confirmation Email
    @PostMapping("/order-confirmation")
    public ResponseEntity<String> sendOrderConfirmation(@RequestBody OrderConfirmationRequest request) {
        emailService.sendOrderConfirmation(request);
        return ResponseEntity.ok("Order confirmation notification dispatched for Order #" + request.getOrderId());
    }

    // 3b. Order Cancellation Email (With Rich UI)
    @PostMapping({"/order-cancelled", "/order-cancellation"})
    public ResponseEntity<String> sendOrderCancellation(@RequestBody OrderCancellationRequest request) {
        emailService.sendOrderCancellation(request);
        return ResponseEntity.ok("Order cancellation notification dispatched for Order #" + request.getOrderId());
    }

    // 3c. Live UI Preview of Cancel Notification
    @GetMapping(value = "/preview/cancel", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String previewCancellationHtml() {
        return emailService.getSampleCancellationHtml();
    }

    // 3d. Delivery Assignment Email (With Rich Logistics UI)
    @PostMapping({"/delivery-assigned", "/delivery-assignment"})
    public ResponseEntity<String> sendDeliveryAssignment(@RequestBody DeliveryAssignmentRequest request) {
        emailService.sendDeliveryAssignment(request);
        return ResponseEntity.ok("Delivery assignment notification dispatched for Order #" + request.getOrderId() + " to partner " + request.getPartnerName());
    }

    // 3e. Live UI Preview of Delivery Assignment Notification
    @GetMapping(value = "/preview/delivery-assigned", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String previewDeliveryAssignmentHtml() {
        return emailService.getSampleDeliveryAssignmentHtml();
    }

    // 3f. Order Status Update Notification (Shipped, Out for Delivery, Delivered)
    @PostMapping({"/order-status-update", "/order-status"})
    public ResponseEntity<String> sendOrderStatusUpdate(@RequestBody OrderStatusUpdateRequest request) {
        emailService.sendOrderStatusUpdate(request);
        return ResponseEntity.ok("Order status update notification dispatched for Order #" + request.getOrderId() + " (" + request.getStatus() + ")");
    }

    // 3g. Live UI Preview of Order Status Update Notification
    @GetMapping(value = "/preview/status-update", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String previewStatusUpdateHtml() {
        return emailService.getSampleOrderStatusUpdateHtml();
    }

    // 4. Notification History (Audit & UI Inspection)
    @GetMapping("/history")
    public ResponseEntity<List<NotificationRecord>> getNotificationHistory() {
        return ResponseEntity.ok(emailService.getHistory());
    }

    // 5. Notifications for specific user
    @GetMapping("/user/{email}")
    public ResponseEntity<List<NotificationRecord>> getUserNotifications(@PathVariable("email") String email) {
        return ResponseEntity.ok(emailService.getHistoryForUser(email));
    }

    // 6. Get single notification
    @GetMapping("/{id}")
    public ResponseEntity<NotificationRecord> getNotificationById(@PathVariable("id") String id) {
        return emailService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
