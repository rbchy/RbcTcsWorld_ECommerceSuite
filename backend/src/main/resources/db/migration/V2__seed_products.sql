-- Demo catalog so the frontend and automation have data on a fresh database.
INSERT INTO products (name, sku, category, price, stock, active) VALUES
 ('Wireless Mouse',            'ELEC-MOUSE-001', 'electronics',  24.99, 150, TRUE),
 ('Mechanical Keyboard',       'ELEC-KEYB-002',  'electronics',  89.00,  40, TRUE),
 ('27-inch 4K Monitor',        'ELEC-MON-003',   'electronics', 329.99,  12, TRUE),
 ('USB-C Hub 7-in-1',          'ELEC-HUB-004',   'electronics',  45.50,  75, TRUE),
 ('Stainless Water Bottle',    'HOME-BOTL-001',  'home',         19.95, 200, TRUE),
 ('Ceramic Coffee Mug Set',    'HOME-MUG-002',   'home',         29.00,  60, TRUE),
 ('Software Testing Handbook', 'BOOK-QA-001',    'books',        39.99,  25, TRUE),
 ('Limited Edition Headphones','ELEC-HEAD-005',  'electronics', 199.00,   3, TRUE);
