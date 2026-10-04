package com.foodordering.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.foodordering.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u JOIN UserRole ur ON ur.user = u WHERE ur.role.name = :roleName")
    List<User> findAllByRoleName(@Param("roleName") String roleName);

    @Query("SELECT u.id AS id, u.fullName AS nama, u.username AS username, u.email AS email, "
            + "u.phone AS telepon, u.createdAt AS tglDaftar, "
            + "COUNT(o.id) AS jumlahPesanan, COALESCE(SUM(o.totalAmount), 0) AS totalBelanja "
            + "FROM User u LEFT JOIN u.userRoles ur LEFT JOIN u.orders o "
            + "WHERE ur.role.name = 'ROLE_CUSTOMER' "
            + "GROUP BY u.id ORDER BY tglDaftar DESC")
    List<Object[]> findAllCustomersWithOrderCount();
}