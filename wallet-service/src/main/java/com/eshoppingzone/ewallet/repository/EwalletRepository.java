package com.eshoppingzone.ewallet.repository;

import com.eshoppingzone.ewallet.pojo.EWallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EwalletRepository extends JpaRepository<EWallet, Integer> {
}
