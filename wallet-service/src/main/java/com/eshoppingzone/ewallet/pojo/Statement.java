package com.eshoppingzone.ewallet.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "statements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer statementId;

    private String transactionType; // DEPOSIT, WITHDRAW, PAYMENT
    private Double amount;
    private LocalDateTime dateTime;
    private Integer orderId;
    private String transactionRemarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    @JsonIgnore
    private EWallet ewallet;

    public Statement(String transactionType, Double amount, LocalDateTime dateTime, Integer orderId, String transactionRemarks, EWallet ewallet) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.dateTime = dateTime;
        this.orderId = orderId;
        this.transactionRemarks = transactionRemarks;
        this.ewallet = ewallet;
    }
}
