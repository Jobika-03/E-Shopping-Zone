package com.eshoppingzone.ewallet.service;

import com.eshoppingzone.ewallet.pojo.EWallet;
import com.eshoppingzone.ewallet.pojo.Statement;
import com.eshoppingzone.ewallet.repository.EwalletRepository;
import com.eshoppingzone.ewallet.repository.StatementsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EwalletServiceImpl implements EwalletService {

    @Autowired
    private EwalletRepository ewalletRepository;

    @Autowired
    private StatementsRepository statementsRepository;

    @Override
    public List<EWallet> getWallets() {
        return ewalletRepository.findAll();
    }

    @Override
    public EWallet addWallet(EWallet ewallet) {
        if (ewallet.getCurrentBalance() == null) ewallet.setCurrentBalance(0.0);
        return ewalletRepository.save(ewallet);
    }

    @Override
    public void addmoney(EWallet ewallet, double amount, String transactionType, int walletId) {
        depositMoney(walletId, amount, transactionType);
    }

    @Override
    public void update(EWallet ewallet, double amount, String transactionType) {
        depositMoney(ewallet.getWalletId(), amount, transactionType);
    }

    @Override
    public Double getById(int walletId) {
        return ewalletRepository.findById(walletId)
                .map(EWallet::getCurrentBalance)
                .orElse(0.0);
    }

    @Override
    public List<Statement> getStatementsById(int walletId) {
        return statementsRepository.findByEwallet_WalletId(walletId);
    }

    @Override
    public List<Statement> getStatements() {
        return statementsRepository.findAll();
    }

    @Override
    public void deleteById(int walletId) {
        ewalletRepository.deleteById(walletId);
    }

    @Override
    public EWallet depositMoney(int walletId, double amount, String remarks) {
        EWallet wallet = ewalletRepository.findById(walletId)
                .orElse(new EWallet(walletId, 0.0, new java.util.ArrayList<>()));
        wallet.setCurrentBalance(wallet.getCurrentBalance() + amount);
        Statement stmt = new Statement("DEPOSIT", amount, LocalDateTime.now(), null, remarks, wallet);
        wallet.getStatements().add(stmt);
        return ewalletRepository.save(wallet);
    }

    @Override
    public EWallet payMoney(int walletId, double amount, int orderId) {
        EWallet wallet = ewalletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        if (wallet.getCurrentBalance() < amount) {
            throw new RuntimeException("Insufficient wallet balance");
        }
        wallet.setCurrentBalance(wallet.getCurrentBalance() - amount);
        Statement stmt = new Statement("PAYMENT", amount, LocalDateTime.now(), orderId, "Order Payment", wallet);
        wallet.getStatements().add(stmt);
        return ewalletRepository.save(wallet);
    }
}
