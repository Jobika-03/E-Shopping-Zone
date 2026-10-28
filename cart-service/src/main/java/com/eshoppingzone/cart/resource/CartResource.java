package com.eshoppingzone.cart.resource;

import com.eshoppingzone.cart.entity.Cart;
import com.eshoppingzone.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/carts")
public class CartResource {

    @Autowired
    private CartService cartService;

    @GetMapping("/{cartId}")
    public ResponseEntity<Cart> getCart(@PathVariable("cartId") int cartId) {
        return ResponseEntity.ok(cartService.getCartById(cartId));
    }

    @GetMapping
    public ResponseEntity<List<Cart>> getAllCarts() {
        return ResponseEntity.ok(cartService.getAllCarts());
    }

    @PostMapping
    public ResponseEntity<Void> addCart(@RequestBody Cart cart) {
        cartService.addCart(cart);
        return ResponseEntity.ok().build();
    }

    @PutMapping
    public ResponseEntity<Cart> updateCart(@RequestBody Cart cart) {
        return ResponseEntity.ok(cartService.updateCart(cart));
    }
}
