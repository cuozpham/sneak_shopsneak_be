package sneak_shop.controller.admin;

import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.entity.ChatMessageEntity;
import sneak_shop.entity.OrderEntity;
import sneak_shop.entity.UserEntity;
import sneak_shop.repository.ChatRepository;
import sneak_shop.repository.OrderRepository;
import sneak_shop.repository.UserRepository;
import sneak_shop.security.UserContext;
import sneak_shop.service.NotificationService;
import sneak_shop.websocket.RealtimeSocketHub;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/chat")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatController {

    record ChatMessageResponse(
            Integer id,
            String orderCode,
            String senderRole,
            String senderName,
            String senderAvatarUrl,
            String content,
            Instant createdAt,
            Boolean isRead
    ) {
        static ChatMessageResponse from(ChatMessageEntity e, String avatarUrl) {
            return new ChatMessageResponse(
                    e.getId(),
                    e.getOrderCode(),
                    e.getSenderRole(),
                    e.getSenderName(),
                    avatarUrl,
                    e.getContent(),
                    e.getCreatedAt(),
                    e.getIsRead()
            );
        }
    }

    record ConversationResponse(
            String orderCode,
            String displayName,
            String lastContent,
            String lastSenderRole,
            Long unreadCount,
            Instant lastTime,
            String avatarUrl
    ) {}

    private final ChatRepository chatRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RealtimeSocketHub realtimeSocketHub;

    public AdminChatController(ChatRepository chatRepository,
                               OrderRepository orderRepository,
                               UserRepository userRepository,
                               NotificationService notificationService,
                               RealtimeSocketHub realtimeSocketHub) {
        this.chatRepository = chatRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.realtimeSocketHub = realtimeSocketHub;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> getConversations() {
        List<ConversationResponse> conversations = chatRepository.findConversations()
                .stream()
                .map(row -> new ConversationResponse(
                        (String) row[0],
                        row[1] == null ? (String) row[0] : row[1].toString(),
                        (String) row[4],
                        (String) row[5],
                        row[3] instanceof Number n ? n.longValue() : Long.parseLong(row[3].toString()),
                        row[2] instanceof Instant i ? i : ((java.sql.Timestamp) row[2]).toInstant(),
                        row[6] != null ? row[6].toString() : null
                ))
                .toList();
        return ApiResponse.ok(conversations);
    }

    @GetMapping("/{orderCode}")
    @Transactional
    public ApiResponse<List<ChatMessageResponse>> getMessages(@PathVariable String orderCode) {
        chatRepository.markUserMessagesAsRead(orderCode);
        List<ChatMessageEntity> entities = chatRepository.findByOrderCodeOrderByCreatedAtAsc(orderCode);
        Map<Integer, String> avatarMap = buildAvatarMap(entities);
        List<ChatMessageResponse> messages = entities.stream()
                .map(e -> ChatMessageResponse.from(e, e.getUserId() != null ? avatarMap.get(e.getUserId()) : null))
                .toList();
        return ApiResponse.ok(messages);
    }

    @PostMapping("/{orderCode}/send")
    @Transactional
    public ApiResponse<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal UserContext ctx,
            @PathVariable String orderCode,
            @RequestBody Map<String, String> body
    ) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Noi dung tin nhan khong duoc de trong");
        }
        UserEntity admin = userRepository.findById(ctx.id()).orElse(null);
        String adminName = (admin != null && admin.getFullName() != null && !admin.getFullName().isBlank())
                ? admin.getFullName() : "MANDRO";
        String adminAvatarUrl = admin != null ? admin.getAvatarUrl() : null;
        ChatMessageEntity msg = ChatMessageEntity.builder()
                .orderCode(orderCode)
                .userId(ctx.id())
                .senderRole("ADMIN")
                .senderName(adminName)
                .content(content.trim())
                .build();

        ChatMessageEntity saved = chatRepository.save(msg);
        Integer targetUserId = resolveTargetUserId(orderCode);
        OrderEntity order = resolveOrder(orderCode);
        realtimeSocketHub.afterCommit(() -> {
            if (targetUserId != null) {
                realtimeSocketHub.pushChatMessageToUser(targetUserId, saved);
                notificationService.notifyUser(
                        targetUserId,
                        order,
                        "Tin nhắn mới từ shop",
                        "Shop vừa gửi cho bạn một tin nhắn mới."
                                + (order != null ? " Đơn hàng: " + order.getOrderCode() + "." : ""),
                        "chat_message",
                        null
                );
            }
            realtimeSocketHub.pushChatMessageToAdmins(saved);
        });
        return ApiResponse.ok("Gui tin nhan thanh cong", ChatMessageResponse.from(saved, adminAvatarUrl));
    }

    private Map<Integer, String> buildAvatarMap(List<ChatMessageEntity> entities) {
        Set<Integer> userIds = entities.stream()
                .map(ChatMessageEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return Map.of();
        return userRepository.findAllById(userIds).stream()
                .filter(u -> u.getAvatarUrl() != null && !u.getAvatarUrl().isBlank())
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getAvatarUrl));
    }

    private Integer resolveTargetUserId(String orderCode) {
        if (orderCode != null && orderCode.startsWith("SUPPORT-")) {
            try {
                return Integer.valueOf(orderCode.substring("SUPPORT-".length()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        OrderEntity order = orderRepository.findByOrderCode(orderCode).orElse(null);
        return order != null ? order.getUser().getId() : null;
    }

    private OrderEntity resolveOrder(String orderCode) {
        if (orderCode == null || orderCode.startsWith("SUPPORT-")) {
            return null;
        }
        return orderRepository.findByOrderCode(orderCode).orElse(null);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount() {
        return ApiResponse.ok(chatRepository.countByIsReadFalseAndSenderRole("USER"));
    }
}
