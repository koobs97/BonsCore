<template>
  <div class="estimator-container">
    <div class="card">
      <div class="card-header">
        <h1 class="title">웨이팅 지수 분석기 📈</h1>
        <p class="subtitle">가게의 오늘 웨이팅 지수를 예측해 드립니다.</p>
      </div>

      <!-- 1. 초기 검색 단계 (수정됨) -->
      <div v-if="step === 'search'" class="card-body search-step-body">
        <div class="search-form">
          <input
              type="text"
              v-model="searchQuery"
              placeholder="가게 이름을 입력하세요 (예: 런던베이글)"
              @keyup.enter="searchStores"
          />
          <button
              @click="searchStores"
              :disabled="!searchQuery"
              :class="{ 'is-disabled': !searchQuery }"
          >
            분석 시작
          </button>
        </div>

        <!-- 아래에 추가된 정보 섹션 -->
        <div class="info-section">
          <div class="info-block">
            <h3 class="info-title">이런 가게는 어때요? ✨</h3>
            <ul class="example-list">
              <li>#런던베이글뮤지엄</li>
              <li>#카멜커피</li>
              <li>#다운타우너</li>
              <li>#노티드도넛</li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 2. 지점 선택 단계 -->
      <div v-if="step === 'selectStore'" class="card-body">
        <h2 class="step-title">어느 지점의 웨이팅이 궁금하세요?</h2>
        <ul class="store-list">
          <li v-for="store in foundStores" :key="store.id" @click="selectStore(store)">
            {{ store.name }}
            <span>{{ store.address }}</span>
          </li>
        </ul>
        <button class="back-button" @click="reset">처음으로</button>
      </div>

      <!-- 2.5. ★★★ 방문 시간 선택 단계 (새로 추가) ★★★ -->
      <div v-if="step === 'selectTime'" class="card-body">
        <h2 class="step-title">방문 예정 시간을 선택해주세요.</h2>
        <div class="time-slots">
          <button
              v-for="time in timeSlots"
              :key="time.value"
              class="time-slot-btn"
              :class="{ active: selectedTime === time.value }"
              @click="selectTimeSlot(time.value)"
          >
            {{ time.label }}
          </button>
        </div>
        <div class="button-group">
          <button class="back-button" @click="step = 'selectStore'">지점 다시 선택</button>
          <button @click="confirmTimeAndAnalyze" :disabled="!selectedTime">분석하기</button>
        </div>
      </div>

      <!-- 3. 데이터 분석 중 (로딩) 단계 -->
      <div v-if="step === 'loading'" class="card-body loading-state">
        <div class="spinner"></div>
        <h2 class="step-title">{{ selectedStore.name }} 분석 중...</h2>
        <p class="loading-message">잠시만 기다려주세요. 실시간 데이터를 수집하고 있습니다.</p>
        <div class="progress-list">
          <p :class="{ done: progress.weather }">기상청 날씨 정보 수집</p>
          <p :class="{ done: progress.reviews }">네이버 리뷰 및 인지도 분석</p>
          <p :class="{ done: progress.sns }">실시간 SNS 언급량 확인</p>
          <p :class="{ done: progress.map }">지도 앱 혼잡도 데이터 크롤링</p>
        </div>
      </div>

      <!-- 4. 결과 표시 단계 (대대적 개선) -->
      <div v-if="step === 'result'" class="card-body result-state">
        <!-- 상단 요약 결과 -->
        <div class="result-summary">
          <span class="result-emoji">{{ result.emoji }}</span>
          <div class="result-text">
            <h2 class="result-index">{{ selectedStore.name }}은(는) 현재 <span :class="result.waitingIndex">{{ result.waitingIndex }}</span></h2>
            <p class="result-message">{{ result.message }}</p>
          </div>
        </div>

        <!-- 상세 점수 분석 (스크롤 영역) -->
        <div class="score-details">
          <h3 class="details-title">상세 점수 분석</h3>
          <ul class="details-list">
            <li v-for="(detail, index) in scoreDetails" :key="index">
              <span class="factor">{{ detail.factor }}</span>
              <span class="condition">{{ detail.condition }}</span>
              <span class="score" :class="{ positive: detail.score > 0, negative: detail.score < 0 }">
                {{ detail.score > 0 ? '+' : '' }}{{ detail.score }}
              </span>
            </li>
          </ul>
        </div>

        <!-- 최종 점수 및 리셋 버튼 -->
        <div class="result-footer">
          <div class="total-score">
            <span class="factor">최종 웨이팅 점수</span>
            <span class="score">{{ result.totalScore }}</span>
          </div>
          <button class="reset-button" @click="reset">새로운 가게 분석하기</button>
        </div>
      </div>

      <!-- 휴무일 예외 처리 -->
      <div v-if="step === 'closed'" class="card-body closed-state">
        <span class="result-emoji">💤</span>
        <h2 class="result-index">오늘은 휴무일입니다</h2>
        <p class="result-message">선택하신 {{ selectedStore.name }}은(는) 오늘 영업하지 않아요.</p>
        <button class="reset-button" @click="reset">다른 가게 분석하기</button>
      </div>
    </div>

