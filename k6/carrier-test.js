import http from 'k6/http';
import { check, sleep, group } from 'k6';

// ─── 토큰 설정 ───
// k6 run -e TOKEN=eyJhbG... k6/carrier-test.js
// k6 run -e TOKEN=eyJhbG... -e BASE_URL=https://yogurtte.com/api k6/carrier-test.js
const TOKEN = __ENV.TOKEN || '';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082/api';

if (!TOKEN) {
    console.warn('⚠ TOKEN이 설정되지 않았습니다. -e TOKEN=xxx 옵션으로 전달하세요.');
}

// ─── 캐리어 시나리오 (총 5000 VU) ───
export const options = {
    scenarios: {
        // 50% - 캐리어 조회 유저
        carrier_viewer: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 2500 },  // 워밍업
                { duration: '7m', target: 2500 },  // 유지
                { duration: '2m', target: 0 },     // 쿨다운
            ],
            exec: 'carrierViewer',
        },
        // 25% - 아이템 관리 유저
        item_manager: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 1250 },
                { duration: '7m', target: 1250 },
                { duration: '2m', target: 0 },
            ],
            exec: 'itemManager',
        },
        // 15% - 스티커 편집 유저
        sticker_editor: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 750 },
                { duration: '7m', target: 750 },
                { duration: '2m', target: 0 },
            ],
            exec: 'stickerEditor',
        },
        // 10% - 캐리어 생성/삭제 유저
        carrier_creator: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 500 },
                { duration: '7m', target: 500 },
                { duration: '2m', target: 0 },
            ],
            exec: 'carrierCreator',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

// ─── 공통 ───
function authHeaders() {
    return {
        headers: {
            'Authorization': `Bearer ${TOKEN}`,
            'Content-Type': 'application/json',
        },
    };
}

function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomFloat(min, max) {
    return Math.random() * (max - min) + min;
}

const SKIN_TYPES = ['DEFAULT', 'METAL', 'WOOD', 'LEATHER', 'NEON'];
const SKIN_COLORS = ['#FF5733', '#33FF57', '#3357FF', '#FF33A1', '#FFFF33', '#FFFFFF', '#000000'];
const STICKER_TYPES = ['TEXT', 'PHOTO_TAG', 'IMAGE'];

// ─── 시나리오 1: 캐리어 조회 유저 (50%) ───
export function carrierViewer() {
    const headers = authHeaders();

    group('내 캐리어 목록 조회', () => {
        const res = http.get(`${BASE_URL}/carriers/me`, headers);
        check(res, { 'my carriers 200': (r) => r.status === 200 });
    });

    sleep(2);

    group('캐리어 상세 조회', () => {
        const carrierId = randomInt(1, 20);
        const res = http.get(`${BASE_URL}/carriers/${carrierId}`, headers);
        check(res, { 'carrier detail 200': (r) => r.status === 200 });
    });

    sleep(3);

    group('다른 캐리어 상세 조회', () => {
        const carrierId = randomInt(1, 20);
        const res = http.get(`${BASE_URL}/carriers/${carrierId}`, headers);
        check(res, { 'carrier detail2 200': (r) => r.status === 200 });
    });

    sleep(1);
}

