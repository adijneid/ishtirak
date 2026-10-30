-- Application users. Passwords are stored as BCrypt hashes only.
--   admin     / admin123      (ADMIN)
--   collector / collector123  (COLLECTOR)
--   former    / disabled123   (COLLECTOR, disabled: login answers 403 ACCOUNT_DISABLED)
-- Change these passwords before any real use.
INSERT INTO app_user (username, password_hash, full_name, role, active) VALUES
    ('admin',     '$2a$10$6pVt9iMcB3cGd.Yzg0E2rO.kMx5K9Sb8AQfVVhZga08lwOB/logSq', 'Operator Admin',   'ADMIN',     TRUE),
    ('collector', '$2a$10$JjkX80L0BVxxvI3WMNxhA.OoszwA83jQlFeIKZn2Hee8XNHFJCu9G', 'Field Collector',  'COLLECTOR', TRUE),
    ('former',    '$2a$10$gbOHPQ66uq/0lRlS8MLJcOCF7pPdz0iSn01NQwFEQHOsoeS9a.QhO', 'Former Collector', 'COLLECTOR', FALSE);
