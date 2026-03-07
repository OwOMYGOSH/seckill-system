import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

// 🔹 建立自定義計數器
const SuccessCounter = new Counter('success_seckills');

// 🔹 設定模擬參數
export const options = {
  scenarios: {
    seckill_rush: {
      executor: 'per-vu-iterations',
      vus: 100,           // 模擬 100 個不同的虛擬用戶 (Virtual Users)
      iterations: 1,      // 每個用戶只會嘗試搶購一次 (符合秒殺場景)
      maxDuration: '30s',
    },
  },
};

export default function () {
  // 🔹 userId 必須是純數字 (Java Long)，加上隨機偏移量防止重複購買
  const userId = __VU + Math.floor(Math.random() * 1000000);
  const productId = 52; 

  // 🔹 模擬不同的 IP，讓 RateLimiterFilter 放行
  const params = {
    headers: {
      'X-Forwarded-For': `192.168.1.${__VU}`,
    },
  };

  // 🔹 執行 POST 請求
  const res = http.post(`http://localhost:8080/api/seckill?userId=${userId}&productId=${productId}`, null, params);

  // 🔹 增加更詳細的 Console Log 來診斷
  if (res.status === 200) {
    SuccessCounter.add(1); 
    console.log(`🎉 [虛擬用戶 ${__VU}] 搶購成功！(userId: ${userId})`);
  } else if (res.status === 400) {
    console.log(`❌ [虛擬用戶 ${__VU}] 搶購失敗: ${res.body}`);
  } else if (res.status === 429) {
    console.log(`🚫 [虛擬用戶 ${__VU}] 被限流了！(429 Too Many Requests)`);
  } else {
    console.log(`⚠️ [虛擬用戶 ${__VU}] 收到非預期的狀態碼 ${res.status}: ${res.body}`);
  }

  check(res, {
    'status is 200 or 400': (r) => r.status === 200 || r.status === 400,
  });

  // 每個用戶跑完就休息一下，減少負擔
  sleep(0.1);
}