// ─── 시나리오 2: 아이템 관리 유저 (25%) ───
export function itemManager() {
    const headers = authHeaders();

    group('캐리어 상세 조회', () => {
        const carrierId = randomInt(1, 10);
        const res = http.get(`${BASE_URL}/carriers/${carrierId}`, headers);
        check(res, { 'carrier detail 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('아이템 추가', () => {
        const carrierId = randomInt(1, 10);
        const res = http.post(`${BASE_URL}/carriers/${carrierId}/items`, JSON.stringify({
            productId: randomInt(1, 50),
            positionX: randomFloat(0.0, 1.0),
            positionY: randomFloat(0.0, 1.0),
            zIndex: randomInt(0, 10),
        }), headers);
        check(res, { 'item added': (r) => r.status === 200 || r.status === 201 });
    });

    sleep(2);

    group('아이템 위치 변경', () => {
        const carrierId = randomInt(1, 10);
        const itemId = randomInt(1, 30);
        const res = http.patch(`${BASE_URL}/carriers/${carrierId}/items/${itemId}/position`, JSON.stringify({
            positionX: randomFloat(0.0, 1.0),
            positionY: randomFloat(0.0, 1.0),
            zIndex: randomInt(0, 10),
        }), headers);
        check(res, { 'position updated': (r) => r.status === 200 });
    });

    sleep(1);

    group('아이템 위치 벌크 업데이트', () => {
        const carrierId = randomInt(1, 10);
        const items = [];
        const count = randomInt(2, 5);
        for (let i = 0; i < count; i++) {
            items.push({
                itemId: randomInt(1, 30),
                positionX: randomFloat(0.0, 1.0),
                positionY: randomFloat(0.0, 1.0),
                zIndex: i,
            });
        }
        const res = http.put(`${BASE_URL}/carriers/${carrierId}/items/positions`, JSON.stringify(items), headers);
        check(res, { 'bulk position updated': (r) => r.status === 200 });
    });

    sleep(1);
}

// ─── 시나리오 3: 스티커 편집 유저 (15%) ───
export function stickerEditor() {
    const headers = authHeaders();

    group('캐리어 상세 조회', () => {
        const carrierId = randomInt(1, 10);
        const res = http.get(`${BASE_URL}/carriers/${carrierId}`, headers);
        check(res, { 'carrier detail 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('스티커 벌크 저장', () => {
        const carrierId = randomInt(1, 10);
        const stickers = [];
        const count = randomInt(1, 5);
        for (let i = 0; i < count; i++) {
            const type = STICKER_TYPES[randomInt(0, STICKER_TYPES.length - 1)];
            stickers.push({
                stickerType: type,
                content: type === 'IMAGE' ? null : `스티커 텍스트 ${Date.now()}_${i}`,
                imageUrl: type === 'IMAGE' ? `https://example.com/sticker_${randomInt(1, 100)}.png` : null,
                positionX: randomFloat(0.0, 1.0),
                positionY: randomFloat(0.0, 1.0),
                zIndex: randomInt(0, 10),
                rotation: randomFloat(-180, 180),
                scaleRatio: randomFloat(0.5, 2.0),
            });
        }
        const res = http.put(`${BASE_URL}/carriers/${carrierId}/stickers`, JSON.stringify({
            stickers: stickers,
        }), headers);
        check(res, { 'stickers saved': (r) => r.status === 200 });
    });

    sleep(2);

    group('기존 스티커 수정 (upsert)', () => {
        const carrierId = randomInt(1, 10);
        const res = http.put(`${BASE_URL}/carriers/${carrierId}/stickers`, JSON.stringify({
            stickers: [
                {
                    stickerId: randomInt(1, 50),
                    stickerType: 'TEXT',
                    content: `수정된 스티커 ${Date.now()}`,
                    positionX: randomFloat(0.0, 1.0),
                    positionY: randomFloat(0.0, 1.0),
                    zIndex: randomInt(0, 10),
                    rotation: randomFloat(-180, 180),
                    scaleRatio: randomFloat(0.5, 2.0),
                },
            ],
        }), headers);
        check(res, { 'sticker upserted': (r) => r.status === 200 });
    });

    sleep(1);

    group('스티커 벌크 삭제', () => {
        const carrierId = randomInt(1, 10);
        const stickerIds = [];
        const count = randomInt(1, 3);
        for (let i = 0; i < count; i++) {
            stickerIds.push(randomInt(1, 50));
        }
        const res = http.post(`${BASE_URL}/carriers/${carrierId}/stickers/delete`, JSON.stringify({
            stickerIds: stickerIds,
        }), headers);
        check(res, { 'stickers deleted': (r) => r.status === 200 });
    });

    sleep(1);
}

// ─── 시나리오 4: 캐리어 생성/삭제 유저 (10%) ───
export function carrierCreator() {
    const headers = authHeaders();

    group('내 캐리어 목록 조회', () => {
        const res = http.get(`${BASE_URL}/carriers/me`, headers);
        check(res, { 'my carriers 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('캐리어 생성', () => {
        const skinType = SKIN_TYPES[randomInt(0, SKIN_TYPES.length - 1)];
        const res = http.post(`${BASE_URL}/carriers`, JSON.stringify({
            name: `k6 캐리어 ${Date.now()}`,
            skinType: skinType,
            skinColor: SKIN_COLORS[randomInt(0, SKIN_COLORS.length - 1)],
        }), headers);
        check(res, { 'carrier created': (r) => r.status === 200 || r.status === 201 });
    });

    sleep(2);

    group('캐리어 스킨 변경', () => {
        const carrierId = randomInt(1, 20);
        const res = http.patch(`${BASE_URL}/carriers/${carrierId}/skin`, JSON.stringify({
            skinType: SKIN_TYPES[randomInt(0, SKIN_TYPES.length - 1)],
            skinColor: SKIN_COLORS[randomInt(0, SKIN_COLORS.length - 1)],
        }), headers);
        check(res, { 'skin updated': (r) => r.status === 200 });
    });

    sleep(1);

    group('캐리어 삭제', () => {
        const carrierId = randomInt(10, 30);
        const res = http.del(`${BASE_URL}/carriers/${carrierId}`, null, headers);
        check(res, { 'carrier deleted': (r) => r.status === 200 || r.status === 404 });
    });

    sleep(1);
}
