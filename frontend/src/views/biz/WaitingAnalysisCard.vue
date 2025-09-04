<script setup lang="ts">
// 스크립트 부분은 수정 없이 그대로 사용합니다.
import { reactive, ref } from 'vue';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { QuestionFilled } from "@element-plus/icons-vue";

const step = ref('search');
const searchQuery = ref('');
const foundStores = ref([]) as any;
const selectedStore = ref(null) as any;
const result = ref(null) as any;
const scoreDetails = ref([]) as any;
const progress = ref({
  weather: false,
  reviews: false,
  holiday: false,
  sns: false,
  map: false
}) as any;

const numberOfPeople = ref(1);

const selectedTime = ref() as any;
const timeSlots = ref([
  { label: '10시 ~ 12시', value: '10-12' },
  { label: '12시 ~ 14시', value: '12-14' },
  { label: '14시 ~ 16시', value: '14-16' },
  { label: '16시 ~ 18시', value: '16-18' },
  { label: '18시 ~ 20시', value: '18-20' },
  { label: '20시 ~ 22시', value: '20-22' },
]);

const searchStores = async () => {
  if (!searchQuery.value) return;

  const result = await Api.post(ApiUrls.NAVER_STORE_SEARCH, {query: searchQuery.value});
  console.log(result)

  foundStores.value = result.data;
  step.value = 'selectStore';
};

// ★★★ 지점 선택 함수 수정 ★★★
const selectStore = (store: any) => {
  selectedStore.value = store;
  selectedTime.value = null; // 시간 선택 초기화
  step.value = 'selectTime'; // 로딩 대신 시간 선택 단계로 이동
};

// ★★★ 시간 선택 관련 함수들 추가 ★★★
const selectTimeSlot = (timeValue: any) => {
  selectedTime.value = timeValue;
};

/**
 * 현재 시간과 비교하여 이미 지난 시간대인지 확인하는 함수
 * @param timeValue '10-12'와 같은 형태의 시간 값
 */
const isTimeSlotDisabled = (timeValue: string) => {
  const currentHour = new Date().getHours(); // 현재 시간을 24시간 형식으로 가져옵니다. (예: 오후 2시는 14)
  const slotEndHour = parseInt(timeValue.split('-')[1]); // 시간 값('10-12')에서 끝나는 시간(12)을 숫자로 추출합니다.

  // 현재 시간이 시간대의 끝나는 시간보다 크거나 같으면, 그 시간대는 이미 지난 것이므로 비활성화합니다.
  return currentHour >= slotEndHour;
};

const confirmTimeAndAnalyze = async () => {
  if (!selectedTime.value) return;

  step.value = 'loading';
  startAnalysis();
}

/**
 * 최종 분석에 쓰일 결과물
 */
const analysis = reactive({
  reviewCount: 0
})

/**
 * 블로그 건수 조회
 */
const countReviews = async () => {
  console.log(selectedStore.value);

  const param = {
    name: selectedStore.value.name,
    simpleAddress: selectedStore.value.simpleAddress,
    detailAddress: selectedStore.value.simpleAddress,
  }

  console.log(param)

  const result = await Api.post(ApiUrls.NAVER_BLOG_SEARCH, param);
  console.log(result)

  analysis.reviewCount = result.data.blogReviewCount;

}

const getWeatherInfo = async () => {
  const param = {
    name: selectedStore.value.name,
    simpleAddress: selectedStore.value.simpleAddress,
    detailAddress: selectedStore.value.simpleAddress,
  }
  const result = await Api.post(ApiUrls.WEATHER_SEARCH, param);
  console.log(result)
}

const getHolidayInfo = async () => {
  const response = await Api.post(ApiUrls.HOLIDAY_INFO, {});
  console.log(response)
}

/**
 * 데이터 분석 flow
 */
