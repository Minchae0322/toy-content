-- =====================================================================
-- Category type 도입 마이그레이션 (FEED 전용 카테고리 분리)
--
-- 1) categories.type 컬럼 추가 (개발: ddl-auto=update가 자동 생성하지만 운영용 수동 SQL)
-- 2) 기존 행은 default('BATTLE')로 채워짐 — 필요하면 PRODUCT로 보정
-- 3) FEED 카테고리 3개 시드: 핫 먹거리 / 핫 아이템 / 기타
-- 4) 기존 tb_feed.category_id 를 모두 "기타"(FEED)로 일괄 이동
-- =====================================================================

-- 1) 컬럼 추가 (운영 환경용 — dev는 Hibernate가 처리)
ALTER TABLE categories
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'BATTLE';

-- 2) (선택) 기존 행을 PRODUCT로 분류하고 싶다면 주석 해제
-- UPDATE categories SET type = 'PRODUCT';

-- 3) FEED 카테고리 시드 (depth=0, 활성)
INSERT INTO categories (name, description, sort_order, is_active, depth, type, created_at, updated_at)
VALUES
  ('핫 먹거리', '인기 먹거리 피드', 1, 1, 0, 'FEED', NOW(), NOW()),
  ('핫 아이템', '인기 아이템 피드', 2, 1, 0, 'FEED', NOW(), NOW()),
  ('기타',     '기타 피드',        3, 1, 0, 'FEED', NOW(), NOW());

-- 4) 기존 피드 → "기타"(FEED) 일괄 이동
SET @feed_etc_id := (SELECT category_id FROM categories WHERE name = '기타' AND type = 'FEED' LIMIT 1);

UPDATE tb_feed SET category_id = @feed_etc_id;
