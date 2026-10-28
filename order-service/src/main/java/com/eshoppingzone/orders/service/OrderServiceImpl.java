package com.eshoppingzone.orders.service;

import com.eshoppingzone.orders.entity.Address;
import com.eshoppingzone.orders.entity.Orders;
import com.eshoppingzone.orders.repository.AddressRepository;
import com.eshoppingzone.orders.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AddressRepository addressRepository;

    private static final String PROFILE_SERVICE_URL = "http://localhost:8081/profiles";
    private static final String NOTIFY_SERVICE_URL = "http://localhost:8086/notify";
    private static final String WALLET_SERVICE_URL = "http://localhost:8085/wallets";
    private static final String PRODUCT_SERVICE_URL = "http://localhost:8082/products";

    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    @Override
    public List<Orders> getAllOrders() {
        List<Orders> list = orderRepository.findAll();
        for (Orders o : list) {
            if (o.getCustomerName() == null || o.getCustomerName().trim().isEmpty()) {
                enrichCustomerDetails(o);
            }
            if ((o.getDeliveryVehicleNumber() == null || o.getDeliveryVehicleNumber().trim().isEmpty()) && (o.getDeliveryPartnerName() != null || o.getDeliveryPartnerId() != null || "OUT_FOR_DELIVERY".equalsIgnoreCase(o.getOrderStatus()))) {
                o.setDeliveryVehicleNumber(resolveVehicleNumber(o.getDeliveryPartnerId(), o.getDeliveryPartnerName()));
            }
            if ("OUT_FOR_DELIVERY".equalsIgnoreCase(o.getOrderStatus())) {
                o.setEstimatedDeliveryTime(resolveEstimatedDeliveryTime(o));
            }
        }
        return list;
    }

    @Override
    public Orders placeOrder(Orders orders) {
        orders.setOrderDate(LocalDate.now());
        if (orders.getOrderStatus() == null) {
            orders.setOrderStatus("PLACED");
        }

        // Auto-enrich merchantId and sellerName if missing
        if (orders.getMerchantId() == null || orders.getMerchantId() <= 0) {
            enrichMerchantDetails(orders);
        }

        // Auto-enrich customerName if missing
        enrichCustomerDetails(orders);

        Orders saved = orderRepository.save(orders);

        // Deduct ordered inventory from product-service so "how many product left" is always accurate
        try {
            if (saved.getProduct() != null && saved.getProduct().getProductId() != null) {
                int pId = Integer.parseInt(saved.getProduct().getProductId());
                int qty = saved.getQuantity() > 0 ? saved.getQuantity() : 1;
                restTemplate.postForObject(PRODUCT_SERVICE_URL + "/" + pId + "/stock/deduct?quantity=" + qty, null, Object.class);
            }
        } catch (Exception ex) {
            System.err.println("[STOCK DEDUCTION ON ORDER PLACED FAILED]: " + ex.getMessage());
        }

        try {
            if (saved.getCustomerId() > 0) {
                Map profile = restTemplate.getForObject(PROFILE_SERVICE_URL + "/" + saved.getCustomerId(), Map.class);
                if (profile != null && profile.get("emailId") != null) {
                    Map<String, Object> notif = new HashMap<>();
                    notif.put("orderId", saved.getOrderId());
                    notif.put("toEmail", profile.get("emailId").toString());
                    notif.put("customerName", profile.get("fullName") != null ? profile.get("fullName").toString() : "Customer");
                    notif.put("totalAmount", saved.getAmmountPaid());
                    notif.put("paymentMode", saved.getModeOfPayment() != null ? saved.getModeOfPayment() : "ONLINE");
                    notif.put("orderStatus", saved.getOrderStatus());
                    if (saved.getAddress() != null) {
                        Address a = saved.getAddress();
                        String addrStr = (a.getFlatNumber() != null ? "Flat " + a.getFlatNumber() + ", " : "")
                                + (a.getCity() != null ? a.getCity() + ", " : "")
                                + (a.getState() != null ? a.getState() : "")
                                + (a.getPincode() != null ? " - " + a.getPincode() : "");
                        notif.put("shippingAddress", addrStr);
                    }
                    restTemplate.postForObject(NOTIFY_SERVICE_URL + "/order-confirmation", notif, String.class);
                }
            }
        } catch (Exception ignored) {
            // Notification service is non-blocking
        }

        return saved;
    }

    @Override
    public List<Orders> placeMultipleOrders(List<Orders> ordersList) {
        if (ordersList == null || ordersList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Orders> savedList = new ArrayList<>();
        for (Orders ord : ordersList) {
            if (ord != null) {
                savedList.add(placeOrder(ord));
            }
        }
        return savedList;
    }

    @Override
    public void changeStatus(String status, int orderId) {
        changeStatus(status, orderId, null);
    }

    @Override
    public void changeStatus(String status, int orderId, String estimatedTime) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setOrderStatus(status);
            if ("OUT_FOR_DELIVERY".equalsIgnoreCase(status)) {
                if (order.getDeliveryVehicleNumber() == null || order.getDeliveryVehicleNumber().trim().isEmpty()) {
                    order.setDeliveryVehicleNumber(resolveVehicleNumber(order.getDeliveryPartnerId(), order.getDeliveryPartnerName()));
                }
                if (estimatedTime != null && !estimatedTime.trim().isEmpty()) {
                    String cleanTime = estimatedTime.trim();
                    if (cleanTime.contains("%")) {
                        try {
                            cleanTime = java.net.URLDecoder.decode(cleanTime, java.nio.charset.StandardCharsets.UTF_8);
                        } catch (Exception ignored) {}
                    }
                    order.setEstimatedDeliveryTime(cleanTime);
                } else if (order.getEstimatedDeliveryTime() == null || order.getEstimatedDeliveryTime().trim().isEmpty()) {
                    java.time.LocalTime arriveBy = java.time.LocalTime.now().plusMinutes(45);
                    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
                    order.setEstimatedDeliveryTime("Today within 30-45 mins (by " + arriveBy.format(fmt) + ")");
                }
            }
            orderRepository.save(order);
        });
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public boolean cancelOrder(int orderId) {
        try {
            Optional<Orders> opt = orderRepository.findById(orderId);
            if (opt.isEmpty()) {
                return false;
            }
            Orders order = opt.get();
            if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
                return true; // Already cancelled
            }
            order.setOrderStatus("CANCELLED");
            orderRepository.save(order);

            // Refund wallet if payment was made via WALLET
            if (order.getModeOfPayment() != null && order.getModeOfPayment().toUpperCase().contains("WALLET") && order.getAmmountPaid() > 0) {
                try {
                    restTemplate.postForObject(
                        WALLET_SERVICE_URL + "/" + order.getCustomerId() + "/deposit?amount=" + order.getAmmountPaid(),
                        null,
                        Object.class
                    );
                } catch (Exception e) {
                    System.err.println("[WALLET REFUND ERROR]: " + e.getMessage());
                }
            }

            // Restore stock in product-service
            try {
                if (order.getProduct() != null && order.getProduct().getProductId() != null) {
                    int pId = Integer.parseInt(order.getProduct().getProductId());
                    int qty = order.getQuantity() > 0 ? order.getQuantity() : 1;
                    restTemplate.postForObject(PRODUCT_SERVICE_URL + "/" + pId + "/stock/restore?quantity=" + qty, null, Object.class);
                }
            } catch (Exception e) {
                System.err.println("[STOCK RESTORE ON CANCEL FAILED]: " + e.getMessage());
            }

            // Send notification if notification service is available
            try {
                if (order.getCustomerId() > 0) {
                    Map profile = restTemplate.getForObject(PROFILE_SERVICE_URL + "/" + order.getCustomerId(), Map.class);
                    if (profile != null && profile.get("emailId") != null) {
                        Map<String, Object> cancelNotif = new HashMap<>();
                        cancelNotif.put("orderId", order.getOrderId());
                        cancelNotif.put("toEmail", profile.get("emailId").toString());
                        cancelNotif.put("customerName", profile.get("fullName") != null ? profile.get("fullName").toString() : "Customer");
                        cancelNotif.put("totalAmount", order.getAmmountPaid());
                        cancelNotif.put("refundAmount", order.getAmmountPaid());
                        cancelNotif.put("paymentMode", order.getModeOfPayment() != null ? order.getModeOfPayment() : "ONLINE");
                        cancelNotif.put("orderStatus", "CANCELLED");
                        boolean isWallet = order.getModeOfPayment() != null && order.getModeOfPayment().toUpperCase().contains("WALLET");
                        cancelNotif.put("refundStatus", isWallet ? "Refunded to E-Wallet Balance" : "Payment Cancelled / Refund Processed");
                        cancelNotif.put("cancellationReason", "Cancelled by customer");
                        if (order.getProduct() != null) {
                            cancelNotif.put("productName", order.getProduct().getProductName());
                            cancelNotif.put("quantity", order.getQuantity() > 0 ? order.getQuantity() : 1);
                        }
                        if (order.getAddress() != null) {
                            Address a = order.getAddress();
                            String addrStr = (a.getFlatNumber() != null ? "Flat " + a.getFlatNumber() + ", " : "")
                                    + (a.getCity() != null ? a.getCity() + ", " : "")
                                    + (a.getState() != null ? a.getState() : "")
                                    + (a.getPincode() != null ? " - " + a.getPincode() : "");
                            cancelNotif.put("shippingAddress", addrStr);
                        }
                        restTemplate.postForObject(NOTIFY_SERVICE_URL + "/order-cancelled", cancelNotif, String.class);
                    }
                }
            } catch (Exception ignored) {}

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Integer> cancelMultipleOrders(List<Integer> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> cancelled = new ArrayList<>();
        for (Integer id : orderIds) {
            if (id != null && cancelOrder(id)) {
                cancelled.add(id);
            }
        }
        return cancelled;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteOrder(int orderId) {
        try {
            orderRepository.findById(orderId).ifPresent(order -> {
                order.setAddress(null);
                orderRepository.save(order);
                orderRepository.delete(order);
            });
        } catch (Exception e) {
            try {
                orderRepository.deleteById(orderId);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public List<Integer> deleteMultipleOrders(List<Integer> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> deleted = new ArrayList<>();
        for (Integer id : orderIds) {
            if (id != null) {
                deleteOrder(id);
                deleted.add(id);
            }
        }
        return deleted;
    }

    @Override
    public List<Orders> getOrderByCustomerId(int customerId) {
        List<Orders> list = orderRepository.findByCustomerId(customerId);
        for (Orders o : list) {
            if (o.getCustomerName() == null || o.getCustomerName().trim().isEmpty()) {
                enrichCustomerDetails(o);
            }
            if ((o.getDeliveryVehicleNumber() == null || o.getDeliveryVehicleNumber().trim().isEmpty()) && (o.getDeliveryPartnerName() != null || o.getDeliveryPartnerId() != null || "OUT_FOR_DELIVERY".equalsIgnoreCase(o.getOrderStatus()))) {
                o.setDeliveryVehicleNumber(resolveVehicleNumber(o.getDeliveryPartnerId(), o.getDeliveryPartnerName()));
            }
            if ("OUT_FOR_DELIVERY".equalsIgnoreCase(o.getOrderStatus())) {
                o.setEstimatedDeliveryTime(resolveEstimatedDeliveryTime(o));
            }
        }
        return list;
    }

    @Override
    public List<Orders> getOrdersByMerchantId(int merchantId) {
        List<Orders> direct = orderRepository.findByMerchantId(merchantId);
        List<Orders> result = new ArrayList<>(direct);

        // Auto-heal and backfill: check all orders to see if any have products belonging to this merchant
        try {
            Map[] merchantProds = restTemplate.getForObject(PRODUCT_SERVICE_URL + "/merchant/" + merchantId, Map[].class);
            if (merchantProds != null && merchantProds.length > 0) {
                Set<String> mProdNames = new HashSet<>();
                Set<String> mProdIds = new HashSet<>();
                for (Map p : merchantProds) {
                    if (p.get("productName") != null) {
                        mProdNames.add(p.get("productName").toString().trim().toLowerCase());
                    }
                    if (p.get("productId") != null) {
                        mProdIds.add(p.get("productId").toString().trim());
                    }
                }

                List<Orders> all = orderRepository.findAll();
                Set<Integer> existingIds = new HashSet<>();
                for (Orders o : result) {
                    existingIds.add(o.getOrderId());
                }

                for (Orders ord : all) {
                    if (existingIds.contains(ord.getOrderId())) continue;
                    boolean matches = false;
                    if (ord.getProduct() != null) {
                        String pName = ord.getProduct().getProductName();
                        String pId = ord.getProduct().getProductId();
                        if (pName != null && mProdNames.contains(pName.trim().toLowerCase())) {
                            matches = true;
                        } else if (pId != null && mProdIds.contains(pId.trim())) {
                            matches = true;
                        }
                    }

                    if (matches) {
                        // Heal merchantId in DB if missing
                        if (ord.getMerchantId() == null || ord.getMerchantId() <= 0) {
                            ord.setMerchantId(merchantId);
                            orderRepository.save(ord);
                        }
                        result.add(ord);
                        existingIds.add(ord.getOrderId());
                    }
                }
            }
        } catch (Exception ignored) {}

        // Sort descending by orderId so newest orders are first
        result.sort((a, b) -> Integer.compare(b.getOrderId(), a.getOrderId()));
        for (Orders ord : result) {
            if (ord.getCustomerName() == null || ord.getCustomerName().trim().isEmpty()) {
                enrichCustomerDetails(ord);
            }
        }
        return result;
    }

    private void enrichMerchantDetails(Orders orders) {
        if (orders.getProduct() == null) return;
        try {
            String pId = orders.getProduct().getProductId();
            String pName = orders.getProduct().getProductName();

            // 1. Try productId if numeric
            if (pId != null && !pId.trim().isEmpty() && !"0".equals(pId.trim())) {
                try {
                    int numericId = Integer.parseInt(pId.trim());
                    Map prodMap = restTemplate.getForObject(PRODUCT_SERVICE_URL + "/" + numericId, Map.class);
                    if (prodMap != null && prodMap.get("merchantId") != null) {
                        orders.setMerchantId(((Number) prodMap.get("merchantId")).intValue());
                        if (prodMap.get("sellerName") != null) {
                            orders.setSellerName(prodMap.get("sellerName").toString());
                        }
                        return;
                    }
                } catch (Exception ignored) {}
            }

            // 2. Query all products and match by productName (case-insensitive & trimmed)
            if (pName != null && !pName.trim().isEmpty()) {
                Map[] allProds = restTemplate.getForObject(PRODUCT_SERVICE_URL, Map[].class);
                if (allProds != null) {
                    for (Map p : allProds) {
                        Object nameObj = p.get("productName");
                        if (nameObj != null && nameObj.toString().trim().equalsIgnoreCase(pName.trim())) {
                            if (p.get("merchantId") != null) {
                                orders.setMerchantId(((Number) p.get("merchantId")).intValue());
                            }
                            if (p.get("sellerName") != null) {
                                orders.setSellerName(p.get("sellerName").toString());
                            }
                            if (orders.getProduct().getProductId() == null || "0".equals(orders.getProduct().getProductId())) {
                                if (p.get("productId") != null) {
                                    orders.getProduct().setProductId(p.get("productId").toString());
                                }
                            }
                            return;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void enrichCustomerDetails(Orders orders) {
        if (orders == null) return;
        if (orders.getCustomerName() != null && !orders.getCustomerName().trim().isEmpty()) {
            return;
        }
        // 1. Try from address.fullName
        if (orders.getAddress() != null && orders.getAddress().getFullName() != null && !orders.getAddress().getFullName().trim().isEmpty()) {
            orders.setCustomerName(orders.getAddress().getFullName().trim());
            return;
        }
        // 2. Try from profile-service using customerId
        if (orders.getCustomerId() > 0) {
            try {
                Map profile = restTemplate.getForObject(PROFILE_SERVICE_URL + "/" + orders.getCustomerId(), Map.class);
                if (profile != null && profile.get("fullName") != null && !profile.get("fullName").toString().trim().isEmpty()) {
                    orders.setCustomerName(profile.get("fullName").toString().trim());
                    return;
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public Orders assignDeliveryPartner(int orderId, int partnerId, String partnerName, String partnerPhone) {
        return orderRepository.findById(orderId).map(order -> {
            order.setDeliveryPartnerId(partnerId);
            String cleanPartnerName = partnerName;
            if (cleanPartnerName != null && cleanPartnerName.contains("%")) {
                try {
                    cleanPartnerName = java.net.URLDecoder.decode(cleanPartnerName, java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception ignored) {}
            }
            order.setDeliveryPartnerName(cleanPartnerName);
            String cleanPhone = partnerPhone;
            if (cleanPhone != null && cleanPhone.contains("%")) {
                try {
                    cleanPhone = java.net.URLDecoder.decode(cleanPhone, java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception ignored) {}
            }
            order.setDeliveryPartnerPhone(cleanPhone);
            order.setDeliveryAssignedDate(LocalDate.now().toString());
            order.setDeliveryVehicleNumber(resolveVehicleNumber(partnerId, cleanPartnerName));
            if ("PLACED".equalsIgnoreCase(order.getOrderStatus()) || "PLACED_COD".equalsIgnoreCase(order.getOrderStatus())) {
                order.setOrderStatus("CONFIRMED");
            }
            return orderRepository.save(order);
        }).orElse(null);
    }

    private String resolveVehicleNumber(Integer partnerId, String partnerName) {
        if (partnerId != null) {
            return switch (partnerId) {
                case 1 -> "TN-33-EQ-4521 (Express Bike)";
                case 2 -> "TN-01-BD-2202 (Fleet Van)";
                case 3 -> "TN-33-EV-3003 (Electric Van)";
                case 4 -> "TN-38-SF-4404 (Hyperlocal Bike)";
                case 5 -> "TN-28-TR-5505 (Cargo Truck)";
                default -> "TN-33-DV-" + (1000 + (partnerId * 111) % 9000);
            };
        }
        if (partnerName != null && !partnerName.trim().isEmpty()) {
            String lower = partnerName.toLowerCase();
            if (lower.contains("alex") || lower.contains("bike")) return "TN-33-EQ-4521 (Express Bike)";
            if (lower.contains("bluedart") || lower.contains("fleet")) return "TN-01-BD-2202 (Fleet Van)";
            if (lower.contains("priya") || lower.contains("electric")) return "TN-33-EV-3003 (Electric Van)";
            if (lower.contains("shadowfax")) return "TN-38-SF-4404 (Hyperlocal Bike)";
            if (lower.contains("vikram") || lower.contains("truck") || lower.contains("heavy")) return "TN-28-TR-5505 (Cargo Truck)";
        }
        return "TN-33-EQ-4521 (Express Delivery Vehicle)";
    }

    @Override
    public List<Orders> getOrdersByDeliveryPartnerId(int partnerId) {
        List<Orders> list = orderRepository.findByDeliveryPartnerId(partnerId);
        list.sort((a, b) -> Integer.compare(b.getOrderId(), a.getOrderId()));
        return list;
    }

    @Override
    public void storeAddress(Address address) {
        addressRepository.save(address);
    }

    @Override
    public List<Address> getAddressByCustomerId(int customerId) {
        return addressRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Address> getAllAddress() {
        return addressRepository.findAll();
    }

    private String resolveEstimatedDeliveryTime(Orders o) {
        if (o == null) return null;
        if (o.getEstimatedDeliveryTime() != null && !o.getEstimatedDeliveryTime().trim().isEmpty()) {
            return o.getEstimatedDeliveryTime();
        }
        if ("OUT_FOR_DELIVERY".equalsIgnoreCase(o.getOrderStatus())) {
            java.time.LocalTime arriveBy = java.time.LocalTime.now().plusMinutes(45);
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
            return "Today within 30-45 mins (by " + arriveBy.format(fmt) + ")";
        }
        return null;
    }

    @Override
    public Optional<Orders> getOrderById(int orderId) {
        Optional<Orders> opt = orderRepository.findById(orderId);
        opt.ifPresent(o -> {
            if (o.getCustomerName() == null || o.getCustomerName().trim().isEmpty()) {
                enrichCustomerDetails(o);
            }
            if ((o.getDeliveryVehicleNumber() == null || o.getDeliveryVehicleNumber().trim().isEmpty()) && (o.getDeliveryPartnerName() != null || o.getDeliveryPartnerId() != null || "OUT_FOR_DELIVERY".equalsIgnoreCase(o.getOrderStatus()))) {
                o.setDeliveryVehicleNumber(resolveVehicleNumber(o.getDeliveryPartnerId(), o.getDeliveryPartnerName()));
            }
            if ("OUT_FOR_DELIVERY".equalsIgnoreCase(o.getOrderStatus())) {
                o.setEstimatedDeliveryTime(resolveEstimatedDeliveryTime(o));
            }
        });
        return opt;
    }

    @Override
    public boolean requestReturn(int orderId, String reason) {
        return requestReturn(orderId, reason, null, null, null, null);
    }

    @Override
    public boolean requestReturn(int orderId, String reason, Double latitude, Double longitude, String mapUrl, String locationAddress) {
        return orderRepository.findById(orderId).map(order -> {
            order.setOrderStatus("RETURN_REQUESTED");
            order.setReturnReason(reason != null ? reason : "Customer return requested");
            if (latitude != null) order.setReturnLatitude(latitude);
            if (longitude != null) order.setReturnLongitude(longitude);
            if (mapUrl != null && !mapUrl.trim().isEmpty()) {
                order.setReturnLocationMapUrl(mapUrl.trim());
            } else if (latitude != null && longitude != null) {
                order.setReturnLocationMapUrl("https://www.google.com/maps?q=" + latitude + "," + longitude);
            }
            if (locationAddress != null && !locationAddress.trim().isEmpty()) {
                order.setReturnLocationAddress(locationAddress.trim());
            }
            order.setReturnRejectionReason(null);
            orderRepository.save(order);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean approveReturn(int orderId) {
        return approveReturn(orderId, null, null);
    }

    @Override
    public boolean approveReturn(int orderId, Integer deliveryPartnerId, String remarks) {
        return orderRepository.findById(orderId).map(order -> {
            order.setOrderStatus("REFUNDED");
            if (deliveryPartnerId != null && deliveryPartnerId > 0) {
                order.setDeliveryPartnerId(deliveryPartnerId);
            }
            orderRepository.save(order);

            // 1. Process Refund into Customer Wallet
            if (order.getAmmountPaid() > 0) {
                try {
                    restTemplate.postForObject(WALLET_SERVICE_URL + "/" + order.getCustomerId() + "/deposit?amount=" + order.getAmmountPaid(), null, Object.class);
                } catch (Exception ex) {
                    System.err.println("[WALLET RETURN REFUND FAILED]: " + ex.getMessage());
                }
            }

            // 2. Restore Stock in product-service
            try {
                if (order.getProduct() != null && order.getProduct().getProductId() != null) {
                    int pId = Integer.parseInt(order.getProduct().getProductId());
                    int qty = order.getQuantity() > 0 ? order.getQuantity() : 1;
                    restTemplate.postForObject("http://localhost:8082/products/" + pId + "/stock/restore?quantity=" + qty, null, Object.class);
                }
            } catch (Exception ex) {
                System.err.println("[STOCK RESTORE FAILED]: " + ex.getMessage());
            }

            // 3. Notify Delivery Partner for return pickup if assigned
            try {
                Integer pId = deliveryPartnerId != null && deliveryPartnerId > 0 ? deliveryPartnerId : order.getDeliveryPartnerId();
                if (pId != null && pId > 0) {
                    java.util.Map<String, Object> req = new java.util.HashMap<>();
                    req.put("orderId", orderId);
                    req.put("partnerId", pId);
                    req.put("remarks", remarks != null ? remarks : "Return item pickup assigned by merchant");
                    req.put("latitude", order.getReturnLatitude());
                    req.put("longitude", order.getReturnLongitude());
                    req.put("mapUrl", order.getReturnLocationMapUrl());
                    req.put("returnReason", order.getReturnReason());
                    restTemplate.postForObject("http://localhost:8088/delivery/assign-return", req, Object.class);
                }
            } catch (Exception ex) {
                System.err.println("[DELIVERY PARTNER RETURN NOTIFICATION FAILED]: " + ex.getMessage());
            }

            return true;
        }).orElse(false);
    }

    @Override
    public boolean rejectReturn(int orderId) {
        return rejectReturn(orderId, "Return request was reviewed and rejected by merchant");
    }

    @Override
    public boolean rejectReturn(int orderId, String rejectionReason) {
        return orderRepository.findById(orderId).map(order -> {
            order.setOrderStatus("RETURN_REJECTED");
            order.setReturnRejectionReason(rejectionReason != null && !rejectionReason.trim().isEmpty() 
                    ? rejectionReason.trim() 
                    : "Return request was reviewed and rejected by merchant.");
            orderRepository.save(order);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean markDeliveryFailed(int orderId, String failureReason) {
        return orderRepository.findById(orderId).map(order -> {
            order.setOrderStatus("DELIVERY_FAILED");
            order.setFailureReason(failureReason != null ? failureReason : "Customer unreachable");
            orderRepository.save(order);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean verifyDeliveryOtp(int orderId, String otp) {
        return orderRepository.findById(orderId).map(order -> {
            order.setOrderStatus("DELIVERED");
            order.setDeliveryOtp(otp);
            orderRepository.save(order);
            return true;
        }).orElse(false);
    }
}
