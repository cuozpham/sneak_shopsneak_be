-- Remove legacy tables and obsolete schema fragments no longer used by the current code.

ALTER TABLE chat_messages
    DROP FOREIGN KEY FKqgkanrr90j46564w4ww63jcna;

ALTER TABLE chat_messages
    DROP COLUMN conversation_id;

ALTER TABLE orders
    DROP FOREIGN KEY FKdimvsocblb17f45ikjr6xn1wj;

ALTER TABLE orders
    DROP COLUMN voucher_id;

DROP TABLE IF EXISTS coupon_usages;
DROP TABLE IF EXISTS voucher_usages;
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS chat_conversations;
DROP TABLE IF EXISTS view_change_history;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS contact_messages;
DROP TABLE IF EXISTS inventory_histories;
DROP TABLE IF EXISTS shipping_tracking;
DROP TABLE IF EXISTS site_settings;
DROP TABLE IF EXISTS blog_posts;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS vouchers;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS customer_tiers;
