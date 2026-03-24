import http from 'k6/http';
import { check, sleep } from 'k6';

const TOKEN = __ENV.TOKEN || '';
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const ENDPOINT = __ENV.ENDPOINT || '/feeds/scroll?size=20&isActive=true';

export const options = {
    vus: 300,
    duration: '1m',
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

export default function () {
    const res = http.get(`${BASE_URL}${ENDPOINT}`, authHeaders());
    check(res, { 'status 200': (r) => r.status === 200 });
    sleep(1);
}