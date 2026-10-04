package com.foodordering.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodordering.dto.CheckoutRequest;
import com.foodordering.exception.BadRequestException;
import com.foodordering.model.Cart;
import com.foodordering.model.CartItem;
import com.foodordering.model.Order;
import com.foodordering.model.OrderItem;
import com.foodordering.model.OrderStatus;
import com.foodordering.model.Payment;
import com.foodordering.model.User;
import com.foodordering.repository.CartItemRepository;
import com.foodordering.repository.OrderRepository;
import com.foodordering.repository.PaymentRepository;

import java.math.BigDecimal;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentRepository paymentRepository;
    private final CartService cartService;
    private final NotificationService notificationService;

    @Value("${app.shipping-fee:10000}")
    private long shippingFee;

    @Value("${app.shipping-free-threshold:100000}")
    private long freeShippingThreshold;

    public OrderService(OrderRepository orderRepository, CartItemRepository cartItemRepository,
                        PaymentRepository paymentRepository, CartService cartService,
                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.paymentRepository = paymentRepository;
        this.cartService = cartService;
        this.notificationService = notificationService;
    }

    /**
     * Membuat pesanan dari keranjang. Harga dihitung ulang dari database
     * (bukan dari input klien) dan disimpan sebagai snapshot di OrderItem.
     */
    @Transactional
    public Order placeOrder(User user, CheckoutRequest request) {
        Cart cart = cartService.getCart(user);
        if (cart == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Keranjang masih kosong");
        }

        for (CartItem item : cart.getItems()) {
            if (!item.getFood().isAvailable()) {
                throw new BadRequestException(
                        "Menu \"" + item.getFood().getName() + "\" sedang tidak tersedia, hapus dari keranjang");
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderCode(generateOrderCode());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerAddress(request.getCustomerAddress());
        order.setNote(request.getNote());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            BigDecimal unitPrice = item.getFood().getPrice();
            OrderItem orderItem = new OrderItem(order, item.getFood(), item.getFood().getName(),
                    unitPrice, item.getQuantity());
            order.getItems().add(orderItem);
            subtotal = subtotal.add(orderItem.getSubtotal());
        }

        BigDecimal shipping = computeShipping(subtotal);

        order.setSubtotal(subtotal);
        order.setShippingFee(shipping);
        order.setTotalAmount(subtotal.add(shipping));

        Order saved = orderRepository.save(order);

        Payment payment = new Payment(saved, saved.getTotalAmount());
        paymentRepository.save(payment);

        cartItemRepository.deleteByCartId(cart.getId());

        notificationService.notifyAdmins(
                "Pesanan baru: " + saved.getOrderCode() + " oleh " + user.getFullName(),
                "/admin/orders/" + saved.getId());

        return saved;
    }

    @Transactional
    public Order cancelByCustomer(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Pesanan tidak ditemukan"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Akses pesanan ditolak");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Pesanan hanya dapat dibatalkan saat status " +
                    OrderStatus.PENDING.getLabel());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setProcessedAt(LocalDateTime.now());
        paymentRepository.findByOrderId(orderId).ifPresent(p -> {
            p.setStatus(com.foodordering.model.PaymentStatus.CANCELLED);
            paymentRepository.save(p);
        });
        Order saved = orderRepository.save(order);
        notificationService.notifyAdmins(
                "Pesanan " + saved.getOrderCode() + " dibatalkan oleh pelanggan",
                "/admin/orders/" + saved.getId());
        return saved;
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Pesanan tidak ditemukan"));
        order.setStatus(status);
        if (status == OrderStatus.COMPLETED) {
            paymentRepository.findByOrderId(orderId).ifPresent(p -> p.setStatus(com.foodordering.model.PaymentStatus.PAID));
        }
        if (status == OrderStatus.CANCELLED) {
            paymentRepository.findByOrderId(orderId).ifPresent(p -> p.setStatus(com.foodordering.model.PaymentStatus.CANCELLED));
        }
        order.setProcessedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        notificationService.notifyUser(order.getUser(),
                "Pesanan " + saved.getOrderCode() + ": " + status.getLabel(),
                "/orders/" + saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Order findById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Pesanan tidak ditemukan"));
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> findAllPaged(int page, int size) {
        return orderRepository.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> findByStatusPaged(OrderStatus status, int page, int size) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status,
                org.springframework.data.domain.PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Order findOwnedOrder(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Pesanan tidak ditemukan"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Akses pesanan ditolak");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> findByUser(User user, int page, int size) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId(),
                org.springframework.data.domain.PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Payment findPayment(Order order) {
        return paymentRepository.findByOrderId(order.getId()).orElse(null);
    }

    public BigDecimal computeShipping(BigDecimal subtotal) {
        return subtotal.compareTo(BigDecimal.valueOf(freeShippingThreshold)) >= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(shippingFee);
    }

    public static final java.util.List<OrderStatus> FLOW = java.util.List.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PREPARING,
            OrderStatus.DELIVERING, OrderStatus.COMPLETED);

    public int statusIndex(OrderStatus status) {
        return FLOW.indexOf(status);
    }

    public java.util.List<OrderStatus> statusFlow() {
        return FLOW;
    }

    private String generateOrderCode() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "ORD-" + ts + "-" + rand;
    }
}