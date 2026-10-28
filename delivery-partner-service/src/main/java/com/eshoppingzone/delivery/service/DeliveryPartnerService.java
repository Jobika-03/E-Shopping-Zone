package com.eshoppingzone.delivery.service;

import com.eshoppingzone.delivery.dto.PartnerAuthDto;
import com.eshoppingzone.delivery.entity.DeliveryAssignment;
import com.eshoppingzone.delivery.entity.DeliveryPartner;
import java.util.List;
import java.util.Optional;

public interface DeliveryPartnerService {
    List<DeliveryPartner> getAllPartners();
    Optional<DeliveryPartner> getPartnerById(int partnerId);
    PartnerAuthDto.LoginResponse login(PartnerAuthDto.LoginRequest request);
    DeliveryPartner registerPartner(DeliveryPartner partner);

    DeliveryAssignment assignOrder(int orderId, int partnerId, String remarks);
    List<DeliveryAssignment> getDeliveriesForPartner(int partnerId);
    Optional<DeliveryAssignment> getDeliveryByOrderId(int orderId);
    DeliveryAssignment updateDeliveryStatus(int orderId, String status, String remarks);
    default DeliveryAssignment updateDeliveryStatus(int orderId, String status, String remarks, String estimatedTime) {
        return updateDeliveryStatus(orderId, status, remarks);
    }
    List<DeliveryAssignment> getAllDeliveries();
    List<Object> getPartnerNotifications(int partnerId);
    DeliveryPartner updatePartnerStatus(int partnerId, String status);
    DeliveryAssignment failDelivery(int orderId, String reason);
    DeliveryAssignment verifyOtpAndDeliver(int orderId, String otp);
    DeliveryAssignment assignReturnPickup(int orderId, int partnerId, String remarks, Double lat, Double lng, String mapUrl, String returnReason);
}