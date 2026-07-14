package sneak_shop.service;

import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.response.NotificationResponse;
import sneak_shop.entity.OrderEntity;
import sneak_shop.entity.ProductEntity;

public interface NotificationService {
    PageResponse<NotificationResponse> getAll(Integer userId, int page, int size);
    long countUnread(Integer userId);
    void markRead(Integer userId, Integer notifId);
    void markAllRead(Integer userId);
    default void notifyUser(Integer userId, String title, String body, String type, String imageUrl) {
        notifyUser(userId, null, title, body, type, imageUrl);
    }
    default void notifyUser(Integer userId, OrderEntity order, String title, String body, String type, String imageUrl) {
        notifyUser(userId, order, null, title, body, type, imageUrl);
    }
    void notifyUser(Integer userId, OrderEntity order, ProductEntity product, String title, String body, String type, String imageUrl);
    default void notifyAdmins(String title, String body, String type, String imageUrl) {
        notifyAdmins(null, title, body, type, imageUrl);
    }
    void notifyAdmins(OrderEntity order, String title, String body, String type, String imageUrl);
}
