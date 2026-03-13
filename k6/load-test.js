import http from 'k6/http';
import { check, sleep, group } from 'k6';

// ─── 토큰 설정 ───
// 실행 시 환경변수로 토큰 전달:
//   k6 run -e TOKEN=eyJhbG... k6/load-test.js
//   k6 run -e TOKEN=eyJhbG... -e BASE_URL=https://yogurtte.com/api k6/load-test.js
const TOKEN = __ENV.TOKEN || '';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082/api';

if (!TOKEN) {
    console.warn('⚠ TOKEN이 설정되지 않았습니다. -e TOKEN=xxx 옵션으로 전달하세요.');
}

// ─── 시나리오별 동접 비율 (총 5000 VU) ───
export const options = {
    scenarios: {
        // 60% - 피드 탐색 유저
        feed_browser: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 3000 },  // 워밍업
                { duration: '7m', target: 3000 },  // 유지
                { duration: '2m', target: 0 },     // 쿨다운
            ],
            exec: 'feedBrowser',
        },
        // 20% - 활동적 유저 (좋아요, 댓글 등)
        active_user: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 1000 },
                { duration: '7m', target: 1000 },
                { duration: '2m', target: 0 },
            ],
            exec: 'activeUser',
        },
        // 10% - 상품 탐색 유저
        product_browser: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 500 },
                { duration: '7m', target: 500 },
                { duration: '2m', target: 0 },
            ],
            exec: 'productBrowser',
        },
        // 5% - 배틀 참여 유저
        battle_voter: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 250 },
                { duration: '7m', target: 250 },
                { duration: '2m', target: 0 },
            ],
            exec: 'battleVoter',
        },
        // 5% - 종합 탐색 유저 (카테고리, 해시태그, 대시보드)
        general_browser: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 250 },
                { duration: '7m', target: 250 },
                { duration: '2m', target: 0 },
            ],
            exec: 'generalBrowser',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],   // 95% 요청이 500ms 이내
        http_req_failed: ['rate<0.01'],      // 에러율 1% 미만
    },
};

// ─── 공통 헤더 ───
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