<!--    <footer class="footer">-->
<!--      <p>본 분석 결과는 참고용이며, 실제 웨이팅 상황과 다를 수 있습니다.</p>-->
<!--      <p class="copyright">© 2024 AI Waiting Analyzer. All rights reserved.</p>-->
<!--    </footer>-->
  </div>
</template>

<script setup>
// 스크립트 부분은 수정 없이 그대로 사용합니다.
import { ref } from 'vue';

const step = ref('search');
const searchQuery = ref('');
const foundStores = ref([]);
const selectedStore = ref(null);
const result = ref(null);
const scoreDetails = ref([]);
const progress = ref({
  weather: false,
  reviews: false,
  sns: false,
  map: false
});

// ★★★ 방문 시간 선택 관련 ref 추가 ★★★
const selectedTime = ref(null);
const timeSlots = ref([
  { label: '평일 점심 (12-14시)', value: 'weekdayLunch' },
  { label: '평일 저녁 (18-20시)', value: 'weekdayDinner' },
  { label: '주말 점심 (12-14시)', value: 'weekendLunch' },
  { label: '주말 저녁 (18-20시)', value: 'weekendDinner' },
  { label: '애매한 시간 (15-17시)', value: 'offPeak' },
  { label: '기타 시간', value: 'etc' },
]);

const searchStores = () => {
  if (!searchQuery.value) return;
  foundStores.value = [
    { id: 1, name: `${searchQuery.value} 강남점`, address: '서울 강남구' },
    { id: 2, name: `${searchQuery.value} 홍대점`, address: '서울 마포구' },
    { id: 3, name: `${searchQuery.value} 잠실점`, address: '서울 송파구' },
  ];
  step.value = 'selectStore';
};

// ★★★ 지점 선택 함수 수정 ★★★
const selectStore = (store) => {
  selectedStore.value = store;
  selectedTime.value = null; // 시간 선택 초기화
  step.value = 'selectTime'; // 로딩 대신 시간 선택 단계로 이동
};

// ★★★ 시간 선택 관련 함수들 추가 ★★★
const selectTimeSlot = (timeValue) => {
  selectedTime.value = timeValue;
};

const confirmTimeAndAnalyze = () => {
  if (!selectedTime.value) return;
  step.value = 'loading';
  startAnalysis();
}

const startAnalysis = () => {
  Object.keys(progress.value).forEach(k => progress.value[k] = false);
  setTimeout(() => progress.value.weather = true, 300);
  setTimeout(() => progress.value.reviews = true, 1000);
  setTimeout(() => progress.value.sns = true, 1800);
  setTimeout(() => progress.value.map = true, 2500);
  setTimeout(() => {
    calculateScore();
  }, 3000);
};

