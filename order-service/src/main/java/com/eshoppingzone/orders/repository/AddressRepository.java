package com.eshoppingzone.orders.repository;

import com.eshoppingzone.orders.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Integer> {
    List<Address> findByCustomerId(int customerId);
}
