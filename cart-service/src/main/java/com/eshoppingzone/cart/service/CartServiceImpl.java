package com.eshoppingzone.cart.service;

import com.eshoppingzone.cart.entity.Cart;
import com.eshoppingzone.cart.entity.Items;
import com.eshoppingzone.cart.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Override
    public Cart getCartById(int cartId) {
        Cart cart = cartRepository.findByCartId(cartId);
        if (cart == null) {
            cart = new Cart(cartId, 0.0, new ArrayList<>());
            cartRepository.save(cart);
        }
        return cart;
    }

    @Override
    public Cart updateCart(Cart cart) {
        if (cart == null) return null;

        Cart existing = cartRepository.findById(cart.getCartId()).orElse(null);
        if (existing == null) {
            existing = cartRepository.findByCartId(cart.getCartId());
        }

        if (existing == null) {
            existing = new Cart(cart.getCartId(), 0.0, new ArrayList<>());
            existing = cartRepository.save(existing);
        }

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            existing.getItems().clear();
        } else {
            Set<Items> retainedExisting = new HashSet<>();

            for (Items incoming : cart.getItems()) {
                if (incoming == null) continue;

                // Match in existing by database itemId, or by productId, or by productName (case-insensitive)
                Items matched = null;
                for (Items ex : existing.getItems()) {
                    if (!retainedExisting.contains(ex)) {
                        if (incoming.getItemId() > 0 && incoming.getItemId() == ex.getItemId()) {
                            matched = ex;
                            break;
                        }
                        if (incoming.getProductId() > 0 && incoming.getProductId() == ex.getProductId()) {
                            matched = ex;
                            break;
                        }
                        if (incoming.getProductName() != null && ex.getProductName() != null
                                && incoming.getProductName().trim().equalsIgnoreCase(ex.getProductName().trim())) {
                            matched = ex;
                            break;
                        }
                    }
                }

                if (matched != null) {
                    matched.setPrice(incoming.getPrice());
                    matched.setQuantity(incoming.getQuantity());
                    if (incoming.getProductName() != null) {
                        matched.setProductName(incoming.getProductName().trim());
                    }
                    if (incoming.getProductId() > 0) {
                        matched.setProductId(incoming.getProductId());
                    }
                    retainedExisting.add(matched);
                } else {
                    // New item: itemId must be 0 so MySQL assigns auto-increment ID
                    int prodId = incoming.getProductId() > 0 ? incoming.getProductId() : incoming.getItemId();
                    Items newItem = new Items();
                    newItem.setItemId(0);
                    newItem.setProductId(prodId);
                    newItem.setProductName(incoming.getProductName() != null ? incoming.getProductName().trim() : "");
                    newItem.setPrice(incoming.getPrice());
                    newItem.setQuantity(incoming.getQuantity() > 0 ? incoming.getQuantity() : 1);
                    existing.getItems().add(newItem);
                    retainedExisting.add(newItem);
                }
            }

            // Remove any items that are no longer present in incoming cart
            existing.getItems().removeIf(ex -> !retainedExisting.contains(ex));
        }

        existing.setTotalPrice(cartTotal(existing));
        return cartRepository.save(existing);
    }

    @Override
    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    @Override
    public double cartTotal(Cart cart) {
        if (cart == null || cart.getItems() == null) return 0.0;
        return cart.getItems().stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();
    }

    @Override
    public Cart addCart(Cart cart) {
        cart.setTotalPrice(cartTotal(cart));
        return cartRepository.save(cart);
    }
}
