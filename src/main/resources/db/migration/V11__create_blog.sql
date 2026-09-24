-- The blog. Categories are a table rather than an enum because they are content a teacher edits,
-- not states the code branches on.

CREATE TABLE blog_categories (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    slug       VARCHAR(120) NOT NULL,
    color      VARCHAR(7),

    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT uk_blog_categories_slug UNIQUE (slug)
);

CREATE TABLE blog_posts (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    -- The public URL, stable once published: editing the title does not move the article
    slug          VARCHAR(200) NOT NULL,
    excerpt       VARCHAR(500),
    content       TEXT         NOT NULL,
    category_id   BIGINT       NOT NULL,
    author_id     BIGINT       NOT NULL,
    cover_file_id BIGINT,
    status        VARCHAR(20)  NOT NULL,
    read_minutes  INT          NOT NULL DEFAULT 1,
    view_count    BIGINT       NOT NULL DEFAULT 0,
    published_at  TIMESTAMP(6) WITH TIME ZONE,

    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    updated_by    VARCHAR(100) NOT NULL,
    deleted_at    TIMESTAMP(6) WITH TIME ZONE,

    CONSTRAINT uk_blog_posts_slug UNIQUE (slug),
    CONSTRAINT fk_blog_posts_category FOREIGN KEY (category_id) REFERENCES blog_categories (id),
    CONSTRAINT fk_blog_posts_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_blog_posts_cover FOREIGN KEY (cover_file_id) REFERENCES stored_files (id),
    CONSTRAINT ck_blog_posts_counts CHECK (read_minutes >= 1 AND view_count >= 0)
);

-- The public list: published, newest first
CREATE INDEX idx_blog_posts_status_published ON blog_posts (status, published_at DESC);
CREATE INDEX idx_blog_posts_category ON blog_posts (category_id);
