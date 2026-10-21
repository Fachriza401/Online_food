package com.foodordering.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodordering.model.RestaurantSetting;
import com.foodordering.repository.RestaurantSettingRepository;

@Service
public class RestaurantSettingService {

    private final RestaurantSettingRepository repository;
    private final FileStorageService fileStorageService;

    public RestaurantSettingService(RestaurantSettingRepository repository,
                                    FileStorageService fileStorageService) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
    }

    /** Mengembalikan baris singleton, membuatnya dengan nilai bawaan bila belum ada. */
    @Transactional
    public RestaurantSetting get() {
        return repository.findById(RestaurantSetting.SINGLETON_ID).orElseGet(() -> {
            RestaurantSetting setting = new RestaurantSetting();
            setting.setAddress("JL. HM. JONINO.61A, KEL. TELADAN TIMUR, KEC. MEDAN AREA, "
                    + "KOTA MEDAN, SUMATERA UTARA KODE POS 20217");
            setting.setPhone("0853-6315-8834");
            setting.setEmail("halo@rmkakros.example");
            setting.setDescription("Website pemesanan makanan online. Pesan makanan favoritmu dengan mudah, "
                    + "cepat, dan aman — di antar sampai depan pintu.");
            return repository.save(setting);
        });
    }

    @Transactional
    public RestaurantSetting update(RestaurantSetting incoming, String newLogoUrl) {
        RestaurantSetting setting = get();
        if (incoming.getRestaurantName() != null && !incoming.getRestaurantName().isBlank()) {
            setting.setRestaurantName(incoming.getRestaurantName().trim());
        }
        setting.setAddress(trimOrNull(incoming.getAddress()));
        setting.setPhone(trimOrNull(incoming.getPhone()));
        setting.setEmail(trimOrNull(incoming.getEmail()));
        setting.setDescription(trimOrNull(incoming.getDescription()));
        if (newLogoUrl != null && !newLogoUrl.isBlank()) {
            fileStorageService.deleteIfUploaded(setting.getLogoUrl());
            setting.setLogoUrl(newLogoUrl);
        }
        return repository.save(setting);
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
