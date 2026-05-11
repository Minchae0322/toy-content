-- =====================================================================
-- tb_product soft delete 컬럼 추가 (스키마 변경 이력)
--
-- [적용 방법]
-- 본 프로젝트는 ddl-auto=update 로 동작하므로 아래 변경은
-- 애플리케이션 재기동 시 Hibernate 가 자동 반영한다.
-- 따라서 이 스크립트는 수동 실행할 필요가 없으며,
-- 스키마 변경 이력 추적 용도로만 보관한다.
--
-- 변경 내용:
-- 1) is_deleted 컬럼 추가 (기본 false)
-- 2) deleted_at 컬럼 추가 (nullable)
-- 3) is_deleted 인덱스 추가
-- =====================================================================

-- 1) is_deleted 컬럼 추가 (기존 행은 false 로 채워짐)
ALTER TABLE tb_product
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '삭제 여부 (soft delete)';

-- 2) deleted_at 컬럼 추가
ALTER TABLE tb_product
    ADD COLUMN deleted_at DATETIME NULL COMMENT '삭제 일시';

-- 3) is_deleted 인덱스 추가
ALTER TABLE tb_product
    ADD INDEX idx_product_is_deleted (is_deleted);