// ─── 시나리오 1: 피드 탐색 유저 (60%) ───
export function feedBrowser() {
    const headers = authHeaders();

    group('피드 목록 조회', () => {
        const res = http.get(`${BASE_URL}/feeds?page=0&size=20`, headers);
        check(res, { 'feed list 200': (r) => r.status === 200 });
    });

    sleep(2);

    group('피드 상세 조회', () => {
        const feedId = randomInt(1, 100);
        const res = http.get(`${BASE_URL}/feeds/${feedId}`, headers);
        check(res, { 'feed detail 200': (r) => r.status === 200 });
    });

    sleep(3);

    group('피드 다음 페이지', () => {
        const page = randomInt(1, 5);
        const res = http.get(`${BASE_URL}/feeds?page=${page}&size=20`, headers);
        check(res, { 'feed next page 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('인기 피드 조회', () => {
        const res = http.get(`${BASE_URL}/feeds/hot?page=0&size=20`, headers);
        check(res, { 'hot feed 200': (r) => r.status === 200 });
    });

    sleep(1);
}

// ─── 시나리오 2: 활동적 유저 (20%) ───
export function activeUser() {
    const headers = authHeaders();

    group('피드 조회', () => {
        const res = http.get(`${BASE_URL}/feeds?page=0&size=20`, headers);
        check(res, { 'feed list 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('피드 좋아요', () => {
        const feedId = randomInt(1, 100);
        const res = http.post(`${BASE_URL}/feeds/${feedId}/reaction`, JSON.stringify({
            reactionType: 'LIKE',
        }), headers);
        check(res, { 'like success': (r) => r.status === 200 || r.status === 201 });
    });

    sleep(2);

    group('댓글 작성', () => {
        const feedId = randomInt(1, 100);
        const res = http.post(`${BASE_URL}/feeds/${feedId}/comments`, JSON.stringify({
            content: `k6 부하테스트 댓글 ${Date.now()}`,
        }), headers);
        check(res, { 'comment created': (r) => r.status === 200 || r.status === 201 });
    });

    sleep(1);

    group('팔로잉 피드 조회', () => {
        const res = http.get(`${BASE_URL}/feeds/following?page=0&size=20`, headers);
        check(res, { 'following feed 200': (r) => r.status === 200 });
    });

    sleep(1);
}

// ─── 시나리오 3: 상품 탐색 유저 (10%) ───
export function productBrowser() {
    const headers = authHeaders();

    group('상품 목록 조회', () => {
        const res = http.get(`${BASE_URL}/products?page=0&size=20`, headers);
        check(res, { 'product list 200': (r) => r.status === 200 });
    });

    sleep(2);

    group('상품 상세 조회', () => {
        const productId = randomInt(1, 50);
        const res = http.get(`${BASE_URL}/products/${productId}`, headers);
        check(res, { 'product detail 200': (r) => r.status === 200 });
    });

    sleep(3);

    group('상품 검색', () => {
        const keywords = ['맥북', '아이폰', '갤럭시', '에어팟', '키보드'];
        const keyword = keywords[randomInt(0, keywords.length - 1)];
        const res = http.get(`${BASE_URL}/products?keyword=${encodeURIComponent(keyword)}`, headers);
        check(res, { 'product search 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('상품 리뷰 조회', () => {
        const productId = randomInt(1, 50);
        const res = http.get(`${BASE_URL}/products/${productId}/reviews?page=0&size=10`, headers);
        check(res, { 'product review 200': (r) => r.status === 200 });
    });

    sleep(1);
}

// ─── 시나리오 4: 배틀 참여 유저 (5%) ───
export function battleVoter() {
    const headers = authHeaders();

    group('배틀 목록 조회', () => {
        const res = http.get(`${BASE_URL}/battles?page=0&size=20`, headers);
        check(res, { 'battle list 200': (r) => r.status === 200 });
    });

    sleep(2);

    group('인기 배틀 조회', () => {
        const res = http.get(`${BASE_URL}/battles/hot?page=0&size=10`, headers);
        check(res, { 'hot battle 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('배틀 상세 조회', () => {
        const battleId = randomInt(1, 20);
        const res = http.get(`${BASE_URL}/battles/${battleId}`, headers);
        check(res, { 'battle detail 200': (r) => r.status === 200 });
    });

    sleep(2);

    group('배틀 투표', () => {
        const battleId = randomInt(1, 20);
        const itemId = randomInt(1, 10);
        const res = http.post(`${BASE_URL}/battles/${battleId}/items/${itemId}/vote`, null, headers);
        check(res, { 'vote success': (r) => r.status === 200 || r.status === 201 });
    });

    sleep(1);
}

// ─── 시나리오 5: 종합 탐색 유저 (5%) ───
export function generalBrowser() {
    const headers = authHeaders();

    group('카테고리 목록 조회', () => {
        const res = http.get(`${BASE_URL}/categories`, headers);
        check(res, { 'category list 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('인기 카테고리 조회', () => {
        const res = http.get(`${BASE_URL}/categories/popular`, headers);
        check(res, { 'popular category 200': (r) => r.status === 200 });
    });

    sleep(2);

    group('인기 해시태그 조회', () => {
        const res = http.get(`${BASE_URL}/hashtags/hot`, headers);
        check(res, { 'hot hashtag 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('대시보드 조회', () => {
        const res = http.get(`${BASE_URL}/dashboard/summary`, headers);
        check(res, { 'dashboard 200': (r) => r.status === 200 });
    });

    sleep(2);

    group('내 캐리어 조회', () => {
        const res = http.get(`${BASE_URL}/carriers/my`, headers);
        check(res, { 'my carriers 200': (r) => r.status === 200 });
    });

    sleep(1);
}
