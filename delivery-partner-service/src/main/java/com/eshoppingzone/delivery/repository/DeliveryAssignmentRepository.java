package com.eshoppingzone.delivery.repository;

import com.eshoppingzone.delivery.entity.DeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Integer> {
    List<DeliveryAssignment> findByPartnerId(int partnerId);
    List<DeliveryAssignment> findByPartnerIdOrderByAssignmentIdDesc(int partnerId);
    Optional<DeliveryAssignment> findByOrderId(int orderId);
    List<DeliveryAssignment> findByMerchantId(Integer merchantId);
    List<DeliveryAssignment> findByStatus(String status);
}