package com.connect.pairr.auth.websocket;

import com.connect.pairr.auth.JwtService;
import com.connect.pairr.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");

        if (token == null || token.isBlank()) {
            log.warn("WebSocket handshake rejected: no token provided");
            return false;
        }

        try {
            Claims payload = jwtService.getPayload(token);
            UUID userId = UUID.fromString(payload.getSubject());
            String role = payload.get("role", String.class);

            if (!userService.userExists(userId)) {
                log.warn("WebSocket handshake rejected: user {} not found", userId);
                return false;
            }

            attributes.put("userId", userId);
            attributes.put("role", role);
            return true;

        } catch (JwtException ex) {
            log.warn("WebSocket handshake rejected: invalid token — {}", ex.getMessage());
            return false;
        } catch (IllegalArgumentException ex) {
            log.warn("WebSocket handshake rejected: invalid user ID in token");
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op
    }
}