const startAnalysis = async () => {
  Object.keys(progress.value).forEach(k => progress.value[k] = false);

  // 날씨 api
  setTimeout(() => {
    getWeatherInfo().then(() => {
      progress.value.weather = true;
    });
  }, 1000);

  // 네이버 블로그 검색 건수 조회
  setTimeout(() => {
    countReviews().then(() => {
      progress.value.reviews = true;
    });
  }, 1000);

  // 공휴일 정보
  setTimeout(() => {
    getHolidayInfo().then(() => {
      progress.value.holiday = true;
    });
  }, 1000);



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

  const reviewCount = analysis.reviewCount;

  let reviewScore = 0;
  if (reviewCount > 1000) reviewScore = 15;
  else if (reviewCount > 500) reviewScore = 10;
  else if (reviewCount > 100) reviewScore = 5;
  if (reviewScore > 0) {
    const fomattedCount = new Intl.NumberFormat().format(analysis.reviewCount);
    details.push({ factor: '인지도(리뷰 수)', condition: `리뷰 ${fomattedCount}개`, score: reviewScore });
    totalScore += reviewScore;
  }

  const rating = (Math.random() * 1.5 + 3.5).toFixed(1) as any;
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

const generateFinalResult = (totalScore: any) => {
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

<template>
  <div class="estimator-container">
    <div class="card">
      <div class="card-header">
        <h1 class="title">웨이팅 지수 분석기 📈</h1>
        <p class="subtitle">가게의 오늘 웨이팅 지수를 예측해 드립니다.</p>
      </div>

      <!-- 1. 초기 검색 단계 (수정됨) -->
      <div v-if="step === 'search'" class="card-body search-step-body">
        <!-- 검색 UI를 감싸는 컨테이너 추가 -->
        <div class="search-container">
          <div class="search-form">
            <el-input
                v-model="searchQuery"
                placeholder="가게 이름을 입력하세요 (예: 런던베이글)"
                @keyup.enter="searchStores"
                size="large"
                clearable
            >
              <template #suffix>
                <el-popover
                    placement="top"
                    :width="470"
                    trigger="hover"
                    popper-class="search-tip-popover"
                >
                  <!-- Popover의 트리거가 되는 아이콘 -->
                  <template #reference>
                    <el-icon class="info-icon"><QuestionFilled /></el-icon>
                  </template>

                  <!-- ★★★ Popover의 내용물을 ElAlert로 변경 ★★★ -->
                  <div class="modern-alert modern-alert-info">
                    <div class="modern-alert-icon">
                      <!-- 아이콘 (예: SVG 또는 아이콘 폰트) -->
                      <i class="el-icon-info"></i>
                    </div>
                    <div class="modern-alert-content">
                      <p class="modern-alert-title">빠른 검색 팁!</p>
                      <p class="modern-alert-description">
                        네이버 정책에 따라 검색 결과는 <strong>최대 5개</strong>까지 제공됩니다.
                      </p>
                    </div>
                  </div>

                </el-popover>
              </template>
            </el-input>

            <button
                @click="searchStores"
                :disabled="!searchQuery"
                :class="{ 'is-disabled': !searchQuery }"
                class="search-button"
            >
              분석 시작
            </button>
          </div>
        </div>

        <!-- 아래 정보 섹션은 그대로 유지 -->
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
            <el-text>{{ store.name }}</el-text>
            <span>{{ store.simpleAddress }}</span>
          </li>
        </ul>
        <button class="back-button" @click="reset">처음으로</button>
      </div>

      <!-- 2.5. ★★★ 방문 시간 선택 단계 (새로 추가) ★★★ -->
      <div v-if="step === 'selectTime'" class="card-body">
        <div class="input-group">
          <h3 class="input-label">방문 인원을 설정해주세요.</h3>
          <el-input-number
              v-model="numberOfPeople"
              :min="1"
              :max="10"
              size="large"
              controls-position="right"
              style="width: 100%;"
          />
        </div>
        <h2 class="step-title">방문 예정 시간을 선택해주세요.</h2>
        <button
            class="skip-time-btn"
            @click="selectedTime = 'now'; confirmTimeAndAnalyze()"
        >
          <span>⚡️ 시간 미정 (현재 시점 분석)</span>
        </button>
        <div class="time-slots">
          <button
              v-for="time in timeSlots"
              :key="time.value"
              class="time-slot-btn"
              :class="{ active: selectedTime === time.value }"
              @click="selectTimeSlot(time.value)"
              :disabled="isTimeSlotDisabled(time.value)"
          >
            {{ time.label }}
          </button>
        </div>
        <div class="button-group">
          <button class="back-button" @click="step = 'selectStore'">지점 다시 선택</button>
          <button class="right-button"@click="confirmTimeAndAnalyze" :disabled="!selectedTime">분석하기</button>
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
          <el-button type="primary" class="reset-button" @click="reset">새로운 가게 분석하기</el-button>
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
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700&display=swap');

:global(.el-popper.search-tip-popover) {
  /* 배경, 테두리, 그림자를 모두 제거하여 투명하게 만듭니다. */
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  /* 내부 여백도 제거합니다. */
  padding: 0 !important;
}

/* Popover에 기본으로 달려있는 작은 화살표를 숨깁니다. */
:global(.el-popper.search-tip-popover .el-popper__arrow) {
  display: none !important;
}

.modern-alert {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 8px 12px rgba(0, 0, 0, 0.1);
}

.modern-alert-info {
  background-color: var(--black-white-color); /* 부드러운 파란색 계열 */
  border-left: 5px solid var(--el-overlay-color-light);
}

.modern-alert-icon {
  margin-right: 12px;
  font-size: 24px;
  color: #5096FF;
  /* 아이콘이 없어서 임시로 아이콘 폰트를 위한 공간 설정 */
  width: 24px;
  text-align: center;
}

.modern-alert-content {
  flex-grow: 1;
}

.modern-alert-title {
  margin: 0;
  font-weight: 600;
  color: var(--main-header-text-color2);
  font-size: 16px;
}

.modern-alert-description {
  margin: 4px 0 0;
  color: var(--text-color3);
  font-size: 14px;
}

.estimator-container {
  font-family: 'Noto Sans KR', sans-serif;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 620px;
  background-color: var(--el-bg-color);
  padding: 4px 0 0 0;
}
.card {
  width: calc(100% - 2px);
  height: 100%;
  padding: 0;
  background: var(--el-bg-color);
  border-radius: 4px;
  border: 1px solid var(--el-border-color);
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
.title { font-size: 1.4rem; margin: 0; font-weight: 700; color: var(--el-fill-color); }
.subtitle { font-size: 0.8rem; margin: 4px 0 0; opacity: 0.9; color: var(--el-fill-color); }
.card-body {
  padding: 20px;
  flex-grow: 1;
  flex-direction: column;
  min-height: 0;
}
.card-body.search-step-body {
  justify-content: space-between; /* 검색창은 위로, 정보 섹션은 아래 근처로 */
  padding: 25px 20px;
  background-color: var(--el-bg-color);
}

/* 기존 search-form의 중앙 정렬을 제거합니다. */
.search-form {
  display: flex;
  align-items: center; /* ElInput(large)와 버튼의 높이를 맞춤 */
  gap: 8px;
  margin: auto 0;
}
.search-input-with-icon {
  flex-grow: 1;
}

/* ElInput 오른쪽의 도움말 아이콘 스타일 */
.info-icon {
  cursor: pointer;
  color: var(--el-text-color-placeholder);
  font-size: 16px;
  transition: color 0.2s;
}
.info-icon:hover {
  color: var(--el-color-primary);
}

/* Popover 내부 p 태그 스타일 (전역 CSS 오염 방지) */
.popover-content {
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
  color: var(--el-text-color-regular);
}

/* ElInput(size="large")에 맞춰 버튼 높이 조정 */
.search-button {
  height: 40px;
  padding: 0 18px;
}

.step-title {
  text-align: center; margin-top: 30px; margin-bottom: 20px; font-weight: 500; font-size: 1.1rem; color: var(--el-color-primary); flex-shrink: 0; }
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
  margin-top: 20px;
  text-align: center;
}

.info-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--el-color-primary);
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
  background-color: var(--el-border-color-extra-light);
  color: var(--el-color-primary);
  padding: 6px 12px;
  border-radius: 15px;
  font-size: 0.8rem;
  font-weight: 500;
  cursor: default; /* 클릭 기능이 없으므로 기본 커서로 */
}
button {
  padding: 12px 18px;
  background-color: var(--el-color-primary);
  color: var(--el-bg-color);
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
.store-list {
  list-style: none;
  padding: 0;
  margin: 12px 0 0 0; /* 위쪽 제목과의 간격을 margin-top으로 조정 */
  flex-grow: 1;       /* 1. 부모(.card-body)의 남은 세로 공간을 모두 차지합니다. */
  overflow-y: auto;   /* 2. 내용이 영역을 벗어나면 세로 스크롤바를 표시합니다. */
  height: 300px;
  min-height: 0;      /* 3. flex-grow와 overflow가 올바르게 작동하기 위한 필수 속성입니다. */
}
.store-list li { margin-top: 4px; padding: 12px 15px; border: 1px solid var(--el-color-primary); border-radius: 8px; margin-bottom: 8px; cursor: pointer; transition: background-color 0.2s, border-color 0.2s, transform 0.2s; display: flex; justify-content: space-between; align-items: center; font-size: 0.9rem; }
.store-list li span { font-size: 0.8rem; color: var(--el-color-primary); }
.store-list li:hover { background-color: var(--el-bg-color); border-color: var(--el-color-primary); transform: translateY(-2px); }
.back-button { width: 100%; margin-top: 15px; background-color: #7f8c8d; }
.back-button:hover { background-color: #6c7a7b; }
.time-slots {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin: 16px 0; /* '시간 미정' 버튼과 하단 버튼 그룹 사이의 여백 확보 */
  flex-grow: 1;
  align-content: center;
}

.time-slot-btn {
  padding: 14px 10px;
  font-size: 0.9rem;
  font-weight: 500;
  /* 기본 상태: 은은한 배경색과 일반 텍스트 색상 */
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  /* 레이아웃 깨짐 방지를 위한 투명 테두리 */
  border: 2px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease-in-out;
}

.time-slot-btn:hover:not(:disabled) {
  /* 마우스 오버: 테마 색상을 활용하여 상호작용 피드백 제공 */
  border-color: var(--el-color-primary-light-5);
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  transform: translateY(-2px);
}

.time-slot-btn.active {
  /* 활성 상태: 주 색상으로 명확하게 선택 표시 */
  background-color: var(--el-color-primary);
  color: var(--el-bg-color); /* 배경색과 대비되는 텍스트 색상 */
  border-color: var(--el-color-primary);
  font-weight: 700;
  box-shadow: var(--el-box-shadow-light); /* 입체감을 위한 그림자 */
  transform: translateY(-2px);
}

.time-slot-btn:disabled {
  /* 비활성 상태: 테마의 비활성 변수 사용 */
  background-color: var(--el-disabled-bg-color);
  color: var(--el-disabled-text-color);
  border-color: transparent;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
  opacity: 0.8; /* 비활성화된 느낌을 강조 */
}
/* 비활성화된 버튼 위에서는 hover 효과도 없애줍니다. */
.time-slot-btn:disabled:hover {
  background-color: var(--el-disabled-bg-color);
  border-color: var(--el-disabled-bg-color);
}
.skip-time-btn {
  width: 100%;
  padding: 10px;
  margin-bottom: 20px; /* 시간 선택 슬롯과의 간격 */
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--el-color-primary);
  background-color: transparent;
  border: 1px dashed var(--el-color-primary-light-5);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px; /* 아이콘과 텍스트 사이 간격 */
}

.skip-time-btn:hover {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-3);
  color: var(--el-color-primary-light-3);
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
.button-group .right-button {
  margin-top: 0; /* 기존 back-button의 margin-top 제거 */
  background-color: var(--el-color-primary);
  color: var(--el-bg-color);
}
.loading-state { justify-content: center; text-align: center; }
.spinner { width: 40px; height: 40px; border: 4px solid rgba(108, 92, 231, 0.2); border-top-color: var(--el-color-primary); border-radius: 50%; animation: spin 1s linear infinite; margin: 15px auto 15px; }
@keyframes spin { to { transform: rotate(360deg); } }
.loading-message { color: var(--el-color-primary); font-size: 0.9rem; margin-bottom: 20px; }
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
  background-color: var(--el-fill-color);
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
  color: var(--el-color-primary);
}
.result-index {
  font-size: 1rem; /* 폰트 크기 조정 */
  font-weight: 700;
  margin: 0;
  color: var(--el-color-primary);
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
  color: var(--el-color-primary);
  flex-shrink: 0; /* 높이 고정 */
}
.details-list {
  list-style: none;
  padding: 0;
  margin: 0;
  border: 1px solid var(--el-color-primary);
  overflow-y: auto;
  flex-grow: 1;
  background-color: var(--el-bg-color); /* 스크롤 영역에 흰색 배경을 줘서 구분 */
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
  color: var(--el-color-primary);
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
  background-color: var(--primary-color-light);
  border-radius: 8px;
  border: 1px solid var(--border-color-light);
}
.total-score .factor { font-size: 0.9rem; color: var(--el-color-primary); font-weight: 700; }
.total-score .score { font-size: 1.2rem; color: var(--el-color-primary); font-weight: 700; }
.reset-button { width: 100%; height: 50px; color: var(--el-bg-color); }

/* 휴무일 화면 스타일 */
.closed-state {
  justify-content: center;
  align-items: center;
  text-align: center;
  gap: 10px;
}
.closed-state .result-index { font-size: 1.3rem; font-weight: 700; }
.closed-state .reset-button { margin-top: 15px; }
.input-group {
  margin-bottom: 24px; /* 아래 요소와의 간격 */
  text-align: left; /* 라벨 왼쪽 정렬 */
}

.input-label {
  font-size: 1rem;
  font-weight: 500;
  color: var(--el-text-color-regular);
  margin-bottom: 8px; /* 입력창과의 간격 */
}

</style>