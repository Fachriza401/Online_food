package com.foodordering.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.foodordering.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndFoodId(Long cartId, Long foodId);

    List<CartItem> findAllByCartIdOrderByIdAsc(Long cartId);

    Long deleteByFoodId(Long foodId);

    /**
     * Hapus seluruh item milik satu keranjang dengan satu perintah bulk delete.
     * Bulk delete langsung dieksekusi di database sehingga isi keranjang benar-benar
     * kosong setelah checkout (tidak meninggalkan entitas basi di persistence context).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CartItem ci where ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);
}