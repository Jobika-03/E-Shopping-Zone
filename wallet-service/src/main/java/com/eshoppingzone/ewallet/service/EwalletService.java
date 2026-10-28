package com.eshoppingzone.ewallet.service;

import com.eshoppingzone.ewallet.pojo.EWallet;
import com.eshoppingzone.ewallet.pojo.Statement;
import java.util.List;

public interface EwalletService {
    List<EWallet> getWallets();
    EWallet addWallet(EWallet ewallet);
    void addmoney(EWallet ewallet, double amount, String transactionType, int walletId);
    void update(EWallet ewallet, double amount, String transactionType);
    Double getById(int walletId);
    List<Statement> getStatementsById(int walletId);
    List<Statement> getStatements();
    void deleteById(int walletId);
    EWallet depositMoney(int walletId, double amount, String remarks);
    EWallet payMoney(int walletId, double amount, int orderId);
}
