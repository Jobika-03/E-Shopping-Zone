package com.eshoppingzone.cart.repository;

import com.eshoppingzone.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Integer> {
    Cart findByCartId(int cartId);
}
