-- =====================================================================
-- 배틀 게스트 투표 지원 마이그레이션
--
-- 1) tb_battle_vote.user_id 를 nullable 로 변경 (게스트 투표는 user_id NULL)
-- 2) guest_id 컬럼 추가 (쿠키 기반 UUID, 36자)
-- 3) (battle_id, user_id, vote_rank) / (battle_id, guest_id, vote_rank) 유니크 제약 추가
-- 4) user_id 또는 guest_id 중 하나는 반드시 존재해야 함 (CHECK)
--
-- ddl-auto=update 는 컬럼 추가/nullable 변경까지만 처리하고,
-- 기존 테이블에 대한 UNIQUE/CHECK 제약은 추가하지 않으므로 수동 적용 필요.
-- 이미 적용된 단계는 skip 해도 무방하도록 IF EXISTS / IF NOT EXISTS 의도로 작성.
-- =====================================================================

-- 1) user_id nullable 변경 (이미 적용됐을 수 있음)
ALTER TABLE tb_battle_vote
    MODIFY COLUMN user_id BIGINT NULL;

-- 2) guest_id 컬럼 추가 (이미 적용됐을 수 있음 — 적용됐다면 이 구문은 에러, skip)
ALTER TABLE tb_battle_vote
    ADD COLUMN guest_id VARCHAR(36) NULL;

-- 3-1) 유저 투표 유니크 제약 (Hibernate 가 자동 추가하지 않으므로 수동 적용)
ALTER TABLE tb_battle_vote
    ADD CONSTRAINT uk_battle_user_rank UNIQUE (battle_id, user_id, vote_rank);

-- 3-2) 게스트 투표 유니크 제약
ALTER TABLE tb_battle_vote
    ADD CONSTRAINT uk_battle_guest_rank UNIQUE (battle_id, guest_id, vote_rank);

-- 4) 투표자 식별 무결성 제약 (MySQL 8.0.16+)
ALTER TABLE tb_battle_vote
    ADD CONSTRAINT chk_battle_vote_voter
        CHECK (user_id IS NOT NULL OR guest_id IS NOT NULL);
