package com.eshoppingzone.ewallet.repository;

import com.eshoppingzone.ewallet.pojo.Statement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StatementsRepository extends JpaRepository<Statement, Integer> {
    List<Statement> findByEwallet_WalletId(int walletId);
}
