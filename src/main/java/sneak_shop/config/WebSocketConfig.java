package sneak_shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import sneak_shop.websocket.RealtimeWebSocketAuthInterceptor;
import sneak_shop.websocket.RealtimeWebSocketHandler;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${web.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    private final RealtimeWebSocketHandler realtimeWebSocketHandler;
    private final RealtimeWebSocketAuthInterceptor realtimeWebSocketAuthInterceptor;

    public WebSocketConfig(RealtimeWebSocketHandler realtimeWebSocketHandler,
                           RealtimeWebSocketAuthInterceptor realtimeWebSocketAuthInterceptor) {
        this.realtimeWebSocketHandler = realtimeWebSocketHandler;
        this.realtimeWebSocketAuthInterceptor = realtimeWebSocketAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeWebSocketHandler, "/ws/realtime")
                .addInterceptors(realtimeWebSocketAuthInterceptor)
                .setAllowedOrigins(parseAllowedOrigins().toArray(String[]::new));
    }

    private List<String> parseAllowedOrigins() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return List.of("http://localhost:3000");
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toList());
    }
}
