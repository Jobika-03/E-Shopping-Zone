package com.eshoppingzone.delivery.resource;

import com.eshoppingzone.delivery.dto.AssignmentRequestDto;
import com.eshoppingzone.delivery.dto.PartnerAuthDto;
import com.eshoppingzone.delivery.entity.DeliveryAssignment;
import com.eshoppingzone.delivery.entity.DeliveryPartner;
import com.eshoppingzone.delivery.service.DeliveryPartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/delivery")
@CrossOrigin(origins = "*")
public class DeliveryPartnerResource {

    @Autowired
    private DeliveryPartnerService deliveryService;

    // 1. List all available delivery partners (for Merchant assignment dropdown)
    @GetMapping("/partners")
    public ResponseEntity<List<DeliveryPartner>> getAllPartners() {
        return ResponseEntity.ok(deliveryService.getAllPartners());
    }

    @GetMapping("/partners/{id}")
    public ResponseEntity<DeliveryPartner> getPartnerById(@PathVariable("id") int id) {
        return deliveryService.getPartnerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 2. Partner Authentication
    @PostMapping({"/partners/login", "/login"})
    public ResponseEntity<PartnerAuthDto.LoginResponse> login(@RequestBody(required = false) PartnerAuthDto.LoginRequest request,
                                                              @RequestParam(value = "email", required = false) String emailParam,
                                                              @RequestParam(value = "password", required = false) String passwordParam) {
        PartnerAuthDto.LoginRequest req = request != null ? request : new PartnerAuthDto.LoginRequest();
        if ((req.getEmail() == null || req.getEmail().isEmpty()) && emailParam != null) {
            req.setEmail(emailParam);
        }
        if ((req.getPassword() == null || req.getPassword().isEmpty()) && passwordParam != null) {
            req.setPassword(passwordParam);
        }
        PartnerAuthDto.LoginResponse resp = deliveryService.login(req);
        if (!resp.isSuccess()) {
            return ResponseEntity.status(401).body(resp);
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/partners/register")
    public ResponseEntity<DeliveryPartner> register(@RequestBody DeliveryPartner partner) {
        return ResponseEntity.ok(deliveryService.registerPartner(partner));
    }

    // 3. Assign Order to Delivery Partner (called by Merchant)
    @RequestMapping(value = "/assign", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<?> assignOrder(@RequestParam(value = "orderId", required = false) Integer orderIdParam,
                                         @RequestParam(value = "partnerId", required = false) Integer partnerIdParam,
                                         @RequestParam(value = "remarks", required = false) String remarksParam,
                                         jakarta.servlet.http.HttpServletRequest httpRequest) {
        int orderId = orderIdParam != null ? orderIdParam : 0;
        int partnerId = partnerIdParam != null ? partnerIdParam : 0;
        String remarks = remarksParam;

        if (httpRequest != null) {
            if (orderId <= 0 && httpRequest.getParameter("orderId") != null) {
                try { orderId = Integer.parseInt(httpRequest.getParameter("orderId")); } catch (Exception ignored) {}
            }
            if (partnerId <= 0 && httpRequest.getParameter("partnerId") != null) {
                try { partnerId = Integer.parseInt(httpRequest.getParameter("partnerId")); } catch (Exception ignored) {}
            }
            if (remarks == null) {
                remarks = httpRequest.getParameter("remarks");
            }
            String cType = httpRequest.getContentType();
            if (cType != null && cType.toLowerCase().contains("json")) {
                try {
                    String bodyStr = new String(httpRequest.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    if (!bodyStr.trim().isEmpty()) {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        java.util.Map map = mapper.readValue(bodyStr, java.util.Map.class);
                        if (map != null) {
                            if (orderId <= 0 && map.get("orderId") != null) {
                                orderId = ((Number) map.get("orderId")).intValue();
                            }
                            if (partnerId <= 0 && map.get("partnerId") != null) {
                                partnerId = ((Number) map.get("partnerId")).intValue();
                            }
                            if (remarks == null && map.get("remarks") != null) {
                                remarks = map.get("remarks").toString();
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (orderId <= 0) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Valid orderId is required"));
        }
        if (partnerId <= 0) {
            partnerId = 1; // Default to Alex Rivera
        }

        DeliveryAssignment assignment = deliveryService.assignOrder(orderId, partnerId, remarks);
        return ResponseEntity.ok(assignment);
    }

    // 4. Get shipments for a specific Delivery Partner
    @GetMapping("/partner/{partnerId}/deliveries")
    public ResponseEntity<List<DeliveryAssignment>> getDeliveriesForPartner(@PathVariable("partnerId") int partnerId) {
        return ResponseEntity.ok(deliveryService.getDeliveriesForPartner(partnerId));
    }

    // 5. Get assignment by Order ID
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<DeliveryAssignment> getDeliveryByOrderId(@PathVariable("orderId") int orderId) {
        return deliveryService.getDeliveryByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 6. Delivery Partner Updates Delivery Milestone (SHIPPED, OUT_FOR_DELIVERY, DELIVERED)
    @RequestMapping(value = "/orders/{orderId}/status", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.GET})
    public ResponseEntity<DeliveryAssignment> updateStatus(@PathVariable("orderId") int orderId,
                                                           @RequestParam(value = "status", required = false) String statusParam,
                                                           @RequestParam(value = "remarks", required = false) String remarksParam,
                                                           @RequestParam(value = "estimatedTime", required = false) String estimatedTimeParam,
                                                           jakarta.servlet.http.HttpServletRequest httpRequest) {
        String status = statusParam;
        String remarks = remarksParam;
        String estimatedTime = estimatedTimeParam;

        if (httpRequest != null) {
            if (status == null || status.trim().isEmpty()) {
                status = httpRequest.getParameter("status");
            }
            if (remarks == null || remarks.trim().isEmpty()) {
                remarks = httpRequest.getParameter("remarks");
            }
            if (estimatedTime == null || estimatedTime.trim().isEmpty()) {
                estimatedTime = httpRequest.getParameter("estimatedTime");
            }
            String cType = httpRequest.getContentType();
            if (cType != null && cType.toLowerCase().contains("json")) {
                try {
                    String bodyStr = new String(httpRequest.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    if (!bodyStr.trim().isEmpty()) {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        java.util.Map map = mapper.readValue(bodyStr, java.util.Map.class);
                        if (map != null) {
                            if (map.get("status") != null) status = map.get("status").toString();
                            if (map.get("remarks") != null) remarks = map.get("remarks").toString();
                            if (map.get("estimatedTime") != null) estimatedTime = map.get("estimatedTime").toString();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        if (status == null || status.trim().isEmpty()) {
            status = "SHIPPED";
        }

        DeliveryAssignment updated = deliveryService.updateDeliveryStatus(orderId, status, remarks, estimatedTime);
        return ResponseEntity.ok(updated);
    }

    // 7. Get All Deliveries (audit)
    @GetMapping("/deliveries")
    public ResponseEntity<List<DeliveryAssignment>> getAllDeliveries() {
        return ResponseEntity.ok(deliveryService.getAllDeliveries());
    }

    // 8. Get Notifications for Partner
    @GetMapping("/partner/{partnerId}/notifications")
    public ResponseEntity<List<Object>> getPartnerNotifications(@PathVariable("partnerId") int partnerId) {
        return ResponseEntity.ok(deliveryService.getPartnerNotifications(partnerId));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Object>> getAllPartnerNotifications(@RequestParam(value = "partnerId", required = false, defaultValue = "1") int partnerId) {
        return ResponseEntity.ok(deliveryService.getPartnerNotifications(partnerId));
    }

    @RequestMapping(value = "/partners/{id}/status", method = {RequestMethod.PUT, RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<DeliveryPartner> updatePartnerStatus(@PathVariable("id") int partnerId,
                                                               @RequestParam(value = "status", required = false) String statusParam,
                                                               jakarta.servlet.http.HttpServletRequest httpRequest) {
        String status = statusParam;
        if (httpRequest != null) {
            if (status == null || status.trim().isEmpty()) {
                status = httpRequest.getParameter("status");
            }
            String cType = httpRequest.getContentType();
            if (cType != null && cType.toLowerCase().contains("json")) {
                try {
                    String bodyStr = new String(httpRequest.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    if (!bodyStr.trim().isEmpty()) {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        java.util.Map map = mapper.readValue(bodyStr, java.util.Map.class);
                        if (map != null && map.get("status") != null) {
                            status = map.get("status").toString();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        if (status == null || status.trim().isEmpty()) {
            status = "AVAILABLE";
        }
        DeliveryPartner updated = deliveryService.updatePartnerStatus(partnerId, status);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @PostMapping("/orders/{orderId}/fail")
    public ResponseEntity<DeliveryAssignment> failDelivery(
            @PathVariable("orderId") int orderId,
            @RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(deliveryService.failDelivery(orderId, reason));
    }

    @PostMapping("/orders/{orderId}/verify-otp")
    public ResponseEntity<DeliveryAssignment> verifyOtp(
            @PathVariable("orderId") int orderId,
            @RequestParam("otp") String otp) {
        return ResponseEntity.ok(deliveryService.verifyOtpAndDeliver(orderId, otp));
    }

    @PostMapping("/assign-return")
    public ResponseEntity<DeliveryAssignment> assignReturnPickup(@RequestBody java.util.Map<String, Object> req) {
        int orderId = Integer.parseInt(req.get("orderId").toString());
        int partnerId = Integer.parseInt(req.get("partnerId").toString());
        String remarks = req.get("remarks") != null ? req.get("remarks").toString() : "Return pickup";
        Double lat = req.get("latitude") != null ? Double.parseDouble(req.get("latitude").toString()) : null;
        Double lng = req.get("longitude") != null ? Double.parseDouble(req.get("longitude").toString()) : null;
        String mapUrl = req.get("mapUrl") != null ? req.get("mapUrl").toString() : null;
        String returnReason = req.get("returnReason") != null ? req.get("returnReason").toString() : null;

        DeliveryAssignment da = deliveryService.assignReturnPickup(orderId, partnerId, remarks, lat, lng, mapUrl, returnReason);
        return ResponseEntity.ok(da);
    }
}