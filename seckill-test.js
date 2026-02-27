import http from 'k6/http';
import { check, sleep } from 'k6';

// 測試配置
export const options = {
    vus: 50,          // 50 個同時線上的虛擬使用者
    duration: '10s',  // 壓力測試持續 10 秒
};

export default function () {
    // 模擬隨機的使用者 ID (1000 ~ 9999)
    const userId = Math.floor(Math.random() * 9000) + 1000;
    const productId = 1; // 搶購 iPhone 17 Pro

    const url = `http://localhost:8080/api/seckill?userId=${userId}&productId=${productId}`;
    
    // 發送 POST 請求
    const res = http.post(url);

    // 驗證回傳狀態
    check(res, {
        '成功搶購 (200)': (r) => r.status === 200,
        '庫存售罄或重複 (400)': (r) => r.status === 400,
    });

    // 每個虛擬使用者每次請求後休息 0.1 秒
    sleep(0.1);
}
