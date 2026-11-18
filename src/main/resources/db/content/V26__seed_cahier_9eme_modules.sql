-- The 9ᵉ book (programme 2024) videos: one module per book chapter, INF9-C1 … INF9-C4, each with
-- « L'essentiel en vidéo » (the « À retenir » video) and « Corrections des TP ». The lessons link to
-- /v/<code>, the same short links as the QR codes printed in the book. A module that already exists
-- is left as the admin made it.

DO $$
DECLARE
    v_course  BIGINT;
    v_chapter BIGINT;
    m         RECORD;
    l         RECORD;
BEGIN
    FOR m IN
        SELECT * FROM (VALUES
            (1, 'INF9-C1', 'Cahier 9ᵉ · Je découvre les objets communicants',
                'Capteurs, actionneurs, carte micro:bit et technologies sans fil : la vidéo « À retenir » et la correction des TP du cahier.', '#2563eb'),
            (2, 'INF9-C2', 'Cahier 9ᵉ · Je collabore en ligne et je protège mon identité numérique',
                'Outils de collaboration, traces et identité numérique : la vidéo « À retenir » et la correction des TP du cahier.', '#059669'),
            (3, 'INF9-C3', 'Cahier 9ᵉ · Je programme un objet communicant',
                'Analyser un problème et programmer la micro:bit avec MakeCode : la vidéo « À retenir » et la correction des TP du cahier.', '#7c3aed'),
            (4, 'INF9-C4', 'Cahier 9ᵉ · Je crée une application mobile et mon projet',
                'App Inventor et le projet « station météo de la classe » : la vidéo « À retenir » et la correction des TP du cahier.', '#ea580c')
        ) AS t(num, code, title, description, color)
    LOOP
        IF EXISTS (SELECT 1 FROM courses WHERE code = m.code) THEN
            CONTINUE;
        END IF;

        INSERT INTO courses (code, title, description, level, color, created_at, updated_at, created_by, updated_by)
        VALUES (m.code, m.title, m.description, 'NINTH_BASE', m.color, NOW(), NOW(), 'system', 'system')
        RETURNING id INTO v_course;

        FOR l IN
            SELECT * FROM (VALUES
                (1, 1, 'L''essentiel en vidéo', 1, 'À retenir · Les objets communicants', 2, '/v/9-r1'),
                (1, 2, 'Corrections des TP', 1, 'Correction · TP 1.1 Je découvre la carte micro:bit', 2, '/v/9-1-1'),
                (1, 2, 'Corrections des TP', 2, 'Correction · TP 1.2 J''enquête sur les objets du quotidien', 2, '/v/9-1-2'),
                (2, 1, 'L''essentiel en vidéo', 1, 'À retenir · Je collabore et je me protège', 2, '/v/9-r2'),
                (2, 2, 'Corrections des TP', 1, 'Correction · TP 2.1 Une carte mentale à plusieurs', 2, '/v/9-2-1'),
                (2, 2, 'Corrections des TP', 2, 'Correction · TP 2.2 Je contrôle mes traces numériques', 2, '/v/9-2-2'),
                (3, 1, 'L''essentiel en vidéo', 1, 'À retenir · Je programme un objet', 2, '/v/9-r3'),
                (3, 2, 'Corrections des TP', 1, 'Correction · TP 3.1 La veilleuse automatique', 2, '/v/9-3-1'),
                (3, 2, 'Corrections des TP', 2, 'Correction · TP 3.2 Deux cartes par radio', 2, '/v/9-3-2'),
                (4, 1, 'L''essentiel en vidéo', 1, 'À retenir · Je crée une application', 2, '/v/9-r4'),
                (4, 2, 'Corrections des TP', 1, 'Correction · TP 4.1 Ma première application mobile', 2, '/v/9-4-1'),
                (4, 2, 'Corrections des TP', 2, 'Correction · TP 4.2 Projet station météo', 2, '/v/9-4-2')
            ) AS t(num, ch_pos, ch_title, pos, title, minutes, url)
            WHERE t.num = m.num
            ORDER BY t.ch_pos, t.pos
        LOOP
            SELECT id INTO v_chapter FROM chapters WHERE course_id = v_course AND position = l.ch_pos;
            IF v_chapter IS NULL THEN
                INSERT INTO chapters (course_id, position, title, created_at, updated_at, created_by, updated_by)
                VALUES (v_course, l.ch_pos, l.ch_title, NOW(), NOW(), 'system', 'system')
                RETURNING id INTO v_chapter;
            END IF;

            INSERT INTO lessons (chapter_id, position, title, type, duration_minutes, content_url,
                                 created_at, updated_at, created_by, updated_by)
            VALUES (v_chapter, l.pos, l.title, 'VIDEO', l.minutes, l.url, NOW(), NOW(), 'system', 'system');
            v_chapter := NULL;
        END LOOP;
    END LOOP;
END $$;