const calculateScore = () => {
  let totalScore = 0;
  const details = [];

  if (Math.random() < 0.05) {
    step.value = 'closed';
    return;
  }

  const timeFactors = [ { condition: '금요일 저녁 (18-20시)', score: 30 }, { condition: '평일 저녁 (18-20시)', score: 20 }, { condition: '주말 점심 (12-14시)', score: 20 }, { condition: '평일 점심 (12-13시)', score: 15 }, { condition: '애매한 시간 (15-17시)', score: -10 }, ];
  const timeFactor = timeFactors[Math.floor(Math.random() * timeFactors.length)];
  details.push({ factor: '시간/요일', ...timeFactor });
  totalScore += timeFactor.score;

  const reviewCount = Math.floor(Math.random() * 2000);
  let reviewScore = 0;
  if (reviewCount > 1000) reviewScore = 15;
  else if (reviewCount > 500) reviewScore = 10;
  else if (reviewCount > 100) reviewScore = 5;
  if (reviewScore > 0) {
    details.push({ factor: '인지도(리뷰 수)', condition: `리뷰 ${reviewCount}개`, score: reviewScore });
    totalScore += reviewScore;
  }

  const rating = (Math.random() * 1.5 + 3.5).toFixed(1);
  const ratingScore = rating >= 4.2 ? 10 : -5;
  details.push({ factor: '만족도(별점)', condition: `네이버 별점 ${rating}`, score: ratingScore });
  totalScore += ratingScore;

  const weatherFactors = [ { condition: '폭우, 폭설, 폭염', score: -15 }, { condition: '맑고 쾌적한 날씨', score: 5 }, { condition: '흐림/구름 많음', score: 0 }, ];
  const weatherFactor = weatherFactors[Math.floor(Math.random() * weatherFactors.length)];
  if(weatherFactor.score !== 0) {
    details.push({ factor: '현재 날씨', ...weatherFactor });
    totalScore += weatherFactor.score;
  }

  const mapFactors = [ { condition: '평소보다 매우 붐빔', score: 30 }, { condition: '평소보다 약간 붐빔', score: 15 }, { condition: '평소와 비슷함', score: 0 }, { condition: '한산함', score: -20 }, ];
  const mapFactor = mapFactors[Math.floor(Math.random() * mapFactors.length)];
  if(mapFactor.score !== 0) {
    details.push({ factor: '지도 앱 혼잡도', ...mapFactor });
    totalScore += mapFactor.score;
  }

  if (Math.random() > 0.6) {
    const snsScore = 10;
    details.push({ factor: '실시간 SNS', condition: '최근 1시간 내 언급', score: snsScore });
    totalScore += snsScore;
  }

  scoreDetails.value = details;
  generateFinalResult(totalScore);
};

const generateFinalResult = (totalScore) => {
  let waitingIndex = '';
  let message = '';
  let emoji = '';

  if (totalScore >= 70) { // 점수 구간 조정
    waitingIndex = '매우 혼잡';
    emoji = '🌋';
    message = '웨이팅이 매우 길 것으로 예상돼요. 원격 줄서기나 다른 가게를 추천해요.';
  } else if (totalScore >= 50) {
    waitingIndex = '혼잡';
    emoji = '🔴';
    message = '웨이팅이 있을 가능성이 높아요. 방문에 참고하세요.';
  } else if (totalScore >= 30) {
    waitingIndex = '보통';
    emoji = '🟡';
    message = '붐비기 시작하는 시간이네요. 약간의 대기가 있을 수 있어요.';
  } else if (totalScore >= 10) {
    waitingIndex = '여유';
    emoji = '🟢';
    message = '아직은 여유로운 편이에요. 지금 방문하면 좋을 것 같아요.';
  } else {
    waitingIndex = '한산';
    emoji = '🔵';
    message = '매우 한산해요! 기다림 없이 바로 즐길 수 있어요.';
  }

  result.value = { totalScore, waitingIndex, message, emoji };
  step.value = 'result';
};

const reset = () => {
  step.value = 'search';
  searchQuery.value = '';
  foundStores.value = [];
  selectedStore.value = null;
  result.value = null;
  scoreDetails.value = [];
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700&display=swap');

:root {
  --primary-color: #6c5ce7;
  --bg-color: #f4f7f9;
  --card-bg: #ffffff;
  --text-color: #333;
  --light-text-color: #777;
  --border-color: #e9e9e9;
  --green: #2ecc71;
  --yellow: #f1c40f;
  --orange: #e67e22;
  --red: #e74c3c;
  --blue: #3498db;
}

.estimator-container {
  font-family: 'Noto Sans KR', sans-serif;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 600px;
  background-color: var(--bg-color);
  padding: 4px 0 0 0;
}
.card {
  width: calc(100% - 2px);
  height: 100%;
  padding: 0;
  background: var(--card-bg);
  border-radius: 4px;
  border: 1px solid rgba(108, 92, 231, 0.2);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.card-header {
  background: var(--el-color-primary);
  color: white;
  padding: 15px 20px;
  text-align: center;
  flex-shrink: 0;
}
.title { font-size: 1.4rem; margin: 0; font-weight: 700; }
.subtitle { font-size: 0.8rem; margin: 4px 0 0; opacity: 0.9; }
.card-body {
  padding: 20px;
  flex-grow: 1;
  flex-direction: column;
  min-height: 0;
}
.card-body.search-step-body {
  justify-content: space-between; /* 검색창은 위로, 정보 섹션은 아래 근처로 */
  padding: 25px 20px;

}

/* 기존 search-form의 중앙 정렬을 제거합니다. */
.search-form {
  display: flex;
  gap: 8px;
}

.step-title { text-align: center; margin-top: 0; margin-bottom: 20px; font-weight: 500; font-size: 1.1rem; color: var(--primary-color); flex-shrink: 0; }
.search-form { display: flex; gap: 8px; margin: auto 0; }
input[type="text"] {
  flex-grow: 1;
  padding: 12px;
  /* border-color를 조금 더 진한 색으로 변경하여 항상 보이게 함 */
  border: 2px solid #ccc;
  border-radius: 8px;
  font-size: 0.95rem;
  transition: all 0.2s ease;
}

/* 포커스될 때의 스타일은 그대로 유지하여 시각적 피드백을 줍니다. */
input[type="text"]:focus {
  outline: none;
  border-color: var(--el-border-color); /* 포커스 시에는 메인 색상으로 변경 */
  box-shadow: 0 0 0 3px rgba(108, 92, 231, 0.15);
}

.info-section {
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--border-color);
  margin-top: 20px;
}

.info-block {
  text-align: center;
}

.info-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-color);
  margin: 0 0 8px 0;
}

