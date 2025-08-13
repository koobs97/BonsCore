<template>
  <div class="component-wrapper">
    <el-input
        v-model="searchQuery"
        placeholder="검색어를 입력해 주세요"
        size="large"
        class="search-input"
        :prefix-icon="Search"
        clearable
    />
    <el-card shadow="never" class="content-card">
      <div class="info-popover-trigger-area">
        <el-popover
            placement="right-start"
            :width="420"
            v-model:visible="isPopoverVisible"
            popper-class="congestion-guide-popper"
        >
          <template #reference>
            <el-button :icon="InfoFilled" circle class="info-button" @click="isPopoverVisible = true"/>
          </template>
          <div class="guide-content">
            <h4 class="guide-title">혼잡도 안내</h4>
            <ul class="guide-list">
              <li v-for="item in congestionGuide" :key="item.level" class="guide-item">
                <span class="guide-level" :class="item.colorClass">
                  {{ item.emoji }} {{ item.level }}
                </span>
                <p class="guide-message">{{ item.message }}</p>
              </li>
            </ul>
          </div>
        </el-popover>
      </div>
      <div class="store-info">
        <h1 class="store-name">{{ analysisData.storeName }}</h1>
        <p class="store-address">{{ analysisData.address }}</p>
      </div>
      <div class="gauge-wrapper">
        <svg class="gauge-svg" viewBox="0 0 120 120">
          <defs>
            <linearGradient id="free-gradient" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" style="stop-color:#6EE7B7;"/><stop offset="100%" style="stop-color:#34D399;"/></linearGradient>
            <linearGradient id="normal-gradient" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" style="stop-color:#FDE047;"/><stop offset="100%" style="stop-color:#FACC15;"/></linearGradient>
            <linearGradient id="crowded-gradient" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" style="stop-color:#FDBA74;"/><stop offset="100%" style="stop-color:#FB923C;"/></linearGradient>
            <linearGradient id="very-crowded-gradient" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" style="stop-color:#FCA5A5;"/><stop offset="100%" style="stop-color:#F87171;"/></linearGradient>
          </defs>
          <circle class="gauge-track" cx="60" cy="60" r="54"/>
          <circle class="gauge-bar" cx="60" cy="60" r="54" :stroke="gaugeGradientUrl" :stroke-dasharray="circumference" :stroke-dashoffset="gaugeOffset"/>
        </svg>
        <div class="gauge-content">
          <div class="gauge-score">{{ analysisData.score === 1 ? '?' : analysisData.score }}</div>
          <div class="gauge-label" :class="textColorClass">{{ statusText }}</div>
        </div>
      </div>
      <div class="ai-comment">
        <div class="ai-icon-wrapper"><i class="fas fa-robot"></i></div>
        <p>"{{ analysisData.aiComment }}"</p>
      </div>
      <div class="details-section">
        <div class="detail-item"><i class="fas fa-clock detail-icon"></i><span class="detail-label">예상 대기</span><span class="detail-value">{{ analysisData.estimatedWaitTime }}</span></div>
        <div class="detail-item"><i class="fas fa-users detail-icon"></i><span class="detail-label">실시간 인원</span><span class="detail-value">{{ analysisData.liveCrowdStatus }}</span></div>
        <div class="detail-item"><i class="fas fa-cloud-sun-rain detail-icon"></i><span class="detail-label">현재 날씨</span><span class="detail-value">{{ analysisData.weather }}</span></div>
        <div class="detail-item"><i class="fas fa-calendar-day detail-icon"></i><span class="detail-label">분석 시간</span><span class="detail-value">{{ analysisData.analysisTime }}</span></div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
// Script 부분은 변경사항 없습니다.
import { ref, computed, onMounted } from 'vue';
import { Search, InfoFilled } from "@element-plus/icons-vue";

const isPopoverVisible = ref(false);
onMounted(() => { setTimeout(() => { isPopoverVisible.value = true; }, 300); });
const analysisData = ref({ storeName: "[가게 이름]", address: "[가게 주소]", score: 1, aiComment: "혼잡도 안내 메시지", estimatedWaitTime: "[예상 대기 시간]", liveCrowdStatus: "[인원 수치]", weather: "[날씨, 온도]", analysisTime: "[기준 시간]" });
const searchQuery = ref('');
const congestionGuide = [ { level: "매우 혼잡", emoji: "🌋", message: "웨이팅이 매우 길 것으로 예상돼요. 원격 줄서기나 다른 가게를 추천해요.", colorClass: "text-very-crowded" }, { level: "혼잡", emoji: "🔴", message: "웨이팅이 있을 가능성이 높아요. 방문에 참고하세요.", colorClass: "text-crowded" }, { level: "보통", emoji: "🟡", message: "붐비기 시작하는 시간이네요. 약간의 대기가 있을 수 있어요.", colorClass: "text-normal" }, { level: "여유", emoji: "🟢", message: "아직은 여유로운 편이에요. 지금 방문하면 좋을 것 같아요.", colorClass: "text-free" }, { level: "한산", emoji: "🔵", message: "매우 한산해요! 기다림 없이 바로 즐길 수 있어요.", colorClass: "text-free" }, { level: "휴무일", emoji: "💤", message: "오늘은 가게가 쉬는 날이에요.", colorClass: "text-secondary" }, ];
const circumference = 2 * Math.PI * 54;
const gaugeOffset = computed(() => (circumference * (1 - analysisData.value.score / 100)));
const statusText = computed(() => { const score = analysisData.value.score; if (score > 80) return "매우 혼잡"; if (score > 60) return "혼잡"; if (score > 40) return "보통"; if (score > 20) return "여유"; if (score <= 1) return "[혼잡도]"; return "한산"; });
const gaugeGradientUrl = computed(() => { const score = analysisData.value.score; if (score > 80) return "url(#very-crowded-gradient)"; if (score > 60) return "url(#crowded-gradient)"; if (score > 40) return "url(#normal-gradient)"; return "url(#free-gradient)"; });
const textColorClass = computed(() => { const score = analysisData.value.score; if (score > 80) return "text-very-crowded"; if (score > 60) return "text-crowded"; if (score > 40) return "text-normal"; if (score <= 1) return "text-secondary"; return "text-free"; });
</script>

