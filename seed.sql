SET SESSION cte_max_recursion_depth = 200001;

INSERT INTO product_data (name, category, price, created_at, updated_at)
WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 200000)
SELECT CONCAT(
               ELT(1 + (n MOD 6), 'ELECTRONICS', 'CLOTHING', 'BOOKS', 'BEAUTY', 'SPORTS', 'HEALTH'),
               '-product-',
               LPAD(n, 6, '0')
       )                                                                                    AS name,
       ELT(1 + (n MOD 6), 'ELECTRONICS', 'CLOTHING', 'BOOKS', 'BEAUTY', 'SPORTS', 'HEALTH') AS category,
       ROUND(100 + RAND(n) * 9900, 2)                                                       AS price,
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND(n * 3) * 525600) MINUTE)                         AS created_at,
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND(n * 7) * 259200) MINUTE)                         AS updated_at

FROM seq;
