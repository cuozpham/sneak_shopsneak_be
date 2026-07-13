UPDATE users u
INNER JOIN (SELECT id FROM product_shops ORDER BY id LIMIT 1) s
   ON 1 = 1
SET u.shop_id = s.id
WHERE u.role = 'admin'
  AND u.shop_id IS NULL;
