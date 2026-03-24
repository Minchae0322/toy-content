import http from 'k6/http';
import { check, sleep, group } from 'k6';

const TOKEN = __ENV.TOKEN || '';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';

if (!TOKEN) {
    console.warn('⚠ TOKEN이 설정되지 않았습니다. -e TOKEN=xxx 옵션으로 전달하세요.');
}

// ─── 시나리오별 동접 비율 (총 300 VU, 5분) ───
export const options = {
    scenarios: {
        feed_browser: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 180 },
                { duration: '3m', target: 180 },
                { duration: '1m', target: 0 },
            ],
            exec: 'feedBrowser',
        },
        active_user: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 60 },
                { duration: '3m', target: 60 },
                { duration: '1m', target: 0 },
            ],
            exec: 'activeUser',
        },
        product_browser: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 30 },
                { duration: '3m', target: 30 },
                { duration: '1m', target: 0 },
            ],
            exec: 'productBrowser',
        },
        battle_voter: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 15 },
                { duration: '3m', target: 15 },
                { duration: '1m', target: 0 },
            ],
            exec: 'battleVoter',
        },
        general_browser: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 15 },
                { duration: '3m', target: 15 },
                { duration: '1m', target: 0 },
            ],
            exec: 'generalBrowser',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

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

function extractIds(res, idField = 'id') {
    if (res.status !== 200) return [];
    try {
        const body = res.json();
        const list = body.data?.content || body.data?.feeds || body.data || body.content || [];
        if (Array.isArray(list)) {
            return list.map(item => item[idField]).filter(Boolean);
        }
    } catch (e) {}
    return [];
}

function pickRandom(arr) {
    if (!arr || arr.length === 0) return null;
    return arr[randomInt(0, arr.length - 1)];
}

export function feedBrowser() {
    const headers = authHeaders();

    let feedIds = [];
    let nextCursor = null;

    group('피드 목록 조회', () => {
        const res = http.get(`${BASE_URL}/feeds/scroll?size=20&isActive=true`, headers);
        check(res, { 'feed list 200': (r) => r.status === 200 });
        feedIds = extractIds(res);
        if (res.status === 200) {
            try {
                const body = res.json();
                nextCursor = body.data?.nextCursor || body.nextCursor;
            } catch (e) {}
        }
    });

    sleep(2);

    group('피드 상세 조회', () => {
        const feedId = pickRandom(feedIds) || randomInt(1, 10);
        const res = http.get(`${BASE_URL}/feeds/${feedId}`, headers);
        check(res, { 'feed detail 200': (r) => r.status === 200 });
    });

    sleep(3);

    group('피드 다음 페이지', () => {
        let url = `${BASE_URL}/feeds/scroll?size=20&isActive=true`;
        if (nextCursor) {
            url += `&cursor=${nextCursor}`;
        }
        const res = http.get(url, headers);
        check(res, { 'feed next page 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('인기 피드 조회', () => {
        const res = http.get(`${BASE_URL}/feeds/hot?page=0&size=20`, headers);
        check(res, { 'hot feed 200': (r) => r.status === 200 });
    });

    sleep(1);
}

export function activeUser() {
    const headers = authHeaders();

    let feedIds = [];
    group('피드 조회', () => {
        const res = http.get(`${BASE_URL}/feeds/scroll?size=20&isActive=true`, headers);
        check(res, { 'feed list 200': (r) => r.status === 200 });
        feedIds = extractIds(res);
    });

    sleep(1);

    group('피드 상세 조회', () => {
        const feedId = pickRandom(feedIds) || randomInt(1, 10);
        const res = http.get(`${BASE_URL}/feeds/${feedId}`, headers);
        check(res, { 'feed detail 200': (r) => r.status === 200 });
    });

    sleep(2);

    group('팔로잉 피드 조회', () => {
        const res = http.get(`${BASE_URL}/feeds/following?size=20`, headers);
        check(res, { 'following feed 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('인기 피드 조회', () => {
        const res = http.get(`${BASE_URL}/feeds/hot?page=0&size=20`, headers);
        check(res, { 'hot feed 200': (r) => r.status === 200 });
    });

    sleep(1);
}

export function productBrowser() {
    const headers = authHeaders();

    let productIds = [];
    group('상품 목록 조회', () => {
        const res = http.get(`${BASE_URL}/products?page=0&size=10`, headers);
        check(res, { 'product list 200': (r) => r.status === 200 });
        productIds = extractIds(res);
    });

    sleep(2);

    group('상품 상세 조회', () => {
        const productId = pickRandom(productIds) || randomInt(1, 10);
        const res = http.get(`${BASE_URL}/products/${productId}`, headers);
        check(res, { 'product detail 200': (r) => r.status === 200 });
    });

    sleep(3);

    group('상품 검색', () => {
        const keywords = ['요거트', '그릭', '딸기', '블루베리', '프로틴', '플레인', '바닐라'];
        const keyword = keywords[randomInt(0, keywords.length - 1)];
        const res = http.get(`${BASE_URL}/products?keyword=${encodeURIComponent(keyword)}&page=0&size=10`, headers);
        check(res, { 'product search 200': (r) => r.status === 200 });
    });

    sleep(1);

    /*group('상품 리뷰 조회', () => {
        const productId = pickRandom(productIds) || randomInt(1, 10);
        const res = http.get(`${BASE_URL}/products/${productId}/reviews?page=0&size=10`, headers);
        check(res, { 'product review 200': (r) => r.status === 200 });
    });*/

    sleep(1);
}

export function battleVoter() {
    const headers = authHeaders();

    let battleIds = [];
    group('배틀 목록 조회', () => {
        const res = http.get(`${BASE_URL}/battles?page=0&size=20`, headers);
        check(res, { 'battle list 200': (r) => r.status === 200 });
        battleIds = extractIds(res);
    });

    sleep(2);

    group('인기 배틀 조회', () => {
        const res = http.get(`${BASE_URL}/battles/hot?page=0&size=3&sort=hotScore,desc`, headers);
        check(res, { 'hot battle 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('배틀 상세 조회', () => {
        const battleId = pickRandom(battleIds) || randomInt(1, 7);
        const res = http.get(`${BASE_URL}/battles/${battleId}`, headers);
        check(res, { 'battle detail 200': (r) => r.status === 200 });
    });

    sleep(2);
}

export function generalBrowser() {
    const headers = authHeaders();

    group('카테고리 목록 조회', () => {
        const res = http.get(`${BASE_URL}/categories/list`, headers);
        check(res, { 'category list 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('인기 해시태그 조회', () => {
        const res = http.get(`${BASE_URL}/hashtags/hot?size=10&sort=usageCount,DESC`, headers);
        check(res, { 'hot hashtag 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('대시보드 조회', () => {
        const res = http.get(`${BASE_URL}/dashboard/summary`, headers);
        check(res, { 'dashboard 200': (r) => r.status === 200 });
    });

    sleep(2);

    group('대시보드 인기 피드', () => {
        const res = http.get(`${BASE_URL}/feeds/hot?page=0&size=5`, headers);
        check(res, { 'dashboard hot feeds 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('내 캐리어 조회', () => {
        const res = http.get(`${BASE_URL}/carriers/me`, headers);
        check(res, { 'my carriers 200': (r) => r.status === 200 });
    });

    sleep(1);
}