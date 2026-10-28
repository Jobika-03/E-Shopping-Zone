package com.eshoppingzone.ewallet.pojo;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @ToString
public class EWallet {

    @Id
    private int walletId;

    private Double currentBalance = 0.0;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "ewallet", fetch = FetchType.EAGER)
    private List<Statement> statements = new ArrayList<>();
}
