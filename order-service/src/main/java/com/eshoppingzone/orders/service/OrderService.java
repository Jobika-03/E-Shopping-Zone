package com.eshoppingzone.orders.service;

import com.eshoppingzone.orders.entity.Address;
import com.eshoppingzone.orders.entity.Orders;
import java.util.List;
import java.util.Optional;

public interface OrderService {
    List<Orders> getAllOrders();
    Orders placeOrder(Orders orders);
    List<Orders> placeMultipleOrders(List<Orders> ordersList);
    void changeStatus(String status, int orderId);
    default void changeStatus(String status, int orderId, String estimatedTime) {
        changeStatus(status, orderId);
    }
    boolean cancelOrder(int orderId);
    List<Integer> cancelMultipleOrders(List<Integer> orderIds);
    void deleteOrder(int orderId);
    List<Integer> deleteMultipleOrders(List<Integer> orderIds);
    List<Orders> getOrderByCustomerId(int customerId);
    List<Orders> getOrdersByMerchantId(int merchantId);
    Orders assignDeliveryPartner(int orderId, int partnerId, String partnerName, String partnerPhone);
    List<Orders> getOrdersByDeliveryPartnerId(int partnerId);
    void storeAddress(Address address);
    List<Address> getAddressByCustomerId(int customerId);
    List<Address> getAllAddress();
    Optional<Orders> getOrderById(int orderId);
    boolean requestReturn(int orderId, String reason);
    default boolean requestReturn(int orderId, String reason, Double latitude, Double longitude, String mapUrl, String locationAddress) {
        return requestReturn(orderId, reason);
    }
    boolean approveReturn(int orderId);
    default boolean approveReturn(int orderId, Integer deliveryPartnerId, String remarks) {
        return approveReturn(orderId);
    }
    boolean rejectReturn(int orderId);
    default boolean rejectReturn(int orderId, String rejectionReason) {
        return rejectReturn(orderId);
    }
    boolean markDeliveryFailed(int orderId, String failureReason);
    boolean verifyDeliveryOtp(int orderId, String otp);
}
