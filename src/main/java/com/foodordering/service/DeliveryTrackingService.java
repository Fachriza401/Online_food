package com.foodordering.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.foodordering.model.Order;
import com.foodordering.model.OrderStatus;

/**
 * Simulasi posisi kurir untuk demo. Saat pesanan berstatus SEDANG DIANTAR,
 * posisi bergerak linear dari warung menuju area pelanggan mengikuti waktu.
 */
@Service
public class DeliveryTrackingService {

    @Value("${app.restaurant-lat:-6.2088}")
    private double restaurantLat;

    @Value("${app.restaurant-lng:106.8456}")
    private double restaurantLng;

    @Value("${app.customer-lat:-6.1769}")
    private double customerLat;

    @Value("${app.customer-lng:106.8629}")
    private double customerLng;

    @Value("${app.delivery-minutes:14}")
    private long deliveryMinutes;

    public Map<String, Object> track(Order order) {
        Map<String, Object> result = new LinkedHashMap<>();
        OrderStatus status = order.getStatus();
        result.put("status", status.name());
        result.put("label", status.getLabel());
        result.put("inProgress", status == OrderStatus.DELIVERING);
        result.put("done", status == OrderStatus.COMPLETED);
        result.put("active", status == OrderStatus.DELIVERING || status == OrderStatus.COMPLETED);

        if (status == OrderStatus.COMPLETED) {
            result.put("lat", customerLat);
            result.put("lng", customerLng);
            result.put("etaMinutes", 0);
            result.put("progress", 1.0);
            result.put("message", "Pesanan sudah sampai tujuannya.");
            return result;
        }
        if (status != OrderStatus.DELIVERING) {
            result.put("etaMinutes", -1);
            result.put("progress", 0.0);
            result.put("message", "Pengantar dijadwalkan setelah pesanan dikonfirmasi dan dimasak.");
            return result;
        }

        LocalDateTime start = order.getProcessedAt() != null ? order.getProcessedAt() : order.getCreatedAt();
        long totalSeconds = deliveryMinutes * 60;
        long elapsedSeconds = Duration.between(start, LocalDateTime.now()).getSeconds();
        double fraction = Math.min(1.0, Math.max(0.0, (double) elapsedSeconds / totalSeconds));

        double lat = restaurantLat + (customerLat - restaurantLat) * fraction;
        double lng = restaurantLng + (customerLng - restaurantLng) * fraction;
        long remainingMinutes = (long) Math.ceil((totalSeconds - elapsedSeconds) / 60.0);
        if (remainingMinutes < 0) {
            remainingMinutes = 0;
        }

        result.put("lat", round5(lat));
        result.put("lng", round5(lng));
        result.put("etaMinutes", remainingMinutes);
        result.put("progress", Math.round(fraction * 100.0) / 100.0);
        result.put("message", fraction >= 1.0 ? "Kurir sudah tiba di tujuan." : "Kurir sedang menuju lokasi Anda.");
        return result;
    }

    private double round5(double value) {
        return Math.round(value * 100000.0) / 100000.0;
    }
}