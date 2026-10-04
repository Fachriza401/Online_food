package com.foodordering.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.foodordering.model.Category;
import com.foodordering.model.Food;
import com.foodordering.model.Order;
import com.foodordering.model.OrderItem;
import com.foodordering.model.OrderStatus;
import com.foodordering.model.Payment;
import com.foodordering.model.PaymentStatus;
import com.foodordering.model.Review;
import com.foodordering.model.Role;
import com.foodordering.model.User;
import com.foodordering.model.UserRole;
import com.foodordering.repository.CategoryRepository;
import com.foodordering.repository.FoodRepository;
import com.foodordering.repository.OrderRepository;
import com.foodordering.repository.PaymentRepository;
import com.foodordering.repository.ReviewRepository;
import com.foodordering.repository.RoleRepository;
import com.foodordering.repository.UserRepository;
import com.foodordering.repository.UserRoleRepository;
import com.foodordering.service.FileStorageService;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final CategoryRepository categoryRepository;
    private final FoodRepository foodRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
                           UserRoleRepository userRoleRepository, CategoryRepository categoryRepository,
                           FoodRepository foodRepository, OrderRepository orderRepository,
                           PaymentRepository paymentRepository, ReviewRepository reviewRepository,
                           PasswordEncoder passwordEncoder, FileStorageService fileStorageService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.categoryRepository = categoryRepository;
        this.foodRepository = foodRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRoles();
        User admin = seedAdmin();
        User customer = seedCustomer();
        seedCatalog();
        syncCategoryImages();
        syncFoodImages();
        seedSampleOrders(customer);
        seedSampleReviews(customer);
        log.info("Seeding selesai. Admin: admin/admin123 | Customer demo: customer/customer123 | Ulasan demo: rina & bayu (customer123)");
    }

    private void seedRoles() {
        roleRepository.saveIfAbsent("ROLE_ADMIN");
        roleRepository.saveIfAbsent("ROLE_CUSTOMER");
    }

    private User seedAdmin() {
        return userRepository.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin");
            u.setEmail("admin@tokokita.com");
            u.setFullName("Administrator");
            u.setPhone("081234567890");
            u.setAddress("Jl. Sudirman No. 1, Jakarta");
            u.setEnabled(true);
            u.setPasswordHash(passwordEncoder.encode("admin123"));
            User saved = userRepository.save(u);
            userRoleRepository.save(new UserRole(saved, roleRepository.findByName("ROLE_ADMIN").orElseThrow()));
            return saved;
        });
    }

    private User seedCustomer() {
        return userRepository.findByUsername("customer").orElseGet(() -> {
            User u = new User();
            u.setUsername("customer");
            u.setEmail("customer@tokokita.com");
            u.setFullName("Pelanggan Demo");
            u.setPhone("085712345678");
            u.setAddress("Jl. Mangga Dua No. 10, Jakarta");
            u.setEnabled(true);
            u.setPasswordHash(passwordEncoder.encode("customer123"));
            User saved = userRepository.save(u);
            userRoleRepository.save(new UserRole(saved, roleRepository.findByName("ROLE_CUSTOMER").orElseThrow()));
            return saved;
        });
    }

    private void seedCatalog() {
        if (foodRepository.count() > 0) {
            return;
        }
        Category nasi = category("Nasi", "Hidangan berbahan dasar nasi", "/images/foods/nasi-goreng-spesial.png");
        Category ayam = category("Ayam", "Olahan ayam goreng dan bakar", "/images/foods/ayam-bakar.webp");
        Category sate = category("Sate & Bakar", "Sate dan makanan bakar", "/images/foods/sate-ayam.png");
        Category lauk = category("Lauk Sampingan", "Pelengkap hidangan utama", "/images/foods/telur-balado.png");
        Category minuman = category("Minuman", "Minuman segar", "/images/foods/es-teh-manis.png");

        food(nasi, "Nasi Goreng Spesial", "Nasi goreng dengan telur, ayam suwir, udang dan kerupuk.", 28000, "nasi-goreng-spesial.png", true);
        food(nasi, "Nasi Goreng Ayam", "Nasi goreng dengan potongan ayam dan sayuran segar.", 24000, "nasi-goreng-ayam.png", true);
        food(nasi, "Nasi Uduk Komplit", "Nasi uduk dengan ayam suwir, tempe, bihun dan sambal.", 23000, "nasi-uduk-komplit.png", true);

        food(ayam, "Ayam Bakar", "Ayam bakar bumbu kecap, disajikan dengan sambal dan lalapan.", 26000, "ayam-bakar.webp", true);
        food(ayam, "Ayam Goreng Krispi", "Ayam goreng tepung krispi dengan sambal terasi.", 25000, "ayam-goreng-krispi.png", true);
        food(ayam, "Ayam Geprek", "Ayam geprek pedas level sesuai selera dengan nasi.", 22000, "ayam-geprek.webp", true);

        food(sate, "Sate Ayam (10 tusuk)", "Sate ayam bumbu kacang dengan lontong dan acar.", 30000, "sate-ayam.png", true);
        food(sate, "Sate Kambing (10 tusuk)", "Sate kambing muda dengan sambal kecap.", 40000, "sate-kambing.png", true);
        food(sate, "Ikan Bakar", "Ikan bakar dengan sambal dabu-dabu dan lalapan.", 32000, "ikan-bakar.png", false);

        food(lauk, "Telur Balado", "Telur ceplok dengan sambal balado khas Padang.", 12000, "telur-balado.png", true);
        food(lauk, "Tempe & Tahu Goreng", "Tempe dan tahu goreng garing dengan sambal.", 10000, "tempe-tahu.png", true);

        food(minuman, "Es Teh Manis", "Teh manis dingin yang menyegarkan.", 6000, "es-teh-manis.png", true);
        food(minuman, "Es Jeruk Peras", "Jeruk peras segar dengan es batu.", 9000, "es-jeruk-peras.png", true);
        food(minuman, "Jus Alpukat", "Jus alpukat kental dengan susu cokelat.", 15000, "jus-alpukat.png", true);
    }

    private static final Map<String, String> BUNDLED_IMAGES = Map.ofEntries(
            Map.entry("Nasi Goreng Spesial", "/images/foods/nasi-goreng-spesial.png"),
            Map.entry("Nasi Goreng Ayam", "/images/foods/nasi-goreng-ayam.png"),
            Map.entry("Nasi Uduk Komplit", "/images/foods/nasi-uduk-komplit.png"),
            Map.entry("Ayam Bakar", "/images/foods/ayam-bakar.webp"),
            Map.entry("Ayam Goreng Krispi", "/images/foods/ayam-goreng-krispi.png"),
            Map.entry("Ayam Geprek", "/images/foods/ayam-geprek.webp"),
            Map.entry("Sate Ayam (10 tusuk)", "/images/foods/sate-ayam.png"),
            Map.entry("Sate Kambing (10 tusuk)", "/images/foods/sate-kambing.png"),
            Map.entry("Ikan Bakar", "/images/foods/ikan-bakar.png"),
            Map.entry("Telur Balado", "/images/foods/telur-balado.png"),
            Map.entry("Tempe & Tahu Goreng", "/images/foods/tempe-tahu.png"),
            Map.entry("Es Teh Manis", "/images/foods/es-teh-manis.png"),
            Map.entry("Es Jeruk Peras", "/images/foods/es-jeruk-peras.png"),
            Map.entry("Jus Alpukat", "/images/foods/jus-alpukat.png"));

    private static final Map<String, String> BUNDLED_CATEGORY_IMAGES = Map.of(
            "Nasi", "/images/foods/nasi-goreng-spesial.png",
            "Ayam", "/images/foods/ayam-bakar.webp",
            "Sate & Bakar", "/images/foods/sate-ayam.png",
            "Lauk Sampingan", "/images/foods/telur-balado.png",
            "Minuman", "/images/foods/es-teh-manis.png");

    private void syncCategoryImages() {
        for (Category category : categoryRepository.findAll()) {
            String target = BUNDLED_CATEGORY_IMAGES.get(category.getName());
            String current = category.getImageUrl();
            if (target == null || target.equals(current)) {
                continue;
            }
            if (current == null || current.isBlank()
                    || current.startsWith("/images/categories/")
                    || current.toLowerCase().endsWith(".svg")) {
                category.setImageUrl(target);
                categoryRepository.save(category);
                log.info("Foto kategori diperbarui: {} -> {}", category.getName(), target);
            }
        }
    }

    /** Selaraskan foto bawaan dengan aset terbaru, tanpa menyentuh foto hasil upload admin. */
    private void syncFoodImages() {
        for (Food food : foodRepository.findAll()) {
            String target = BUNDLED_IMAGES.get(food.getName());
            String current = food.getImageUrl();
            if (target == null || target.equals(current)) {
                continue;
            }
            boolean bundled = current == null || current.isBlank() || current.startsWith("/images/foods/");
            boolean brokenUpload = current.startsWith("/uploads/") && !fileStorageService.isUsableUpload(current);
            if (bundled || brokenUpload) {
                food.setImageUrl(target);
                foodRepository.save(food);
                log.info("Foto menu diperbarui: {} -> {}", food.getName(), target);
            }
        }
    }

    private void seedSampleOrders(User customer) {
        if (orderRepository.count() > 0) {
            return;
        }
        List<Food> foods = foodRepository.findAll();

        createOrder(customer, foods, 0, 1, 12, OrderStatus.COMPLETED, 3,
                "Jl. Mangga Dua No. 10, Jakarta", "Pesan porsi ekstra pedas ya.");
        createOrder(customer, foods, 4, 9, 13, OrderStatus.DELIVERING, 1,
                "Jl. Mangga Dua No. 10, Jakarta", "");
        createOrder(customer, foods, 3, 9, 10, OrderStatus.PENDING, 0,
                "Jl. Mangga Dua No. 10, Jakarta", "");
    }

    private void seedSampleReviews(User customer) {
        if (reviewRepository.count() > 0) {
            return;
        }
        User rina = seedReviewer("rina", "Rina Puspita", "rina@tokokita.com");
        User bayu = seedReviewer("bayu", "Bayu Pratama", "bayu@tokokita.com");

        review(customer, "Nasi Goreng Spesial", 5, "Bumbunya gurih dan porsinya besar. Sampai rumah masih hangat!");
        review(customer, "Nasi Goreng Ayam", 4, "Enak, tapi kerupuknya kurang renyah.");
        review(customer, "Es Jeruk Peras", 5, "Segar banget, es cubanya banyak. Pasti langganan!");
        review(rina, "Ayam Bakar", 5, "Sambalnya pedas manis, ayamnya juicy. Recommended!");
        review(rina, "Es Teh Manis", 4, "Simple tapi enak, harganya juga bersahabat.");
        review(bayu, "Ayam Goreng Krispi", 4, "Krispinya masih renyah walau sudah diantar 20 menit.");
        review(bayu, "Nasi Goreng Spesial", 4, "Porsinya membantu banget untuk makan siang.");
        review(bayu, "Es Jeruk Peras", 4, "Rasanya segar, harganya sesuai.");
    }

    private User seedReviewer(String username, String fullName, String email) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setEmail(email);
            u.setFullName(fullName);
            u.setPhone("0813" + (10000000L + Math.abs(fullName.hashCode()) % 89999999L));
            u.setAddress("Jl. Contoh No. 1, Jakarta");
            u.setEnabled(true);
            u.setPasswordHash(passwordEncoder.encode("customer123"));
            User saved = userRepository.save(u);
            userRoleRepository.save(new UserRole(saved, roleRepository.findByName("ROLE_CUSTOMER").orElseThrow()));
            return saved;
        });
    }

    private void review(User user, String foodName, int rating, String comment) {
        foodRepository.findByName(foodName).ifPresent(food -> {
            Review r = new Review(food, user, rating, comment);
            r.setCreatedAt(LocalDateTime.now().minusDays(1 + Math.abs(comment.hashCode()) % 20));
            reviewRepository.save(r);
        });
    }

    private void createOrder(User user, List<Food> foods, int idx1, int idx2, int idx3,
                             OrderStatus status, int daysAgo, String address, String note) {
        Food f1 = foods.get(idx1);
        Food f2 = foods.get(idx2);
        Food f3 = idx3 < foods.size() ? foods.get(idx3) : foodRepository.findAll().get(0);

        Order order = new Order();
        order.setOrderCode("ORD-DEMO-" + System.currentTimeMillis());
        order.setUser(user);
        order.setStatus(status);
        order.setCustomerName(user.getFullName());
        order.setCustomerPhone(user.getPhone());
        order.setCustomerAddress(address);
        order.setNote(note);
        order.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        order.setProcessedAt(status == OrderStatus.PENDING ? null : LocalDateTime.now().minusDays(daysAgo));

        BigDecimal subtotal = BigDecimal.ZERO;
        order.getItems().add(new OrderItem(order, f1, f1.getName(), f1.getPrice(), 2));
        order.getItems().add(new OrderItem(order, f2, f2.getName(), f2.getPrice(), 1));
        order.getItems().add(new OrderItem(order, f3, f3.getName(), f3.getPrice(), 1));
        for (OrderItem item : order.getItems()) {
            subtotal = subtotal.add(item.getSubtotal());
        }
        order.setSubtotal(subtotal);
        order.setShippingFee(BigDecimal.valueOf(10000L * (status == OrderStatus.PENDING ? 1 : 0)));
        order.setTotalAmount(subtotal.add(order.getShippingFee()));

        Order saved = orderRepository.save(order);

        Payment payment = new Payment(saved, saved.getTotalAmount());
        payment.setStatus(status == OrderStatus.COMPLETED ? PaymentStatus.PAID : PaymentStatus.PENDING);
        payment.setPaidAt(status == OrderStatus.COMPLETED ? saved.getCreatedAt() : null);
        paymentRepository.save(payment);
    }

    private Category category(String name, String description, String image) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> {
                    Category c = new Category(name, description);
                    c.setImageUrl(image);
                    return categoryRepository.save(c);
                });
    }

    private void food(Category category, String name, String description,
                      long price, String imageFile, boolean available) {
        Food f = new Food();
        f.setName(name);
        f.setDescription(description);
        f.setPrice(new BigDecimal(price));
        f.setImageUrl("/images/foods/" + imageFile);
        f.setAvailable(available);
        f.setCategory(category);
        foodRepository.save(f);
    }
}