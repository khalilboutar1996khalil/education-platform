-- The 7ᵉ book videos (V20) become one module per book chapter instead of a single module:
-- INF7-C1 … INF7-C5, each with « L'essentiel en vidéo » and, when the chapter has TPs,
-- « Corrections des TP ». Existing lessons are MOVED, not recreated, so students keep their
-- progress (lesson_completions point at lesson ids). A lesson an admin deleted is recreated.
-- The old INF7-CAHIER module is then removed, unless something else (quiz, TP, resource,
-- announcement, grade) was attached to it, in which case it is left for the admin to tidy.

DO $$
DECLARE
    v_course  BIGINT;
    v_chapter BIGINT;
    v_lesson  BIGINT;
    m         RECORD;
    l         RECORD;
BEGIN
    FOR m IN
        SELECT * FROM (VALUES
            (1, 'INF7-C1', 'Cahier 7ᵉ · Ordinateur et système d''exploitation',
                'L''ordinateur, ses composants, les fichiers et les dossiers : la vidéo « À retenir » et la correction des TP du cahier.', '#1F6FB2'),
            (2, 'INF7-C2', 'Cahier 7ᵉ · Traitement de textes',
                'Saisir, enregistrer, mettre en forme et en page : la vidéo « À retenir » et la correction des TP du cahier.', '#C0392B'),
            (3, 'INF7-C3', 'Cahier 7ᵉ · Traitement d''images',
                'Dessiner et retoucher une image : la vidéo « À retenir » et la correction des TP du cahier.', '#1E8449'),
            (4, 'INF7-C4', 'Cahier 7ᵉ · Internet et le Web',
                'Naviguer, chercher, vérifier et citer ses sources : la vidéo « À retenir » et la correction des TP du cahier.', '#7D3C98'),
            (5, 'INF7-C5', 'Cahier 7ᵉ · Mon projet',
                'Le projet de fin d''année « autour du texte et de l''image », étape par étape.', '#B9770E')
        ) AS t(num, code, title, description, color)
    LOOP
        IF EXISTS (SELECT 1 FROM courses WHERE code = m.code) THEN
            CONTINUE;
        END IF;

        INSERT INTO courses (code, title, description, level, color, created_at, updated_at, created_by, updated_by)
        VALUES (m.code, m.title, m.description, 'SEVENTH_BASE', m.color, NOW(), NOW(), 'system', 'system')
        RETURNING id INTO v_course;

        FOR l IN
            SELECT * FROM (VALUES
                (1, 1, 'L''essentiel en vidéo', 1, 'À retenir · L''ordinateur et moi', 3, '/v/7-r1'),
                (1, 2, 'Corrections des TP', 1, 'Correction · TP 1.1 Construire mon arborescence', 3, '/v/7-1-1'),
                (1, 2, 'Corrections des TP', 2, 'Correction · TP 1.2 Renommer, supprimer, restaurer', 2, '/v/7-1-2'),
                (2, 1, 'L''essentiel en vidéo', 1, 'À retenir · J''écris avec l''ordinateur', 3, '/v/7-r2'),
                (2, 2, 'Corrections des TP', 1, 'Correction · TP 2.1 Mon collège', 2, '/v/7-2-1'),
                (2, 2, 'Corrections des TP', 2, 'Correction · TP 2.2 Ma carte d''identité scolaire', 2, '/v/7-2-2'),
                (3, 1, 'L''essentiel en vidéo', 1, 'À retenir · Je dessine avec l''ordinateur', 3, '/v/7-r3'),
                (3, 2, 'Corrections des TP', 1, 'Correction · TP 3.1 Le drapeau tunisien', 2, '/v/7-3-1'),
                (3, 2, 'Corrections des TP', 2, 'Correction · TP 3.2 Trois versions d''une photo', 2, '/v/7-3-2'),
                (4, 1, 'L''essentiel en vidéo', 1, 'À retenir · Je surfe sur le Web', 3, '/v/7-r4'),
                (4, 2, 'Corrections des TP', 1, 'Correction · TP 4.1 Découvrir mon navigateur', 2, '/v/7-4-1'),
                (4, 2, 'Corrections des TP', 2, 'Correction · TP 4.2 Mini-exposé : une ville tunisienne', 2, '/v/7-4-2'),
                (5, 1, 'L''essentiel en vidéo', 1, 'À retenir · Mon grand projet', 3, '/v/7-r5')
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

            -- The V20 lesson for this video, if it is still in the old module
            SELECT ls.id INTO v_lesson
            FROM lessons ls
            JOIN chapters ch ON ch.id = ls.chapter_id
            JOIN courses c ON c.id = ch.course_id
            WHERE c.code = 'INF7-CAHIER' AND ls.content_url = l.url
            LIMIT 1;

            IF v_lesson IS NOT NULL THEN
                UPDATE lessons
                SET chapter_id = v_chapter, position = l.pos, updated_at = NOW(), updated_by = 'system'
                WHERE id = v_lesson;
            ELSE
                INSERT INTO lessons (chapter_id, position, title, type, duration_minutes, content_url,
                                     created_at, updated_at, created_by, updated_by)
                VALUES (v_chapter, l.pos, l.title, 'VIDEO', l.minutes, l.url, NOW(), NOW(), 'system', 'system');
            END IF;
            v_chapter := NULL;
            v_lesson := NULL;
        END LOOP;
    END LOOP;

    -- The old single module, now empty of videos
    DELETE FROM courses c
    WHERE c.code = 'INF7-CAHIER'
      AND NOT EXISTS (SELECT 1 FROM quizzes q WHERE q.course_id = c.id)
      AND NOT EXISTS (SELECT 1 FROM assignments a WHERE a.course_id = c.id)
      AND NOT EXISTS (SELECT 1 FROM resources r WHERE r.course_id = c.id)
      AND NOT EXISTS (SELECT 1 FROM announcements an WHERE an.course_id = c.id)
      AND NOT EXISTS (SELECT 1 FROM grades g WHERE g.course_id = c.id)
      AND NOT EXISTS (SELECT 1 FROM chapters ch JOIN lessons ls ON ls.chapter_id = ch.id WHERE ch.course_id = c.id);
END $$;