.info-text {
  font-size: 0.85rem;
  color: var(--light-text-color);
  margin: 0;
  line-height: 1.5;
}

.example-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.example-list li {
  background-color: #f4f7f9;
  color: var(--primary-color);
  padding: 6px 12px;
  border-radius: 15px;
  font-size: 0.8rem;
  font-weight: 500;
  cursor: default; /* 클릭 기능이 없으므로 기본 커서로 */
}
button {
  padding: 12px 18px;
  background-color: var(--el-color-primary);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
  flex-shrink: 0;
}

button:hover {
  background-color: var(--el-color-primary-light-3);
}

/* ★★★ :disabled 대신 클래스로 제어 ★★★ */
button.is-disabled {
  background-color: #b5b5b5;
  cursor: not-allowed;
}

/* .is-disabled 상태일 때는 hover 효과를 없앰 */
button.is-disabled:hover {
  background-color: #b5b5b5;
}
.store-list { list-style: none; padding: 0; margin: 0; overflow-y: auto; flex-grow: 1; }
.store-list li { padding: 12px 15px; border: 1px solid var(--border-color); border-radius: 8px; margin-bottom: 8px; cursor: pointer; transition: background-color 0.2s, border-color 0.2s, transform 0.2s; display: flex; justify-content: space-between; align-items: center; font-size: 0.9rem; }
.store-list li span { font-size: 0.8rem; color: var(--light-text-color); }
.store-list li:hover { background-color: #f9f6ff; border-color: var(--primary-color); transform: translateY(-2px); }
.back-button { width: 100%; margin-top: 15px; background-color: #7f8c8d; }
.back-button:hover { background-color: #6c7a7b; }
.time-slots {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin: auto 0;
  flex-grow: 1;
  align-content: center;
}
.time-slot-btn {
  padding: 15px 10px;
  font-size: 0.9rem;
  font-weight: 500;
  background-color: var(--el-border-color);
  color: var(--primary-color);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease-in-out;
}
.time-slot-btn:hover {
  border-color: var(--el-color-primary-light-3);
  background-color: var(--el-border-color-extra-light);
  transform: translateY(-2px);
}
.time-slot-btn.active {
  background-color: var(--el-bg-color);
  color: var(--el-color-primary);
  font-weight: 700;
  border-color: var(--el-text-color-regular);
  box-shadow: 0 4px 10px rgba(108, 92, 231, 0.15);
  transform: translateY(-2px);
}
.button-group {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  flex-shrink: 0;
}
.button-group button {
  flex: 1;
}
.button-group .back-button {
  margin-top: 0; /* 기존 back-button의 margin-top 제거 */
}
.loading-state { justify-content: center; text-align: center; }
.spinner { width: 40px; height: 40px; border: 4px solid rgba(108, 92, 231, 0.2); border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 15px; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-message { color: var(--light-text-color); font-size: 0.9rem; margin-bottom: 20px; }
.progress-list { text-align: left; background-color: #fafafa; padding: 10px 15px; border-radius: 8px; }
.progress-list p { margin: 8px 0; font-size: 0.85rem; color: var(--light-text-color); transition: all 0.5s ease; }
.progress-list p.done { color: var(--text-color); font-weight: 500; }
.progress-list p.done::after { content: ' ✓'; color: var(--green); }
.footer { margin-top: 20px; text-align: center; font-size: 0.75rem; color: var(--light-text-color); }
.footer p {
  margin: 2px 0; /* 위아래 간격 줄이기 */
}
.footer .copyright {
  font-size: 0.7rem; /* 저작권 폰트는 약간 작게 */
  opacity: 0.8;
}

/* =============================================================== */
/* ============= [핵심] 결과 화면 & 상세 분석 영역 개선 ============= */
/* =============================================================== */

/* 전체 결과 화면 레이아웃 */
.result-state {
  justify-content: space-between; /* 요소들을 위, 중간, 아래로 분산 */
  padding: 15px; /* 패딩 약간 축소 */
}

/* 1. 상단 요약 정보 */
.result-summary {
  display: flex;
  align-items: center;
  gap: 15px;
  background-color: #f8f9fa;
  padding: 12px;
  border-radius: 10px;
  flex-shrink: 0; /* 높이 고정 */
}
.result-emoji { font-size: 2.5rem; }
.result-text {
  display: flex;
  flex-direction: column;
  justify-content: center; /* 주 축(main-axis, 현재는 세로)의 중앙으로 정렬 */
  flex-grow: 1;
}
.result-index {
  font-size: 1rem; /* 폰트 크기 조정 */
  font-weight: 700;
  margin: 0;
  color: var(--text-color);
}
/* 혼잡도 텍스트에 색상 부여 */
.result-index .매우.혼잡, .result-index .혼잡 { color: var(--red); }
.result-index .보통 { color: var(--orange); }
.result-index .여유 { color: var(--green); }
.result-index .한산 { color: var(--blue); }

.result-message {
  font-size: 0.8rem;
  margin: 4px 0 0;
  color: var(--light-text-color);
  line-height: 1.4;
}

/* 2. [핵심] 스크롤되는 상세 분석 영역 */
.score-details {
  display: flex;
  flex-direction: column;
  flex-grow: 1; /* 남은 세로 공간을 모두 차지 */
  min-height: 0; /* 자식 요소(ul)가 넘칠 때 스크롤이 가능하게 하는 핵심 속성 */
  margin: 12px 0;
}
.details-title {
  font-weight: 700;
  font-size: 0.9rem;
  margin-bottom: 8px;
  color: var(--text-color);
  flex-shrink: 0; /* 높이 고정 */
}
.details-list {
  list-style: none;
  padding: 0;
  margin: 0;
  border: 1px solid rgba(108, 92, 231, 0.2);
  overflow-y: auto;
  flex-grow: 1;
  background-color: var(--card-bg); /* 스크롤 영역에 흰색 배경을 줘서 구분 */
}
/* 스크롤바 디자인 (선택사항) */
.details-list::-webkit-scrollbar { width: 6px; }
.details-list::-webkit-scrollbar-thumb { background-color: #ccc; border-radius: 3px; }
.details-list::-webkit-scrollbar-track { background-color: #f1f1f1; }

.details-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 12px;
  border-bottom: 1px solid #f5f5f5;
  font-size: 0.85rem;
}
.details-list li:last-child { border-bottom: none; }

.factor { font-weight: 500; }
.condition {
  color: var(--light-text-color);
  font-size: 0.8rem;
  flex-grow: 1;
  text-align: right;
  margin-right: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.score {
  font-weight: 700;
  min-width: 35px;
  text-align: right;
  font-size: 0.9rem;
}
.score.positive { color: var(--green); }
.score.negative { color: var(--red); }

/* 3. 하단 최종 점수 및 버튼 */
.result-footer {
  flex-shrink: 0; /* 높이 고정 */
}
.total-score {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  margin-bottom: 12px;
  background-color: #f8f6ff;
  border-radius: 8px;
  border: 1px solid #e2dffc;
}
.total-score .factor { font-size: 0.9rem; color: var(--primary-color); font-weight: 700; }
.total-score .score { font-size: 1.2rem; color: var(--primary-color); font-weight: 700; }
.reset-button { width: 100%; }

/* 휴무일 화면 스타일 */
.closed-state {
  justify-content: center;
  align-items: center;
  text-align: center;
  gap: 10px;
}
.closed-state .result-index { font-size: 1.3rem; font-weight: 700; }
.closed-state .reset-button { margin-top: 15px; }

</style>