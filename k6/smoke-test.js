import http from 'k6/http';
import { check, sleep, group } from 'k6';

// k6 run -e TOKEN=eyJhbG... -e BASE_URL=https://yogurtte.com/api k6/smoke-test.js
const TOKEN = __ENV.TOKEN || '';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082/api';

export const options = {
    vus: 1,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        http_req_failed: ['rate<0.05'],
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

export default function () {
    const headers = authHeaders();

    group('피드 목록 조회', () => {
        const res = http.get(`${BASE_URL}/feeds?page=0&size=20`, headers);
        check(res, { 'feed list 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('피드 상세 조회', () => {
        const feedId = randomInt(1, 100);
        const res = http.get(`${BASE_URL}/feeds/${feedId}`, headers);
        check(res, { 'feed detail 2xx': (r) => r.status >= 200 && r.status < 300 });
    });

    sleep(1);

    group('인기 피드 조회', () => {
        const res = http.get(`${BASE_URL}/feeds/hot?page=0&size=20`, headers);
        check(res, { 'hot feed 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('배틀 목록 조회', () => {
        const res = http.get(`${BASE_URL}/battles?page=0&size=20`, headers);
        check(res, { 'battle list 200': (r) => r.status === 200 });
    });

    sleep(1);

    group('카테고리 목록 조회', () => {
        const res = http.get(`${BASE_URL}/categories`, headers);
        check(res, { 'category list 200': (r) => r.status === 200 });
    });

    sleep(1);
}
