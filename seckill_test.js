import http from 'k6/http';
import { check, sleep } from 'k6';

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
  // 🔹 用 __VU 這個變數來模擬不同的 userId
  const userId = __VU;
  const productId = 1; // 假設你初始化 iPhone 17 Pro 的 ID 是 1

  // 🔹 執行 POST 請求
  const res = http.post(`http://localhost:8080/api/seckill?userId=${userId}&productId=${productId}`);

  // 🔹 檢查是否搶購成功或報出售罄 (這些都是合理的反應)
  check(res, {
    'status is 200 or 400': (r) => r.status === 200 || r.status === 400,
  });

  if (res.status === 400) {
    console.log(`[虛擬用戶 ${userId}] 搶購失敗: ${res.body}`);
  } else if (res.status === 200) {
    console.log(`🎉 [虛擬用戶 ${userId}] 搶購成功！`);
  }

  // 每個用戶跑完就休息一下，減少負擔
  sleep(0.1);
}
