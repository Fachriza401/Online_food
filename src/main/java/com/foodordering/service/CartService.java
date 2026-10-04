package com.foodordering.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodordering.exception.BadRequestException;
import com.foodordering.model.Cart;
import com.foodordering.model.CartItem;
import com.foodordering.model.Food;
import com.foodordering.model.User;
import com.foodordering.repository.CartItemRepository;
import com.foodordering.repository.CartRepository;
import com.foodordering.repository.FoodRepository;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodRepository foodRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       FoodRepository foodRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.foodRepository = foodRepository;
    }

    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart cart = new Cart(user);
            return cartRepository.save(cart);
        });
    }

    @Transactional(readOnly = true)
    public Cart getCart(User user) {
        return cartRepository.findByUser(user).orElse(null);
    }

    @Transactional(readOnly = true)
    public int getCartItemCount(User user) {
        Cart cart = cartRepository.findByUser(user).orElse(null);
        if (cart == null) {
            return 0;
        }
        return cartItemRepository.findAllByCartIdOrderByIdAsc(cart.getId()).stream()
                .mapToInt(CartItem::getQuantity).sum();
    }

    @Transactional
    public Cart addItem(User user, Long foodId, int quantity) {
        if (quantity < 1) {
            throw new BadRequestException("Jumlah minimal 1");
        }
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new BadRequestException("Menu tidak ditemukan"));
        if (!food.isAvailable()) {
            throw new BadRequestException("Menu \"" + food.getName() + "\" sedang tidak tersedia");
        }
        Cart cart = getOrCreateCart(user);
        CartItem item = cartItemRepository.findByCartIdAndFoodId(cart.getId(), foodId).orElse(null);
        if (item == null) {
            item = new CartItem(cart, food, quantity);
            cartItemRepository.save(item);
        } else {
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        }
        return cart;
    }

    @Transactional
    public Cart updateQuantity(User user, Long cartItemId, int quantity) {
        Cart cart = getCart(user);
        if (cart == null) {
            throw new BadRequestException("Keranjang kosong");
        }
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BadRequestException("Item keranjang tidak ditemukan"));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Akses item keranjang ditolak");
        }
        if (quantity < 1) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItem(User user, Long cartItemId) {
        Cart cart = getCart(user);
        if (cart == null) {
            throw new BadRequestException("Keranjang kosong");
        }
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BadRequestException("Item keranjang tidak ditemukan"));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Akses item keranjang ditolak");
        }
        cartItemRepository.delete(item);
        return cartRepository.save(cart);
    }

    @Transactional
    public void clear(User user) {
        Cart cart = getCart(user);
        if (cart != null) {
            cartItemRepository.deleteByCartId(cart.getId());
        }
    }
}