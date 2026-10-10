package com.foodordering.util;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilitas untuk mengembalikan user ke halaman asal setelah aksi POST.
 * Header {@code Referer} selalu berupa URL absolut, jadi path-nya harus diurai
 * terlebih dahulu dan hanya dipakai bila host-nya sama dengan host request
 * (mencegah open redirect).
 */
public final class BackUrl {

    private BackUrl() {
    }

    /**
     * @return path (path + query) halaman asal dari header Referer,
     *         atau {@code fallback} bila Referer tidak ada / tidak aman dipakai.
     */
    public static String path(HttpServletRequest request, String fallback) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return fallback;
        }
        try {
            URI uri = new URI(referer);
            String host = uri.getHost();
            if (host == null || !host.equalsIgnoreCase(request.getServerName())) {
                return fallback;
            }
            if (uri.getPort() > 0 && uri.getPort() != request.getServerPort()) {
                return fallback;
            }
            String path = uri.getRawPath();
            if (path == null || path.isEmpty() || !path.startsWith("/")) {
                return fallback;
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (URISyntaxException e) {
            return fallback;
        }
    }
}
