package com.eshoppingzone.cart.service;

import com.eshoppingzone.cart.entity.Cart;
import java.util.List;

public interface CartService {
    Cart getCartById(int cartId);
    Cart updateCart(Cart cart);
    List<Cart> getAllCarts();
    double cartTotal(Cart cart);
    Cart addCart(Cart cart);
}
