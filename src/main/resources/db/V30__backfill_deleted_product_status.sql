UPDATE products SET status = 'inactive' WHERE is_deleted = true AND status <> 'inactive';
