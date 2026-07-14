package sneak_shop.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.response.NotificationResponse;
import sneak_shop.entity.NotificationEntity;
import sneak_shop.entity.OrderEntity;
import sneak_shop.entity.UserEntity;
import sneak_shop.repository.NotificationRepository;
import sneak_shop.repository.UserRepository;
import sneak_shop.service.NotificationService;
import sneak_shop.enums.UserRole;
import sneak_shop.websocket.RealtimeSocketHub;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RealtimeSocketHub realtimeSocketHub;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository,
                                   RealtimeSocketHub realtimeSocketHub) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.realtimeSocketHub = realtimeSocketHub;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAll(Integer userId, int page, int size) {
        return PageResponse.from(notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(notification -> NotificationResponse.from(
                        notification,
                        notification.getOrder() != null ? notification.getOrder().getOrderCode() : null
                )));
    }

    public long countUnread(Integer userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(Integer userId, Integer notifId) {
        notificationRepository.findById(notifId)
                .filter(n -> n.getUser().getId().equals(userId))
                .ifPresent(n -> {
                    n.setIsRead(true);
                    notificationRepository.save(n);
                    realtimeSocketHub.afterCommit(() -> realtimeSocketHub.pushNotificationCount(userId, countUnread(userId)));
                });
    }

    @Transactional
    public void markAllRead(Integer userId) {
        notificationRepository.markAllReadByUserId(userId);
        realtimeSocketHub.afterCommit(() -> realtimeSocketHub.pushNotificationCount(userId, 0));
    }

    @Transactional
    public void notifyUser(Integer userId, OrderEntity order, sneak_shop.entity.ProductEntity product, String title, String body, String type, String imageUrl) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Nguoi dung khong ton tai"));
        String orderCode = order != null ? order.getOrderCode() : null;
        NotificationEntity saved = notificationRepository.save(NotificationEntity.builder()
                .user(user)
                .order(order)
                .product(product)
                .title(title)
                .body(body)
                .type(type != null ? type : "system")
                .imageUrl(imageUrl)
                .isRead(false)
                .build());
        realtimeSocketHub.afterCommit(() ->
                realtimeSocketHub.pushNotificationCreated(userId, NotificationResponse.from(saved, orderCode), countUnread(userId)));
    }

    @Transactional
    public void notifyAdmins(OrderEntity order, String title, String body, String type, String imageUrl) {
        var admins = userRepository.findAllByRoleAndDeletedAtIsNull(UserRole.admin);
        if (admins.isEmpty()) return;
        for (UserEntity admin : admins) {
            String orderCode = order != null ? order.getOrderCode() : null;
            NotificationEntity saved = notificationRepository.save(NotificationEntity.builder()
                    .user(admin)
                    .order(order)
                    .title(title)
                    .body(body)
                    .type(type != null ? type : "system")
                    .imageUrl(imageUrl)
                    .isRead(false)
                    .build());
            realtimeSocketHub.afterCommit(() ->
                    realtimeSocketHub.pushNotificationCreated(admin.getId(), NotificationResponse.from(saved, orderCode), countUnread(admin.getId())));
        }
    }
}
