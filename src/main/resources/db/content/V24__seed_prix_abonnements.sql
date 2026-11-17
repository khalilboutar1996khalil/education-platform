-- Prix de départ des abonnements annuels, choisis par l'admin : 100 DT par an pour la 7ème, la
-- 8ème et la 9ème, en vente. Ils restent modifiables dans Admin → Abonnements ; un niveau qui a
-- déjà un plan n'est pas touché.
INSERT INTO subscription_plans (level, price_dt, duration_days, active, created_at, updated_at, created_by, updated_by)
SELECT l.code, 100.000, 365, TRUE, NOW(), NOW(), 'system', 'system'
FROM school_levels l
WHERE l.code IN ('SEVENTH_BASE', 'EIGHTH_BASE', 'NINTH_BASE')
  AND NOT EXISTS (SELECT 1 FROM subscription_plans p WHERE p.level = l.code);
