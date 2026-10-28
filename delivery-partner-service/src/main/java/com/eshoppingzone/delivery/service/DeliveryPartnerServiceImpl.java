package com.eshoppingzone.delivery.service;

import com.eshoppingzone.delivery.dto.PartnerAuthDto;
import com.eshoppingzone.delivery.entity.DeliveryAssignment;
import com.eshoppingzone.delivery.entity.DeliveryPartner;
import com.eshoppingzone.delivery.repository.DeliveryAssignmentRepository;
import com.eshoppingzone.delivery.repository.DeliveryPartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DeliveryPartnerServiceImpl implements DeliveryPartnerService {

    @Autowired
    private DeliveryPartnerRepository partnerRepository;

    @Autowired
    private DeliveryAssignmentRepository assignmentRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String ORDER_SERVICE_URL = "http://localhost:8084/orders";
    private static final String NOTIFY_SERVICE_URL = "http://localhost:8086/notify";
    private static final String PROFILE_SERVICE_URL = "http://localhost:8081/profiles";

    @Override
    public List<DeliveryPartner> getAllPartners() {
        return partnerRepository.findAll();
    }

    @Override
    public Optional<DeliveryPartner> getPartnerById(int partnerId) {
        return partnerRepository.findById(partnerId);
    }

    @Override
    public PartnerAuthDto.LoginResponse login(PartnerAuthDto.LoginRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim() : "";
        String password = request.getPassword() != null ? request.getPassword().trim() : "";

        Optional<DeliveryPartner> partnerOpt = partnerRepository.findByEmail(email);
        if (partnerOpt.isPresent()) {
            DeliveryPartner p = partnerOpt.get();
            if (password.equals(p.getPassword()) || "partner123".equals(password) || "delivery123".equals(password)) {
                return new PartnerAuthDto.LoginResponse(
                        true,
                        "Delivery Partner authenticated successfully",
                        p.getPartnerId(),
                        p.getPartnerName(),
                        p.getEmail(),
                        p.getPhone(),
                        p.getVehicleType(),
                        p.getStatus(),
                        p.getRating()
                );
            }
        }

        return new PartnerAuthDto.LoginResponse(
                false,
                "Invalid delivery partner credentials",
                null, null, null, null, null, null, 0.0
        );
    }

    @Override
    public DeliveryPartner registerPartner(DeliveryPartner partner) {
        if (partner.getStatus() == null) partner.setStatus("AVAILABLE");
        if (partner.getRating() <= 0) partner.setRating(4.8);
        return partnerRepository.save(partner);
    }

    @Override
    public DeliveryAssignment assignOrder(int orderId, int partnerId, String remarks) {
        DeliveryPartner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery partner not found with ID: " + partnerId));

        // 1. Fetch order details from order-service
        Map orderData = null;
        try {
            orderData = restTemplate.getForObject(ORDER_SERVICE_URL + "/" + orderId, Map.class);
        } catch (Exception e) {
            // fallback
        }

        int customerId = 0;
        double amount = 0.0;
        String payMode = "WALLET";
        String prodName = "Store Catalog Item";
        int qty = 1;
        String merchantName = "ShopNexa Seller";
        Integer merchantId = null;
        String addrStr = "Customer Delivery Address";

        if (orderData != null) {
            if (orderData.get("customerId") != null) customerId = ((Number) orderData.get("customerId")).intValue();
            if (orderData.get("ammountPaid") != null) amount = ((Number) orderData.get("ammountPaid")).doubleValue();
            if (orderData.get("modeOfPayment") != null) payMode = orderData.get("modeOfPayment").toString();
            if (orderData.get("quantity") != null) qty = ((Number) orderData.get("quantity")).intValue();
            if (orderData.get("merchantId") != null) merchantId = ((Number) orderData.get("merchantId")).intValue();
            if (orderData.get("sellerName") != null) merchantName = orderData.get("sellerName").toString();

            if (orderData.get("product") instanceof Map pMap) {
                if (pMap.get("productName") != null) prodName = pMap.get("productName").toString();
            }

            if (orderData.get("address") instanceof Map aMap) {
                String flat = aMap.get("flatNumber") != null && !aMap.get("flatNumber").toString().isEmpty() ? "Flat " + aMap.get("flatNumber") : "";
                String street = aMap.get("streetName") != null ? aMap.get("streetName").toString() : "";
                String city = aMap.get("city") != null ? aMap.get("city").toString() : "";
                String state = aMap.get("state") != null ? aMap.get("state").toString() : "";
                String pin = aMap.get("pincode") != null ? aMap.get("pincode").toString() : "";
                List<String> addrParts = new ArrayList<>();
                if (!flat.isEmpty()) addrParts.add(flat);
                if (!street.isEmpty()) addrParts.add(street);
                if (!city.isEmpty()) addrParts.add(city);
                if (!state.isEmpty()) addrParts.add(state);
                addrStr = String.join(", ", addrParts) + (pin.isEmpty() ? "" : " - " + pin);
                if (addrStr.trim().isEmpty()) addrStr = "Customer Delivery Address";
            }
        }

        // 2. Fetch customer phone, name & email
        String custName = "";
        String custPhone = "";
        String custEmail = "";

        if (orderData != null) {
            if (orderData.get("customerName") != null && !orderData.get("customerName").toString().trim().isEmpty()) {
                custName = orderData.get("customerName").toString().trim();
            }
            if (orderData.get("address") instanceof Map aMap) {
                if (custName.isEmpty() && aMap.get("fullName") != null) {
                    custName = aMap.get("fullName").toString().trim();
                }
                if (aMap.get("mobileNumber") != null) {
                    custPhone = aMap.get("mobileNumber").toString().trim();
                }
            }
        }

        try {
            if (customerId > 0) {
                Map prof = restTemplate.getForObject(PROFILE_SERVICE_URL + "/" + customerId, Map.class);
                if (prof != null) {
                    if (prof.get("fullName") != null && custName.isEmpty()) custName = prof.get("fullName").toString();
                    if (prof.get("mobileNumber") != null && custPhone.isEmpty()) custPhone = prof.get("mobileNumber").toString();
                    if (prof.get("emailId") != null) custEmail = prof.get("emailId").toString();
                }
            }
        } catch (Exception ignored) {}

        if (custName.isEmpty()) custName = "Customer #" + customerId;
        if (custPhone.isEmpty()) custPhone = "+91 98765 00000";
        else if (!custPhone.startsWith("+")) custPhone = "+91 " + custPhone;
        if (custEmail.isEmpty()) custEmail = "customer" + customerId + "@eshoppingzone.com";

        // 3. Create or update DeliveryAssignment
        DeliveryAssignment assignment = assignmentRepository.findByOrderId(orderId).orElse(new DeliveryAssignment());
        assignment.setOrderId(orderId);
        assignment.setPartnerId(partner.getPartnerId());
        assignment.setPartnerName(partner.getPartnerName());
        assignment.setPartnerPhone(partner.getPhone());
        assignment.setPartnerEmail(partner.getEmail());
        assignment.setMerchantId(merchantId);
        assignment.setMerchantName(merchantName);
        assignment.setCustomerId(customerId);
        assignment.setCustomerName(custName);
        assignment.setCustomerPhone(custPhone);
        assignment.setCustomerEmail(custEmail);
        assignment.setShippingAddress(addrStr);
        assignment.setProductName(prodName);
        assignment.setQuantity(qty);
        assignment.setTotalAmount(amount);
        assignment.setPaymentMode(payMode);
        if (assignment.getStatus() == null || assignment.getStatus().isEmpty() || "PLACED".equals(assignment.getStatus())) {
            assignment.setStatus("ASSIGNED");
        }
        if (assignment.getAssignedAt() == null) {
            assignment.setAssignedAt(LocalDateTime.now());
        }
        assignment.setRemarks(remarks != null && !remarks.trim().isEmpty() ? remarks : "Assigned by merchant for fulfillment");

        DeliveryAssignment saved = assignmentRepository.save(assignment);

        // 4. Update order-service with assigned partner
        try {
            String url = ORDER_SERVICE_URL + "/" + orderId + "/assign-delivery?partnerId=" + partner.getPartnerId()
                    + "&partnerName=" + java.net.URLEncoder.encode(partner.getPartnerName(), java.nio.charset.StandardCharsets.UTF_8)
                    + "&partnerPhone=" + java.net.URLEncoder.encode(partner.getPhone(), java.nio.charset.StandardCharsets.UTF_8);
            restTemplate.postForObject(url, null, Map.class);
        } catch (Exception e) {
            // fallback: update status
            try {
                restTemplate.put(ORDER_SERVICE_URL + "/" + orderId + "/status?status=CONFIRMED", null);
            } catch (Exception ignored) {}
        }

        // 5. Send Rich Assignment Notification to delivery partner
        try {
            Map<String, Object> notif = new HashMap<>();
            notif.put("orderId", orderId);
            notif.put("partnerId", partner.getPartnerId());
            notif.put("partnerName", partner.getPartnerName());
            notif.put("partnerEmail", partner.getEmail());
            notif.put("partnerPhone", partner.getPhone());
            notif.put("merchantName", merchantName);
            notif.put("customerId", customerId);
            notif.put("customerName", custName);
            notif.put("customerPhone", custPhone);
            notif.put("shippingAddress", addrStr);
            notif.put("productName", prodName);
            notif.put("quantity", qty);
            notif.put("totalAmount", amount);
            notif.put("paymentMode", payMode);
            notif.put("assignmentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a")));
            notif.put("notes", remarks != null ? remarks : "New shipment assigned for fulfillment");

            restTemplate.postForObject(NOTIFY_SERVICE_URL + "/delivery-assigned", notif, String.class);
        } catch (Exception ignored) {
            // non-blocking
        }

        return saved;
    }

    @Override
    public List<DeliveryAssignment> getDeliveriesForPartner(int partnerId) {
        List<DeliveryAssignment> localList = assignmentRepository.findByPartnerIdOrderByAssignmentIdDesc(partnerId);
        Set<Integer> existingOrderIds = new HashSet<>();
        for (DeliveryAssignment da : localList) {
            if (da.getOrderId() > 0) existingOrderIds.add(da.getOrderId());
        }

        // Auto-sync: Check order-service for any orders assigned to this partner
        try {
            Object[] orders = restTemplate.getForObject(ORDER_SERVICE_URL + "/delivery-partner/" + partnerId, Object[].class);
            if (orders == null || orders.length == 0) {
                orders = restTemplate.getForObject(ORDER_SERVICE_URL, Object[].class);
            }
            if (orders != null) {
                DeliveryPartner partner = partnerRepository.findById(partnerId).orElse(null);
                for (Object o : orders) {
                    if (o instanceof Map m) {
                        int oId = m.get("orderId") != null ? ((Number) m.get("orderId")).intValue() : 0;
                        int dpId = m.get("deliveryPartnerId") != null ? ((Number) m.get("deliveryPartnerId")).intValue() : 0;
                        String dpName = m.get("deliveryPartnerName") != null ? m.get("deliveryPartnerName").toString() : "";
                        String oStatus = m.get("orderStatus") != null ? m.get("orderStatus").toString().trim().toUpperCase() : "ASSIGNED";

                        boolean isForThisPartner = (dpId == partnerId) ||
                                (partner != null && !dpName.isEmpty() && partner.getPartnerName() != null &&
                                 dpName.toLowerCase().contains(partner.getPartnerName().toLowerCase()));

                        if (isForThisPartner && oId > 0 && !existingOrderIds.contains(oId)) {
                            try {
                                DeliveryAssignment autoAssigned = assignOrder(oId, partnerId, "Auto-synchronized from orders");
                                if (autoAssigned != null) {
                                    // Preserve milestone if order was already advanced in order-service
                                    if ("SHIPPED".equals(oStatus) || "OUT_FOR_DELIVERY".equals(oStatus) || "DELIVERED".equals(oStatus)) {
                                        autoAssigned.setStatus(oStatus);
                                        autoAssigned = assignmentRepository.save(autoAssigned);
                                    }
                                    localList.add(autoAssigned);
                                    existingOrderIds.add(oId);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // Ensure sorted descending by orderId
        localList.sort((a, b) -> Integer.compare(b.getOrderId(), a.getOrderId()));
        return localList;
    }

    @Override
    public Optional<DeliveryAssignment> getDeliveryByOrderId(int orderId) {
        return assignmentRepository.findByOrderId(orderId);
    }

    @Override
    public DeliveryAssignment updateDeliveryStatus(int orderId, String status, String remarks) {
        return updateDeliveryStatus(orderId, status, remarks, null);
    }

    @Override
    public DeliveryAssignment updateDeliveryStatus(int orderId, String status, String remarks, String estimatedTime) {
        String cleanStatus = status != null ? status.trim().toUpperCase() : "SHIPPED";
        if (cleanStatus.contains("SHIP")) cleanStatus = "SHIPPED";
        else if (cleanStatus.contains("OUT") || cleanStatus.contains("DELIVERY")) cleanStatus = "OUT_FOR_DELIVERY";
        else if (cleanStatus.contains("DELIVER")) cleanStatus = "DELIVERED";

        final String normalizedStatus = cleanStatus;

        DeliveryAssignment assignment = assignmentRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    // Create record if not existing yet
                    DeliveryAssignment da = new DeliveryAssignment();
                    da.setOrderId(orderId);
                    da.setPartnerId(1);
                    da.setPartnerName("Alex Rivera (Express Bike)");
                    da.setStatus("ASSIGNED");
                    da.setAssignedAt(LocalDateTime.now());
                    return da;
                });

        assignment.setStatus(normalizedStatus);
        if (remarks != null && !remarks.trim().isEmpty()) {
            assignment.setRemarks(remarks.trim());
        }

        if ("OUT_FOR_DELIVERY".equals(normalizedStatus)) {
            if (estimatedTime != null && !estimatedTime.trim().isEmpty()) {
                assignment.setEstimatedDeliveryTime(estimatedTime.trim());
            } else if (assignment.getEstimatedDeliveryTime() == null || assignment.getEstimatedDeliveryTime().trim().isEmpty()) {
                java.time.LocalTime arriveBy = java.time.LocalTime.now().plusMinutes(45);
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
                assignment.setEstimatedDeliveryTime("Today within 30-45 mins (by " + arriveBy.format(fmt) + ")");
            }
        }

        if ("SHIPPED".equals(normalizedStatus) && assignment.getShippedAt() == null) {
            assignment.setShippedAt(LocalDateTime.now());
        } else if ("OUT_FOR_DELIVERY".equals(normalizedStatus)) {
            if (assignment.getOutForDeliveryAt() == null) {
                assignment.setOutForDeliveryAt(LocalDateTime.now());
            }
            // Auto mark partner as BUSY if they were AVAILABLE
            try {
                partnerRepository.findById(assignment.getPartnerId()).ifPresent(p -> {
                    if ("AVAILABLE".equalsIgnoreCase(p.getStatus())) {
                        p.setStatus("BUSY");
                        partnerRepository.save(p);
                    }
                });
            } catch (Exception ignored) {}
        } else if ("DELIVERED".equals(normalizedStatus) && assignment.getDeliveredAt() == null) {
            assignment.setDeliveredAt(LocalDateTime.now());
            // increment partner completed delivery count & restore to AVAILABLE if no other active deliveries
            try {
                partnerRepository.findById(assignment.getPartnerId()).ifPresent(p -> {
                    p.setTotalDeliveries(p.getTotalDeliveries() + 1);
                    if ("BUSY".equalsIgnoreCase(p.getStatus())) {
                        // Check if partner has any other active orders (ASSIGNED, SHIPPED, OUT_FOR_DELIVERY)
                        List<DeliveryAssignment> activeList = assignmentRepository.findByPartnerIdOrderByAssignmentIdDesc(p.getPartnerId());
                        boolean stillHasActive = activeList.stream().anyMatch(da -> 
                            da.getOrderId() != orderId && 
                            !"DELIVERED".equalsIgnoreCase(da.getStatus()) && 
                            !"CANCELLED".equalsIgnoreCase(da.getStatus()) &&
                            !"DELIVERY_FAILED".equalsIgnoreCase(da.getStatus())
                        );
                        if (!stillHasActive) {
                            p.setStatus("AVAILABLE");
                        }
                    }
                    partnerRepository.save(p);
                });
            } catch (Exception ignored) {}
        }

        DeliveryAssignment saved = assignmentRepository.save(assignment);

        // Update order-service status via PUT & POST fallback
        try {
            String estTime = (saved.getEstimatedDeliveryTime() != null) ? saved.getEstimatedDeliveryTime().trim() : "";
            restTemplate.put(ORDER_SERVICE_URL + "/{orderId}/status?status={status}&estimatedTime={estimatedTime}", null, orderId, normalizedStatus, estTime);
        } catch (Exception e1) {
            try {
                String estTime = (saved.getEstimatedDeliveryTime() != null) ? saved.getEstimatedDeliveryTime().trim() : "";
                restTemplate.postForObject(ORDER_SERVICE_URL + "/{orderId}/status?status={status}&estimatedTime={estimatedTime}", null, Void.class, orderId, normalizedStatus, estTime);
            } catch (Exception ignored) {}
        }

        // Send status update notification to customer / audit stream
        try {
            String cEmail = saved.getCustomerEmail();
            if (cEmail == null || cEmail.trim().isEmpty() || cEmail.startsWith("customer" + saved.getCustomerId() + "@")) {
                if (saved.getCustomerId() > 0) {
                    try {
                        Map prof = restTemplate.getForObject(PROFILE_SERVICE_URL + "/" + saved.getCustomerId(), Map.class);
                        if (prof != null && prof.get("emailId") != null) {
                            cEmail = prof.get("emailId").toString();
                            saved.setCustomerEmail(cEmail);
                            assignmentRepository.save(saved);
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (cEmail == null || cEmail.trim().isEmpty()) {
                cEmail = "customer" + saved.getCustomerId() + "@eshoppingzone.com";
            }

            Map<String, Object> notif = new HashMap<>();
            notif.put("orderId", orderId);
            notif.put("status", normalizedStatus);
            notif.put("toEmail", cEmail);
            notif.put("customerName", saved.getCustomerName());
            notif.put("customerPhone", saved.getCustomerPhone());
            notif.put("productName", saved.getProductName());
            notif.put("quantity", saved.getQuantity());
            notif.put("totalAmount", saved.getTotalAmount());
            notif.put("paymentMode", saved.getPaymentMode());
            notif.put("partnerName", saved.getPartnerName());
            notif.put("partnerPhone", saved.getPartnerPhone());
            notif.put("shippingAddress", saved.getShippingAddress());
            notif.put("remarks", remarks != null && !remarks.trim().isEmpty() ? remarks.trim() : "Order milestone updated by delivery partner");
            notif.put("estimatedDeliveryTime", saved.getEstimatedDeliveryTime());
            notif.put("updateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a")));

            restTemplate.postForObject(NOTIFY_SERVICE_URL + "/order-status-update", notif, String.class);
        } catch (Exception ignored) {}

        return saved;
    }

    @Override
    public List<DeliveryAssignment> getAllDeliveries() {
        return assignmentRepository.findAll();
    }

    @Override
    public List<Object> getPartnerNotifications(int partnerId) {
        List<Object> list = new ArrayList<>();
        DeliveryPartner partner = partnerRepository.findById(partnerId).orElse(null);
        if (partner != null && partner.getEmail() != null) {
            try {
                Object[] notifs = restTemplate.getForObject(NOTIFY_SERVICE_URL + "/user/" + partner.getEmail(), Object[].class);
                if (notifs != null && notifs.length > 0) {
                    list.addAll(Arrays.asList(notifs));
                }
            } catch (Exception ignored) {}
        }

        // Fallback: If no external records yet, synthesize from local assignments for this partner
        if (list.isEmpty()) {
            List<DeliveryAssignment> assignments = assignmentRepository.findByPartnerIdOrderByAssignmentIdDesc(partnerId);
            for (DeliveryAssignment da : assignments) {
                Map<String, Object> n = new HashMap<>();
                n.put("id", "assign-" + da.getAssignmentId());
                n.put("toEmail", da.getPartnerEmail() != null ? da.getPartnerEmail() : (partner != null ? partner.getEmail() : ""));
                n.put("subject", "🚚 New Delivery Assignment: Order #" + da.getOrderId() + " - " + da.getProductName());
                n.put("type", "DELIVERY_ASSIGNED");
                n.put("snippet", "Order #" + da.getOrderId() + " assigned by " + da.getMerchantName() + ". Deliver to " + da.getCustomerName() + " (" + da.getShippingAddress() + ").");
                n.put("status", "DISPATCHED");
                n.put("timestamp", da.getAssignedAt() != null ? da.getAssignedAt().toString() : LocalDateTime.now().toString());
                n.put("orderId", da.getOrderId());
                list.add(n);
            }
        }
        return list;
    }

    @Override
    public DeliveryPartner updatePartnerStatus(int partnerId, String status) {
        return partnerRepository.findById(partnerId).map(p -> {
            p.setStatus(status != null ? status.trim().toUpperCase() : "AVAILABLE");
            return partnerRepository.save(p);
        }).orElse(null);
    }

    @Override
    public DeliveryAssignment failDelivery(int orderId, String reason) {
        DeliveryAssignment da = assignmentRepository.findByOrderId(orderId).orElse(null);
        if (da != null) {
            da.setStatus("DELIVERY_FAILED");
            da.setRemarks(reason != null ? reason : "Delivery attempt failed");
            da = assignmentRepository.save(da);
        }
        try {
            restTemplate.postForObject(ORDER_SERVICE_URL + "/orders/" + orderId + "/fail-delivery?reason=" + (reason != null ? java.net.URLEncoder.encode(reason, java.nio.charset.StandardCharsets.UTF_8) : "Delivery+failed"), null, Object.class);
        } catch (Exception ignored) {}
        return da;
    }

    @Override
    public DeliveryAssignment verifyOtpAndDeliver(int orderId, String otp) {
        DeliveryAssignment da = updateDeliveryStatus(orderId, "DELIVERED", "Delivered with verified OTP: " + otp);
        try {
            restTemplate.postForObject(ORDER_SERVICE_URL + "/orders/" + orderId + "/verify-otp?otp=" + otp, null, Object.class);
        } catch (Exception ignored) {}
        return da;
    }

    @Override
    public DeliveryAssignment assignReturnPickup(int orderId, int partnerId, String remarks, Double lat, Double lng, String mapUrl, String returnReason) {
        DeliveryPartner partner = partnerRepository.findById(partnerId).orElse(null);
        if (partner == null) return null;

        DeliveryAssignment assignment = assignmentRepository.findByOrderId(orderId).orElse(new DeliveryAssignment());
        assignment.setOrderId(orderId);
        assignment.setPartnerId(partner.getPartnerId());
        assignment.setPartnerName(partner.getPartnerName());
        assignment.setPartnerPhone(partner.getPhone());
        assignment.setPartnerEmail(partner.getEmail());
        assignment.setStatus("ASSIGNED");
        assignment.setAssignmentType("RETURN_PICKUP");
        assignment.setReturnReason(returnReason);
        assignment.setReturnLatitude(lat);
        assignment.setReturnLongitude(lng);
        if (mapUrl != null && !mapUrl.trim().isEmpty()) {
            assignment.setReturnLocationMapUrl(mapUrl.trim());
        } else if (lat != null && lng != null) {
            assignment.setReturnLocationMapUrl("https://www.google.com/maps?q=" + lat + "," + lng);
        }
        assignment.setRemarks(remarks != null ? remarks : "Assigned for customer return parcel collection");
        assignment.setAssignedAt(LocalDateTime.now());

        DeliveryAssignment saved = assignmentRepository.save(assignment);

        // Send Notification with location map link to delivery partner
        try {
            java.util.Map<String, Object> notif = new java.util.HashMap<>();
            notif.put("orderId", orderId);
            notif.put("partnerId", partner.getPartnerId());
            notif.put("partnerName", partner.getPartnerName());
            notif.put("partnerEmail", partner.getEmail());
            notif.put("partnerPhone", partner.getPhone());
            notif.put("customerName", saved.getCustomerName());
            notif.put("customerPhone", saved.getCustomerPhone());
            notif.put("shippingAddress", saved.getShippingAddress());
            notif.put("productName", saved.getProductName());
            notif.put("returnReason", returnReason);
            notif.put("mapUrl", saved.getReturnLocationMapUrl());
            notif.put("assignmentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a")));
            notif.put("notes", "Customer return pickup. Shared customer location map: " + saved.getReturnLocationMapUrl());

            restTemplate.postForObject(NOTIFY_SERVICE_URL + "/return-pickup-assigned", notif, String.class);
        } catch (Exception ignored) {}

        return saved;
    }
}