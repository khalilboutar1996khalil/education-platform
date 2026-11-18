-- The 8ᵉ book (programme 2024) videos: one module per book chapter, INF8-C1 … INF8-C4, each with
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
            (1, 'INF8-C1', 'Cahier 8ᵉ · Je protège mon environnement numérique',
                'Logiciels malveillants, hameçonnage et bons réflexes : la vidéo « À retenir » et la correction des TP du cahier.', '#2563eb'),
            (2, 'INF8-C2', 'Cahier 8ᵉ · Je communique et je collabore en ligne',
                'E-mail, nétiquette, écriture collaborative et e-réputation : la vidéo « À retenir » et la correction des TP du cahier.', '#059669'),
            (3, 'INF8-C3', 'Cahier 8ᵉ · Je crée des contenus audio et vidéo',
                'Podcast, montage vidéo, formats et droits : la vidéo « À retenir » et la correction des TP du cahier.', '#db2777'),
            (4, 'INF8-C4', 'Cahier 8ᵉ · Je programme avec Scratch',
                'Algorithme, variables, modules et le projet « Attrape les fruits » : la vidéo « À retenir » et la correction des TP du cahier.', '#ea580c')
        ) AS t(num, code, title, description, color)
    LOOP
        IF EXISTS (SELECT 1 FROM courses WHERE code = m.code) THEN
            CONTINUE;
        END IF;

        INSERT INTO courses (code, title, description, level, color, created_at, updated_at, created_by, updated_by)
        VALUES (m.code, m.title, m.description, 'EIGHTH_BASE', m.color, NOW(), NOW(), 'system', 'system')
        RETURNING id INTO v_course;

        FOR l IN
            SELECT * FROM (VALUES
                (1, 1, 'L''essentiel en vidéo', 1, 'À retenir · Je protège mon PC', 2, '/v/8-r1'),
                (1, 2, 'Corrections des TP', 1, 'Correction · TP 1.1 Je sécurise mon ordinateur', 2, '/v/8-1-1'),
                (1, 2, 'Corrections des TP', 2, 'Correction · TP 1.2 Je déjoue l''hameçonnage', 2, '/v/8-1-2'),
                (2, 1, 'L''essentiel en vidéo', 1, 'À retenir · Je communique en ligne', 2, '/v/8-r2'),
                (2, 2, 'Corrections des TP', 1, 'Correction · TP 2.1 Un e-mail avec pièce jointe', 1, '/v/8-2-1'),
                (2, 2, 'Corrections des TP', 2, 'Correction · TP 2.2 Nous écrivons à plusieurs', 2, '/v/8-2-2'),
                (3, 1, 'L''essentiel en vidéo', 1, 'À retenir · Je crée du son et des vidéos', 2, '/v/8-r3'),
                (3, 2, 'Corrections des TP', 1, 'Correction · TP 3.1 J''enregistre un podcast', 2, '/v/8-3-1'),
                (3, 2, 'Corrections des TP', 2, 'Correction · TP 3.2 Je monte une courte vidéo', 2, '/v/8-3-2'),
                (4, 1, 'L''essentiel en vidéo', 1, 'À retenir · Je programme avec Scratch', 2, '/v/8-r4'),
                (4, 2, 'Corrections des TP', 1, 'Correction · TP 4.1 Je programme un quiz', 2, '/v/8-4-1'),
                (4, 2, 'Corrections des TP', 2, 'Correction · TP 4.2 Projet « Attrape les fruits »', 2, '/v/8-4-2')
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
