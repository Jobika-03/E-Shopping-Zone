package com.eshoppingzone.orders.resource;

import com.eshoppingzone.orders.entity.Address;
import com.eshoppingzone.orders.entity.Orders;
import com.eshoppingzone.orders.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderResource {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public ResponseEntity<List<Orders>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PostMapping("/place")
    public ResponseEntity<Orders> placeOrder(@RequestBody Orders orders) {
        return ResponseEntity.ok(orderService.placeOrder(orders));
    }

    @PostMapping("/place-multiple")
    public ResponseEntity<List<Orders>> placeMultipleOrders(@RequestBody List<Orders> ordersList) {
        return ResponseEntity.ok(orderService.placeMultipleOrders(ordersList));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Orders>> getOrderByCustomerId(@PathVariable("customerId") int customerId) {
        return ResponseEntity.ok(orderService.getOrderByCustomerId(customerId));
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<Orders>> getOrdersByMerchantId(@PathVariable("merchantId") int merchantId) {
        return ResponseEntity.ok(orderService.getOrdersByMerchantId(merchantId));
    }

    @GetMapping("/delivery-partner/{partnerId}")
    public ResponseEntity<List<Orders>> getOrdersByDeliveryPartnerId(@PathVariable("partnerId") int partnerId) {
        return ResponseEntity.ok(orderService.getOrdersByDeliveryPartnerId(partnerId));
    }

    @RequestMapping(value = "/{orderId}/assign-delivery", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Orders> assignDeliveryPartner(@PathVariable("orderId") int orderId,
                                                        @RequestParam("partnerId") int partnerId,
                                                        @RequestParam("partnerName") String partnerName,
                                                        @RequestParam(value = "partnerPhone", required = false) String partnerPhone) {
        Orders updated = orderService.assignDeliveryPartner(orderId, partnerId, partnerName, partnerPhone);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Orders> getOrderById(@PathVariable("orderId") int orderId) {
        return orderService.getOrderById(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping(value = "/{orderId}/status", method = {RequestMethod.PUT, RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<Void> changeStatus(@PathVariable("orderId") int orderId, 
                                             @RequestParam("status") String status,
                                             @RequestParam(value = "estimatedTime", required = false) String estimatedTime) {
        orderService.changeStatus(status, orderId, estimatedTime);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/{orderId}/cancel", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.GET})
    public ResponseEntity<java.util.Map<String, Object>> cancelOrder(@PathVariable("orderId") int orderId) {
        boolean cancelled = orderService.cancelOrder(orderId);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", cancelled);
        resp.put("orderId", orderId);
        resp.put("message", cancelled ? "Order cancelled successfully" : "Order not found");
        return ResponseEntity.ok(resp);
    }

    @RequestMapping(value = "/cancel-multiple", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<List<Integer>> cancelMultipleOrders(@RequestBody(required = false) List<Integer> orderIds) {
        return ResponseEntity.ok(orderService.cancelMultipleOrders(orderIds != null ? orderIds : java.util.Collections.emptyList()));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<java.util.Map<String, Object>> deleteOrder(@PathVariable("orderId") int orderId) {
        orderService.deleteOrder(orderId);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", true);
        resp.put("orderId", orderId);
        resp.put("message", "Order deleted successfully");
        return ResponseEntity.ok(resp);
    }

    @RequestMapping(value = "/{orderId}/delete", method = {RequestMethod.DELETE, RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<java.util.Map<String, Object>> deleteOrderPost(@PathVariable("orderId") int orderId) {
        orderService.deleteOrder(orderId);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", true);
        resp.put("orderId", orderId);
        resp.put("message", "Order deleted successfully");
        return ResponseEntity.ok(resp);
    }

    @RequestMapping(value = "/delete-multiple", method = {RequestMethod.DELETE, RequestMethod.POST})
    public ResponseEntity<List<Integer>> deleteMultipleOrders(@RequestBody(required = false) List<Integer> orderIds) {
        return ResponseEntity.ok(orderService.deleteMultipleOrders(orderIds != null ? orderIds : java.util.Collections.emptyList()));
    }

    @PostMapping("/address")
    public ResponseEntity<Void> storeAddress(@RequestBody Address address) {
        orderService.storeAddress(address);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/address/customer/{customerId}")
    public ResponseEntity<List<Address>> getAddressByCustomerId(@PathVariable("customerId") int customerId) {
        return ResponseEntity.ok(orderService.getAddressByCustomerId(customerId));
    }

    @GetMapping("/address")
    public ResponseEntity<List<Address>> getAllAddress() {
        return ResponseEntity.ok(orderService.getAllAddress());
    }

    @PostMapping("/{orderId}/return")
    public ResponseEntity<java.util.Map<String, Object>> requestReturn(
            @PathVariable("orderId") int orderId,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "mapUrl", required = false) String mapUrl,
            @RequestParam(value = "locationAddress", required = false) String locationAddress) {
        boolean success = orderService.requestReturn(orderId, reason, latitude, longitude, mapUrl, locationAddress);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", success);
        resp.put("orderId", orderId);
        resp.put("status", "RETURN_REQUESTED");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{orderId}/approve-return")
    public ResponseEntity<java.util.Map<String, Object>> approveReturn(
            @PathVariable("orderId") int orderId,
            @RequestParam(value = "partnerId", required = false) Integer partnerId,
            @RequestParam(value = "remarks", required = false) String remarks) {
        boolean success = orderService.approveReturn(orderId, partnerId, remarks);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", success);
        resp.put("orderId", orderId);
        resp.put("status", "REFUNDED");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{orderId}/reject-return")
    public ResponseEntity<java.util.Map<String, Object>> rejectReturn(
            @PathVariable("orderId") int orderId,
            @RequestParam(value = "reason", required = false) String reason) {
        boolean success = orderService.rejectReturn(orderId, reason);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", success);
        resp.put("orderId", orderId);
        resp.put("status", "RETURN_REJECTED");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{orderId}/fail-delivery")
    public ResponseEntity<java.util.Map<String, Object>> failDelivery(
            @PathVariable("orderId") int orderId,
            @RequestParam(value = "reason", required = false) String reason) {
        boolean success = orderService.markDeliveryFailed(orderId, reason);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", success);
        resp.put("orderId", orderId);
        resp.put("status", "DELIVERY_FAILED");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{orderId}/verify-otp")
    public ResponseEntity<java.util.Map<String, Object>> verifyOtp(
            @PathVariable("orderId") int orderId,
            @RequestParam("otp") String otp) {
        boolean success = orderService.verifyDeliveryOtp(orderId, otp);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("success", success);
        resp.put("orderId", orderId);
        resp.put("status", "DELIVERED");
        return ResponseEntity.ok(resp);
    }
}