<style scoped>
/* Font Awesome 및 CSS 변수는 변경사항 없습니다. */
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css');
:root { --card-bg: #ffffff; --text-primary: #2c3e50; --text-secondary: #7f8c8d; --border-color: #eef0f5; --accent-color: #5b21b6; --color-free: #34D399; --color-normal: #FACC15; --color-crowded: #FB923C; --color-very-crowded: #F87171; }

/* ================== [수정된 스타일] ================== */

/* [고정] 이 영역의 높이는 변경하지 않습니다. */
.component-wrapper { width: 100%; height: 500px; display: flex; flex-direction: column; gap: 16px; }
.content-card { --el-card-padding: 0; flex-grow: 1; /* 남는 공간을 모두 차지하도록 설정 */ }
.content-card :deep(.el-card__body) {
  position: relative; padding: 24px 20px; display: flex; flex-direction: column;
  align-items: center; justify-content: space-between;
  height: 100%; /* 부모 높이를 100% 사용 */
  box-sizing: border-box;
}
/* ====================================================== */

.info-popover-trigger-area { position: absolute; top: 16px; right: 16px; z-index: 10; }
.info-button { width: 28px; height: 28px; }
.store-info { width: 100%; text-align: center; flex-shrink: 0; /* 높이가 줄어들지 않도록 설정 */ }
.store-name { font-size: 24px; font-weight: 700; margin: 0; }
.store-address { font-size: 14px; color: var(--text-secondary); margin-top: 6px; }

/* [수정] 게이지 확대 */
.gauge-wrapper {
  position: relative;
  width: 170px; /* 150px -> 170px */
  height: 170px; /* 150px -> 170px */
  display: flex; justify-content: center; align-items: center;
  flex-shrink: 0;
}
.gauge-svg { position: absolute; width: 100%; height: 100%; transform: rotate(-90deg); }
.gauge-track, .gauge-bar { fill: none; stroke-width: 12; }
.gauge-track { stroke: #e5e7eb; }
.gauge-bar { stroke-linecap: round; transition: stroke-dashoffset 1.5s cubic-bezier(0.25, 0.46, 0.45, 0.94); }
.gauge-content { position: relative; z-index: 1; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; }
.gauge-score {
  font-size: 48px; /* 42px -> 48px */
  font-weight: 800; line-height: 1;
}
.gauge-label {
  font-size: 17px; /* 16px -> 17px */
  font-weight: 600; margin-top: 8px; transition: color 0.5s;
}

.text-free { color: var(--color-free); }
.text-normal { color: var(--color-normal); }
.text-crowded { color: var(--color-crowded); }
.text-very-crowded { color: var(--color-very-crowded); }
.text-secondary { color: var(--text-secondary); }

.ai-comment {
  display: flex; align-items: center; gap: 12px; padding: 8px 16px; /* 세로 패딩 축소 */
  font-size: 13px; /* 폰트 축소 */ font-weight: 500; text-align: center;
  max-width: 340px; justify-content: center; flex-shrink: 0;
}
.ai-icon-wrapper {
  flex-shrink: 0; width: 28px; height: 28px; /* 아이콘 크기 축소 */
  border-radius: 50%; background-image: linear-gradient(135deg, #a855f7, #6d28d9);
  color: white; display: flex; align-items: center; justify-content: center; font-size: 14px;
}

/* [수정] 세부 정보 축소 */
.details-section {
  display: flex; justify-content: space-around; width: 100%;
  padding-top: 12px; /* 상단 여백 축소 */
  border-top: 1px solid var(--border-color); flex-shrink: 0;
}
.detail-item {
  display: flex; flex-direction: column; align-items: center;
  gap: 2px; /* 내부 간격 대폭 축소 */
  transition: transform 0.2s ease-in-out;
}
.detail-item:hover { transform: translateY(-3px); }
.detail-icon {
  font-size: 18px; /* 아이콘 크기 축소 */
  color: var(--accent-color); margin-bottom: 2px;
}
.detail-label {
  font-size: 11px; /* 라벨 크기 대폭 축소 */
  color: var(--text-secondary);
}
.detail-value {
  font-size: 13px; /* 값 크기 축소 */
  font-weight: 600;
}
</style>

<!-- Popover 전역 스타일 (변경 없음) -->
<style>
.congestion-guide-popper { border-radius: 12px !important; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1) !important; border: 1px solid #f0f0f0 !important; padding: 12px !important; }
.congestion-guide-popper .guide-content { font-family: 'Pretendard', sans-serif; }
.congestion-guide-popper .guide-title { font-size: 16px; font-weight: 600; color: #2c3e50; margin: 0 0 12px 4px; }
.congestion-guide-popper .guide-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 12px; }
.congestion-guide-popper .guide-item { display: flex; flex-direction: column; gap: 4px; }
.congestion-guide-popper .guide-level { font-size: 14px; font-weight: 600; }
.congestion-guide-popper .guide-message { font-size: 13px; color: #576574; margin: 0; line-height: 1.5; }
</style>