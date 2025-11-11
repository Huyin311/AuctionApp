package com.huyin.inner_auction.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    // thêm các System.out/System.err để log (thay thế body doFilterInternal)
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        System.out.println("[JWT_FILTER] Request URI=" + request.getRequestURI() + " Authorization header present=" + (authHeader != null));
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            final String token = authHeader.substring(7);
            System.out.println("[JWT_FILTER] token length=" + (token != null ? token.length() : "null"));
            boolean ok = false;
            try {
                ok = jwtUtil.validate(token);
            } catch (Exception e) {
                System.err.println("[JWT_FILTER] validate threw: " + e.getMessage());
                ok = false;
            }
            System.out.println("[JWT_FILTER] jwtUtil.validate => " + ok);
            if (ok) {
                UUID userId = jwtUtil.getUserId(token);
                System.out.println("[JWT_FILTER] jwtUtil.getUserId => " + userId);
                if (userId != null) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userId.toString(), null, List.of()
                    );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    System.out.println("[JWT_FILTER] Authentication set for userId=" + userId);
                } else {
                    System.out.println("[JWT_FILTER] userId parsed null");
                }
            } else {
                System.out.println("[JWT_FILTER] token invalid -> no authentication set");
            }
        } else {
            if (authHeader == null) {
                System.out.println("[JWT_FILTER] Authorization header is NULL");
            } else {
                System.out.println("[JWT_FILTER] Authorization header does not start with 'Bearer '");
            }
        }

        filterChain.doFilter(request, response);
    }
}