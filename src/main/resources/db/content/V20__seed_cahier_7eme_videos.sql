-- Module of the 7ᵉ parascolaire book: one chapter per book chapter, one lesson per video.
-- The videos themselves are static files of the Angular site (public/videos/7eme); each lesson
-- points at the short link printed as a QR code in the book (/v/<code>), so the module and the
-- book always open the same page. Positions are 1-based, like the ones the API writes (Positions). Skipped if a module with this code already exists.

DO $$
DECLARE
    v_course  BIGINT;
    v_chapter BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM courses WHERE code = 'INF7-CAHIER') THEN
        RETURN;
    END IF;

    INSERT INTO courses (code, title, description, level, color, created_at, updated_at, created_by, updated_by)
    VALUES ('INF7-CAHIER', 'Informatique · cahier 7ᵉ année',
            'Les vidéos du cahier parascolaire : l''essentiel de chaque chapitre (« À retenir ») et la correction pas à pas de chaque TP. Voix en français, mots clés en arabe.',
            'SEVENTH_BASE', '#1F6FB2', NOW(), NOW(), 'system', 'system')
    RETURNING id INTO v_course;

    -- Chapter 1
    INSERT INTO chapters (course_id, position, title, summary, created_at, updated_at, created_by, updated_by)
    VALUES (v_course, 1, 'Ordinateur et système d''exploitation', 'L''ordinateur, ses composants, les fichiers et les dossiers.', NOW(), NOW(), 'system', 'system')
    RETURNING id INTO v_chapter;
    INSERT INTO lessons (chapter_id, position, title, type, duration_minutes, content_url, created_at, updated_at, created_by, updated_by) VALUES
        (v_chapter, 1, 'À retenir · L''ordinateur et moi',             'VIDEO', 3, '/v/7-r1',  NOW(), NOW(), 'system', 'system'),
        (v_chapter, 2, 'Correction · TP 1.1 Construire mon arborescence', 'VIDEO', 3, '/v/7-1-1', NOW(), NOW(), 'system', 'system'),
        (v_chapter, 3, 'Correction · TP 1.2 Renommer, supprimer, restaurer', 'VIDEO', 2, '/v/7-1-2', NOW(), NOW(), 'system', 'system');

    -- Chapter 2
    INSERT INTO chapters (course_id, position, title, summary, created_at, updated_at, created_by, updated_by)
    VALUES (v_course, 2, 'Traitement de textes', 'Saisir, enregistrer, mettre en forme et en page, insérer un tableau et une image.', NOW(), NOW(), 'system', 'system')
    RETURNING id INTO v_chapter;
    INSERT INTO lessons (chapter_id, position, title, type, duration_minutes, content_url, created_at, updated_at, created_by, updated_by) VALUES
        (v_chapter, 1, 'À retenir · J''écris avec l''ordinateur',       'VIDEO', 3, '/v/7-r2',  NOW(), NOW(), 'system', 'system'),
        (v_chapter, 2, 'Correction · TP 2.1 Mon collège',                'VIDEO', 2, '/v/7-2-1', NOW(), NOW(), 'system', 'system'),
        (v_chapter, 3, 'Correction · TP 2.2 Ma carte d''identité scolaire', 'VIDEO', 2, '/v/7-2-2', NOW(), NOW(), 'system', 'system');

    -- Chapter 3
    INSERT INTO chapters (course_id, position, title, summary, created_at, updated_at, created_by, updated_by)
    VALUES (v_course, 3, 'Traitement d''images', 'Dessiner, numériser et appliquer des traitements simples à une image.', NOW(), NOW(), 'system', 'system')
    RETURNING id INTO v_chapter;
    INSERT INTO lessons (chapter_id, position, title, type, duration_minutes, content_url, created_at, updated_at, created_by, updated_by) VALUES
        (v_chapter, 1, 'À retenir · Je dessine avec l''ordinateur',     'VIDEO', 3, '/v/7-r3',  NOW(), NOW(), 'system', 'system'),
        (v_chapter, 2, 'Correction · TP 3.1 Le drapeau tunisien',       'VIDEO', 2, '/v/7-3-1', NOW(), NOW(), 'system', 'system'),
        (v_chapter, 3, 'Correction · TP 3.2 Trois versions d''une photo', 'VIDEO', 2, '/v/7-3-2', NOW(), NOW(), 'system', 'system');

    -- Chapter 4
    INSERT INTO chapters (course_id, position, title, summary, created_at, updated_at, created_by, updated_by)
    VALUES (v_course, 4, 'Internet et le Web', 'Naviguer, chercher une information, la vérifier et citer ses sources.', NOW(), NOW(), 'system', 'system')
    RETURNING id INTO v_chapter;
    INSERT INTO lessons (chapter_id, position, title, type, duration_minutes, content_url, created_at, updated_at, created_by, updated_by) VALUES
        (v_chapter, 1, 'À retenir · Je surfe sur le Web',               'VIDEO', 3, '/v/7-r4',  NOW(), NOW(), 'system', 'system'),
        (v_chapter, 2, 'Correction · TP 4.1 Découvrir mon navigateur',  'VIDEO', 2, '/v/7-4-1', NOW(), NOW(), 'system', 'system'),
        (v_chapter, 3, 'Correction · TP 4.2 Mini-exposé : une ville tunisienne', 'VIDEO', 2, '/v/7-4-2', NOW(), NOW(), 'system', 'system');

    -- Chapter 5
    INSERT INTO chapters (course_id, position, title, summary, created_at, updated_at, created_by, updated_by)
    VALUES (v_course, 5, 'Mon projet : autour du texte et de l''image', 'Le projet de fin d''année, étape par étape.', NOW(), NOW(), 'system', 'system')
    RETURNING id INTO v_chapter;
    INSERT INTO lessons (chapter_id, position, title, type, duration_minutes, content_url, created_at, updated_at, created_by, updated_by) VALUES
        (v_chapter, 1, 'À retenir · Mon grand projet',                  'VIDEO', 3, '/v/7-r5',  NOW(), NOW(), 'system', 'system');
END $$;
