package com.eshoppingzone.delivery.repository;

import com.eshoppingzone.delivery.entity.DeliveryPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Integer> {
    Optional<DeliveryPartner> findByEmail(String email);
    List<DeliveryPartner> findByStatus(String status);
}