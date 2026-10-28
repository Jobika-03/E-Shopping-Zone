package com.eshoppingzone.admin.service;

import com.eshoppingzone.admin.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private RestTemplate restTemplate;

    private static final String PROFILE_SERVICE_URL = "http://localhost:8081";
    private static final String PRODUCT_SERVICE_URL = "http://localhost:8082";
    private static final String ORDER_SERVICE_URL = "http://localhost:8084";


    private static final String NOTIFY_SERVICE_URL = "http://localhost:8086";
    private static final String MERCHANT_SERVICE_URL = "http://localhost:8087";
    private static final String DELIVERY_SERVICE_URL = "http://localhost:8088";

    // 1. Admin Authentication
    @Override
    public AdminAuthDto.LoginResponse login(AdminAuthDto.LoginRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String password = request.getPassword() != null ? request.getPassword().trim() : "";

        // 1. Try JWT Auth endpoint via profile-service
        try {
            Map<String, String> authReq = new HashMap<>();
            authReq.put("email", email);
            authReq.put("password", password);

            Map authResp = restTemplate.postForObject(PROFILE_SERVICE_URL + "/auth/login", authReq, Map.class);
            if (authResp != null && authResp.get("token") != null) {
                String role = authResp.get("role") != null ? authResp.get("role").toString() : "";
                if ("ADMIN".equalsIgnoreCase(role)) {
                    Integer profileId = authResp.get("profileId") != null ? ((Number) authResp.get("profileId")).intValue() : 5;
                    String fullName = authResp.get("fullName") != null ? authResp.get("fullName").toString() : "System Admin";
                    String token = authResp.get("token").toString();

                    return new AdminAuthDto.LoginResponse(true, "Admin authenticated successfully", profileId, fullName, email, "ADMIN", token);
                }
            }
        } catch (Exception ignored) {}

        // 2. Direct fallback for Master Admin
        boolean isAdminEmail = "admin@eshoppingzone.com".equalsIgnoreCase(email);
        boolean isValidAdminPwd = "admin123".equals(password) || "admin".equals(password) || "password123".equals(password) || "Jobika@2004".equals(password) || "123456".equals(password);

        if (isAdminEmail && isValidAdminPwd) {
            return new AdminAuthDto.LoginResponse(true, "Admin authenticated successfully", 5, "System Admin", email, "ADMIN", "demo-admin-token-5");
        }

        // 3. Check profile service fallback by email
        try {
            Map profile = restTemplate.getForObject(PROFILE_SERVICE_URL + "/profiles/email?email=" + email, Map.class);
            if (profile != null) {
                String role = profile.get("role") != null ? profile.get("role").toString() : "";
                if ("ADMIN".equalsIgnoreCase(role)) {
                    Integer profileId = profile.get("profileId") != null ? ((Number) profile.get("profileId")).intValue() : 5;
                    String fullName = profile.get("fullName") != null ? profile.get("fullName").toString() : "System Admin";
                    return new AdminAuthDto.LoginResponse(true, "Admin authenticated successfully", profileId, fullName, email, "ADMIN", "demo-admin-token-" + profileId);
                }
            }
        } catch (Exception ignored) {}

        return new AdminAuthDto.LoginResponse(false, "Access Denied: Invalid Administrator Credentials.", null, null, null, null, null);
    }

    // 2. Platform KPIs Aggregator
    @Override
    public AdminStatsDto getPlatformStats() {
        long users = 0;
        long merchants = 0;
        long deliveryPartners = 0;
        long products = 0;
        long orders = 0;
        double revenue = 0.0;
        long pending = 0;
        long delivered = 0;

        // Profiles count
        try {
            Object[] profs = restTemplate.getForObject(PROFILE_SERVICE_URL + "/profiles", Object[].class);
            if (profs != null) {
                for (Object o : profs) {
                    if (o instanceof Map m) {
                        String role = m.get("role") != null ? m.get("role").toString() : "";
                        if ("MERCHANT".equalsIgnoreCase(role)) merchants++;
                        else if ("CUSTOMER".equalsIgnoreCase(role) || "USER".equalsIgnoreCase(role)) users++;
                    }
                }
            }
        } catch (Exception ignored) {}

        // Delivery partners count
        try {
            Object[] partners = restTemplate.getForObject(DELIVERY_SERVICE_URL + "/delivery/partners", Object[].class);
            if (partners != null) deliveryPartners = partners.length;
        } catch (Exception ignored) {}

        // Products count
        try {
            Object[] prods = restTemplate.getForObject(PRODUCT_SERVICE_URL + "/products", Object[].class);
            if (prods != null) products = prods.length;
        } catch (Exception ignored) {}

        // Orders count & revenue
        try {
            Object[] ords = restTemplate.getForObject(ORDER_SERVICE_URL + "/orders", Object[].class);
            if (ords != null) {
                orders = ords.length;
                for (Object o : ords) {
                    if (o instanceof Map m) {
                        String st = m.get("orderStatus") != null ? m.get("orderStatus").toString().toUpperCase() : "";
                        double amt = m.get("ammountPaid") != null ? ((Number) m.get("ammountPaid")).doubleValue() : 0.0;

                        if (!"CANCELLED".equals(st)) revenue += amt;
                        if ("DELIVERED".equals(st)) delivered++;
                        else if (!"CANCELLED".equals(st)) pending++;
                    }
                }
            }
        } catch (Exception ignored) {}

        return AdminStatsDto.builder()
                .totalUsers(users)
                .totalMerchants(merchants)
                .totalDeliveryPartners(deliveryPartners)
                .totalProducts(products)
                .totalOrders(orders)
                .totalRevenue(revenue)
                .pendingOrders(pending)
                .deliveredOrders(delivered)
                .build();
    }

    // 3. User Management
    @Override
    public List<UserDto> getAllUsers() {
        return fetchProfilesByRole("CUSTOMER");
    }

    @Override
    public UserDto createUser(UserDto user) {
        user.setRole("CUSTOMER");
        return saveProfile(user);
    }

    @Override
    public void deleteUser(int profileId) {
        try {
            restTemplate.delete(PROFILE_SERVICE_URL + "/profiles/" + profileId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 4. Merchant Management
    @Override
    public List<UserDto> getAllMerchants() {
        return fetchProfilesByRole("MERCHANT");
    }

    @Override
    public UserDto createMerchant(UserDto merchant) {
        merchant.setRole("MERCHANT");
        return saveProfile(merchant);
    }

    @Override
    public void deleteMerchant(int profileId) {
        try {
            restTemplate.delete(PROFILE_SERVICE_URL + "/profiles/" + profileId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper: fetch profiles by role
    private List<UserDto> fetchProfilesByRole(String targetRole) {
        List<UserDto> list = new ArrayList<>();
        try {
            Object[] profs = restTemplate.getForObject(PROFILE_SERVICE_URL + "/profiles", Object[].class);
            if (profs != null) {
                for (Object o : profs) {
                    if (o instanceof Map m) {
                        String role = m.get("role") != null ? m.get("role").toString() : "";
                        if (targetRole.equalsIgnoreCase(role) || ("CUSTOMER".equalsIgnoreCase(targetRole) && "USER".equalsIgnoreCase(role))) {
                            UserDto dto = new UserDto();
                            if (m.get("profileId") != null) dto.setProfileId(((Number) m.get("profileId")).intValue());
                            if (m.get("fullName") != null) dto.setFullName(m.get("fullName").toString());
                            if (m.get("emailId") != null) dto.setEmailId(m.get("emailId").toString());
                            if (m.get("mobileNumber") != null) dto.setMobileNumber(((Number) m.get("mobileNumber")).longValue());
                            if (m.get("gender") != null) dto.setGender(m.get("gender").toString());
                            if (m.get("role") != null) dto.setRole(m.get("role").toString());
                            if (m.get("provider") != null) dto.setProvider(m.get("provider").toString());
                            if (m.get("about") != null) dto.setAbout(m.get("about").toString());
                            list.add(dto);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    private UserDto saveProfile(UserDto user) {
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("fullName", user.getFullName());
            req.put("emailId", user.getEmailId());
            req.put("password", user.getPassword() != null ? user.getPassword() : "pass123");
            req.put("mobileNumber", user.getMobileNumber() != null ? user.getMobileNumber() : 9876500000L);
            req.put("gender", user.getGender() != null ? user.getGender() : "Other");
            req.put("role", user.getRole());
            req.put("provider", "LOCAL");
            req.put("about", user.getAbout() != null ? user.getAbout() : "Added via Admin Hub");

            Map resp = restTemplate.postForObject(PROFILE_SERVICE_URL + "/profiles", req, Map.class);
            if (resp != null && resp.get("profileId") != null) {
                user.setProfileId(((Number) resp.get("profileId")).intValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

    // 5. Delivery Partner Management
    @Override
    public List<DeliveryPartnerDto> getAllDeliveryPartners() {
        List<DeliveryPartnerDto> list = new ArrayList<>();
        try {
            Object[] partners = restTemplate.getForObject(DELIVERY_SERVICE_URL + "/delivery/partners", Object[].class);
            if (partners != null) {
                for (Object o : partners) {
                    if (o instanceof Map m) {
                        DeliveryPartnerDto dto = new DeliveryPartnerDto();
                        if (m.get("partnerId") != null) dto.setPartnerId(((Number) m.get("partnerId")).intValue());
                        if (m.get("partnerName") != null) dto.setPartnerName(m.get("partnerName").toString());
                        if (m.get("email") != null) dto.setEmail(m.get("email").toString());
                        if (m.get("phone") != null) dto.setPhone(m.get("phone").toString());
                        if (m.get("vehicleType") != null) dto.setVehicleType(m.get("vehicleType").toString());
                        if (m.get("serviceZone") != null) dto.setServiceZone(m.get("serviceZone").toString());
                        if (m.get("status") != null) dto.setStatus(m.get("status").toString());
                        if (m.get("rating") != null) dto.setRating(((Number) m.get("rating")).doubleValue());
                        if (m.get("totalDeliveries") != null) dto.setTotalDeliveries(((Number) m.get("totalDeliveries")).intValue());
                        dto.setActive(true);
                        list.add(dto);
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    @Override
    public DeliveryPartnerDto createDeliveryPartner(DeliveryPartnerDto partner) {
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("partnerName", partner.getPartnerName());
            req.put("email", partner.getEmail());
            req.put("phone", partner.getPhone());
            req.put("vehicleType", partner.getVehicleType() != null ? partner.getVehicleType() : "Bike");
            req.put("zone", partner.getServiceZone() != null ? partner.getServiceZone() : "Central Metro");
            req.put("password", partner.getPassword() != null ? partner.getPassword() : "partner123");
            req.put("active", true);

            Map resp = restTemplate.postForObject(DELIVERY_SERVICE_URL + "/delivery/register", req, Map.class);
            if (resp != null && resp.get("partnerId") != null) {
                partner.setPartnerId(((Number) resp.get("partnerId")).intValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return partner;
    }

    @Override
    public void deleteDeliveryPartner(int partnerId) {
        try {
            restTemplate.delete(DELIVERY_SERVICE_URL + "/delivery/partners/" + partnerId);
        } catch (Exception e) {
            // fallback: update status
            try {
                restTemplate.put(DELIVERY_SERVICE_URL + "/delivery/partners/" + partnerId + "/status?status=INACTIVE", null);
            } catch (Exception ignored) {}
        }
    }

    // 6. Product Management
    @Override
    public List<ProductDto> getAllProducts() {
        List<ProductDto> list = new ArrayList<>();
        try {
            ProductDto[] prods = restTemplate.getForObject(PRODUCT_SERVICE_URL + "/products", ProductDto[].class);
            if (prods != null) {
                return Arrays.asList(prods);
            }
        } catch (Exception ignored) {}
        return list;
    }

    @Override
    public ProductDto createProduct(ProductDto product) {
        try {
            return restTemplate.postForObject(PRODUCT_SERVICE_URL + "/products", product, ProductDto.class);
        } catch (Exception e) {
            e.printStackTrace();
            return product;
        }
    }

    @Override
    public void deleteProduct(int productId) {
        try {
            restTemplate.delete(PRODUCT_SERVICE_URL + "/products/" + productId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 7. Order Management
    @Override
    public List<OrderDto> getAllOrders() {
        List<OrderDto> list = new ArrayList<>();
        try {
            OrderDto[] ords = restTemplate.getForObject(ORDER_SERVICE_URL + "/orders", OrderDto[].class);
            if (ords != null) {
                return Arrays.asList(ords);
            }
        } catch (Exception ignored) {}
        return list;
    }

    @Override
    public void updateOrderStatus(int orderId, String status) {
        try {
            restTemplate.put(ORDER_SERVICE_URL + "/orders/" + orderId + "/status?status=" + status.trim().toUpperCase(), null);
        } catch (Exception e) {
            try {
                restTemplate.postForObject(ORDER_SERVICE_URL + "/orders/" + orderId + "/status?status=" + status.trim().toUpperCase(), null, Void.class);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void updateUserStatus(int profileId, String status) {
        try {
            restTemplate.put(PROFILE_SERVICE_URL + "/profiles/" + profileId + "/status?status=" + status, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateMerchantStatus(int profileId, String status) {
        updateUserStatus(profileId, status);
    }

    @Override
    public void updateDeliveryPartnerStatus(int partnerId, String status) {
        try {
            restTemplate.put(DELIVERY_SERVICE_URL + "/delivery/partners/" + partnerId + "/status?status=" + status, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getReportsSummary() {
        Map<String, Object> report = new HashMap<>();
        try {
            List<OrderDto> orders = getAllOrders();
            List<UserDto> users = getAllUsers();
            List<UserDto> merchants = getAllMerchants();
            List<DeliveryPartnerDto> partners = getAllDeliveryPartners();
            List<ProductDto> products = getAllProducts();

            double totalSales = orders.stream()
                    .filter(o -> !"CANCELLED".equalsIgnoreCase(o.getOrderStatus()))
                    .mapToDouble(OrderDto::getAmmountPaid)
                    .sum();

            long deliveredCount = orders.stream().filter(o -> "DELIVERED".equalsIgnoreCase(o.getOrderStatus())).count();
            long cancelledCount = orders.stream().filter(o -> "CANCELLED".equalsIgnoreCase(o.getOrderStatus())).count();
            long pendingCount = orders.size() - deliveredCount - cancelledCount;

            report.put("totalOrders", orders.size());
            report.put("totalSales", totalSales);
            report.put("deliveredOrders", deliveredCount);
            report.put("cancelledOrders", cancelledCount);
            report.put("pendingOrders", pendingCount);
            report.put("totalCustomers", users.size());
            report.put("totalMerchants", merchants.size());
            report.put("totalCouriers", partners.size());
            report.put("totalProducts", products.size());
            report.put("generatedAt", java.time.LocalDateTime.now().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return report;
    }

    @Override
    public Map<String, Object> launchMegaSale(String saleTitle, double discountPercent, String category) {
        try {
            String url = PRODUCT_SERVICE_URL + "/products/admin-mega-sale?saleTitle=" 
                    + java.net.URLEncoder.encode(saleTitle != null ? saleTitle : "Big Billion Days Sale", java.nio.charset.StandardCharsets.UTF_8)
                    + "&discountPercent=" + discountPercent;
            if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ALL")) {
                url += "&category=" + java.net.URLEncoder.encode(category, java.nio.charset.StandardCharsets.UTF_8);
            }
            return restTemplate.postForObject(url, null, Map.class);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return err;
        }
    }

    @Override
    public Map<String, Object> endMegaSale() {
        try {
            return restTemplate.postForObject(PRODUCT_SERVICE_URL + "/products/end-mega-sale", null, Map.class);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return err;
        }
    }

    @Override
    public Map<String, Object> getActiveMegaSale() {
        try {
            return restTemplate.getForObject(PRODUCT_SERVICE_URL + "/products/active-mega-sale", Map.class);
        } catch (Exception e) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("active", false);
            return empty;
        }
    }
}
