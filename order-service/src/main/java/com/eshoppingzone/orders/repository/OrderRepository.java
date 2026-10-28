package com.eshoppingzone.orders.repository;

import com.eshoppingzone.orders.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Integer> {
    List<Orders> findByCustomerId(int customerId);
    List<Orders> findByMerchantId(Integer merchantId);
    List<Orders> findByDeliveryPartnerId(Integer deliveryPartnerId);
    Optional<Orders> findFirstByOrderByOrderIdDesc();
}
