package com.eshoppingzone.ewallet.resource;

import com.eshoppingzone.ewallet.pojo.EWallet;
import com.eshoppingzone.ewallet.pojo.Statement;
import com.eshoppingzone.ewallet.service.EwalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/wallets")
public class WalletResource {

    @Autowired
    private EwalletService ewalletService;

    @PostMapping("/{walletId}/create")
    public ResponseEntity<Void> addNewWallet(@PathVariable("walletId") int walletId) {
        EWallet wallet = new EWallet(walletId, 0.0, new java.util.ArrayList<>());
        ewalletService.addWallet(wallet);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<EWallet>> getAllWallets() {
        return ResponseEntity.ok(ewalletService.getWallets());
    }

    @GetMapping("/{walletId}/balance")
    public ResponseEntity<Double> getById(@PathVariable("walletId") int walletId) {
        return ResponseEntity.ok(ewalletService.getById(walletId));
    }

    @PostMapping("/{walletId}/deposit")
    public ResponseEntity<EWallet> addMoney(@PathVariable("walletId") int walletId, 
                                            @RequestParam("amount") double amount) {
        return ResponseEntity.ok(ewalletService.depositMoney(walletId, amount, "Deposit / Recharge"));
    }

    @PostMapping("/{walletId}/pay")
    public ResponseEntity<EWallet> payMoney(@PathVariable("walletId") int walletId, 
                                            @RequestParam("amount") double amount,
                                            @RequestParam("orderId") int orderId) {
        return ResponseEntity.ok(ewalletService.payMoney(walletId, amount, orderId));
    }

    @GetMapping("/{walletId}/statements")
    public ResponseEntity<List<Statement>> getStatementsById(@PathVariable("walletId") int walletId) {
        return ResponseEntity.ok(ewalletService.getStatementsById(walletId));
    }

    @GetMapping("/statements")
    public ResponseEntity<List<Statement>> getStatements() {
        return ResponseEntity.ok(ewalletService.getStatements());
    }

    @DeleteMapping("/{walletId}")
    public ResponseEntity<Void> deleteById(@PathVariable("walletId") int walletId) {
        ewalletService.deleteById(walletId);
        return ResponseEntity.ok().build();
    }
}
