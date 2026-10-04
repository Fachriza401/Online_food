package com.foodordering.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Mengatur Cache-Control per jenis aset agar halaman tidak berat saat dibuka.
 *
 * <p>Spring Security secara bawaan menulis {@code no-store} pada semua respons,
 * sehingga aset statis selalu diunduh ulang. Writer tersebut dimatikan di
 * {@link SecurityConfig}, dan kebijakan cache dikendalikan di sini.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StaticCacheFilter extends OncePerRequestFilter {

    /** Gambar: nama file stabil atau berbasis timestamp, aman di-cache 1 tahun. */
    private static final String IMMUTABLE = "public, max-age=31536000, immutable";

    /** CSS/JS:browser selalu revalidasi lewat ETag (respons 304, hampir tanpa byte). */
    private static final String REVALIDATE = "public, max-age=0, must-revalidate";

    /** Halaman dinamis & request non-GET: memuat data per pengguna, jangan disimpan. */
    private static final String NO_STORE = "no-cache, no-store, must-revalidate";

    private static final String[] STATIC_PREFIXES = { "/css/", "/js/", "/images/", "/uploads/" };

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (response.containsHeader("Cache-Control")) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();
        boolean readOnly = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        String path = request.getRequestURI();

        if (readOnly && isStatic(path)) {
            response.setHeader("Cache-Control", isVersionedAsset(path) ? IMMUTABLE : REVALIDATE);
        } else {
            response.setHeader("Cache-Control", NO_STORE);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isStatic(String path) {
        for (String prefix : STATIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isVersionedAsset(String path) {
        return path.startsWith("/images/") || path.startsWith("/uploads/");
    }
}