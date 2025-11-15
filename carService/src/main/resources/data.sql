-- Seed wash centres — only inserts if not already present
-- This runs automatically on startup when spring.jpa.hibernate.ddl-auto=update

INSERT IGNORE INTO wash_centres
    (name, address, city, pincode, phone, operating_hours,
     price_basic, price_premium, price_full_detail, rating, capacity, is_active, created_at, updated_at)
VALUES
    ('Pune Central', 'MG Road, Camp, Pune', 'Pune', '411001', '9876500001', '07:00-21:00',
     299.00, 499.00, 899.00, 4.5, 4, true, NOW(), NOW()),

    ('Wakad Express Wash', 'Near Wakad Bridge, Wakad, Pune', 'Pune', '411057', '9876500002', '08:00-20:00',
     249.00, 449.00, 799.00, 4.2, 3, true, NOW(), NOW()),

    ('Hinjewadi Tech Park Wash', 'Phase 1, Hinjewadi, Pune', 'Pune', '411057', '9876500003', '07:00-22:00',
     299.00, 549.00, 999.00, 4.7, 5, true, NOW(), NOW()),

    ('Koregaon Park Detailing', 'Lane 5, Koregaon Park, Pune', 'Pune', '411001', '9876500004', '09:00-20:00',
     349.00, 649.00, 1199.00, 4.8, 3, true, NOW(), NOW()),

    ('Hadapsar AutoSpa', 'Magarpatta Road, Hadapsar, Pune', 'Pune', '411028', '9876500005', '07:00-21:00',
     249.00, 449.00, 849.00, 4.1, 4, true, NOW(), NOW()),

    ('Mumbai Andheri Wash', 'Andheri West, Mumbai', 'Mumbai', '400058', '9876500006', '06:00-22:00',
     399.00, 699.00, 1299.00, 4.6, 5, true, NOW(), NOW()),

    ('Bangalore Whitefield Spa', 'Whitefield Main Road, Bangalore', 'Bangalore', '560066', '9876500007', '07:00-21:00',
     349.00, 599.00, 1099.00, 4.4, 4, true, NOW(), NOW());
