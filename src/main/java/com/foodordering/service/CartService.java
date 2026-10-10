package com.foodordering.service;

import java.math.BigDecimal;

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

    /** Batas jumlah satu menu dalam keranjang, mencegah overflow integer pada subtotal. */
    public static final int MAX_QUANTITY_PER_ITEM = 99;

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

    @Transactional(readOnly = true)
    public BigDecimal getCartSubtotal(User user) {
        Cart cart = cartRepository.findByUser(user).orElse(null);
        if (cart == null) {
            return BigDecimal.ZERO;
        }
        return cart.getSubtotal();
    }

    @Transactional
    public Cart addItem(User user, Long foodId, int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY_PER_ITEM) {
            throw new BadRequestException("Jumlah pesanan per menu antara 1 dan " + MAX_QUANTITY_PER_ITEM);
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
            long total = (long) item.getQuantity() + quantity;
            if (total > MAX_QUANTITY_PER_ITEM) {
                throw new BadRequestException("Jumlah \"" + food.getName() + "\" dalam keranjang maksimal "
                        + MAX_QUANTITY_PER_ITEM);
            }
            item.setQuantity((int) total);
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
        } else if (quantity > MAX_QUANTITY_PER_ITEM) {
            throw new BadRequestException("Jumlah per menu maksimal " + MAX_QUANTITY_PER_ITEM);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
        return cartRepository.save(cart);
    }

    /**
     * Muat keranjang dengan kunci baris (SELECT ... FOR UPDATE) selama transaksi berjalan.
     * Request checkout paralel akan saling menunggu sehingga pesanan ganda tidak terjadi.
     */
    @Transactional
    public Cart lockCart(User user) {
        return cartRepository.findByUserForUpdate(user).orElse(null);
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