-- Demo data for the development database only (tests do not load this folder).
-- Tariff values are the Energy and Water Ministry's published 2026 rates, as reported by L'Orient Today:
--   Feb 2026: 30,244 LBP/kWh;  Jun 2026: 41,973 LBP/kWh;  Aug 2026: 48,241 LBP/kWh;
--   fixed fees 385,000 (5 A), 685,000 (10 A), +300,000 per extra 5 A; +10% in rural/mountain areas.
-- Zones, people, phones and meters below are fictional.

INSERT INTO zone (name, is_rural) VALUES
    ('Zgharta Center', FALSE),
    ('Ehden',          TRUE),
    ('Ardeh',          TRUE);

INSERT INTO tariff_month (billing_month, price_per_kwh, fee_5a, fee_10a, fee_per_extra_5a, rural_uplift_pct) VALUES
    ('2026-02-01', 30244, 385000, 685000, 300000, 10.0),
    ('2026-06-01', 41973, 385000, 685000, 300000, 10.0),
    ('2026-08-01', 48241, 385000, 685000, 300000, 10.0);

INSERT INTO subscriber (zone_id, full_name, phone, address, status) VALUES
    ((SELECT zone_id FROM zone WHERE name = 'Zgharta Center'), 'Georges Haddad',  '03 111 201', 'Main street, bldg 4',   'ACTIVE'),
    ((SELECT zone_id FROM zone WHERE name = 'Zgharta Center'), 'Rita Khoury',     '70 222 302', 'Near the church',       'ACTIVE'),
    ((SELECT zone_id FROM zone WHERE name = 'Zgharta Center'), 'Fadi Saliba',     '76 333 403', 'Old souk, 2nd floor',   'ACTIVE'),
    ((SELECT zone_id FROM zone WHERE name = 'Ehden'),          'Maya Douaihy',    '03 444 504', 'Midan square',          'ACTIVE'),
    ((SELECT zone_id FROM zone WHERE name = 'Ehden'),          'Antoine Karam',   '71 555 605', 'Mar Sarkis road',       'ACTIVE'),
    ((SELECT zone_id FROM zone WHERE name = 'Ardeh'),          'Nadine Fenianos', '81 666 706', 'Village entrance',      'ACTIVE');

INSERT INTO subscription (subscriber_id, meter_serial, amperage, initial_reading, start_date) VALUES
    ((SELECT subscriber_id FROM subscriber WHERE phone = '03 111 201'), 'MTR-ZG-0001',  5, 1200.0, '2026-07-01'),
    ((SELECT subscriber_id FROM subscriber WHERE phone = '70 222 302'), 'MTR-ZG-0002', 10,  850.5, '2026-07-01'),
    ((SELECT subscriber_id FROM subscriber WHERE phone = '76 333 403'), 'MTR-ZG-0003', 15, 3000.0, '2026-07-01'),
    ((SELECT subscriber_id FROM subscriber WHERE phone = '03 444 504'), 'MTR-EH-0001', 10,  410.0, '2026-07-01'),
    ((SELECT subscriber_id FROM subscriber WHERE phone = '71 555 605'), 'MTR-EH-0002', 20, 2500.0, '2026-07-01'),
    ((SELECT subscriber_id FROM subscriber WHERE phone = '81 666 706'), 'MTR-AR-0001',  5,   95.0, '2026-07-01');

-- July and August readings, entered by the collector
INSERT INTO meter_reading (subscription_id, billing_month, reading_value, read_at, read_by)
SELECT s.subscription_id, v.month::date, v.value, v.month::timestamp + INTERVAL '27 days',
       (SELECT user_id FROM app_user WHERE username = 'collector')
FROM (VALUES
    ('MTR-ZG-0001', '2026-07-01', 1262.0), ('MTR-ZG-0001', '2026-08-01', 1330.0),
    ('MTR-ZG-0002', '2026-07-01',  981.0), ('MTR-ZG-0002', '2026-08-01', 1125.5),
    ('MTR-ZG-0003', '2026-07-01', 3210.0), ('MTR-ZG-0003', '2026-08-01', 3440.0),
    ('MTR-EH-0001', '2026-07-01',  520.0), ('MTR-EH-0001', '2026-08-01',  640.0),
    ('MTR-EH-0002', '2026-07-01', 2800.0), ('MTR-EH-0002', '2026-08-01', 3120.0),
    ('MTR-AR-0001', '2026-07-01',  130.0)
) AS v (meter, month, value)
JOIN subscription s ON s.meter_serial = v.meter;
