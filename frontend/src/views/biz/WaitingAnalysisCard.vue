<script setup lang="ts">

import { reactive, ref, onMounted, watch, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { QuestionFilled, InfoFilled, Refresh, Search, MoreFilled, WarningFilled } from "@element-plus/icons-vue";
import { userStore } from "@/store/userStore";
import ServiceErrorState from '@/components/biz/ServiceErrorState.vue';

import publicDataPortalLogo from "@/assets/images/data-logo.jpeg";
import naverApiLogo from "@/assets/images/naver-api-logo.png";
import googleApiLogo from "@/assets/images/google_cloud_logo.png";
import naverDataLabLogo from "@/assets/images/naver-datalab-icon.png";
import kakaoApiLogo from "@/assets/images/kakao-api-logo.png";
import Graph from "@/assets/images/graph-icon.png";
import stars from "@/assets/images/stars_icon.png";
import archive from "@/assets/images/archive-icon.png";
import emptyBox from "@/assets/images/enpty_box.png";

// 서비스 장애 상태 변수
const isRecommendationUnavailable = ref(false);
const isArchiveUnavailable = ref(false);

// 기본 추천 가게 데이터 정의
const defaultRecommendedStores = [
  { name: '런던 베이글 뮤지엄', category: '카페, 디저트' },
  { name: '다운타우너 안국', category: '햄버거' },
  { name: '카멜커피 5호점', category: '카페, 디저트' },
  { name: '온천집 익선', category: '일식' },
  { name: '소금집 델리 안국', category: '샌드위치' },
  { name: '진저베어 파이샵', category: '카페, 디저트' },
];

const { t, locale } = useI18n();
const userStoreObj = userStore();
const step = ref('search');
const searchQuery = ref('');
const foundStores = ref([]) as any;
const selectedStore = ref(null) as any;
const result = ref(null) as any;
const scoreDetails = ref([]) as any;
const progress = ref({
  opening: false,
  weather: false,
  reviews: false,
  holiday: false,
  sns: false,
  surround: false,
}) as any;

// 추천 가게 관련 상태 변수
const recommendedStores = ref([]) as any;
const isRecommendationLoading = ref(true);

// 추천 가게 목록을 불러오는 함수
const fetchRecommendedStores = async () => {
  isRecommendationLoading.value = true;
  isRecommendationUnavailable.value = false; // 함수 호출 시 장애 상태 초기화
  try {
    const response = await Api.post(ApiUrls.RANDOM_RECOMMENDATIONS, {});
    recommendedStores.value = response.data;
  } catch (error: any) {
    console.error("추천 가게를 불러오는 데 실패했습니다:", error);
    recommendedStores.value = [];
    isRecommendationUnavailable.value = true;
  } finally {
    isRecommendationLoading.value = false;
  }
};

// "기본 추천 보기" 버튼을 눌렀을 때 실행될 함수
const handleShowDefaultRecommendations = () => {
  recommendedStores.value = defaultRecommendedStores;
  // 기본 데이터를 보여줬으므로, 더 이상 '장애' 상태가 아님. UI를 목록으로 되돌리기 위해 상태 변경
  isRecommendationUnavailable.value = false;
};

// 추천 가게를 선택했을 때의 동작을 정의하는 함수
const selectRecommendedStore = (store: any) => {
  searchQuery.value = store.name; // 검색어에 가게 이름 채우기

  const searchNameForApi = store.nameKo || store.name;

  searchStores(searchNameForApi); // 기존 검색 함수 실행
};

const myArchiveStores = ref<any[]>([]);
const isArchiveLoading = ref(true);

/**
 * 저장소의 특정 맛집 상세 페이지로 이동
 * @param storeId 이동할 가게의 고유 ID
 */
const goToArchiveDetail = (storeId: number) => {
  // router.push(`/archive/${storeId}`); // 예시: /archive/123 과 같은 동적 경로로 이동
  console.log(`저장소의 상세 페이지로 이동합니다. ID: ${storeId}`); // 실제 라우터 push 로직으로 대체 필요
  // 상세 보기 화면으로 이동한 후, 해당 ID를 가진 가게가 활성화되도록 상태 관리(Pinia 등)를 활용할 수도 있습니다.
};

const fetchMyArchiveStores = async () => {
  isArchiveLoading.value = true;
  isArchiveUnavailable.value = false; // 함수 호출 시 장애 상태 초기화
  try {
    // 사용자 ID를 페이로드에 담아 API를 호출합니다.
    const payload = { userId: userStoreObj.getUserInfo.userId };
    const response = await Api.post(ApiUrls.GET_GOURMET_RECORDS, payload);

    // 서버에서 받은 데이터를 사용하기 좋은 형태로 가공합니다.
    const formattedStores = response.data.map((store: any) => {
      // visitDate가 배열(Array) 형태인지 확인합니다. (예: [2023, 10, 26])
      if (Array.isArray(store.visitDate) && store.visitDate.length >= 3) {
        const [year, month, day] = store.visitDate;
        // 'YYYY-MM-DD' 형식의 문자열로 변환합니다.
        const formattedDate = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        // 기존 store 객체를 복사하고, visitDate만 변환된 값으로 덮어씁니다.
        return { ...store, visitDate: formattedDate };
      }
      // 만약 배열이 아니라면 (예외 상황 대비), 기존 데이터를 그대로 반환합니다.
      return store;
    });

    // 가공된 데이터를 최신 방문일 순으로 정렬하여 상태에 저장합니다.
    myArchiveStores.value = formattedStores.sort((a: any, b: any) =>
        new Date(b.visitDate).getTime() - new Date(a.visitDate).getTime()
    );

  } catch (error: any) { // ★★★ catch 블록에서 에러 처리 강화 ★★★
    console.error("내 저장소 목록을 불러오는 데 실패했습니다:", error);
    myArchiveStores.value = [];
    if (error.response?.data?.header?.code || error.response?.data?.code) {
      isArchiveUnavailable.value = true;
    }
  } finally {
    isArchiveLoading.value = false;
  }
};

watch(locale, () => {
  fetchRecommendedStores();
  fetchMyArchiveStores();
});

onMounted(() => {
  fetchRecommendedStores();
  fetchMyArchiveStores();
});

const numberOfPeople = ref(1);

const selectedTime = ref() as any;
const timeSlots = ref([
  { label: t('waitingAnalyzer.steps.selectTime.timeSlots.t10_12'), value: '10-12' },
  { label: t('waitingAnalyzer.steps.selectTime.timeSlots.t12_14'), value: '12-14' },
  { label: t('waitingAnalyzer.steps.selectTime.timeSlots.t14_16'), value: '14-16' },
  { label: t('waitingAnalyzer.steps.selectTime.timeSlots.t16_18'), value: '16-18' },
  { label: t('waitingAnalyzer.steps.selectTime.timeSlots.t18_20'), value: '18-20' },
  { label: t('waitingAnalyzer.steps.selectTime.timeSlots.t20_22'), value: '20-22' },
]);

const searchStores = async (query?: string) => {
  const finalQuery = query || searchQuery.value;
  if (!finalQuery) return;

  const payload = {
    query: finalQuery,
    lang: locale.value
  };

  console.log(payload);

  const response = await Api.post(ApiUrls.NAVER_STORE_SEARCH, payload);
  console.log('가게정보: ', response)

  foundStores.value = response.data;
  step.value = 'selectStore';
};

// 지점 선택 함수
const selectStore = (store: any) => {
  selectedStore.value = store;
  selectedTime.value = null; // 시간 선택 초기화
  step.value = 'selectTime'; // 로딩 대신 시간 선택 단계로 이동
};

// 시간 선택 관련 함수
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

/**
 * 분석 시작
 */
const confirmTimeAndAnalyze = async () => {
  if (!selectedTime.value) return;

  step.value = 'loading';
  await startAnalysis();
}

/**
 * 최종 분석에 쓰일 결과물
 */
const analysis = reactive({
  reviewCount: 0,
  openingInfo: null as any,
  weatherInfo: null as any,
  trendInfo: null as any,
  holidayInfo: null as any,
  surroundingInfo: null as any,
})

const notAvailableInfo = reactive({
  emoji: '',
  title: '',
  message: '',
});

const bestStoreName = computed(() => {
  if (!selectedStore.value) return '';
  // 한국어 이름(nameKo)이 있으면 그것을 최우선으로 사용하고, 없으면 기본 이름(name)을 사용합니다.
  return selectedStore.value.nameKo || selectedStore.value.name;
});

const bestSimpleAddress = computed(() => {
  if (!selectedStore.value) return '';
  // 한국어 주소(simpleAddressKo)가 있으면 그것을 최우선으로 사용하고, 없으면 기본 주소(simpleAddress)를 사용합니다.
  return selectedStore.value.simpleAddressKo || selectedStore.value.simpleAddress;
});

const bestDetailAddress = computed(() => {
  if (!selectedStore.value) return '';
  // 한국어 상세주소(detailAddressKo)가 있으면 그것을 최우선으로 사용하고, 없으면 기본 상세주소(detailAddress)를 사용합니다.
  return selectedStore.value.detailAddressKo || selectedStore.value.detailAddress;
});

/**
 * 블로그 건수 조회
 */
const countReviews = async () => {
  const payload = {
    name: bestStoreName.value,
    simpleAddress: bestSimpleAddress.value,
    detailAddress: bestDetailAddress.value,
  }

  const response = await Api.post(ApiUrls.NAVER_BLOG_SEARCH, payload);
  console.log("블로그 건수:", response.data);

  analysis.reviewCount = response.data.blogReviewCount;
}

/**
 * 날씨 정보 조회
 */
const getWeatherInfo = async () => {
  const payload = {
    name: bestStoreName.value,
    simpleAddress: bestSimpleAddress.value,
    detailAddress: bestDetailAddress.value,
  }
  try {
    const result = await Api.post(ApiUrls.WEATHER_SEARCH, payload);
    analysis.weatherInfo = result.data;
    console.log("날씨 정보:", result.data);
  } catch (error) {
    console.error("날씨 정보 조회 실패:", error);
    analysis.weatherInfo = null;
  }
}

/**
 * 휴일 정보 조회
 */
const getHolidayInfo = async () => {
  try {
    const response = await Api.post(ApiUrls.HOLIDAY_INFO, {});
    analysis.holidayInfo = response.data; // API 결과를 analysis 객체에 저장
    console.log("휴일 정보:", response.data);
  } catch (error) {
    console.error("휴일 정보 조회 실패:", error);
    // API 실패 시, 클라이언트의 현재 날짜를 기반으로 비상용 데이터 생성
    const today = new Date();
    const dayIndex = today.getDay(); // 0:일, 1:월, ..., 6:토
    const dayMap = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
    analysis.holidayInfo = {
      holidayOrWeekend: dayIndex === 0 || dayIndex === 6,
      holidayType: dayIndex === 0 || dayIndex === 6 ? "주말" : "평일",
      todayDayOfWeek: dayMap[dayIndex]
    };
  }
}

/**
 * 데이터랩 검색 추이 조회
 */
const getDataTrend = async () => {
  const payload = {
    query: selectedStore.value.nameKo || selectedStore.value.name,
  }
  try {
    const response = await Api.post(ApiUrls.SEARCH_TREND, payload);
    if (response.data && response.data.results && response.data.results.length > 0) {
      analysis.trendInfo = response.data.results[0].data;
      console.log("데이터랩 검색 추이:", analysis.trendInfo);
    }
  } catch (error) {
    console.error("데이터랩 검색 추이 조회 실패:", error);
    analysis.trendInfo = null;
  }
}

/**
 * 가게 영업 정보 조회
 */
const getOpeningInfo = async () => {
  const payload = {
    name: bestStoreName.value,
    simpleAddress: bestSimpleAddress.value,
    detailAddress: bestDetailAddress.value,
    lang: locale.value
  }
  try {
    const response = await Api.post(ApiUrls.OPENING_INFO, payload);
    console.log('영업 정보 조회:', response.data)
    return response.data; // API 응답 데이터를 반환
  } catch (error) {
    console.error("영업 정보 조회 실패:", error);
    return null; // 실패 시 null 반환
  }
}

/**
 * 영업시간 텍스트를 번역하는 헬퍼 함수
 * @param text 백엔드에서 받은 텍스트 또는 i18n 키
 */
const translateWeekdayText = (text: string) => {
  // 텍스트가 'i18n.'으로 시작하면 번역 함수를 실행합니다.
  if (text.startsWith('i18n.')) {
    // 'i18n.openingInfo.noInfo' -> 'waitingAnalyzer.errors.openingInfo.noInfo' 와 같이 실제 경로에 맞게 변환
    const i18nKey = text.replace('i18n.openingInfo.', 'waitingAnalyzer.errors.openingInfo.');
    return t(i18nKey);
  }
  // 일반적인 영업시간 텍스트는 그대로 반환합니다.
  return text;
};

/**
 * 영업시간 파싱 및 현재 상태 판별 헬퍼 함수
 */
const checkBusinessStateForSelectedTime = (openingInfo: any, selectedTimeValue: string) => {
  if (!openingInfo || !openingInfo.weekdayText) {
    return { status: 'UNKNOWN', message: t('waitingAnalyzer.errors.openingInfo.unavailable') };
  }

  const now = new Date();
  const dayIndex = now.getDay();
  const todayIndex = (dayIndex === 0) ? 6 : dayIndex - 1;
  const todayHoursText = openingInfo.weekdayText[todayIndex];

  if (!todayHoursText || typeof todayHoursText !== 'string') {
    return { status: 'UNKNOWN', message: t('waitingAnalyzer.errors.openingInfo.todayUnavailable') };
  }

  if (todayHoursText.includes('휴무') || todayHoursText.toLowerCase().includes('closed')) {
    return { status: 'CLOSED_TODAY', message: t('waitingAnalyzer.steps.notAvailable.states.closed.message') };
  }

  const colonIndex = todayHoursText.indexOf(':'); // 콜론의 위치를 찾습니다.

  if (colonIndex === -1 || todayHoursText.startsWith('i18n.')) {
    return { status: 'UNKNOWN', message: translateWeekdayText(todayHoursText) };
  }

  const timeInfoString = todayHoursText.substring(colonIndex + 1).trim();

  if (!timeInfoString) {
    return { status: 'UNKNOWN', message: t('waitingAnalyzer.errors.openingInfo.todayUnavailable') };
  }

  const hourBlocks = timeInfoString.split(',').map(s => s.trim());

  const parseTimeWithContext = (timeStr: string, contextPrefix: string | null) => {
    const timeRegex = /(오전|오후|AM|PM)?\s*(\d{1,2}):(\d{2})/;
    const match = timeStr.match(timeRegex);
    if (!match) return null;

    let [, prefix, hourStr, minuteStr] = match;
    let hours = parseInt(hourStr, 10);
    const minutes = parseInt(minuteStr, 10);

    prefix = prefix || contextPrefix;

    if ((prefix === '오후' || prefix === 'PM') && hours !== 12) hours += 12;
    else if ((prefix === '오전' || prefix === 'AM') && hours === 12) hours = 0;

    return hours * 60 + minutes;
  };

  const operatingPeriods: { start: number; end: number; startText: string; endText: string }[] = [];

  for (const block of hourBlocks) {
    const parts = block.split('~').map(p => p.trim());
    if (parts.length !== 2) continue;

    const [startStr, endStr] = parts;
    const startPrefixMatch = startStr.match(/(오전|오후|AM|PM)/);
    const startContext = startPrefixMatch ? startPrefixMatch[0] : (locale.value === 'ko' ? "오전" : "AM");

    const startTime = parseTimeWithContext(startStr, null);
    const endTime = parseTimeWithContext(endStr, startContext);

    if (startTime !== null && endTime !== null) {
      let correctedEndTime = endTime;
      if (endTime < startTime) {
        correctedEndTime += 24 * 60;
      }
      operatingPeriods.push({
        start: startTime,
        end: correctedEndTime,
        startText: startStr,
        endText: endStr,
      });
    }
  }

  if (operatingPeriods.length === 0) {
    return { status: 'UNKNOWN', message: t('waitingAnalyzer.errors.openingInfo.unknownTime') };
  }

  let targetTimeInMinutes: number;
  if (selectedTimeValue === 'now') {
    targetTimeInMinutes = now.getHours() * 60 + now.getMinutes();
  } else {
    const startHour = parseInt(selectedTimeValue.split('-')[0], 10);
    targetTimeInMinutes = startHour * 60;
  }

  const firstOpeningTime = operatingPeriods[0].start;
  const lastClosingTime = operatingPeriods[operatingPeriods.length - 1].end;

  let adjustedTargetTime = targetTimeInMinutes;
  if (lastClosingTime >= 24 * 60 && targetTimeInMinutes < firstOpeningTime) {
    adjustedTargetTime += 24 * 60;
  }

  if (adjustedTargetTime < firstOpeningTime) {
    return { status: 'BEFORE_OPENING', message: t('waitingAnalyzer.steps.notAvailable.states.before_opening.messageTemplate', { startTime: operatingPeriods[0].startText }) };
  }

  if (adjustedTargetTime >= lastClosingTime) {
    return { status: 'AFTER_CLOSING', message: t('waitingAnalyzer.steps.notAvailable.states.after_closing.message') };
  }

  for (const period of operatingPeriods) {
    if (adjustedTargetTime >= period.start && adjustedTargetTime < period.end) {
      return { status: 'OPERATIONAL', message: t('waitingAnalyzer.steps.notAvailable.states.operational.message') };
    }
  }

  for (let i = 0; i < operatingPeriods.length - 1; i++) {
    if (adjustedTargetTime >= operatingPeriods[i].end && adjustedTargetTime < operatingPeriods[i + 1].start) {
      return { status: 'BREAK_TIME', message: t('waitingAnalyzer.steps.notAvailable.states.break_time.messageTemplate', { startTime: operatingPeriods[i].endText, endTime: operatingPeriods[i+1].startText }) };
    }
  }

  return { status: 'UNKNOWN', message: t('waitingAnalyzer.errors.openingInfo.unknownStatus') };
};

/**
 * 주변 상권 정보 조회
 */
const getSurroundingData = async () => {
  const payload = {
    name: selectedStore.value.nameKo || selectedStore.value.name,
    simpleAddress: selectedStore.value.simpleAddress,
  }
  try {
    const response = await Api.post(ApiUrls.SURROUNDING_INFO, payload);
    analysis.surroundingInfo = response.data;
    console.log("주변 상권 정보:", response.data);
  } catch (error) {
    console.error("주변 상권 정보 조회 실패:", error);
    analysis.surroundingInfo = null; // 실패 시 null 처리
  }
};

/**
 * 데이터 분석 flow
 */
const startAnalysis = async () => {
  // 1. 프로그레스 초기화
  Object.keys(progress.value).forEach(k => progress.value[k] = false);

  const delay = (ms: number) => new Promise(res => setTimeout(res, ms));

  const openingInfo = await getOpeningInfo();
  analysis.openingInfo = openingInfo;
  await delay(300);
  progress.value.opening = true;

  // 헬퍼 함수를 호출하여 현재의 정확한 상태를 파악
  const currentState = checkBusinessStateForSelectedTime(openingInfo, selectedTime.value);

  // 'OPERATIONAL' 상태가 아니면, 분석을 중단하고 상태에 맞는 메시지를 표시
  if (currentState.status !== 'OPERATIONAL') {
    const stateKey = currentState.status.toLowerCase().replace('_today', '');
    notAvailableInfo.emoji = t(`waitingAnalyzer.steps.notAvailable.states.${stateKey}.emoji`);
    notAvailableInfo.title = t(`waitingAnalyzer.steps.notAvailable.states.${stateKey}.title`);
    notAvailableInfo.message = currentState.message;
    step.value = 'notAvailable';
    return;
  }

  // 4. 나머지 데이터 순차적으로 수집
  await getWeatherInfo();
  await delay(300);
  progress.value.weather = true;

  await countReviews();
  await delay(300);
  progress.value.reviews = true;

  await getHolidayInfo();
  await delay(300);
  progress.value.holiday = true;

  await getDataTrend();
  await delay(300);
  progress.value.sns = true;

  await getSurroundingData();
  await delay(300);
  progress.value.surround = true;

  // 5. 모든 데이터 수집 후 점수 계산
  // 약간의 지연을 주어 로딩 애니메이션이 보이도록 함
  setTimeout(() => {
    calculateScore();
  }, 500);
};

const calculateScore = () => {
  let totalScore = 0;
  const details = [];

  // 시간/요일 점수 계산 (사용자 선택 및 실제 데이터 기반)
  if (analysis.holidayInfo) {
    const { holidayOrWeekend, todayDayOfWeek } = analysis.holidayInfo;
    const dayInKorean = t(`waitingAnalyzer.analysis.conditions.dayOfWeek.${todayDayOfWeek}`);

    let timeScore = 0;
    let timeDescription = '';
    let targetHour = selectedTime.value === 'now' ? new Date().getHours() : parseInt(selectedTime.value.split('-')[0], 10);

    if (targetHour >= 10 && targetHour < 12) { timeDescription = t('waitingAnalyzer.analysis.conditions.timeOfDay.morning'); timeScore = 5; }
    else if (targetHour >= 12 && targetHour < 14) { timeDescription = t('waitingAnalyzer.analysis.conditions.timeOfDay.lunchPeak'); timeScore = 15; }
    else if (targetHour >= 14 && targetHour < 17) { timeDescription = t('waitingAnalyzer.analysis.conditions.timeOfDay.afternoon'); timeScore = -10; }
    else if (targetHour >= 17 && targetHour < 21) { timeDescription = t('waitingAnalyzer.analysis.conditions.timeOfDay.dinnerPeak'); timeScore = 20; }
    else { timeDescription = t('waitingAnalyzer.analysis.conditions.timeOfDay.lateNight'); timeScore = -10; }

    if (holidayOrWeekend) {
      if (timeDescription.includes(t('waitingAnalyzer.terms.peak'))) timeScore += 15;
      else timeScore += 10;
    }
    if (!holidayOrWeekend && todayDayOfWeek === 'FRIDAY' && timeDescription === t('waitingAnalyzer.analysis.conditions.timeOfDay.dinnerPeak')) {
      timeScore += 5;
    }

    let finalCondition = `${dayInKorean} ${timeDescription}`;
    if (holidayOrWeekend && !['SATURDAY', 'SUNDAY'].includes(todayDayOfWeek)) {
      finalCondition = `${t('waitingAnalyzer.analysis.conditions.holiday')} ${timeDescription}`;
    }
    if (selectedTime.value !== 'now') {
      const selectedSlot = timeSlots.value.find(slot => slot.value === selectedTime.value);
      if (selectedSlot) finalCondition += ` (${selectedSlot.label})`;
    }

    details.push({
      factor: t('waitingAnalyzer.analysis.scoreFactors.timeAndDay'),
      condition: finalCondition,
      score: timeScore,
      apiInfo: { name: t('waitingAnalyzer.api.dataPortalHoliday'), logo: publicDataPortalLogo }
    });
    totalScore += timeScore;
  }

  if (numberOfPeople.value > 1) {
    let peopleScore = 0;
    let peopleCondition = t('waitingAnalyzer.analysis.conditions.headcount', { count: numberOfPeople.value });
    if (numberOfPeople.value >= 5) {
      peopleScore = 15;
      peopleCondition += ` ${t('waitingAnalyzer.analysis.conditions.headcountGroup')}`;
    } else {
      peopleScore = 5;
    }
    details.push({ factor: t('waitingAnalyzer.analysis.scoreFactors.headcount'), condition: peopleCondition, score: peopleScore });
    totalScore += peopleScore;
  }

  if (analysis.reviewCount) {
    let reviewScore = 0;
    if (analysis.reviewCount > 1000) reviewScore = 15;
    else if (analysis.reviewCount > 500) reviewScore = 10;
    else if (analysis.reviewCount > 100) reviewScore = 5;
    if (reviewScore > 0) {
      details.push({
        factor: t('waitingAnalyzer.analysis.scoreFactors.reviews'),
        condition: t('waitingAnalyzer.analysis.conditions.reviewCount', { count: new Intl.NumberFormat().format(analysis.reviewCount) }),
        score: reviewScore,
        apiInfo: { name: t('waitingAnalyzer.api.naverDevelopers'), logo: naverApiLogo }
      });
      totalScore += reviewScore;
    }
  }

  if (analysis.weatherInfo) {
    const weather = analysis.weatherInfo;
    const temp = parseInt(weather.temperature, 10);
    let weatherCondition = '';
    let weatherScore = 0;

    if (weather.precipitation && weather.precipitation !== '없음' && weather.precipitation !== 'No precipitation') {
      weatherCondition = t('waitingAnalyzer.analysis.conditions.weather.heavyRain', { precipitation: weather.precipitation, temp });
      if (weather.precipitation.includes('비') || weather.precipitation.includes('소나기') || weather.precipitation.toLowerCase().includes('rain')) weatherScore = -15;
      else if (weather.precipitation.includes('눈') || weather.precipitation.toLowerCase().includes('snow')) weatherScore = -10;
    } else if (temp >= 30) { weatherCondition = t('waitingAnalyzer.analysis.conditions.weather.heatWave', { temp }); weatherScore = -15; }
    else if (temp <= 0) { weatherCondition = t('waitingAnalyzer.analysis.conditions.weather.coldWave', { temp }); weatherScore = -10; }
    else if (weather.sky === '맑음' || weather.sky.toLowerCase() === 'clear') { weatherCondition = t('waitingAnalyzer.analysis.conditions.weather.clear', { temp }); weatherScore = 10; }
    else if (weather.sky === '흐림' || weather.sky.toLowerCase() === 'cloudy') { weatherCondition = t('waitingAnalyzer.analysis.conditions.weather.cloudy', { temp }); weatherScore = -5; }
    else if (weather.sky === '구름많음' || weather.sky.toLowerCase().includes('cloudy')) { weatherCondition = t('waitingAnalyzer.analysis.conditions.weather.mostlyCloudy', { temp }); weatherScore = 0; }

    if (weatherScore !== 0) {
      details.push({
        factor: t('waitingAnalyzer.analysis.scoreFactors.weather'),
        condition: weatherCondition,
        score: weatherScore,
        apiInfo: { name: t('waitingAnalyzer.api.dataPortalWeather'), logo: publicDataPortalLogo }
      });
      totalScore += weatherScore;
    }
  }

  if (analysis.trendInfo && analysis.trendInfo.length >= 2) {
    const trendData = [...analysis.trendInfo];
    const now = new Date();
    const latestData = trendData[trendData.length - 1];
    const [latestYear, latestMonth] = latestData.period.split('-').map(Number);
    let latestRatio = latestData.ratio;

    if (latestYear === now.getFullYear() && latestMonth === now.getMonth() + 1 && now.getDate() > 1) {
      const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
      latestRatio = Math.min((latestData.ratio / now.getDate()) * daysInMonth, 100);
    }

    const change = latestRatio - trendData[trendData.length - 2].ratio;
    let trendCondition = '';
    let trendScore = 0;

    if (change > 20) { trendScore = 15; trendCondition = t('waitingAnalyzer.analysis.conditions.trend.skyrocketing'); }
    else if (latestRatio > 85) { trendScore = 10; trendCondition = t('waitingAnalyzer.analysis.conditions.trend.peakInterest'); }
    else if (change > 5) { trendScore = 8; trendCondition = t('waitingAnalyzer.analysis.conditions.trend.rising'); }
    else if (change < -10) { trendScore = -5; trendCondition = t('waitingAnalyzer.analysis.conditions.trend.declining'); }
    else { trendScore = 5; trendCondition = t('waitingAnalyzer.analysis.conditions.trend.steady'); }

    if (trendScore !== 0) {
      details.push({
        factor: t('waitingAnalyzer.analysis.scoreFactors.trend'),
        condition: trendCondition,
        score: trendScore,
        apiInfo: { name: t('waitingAnalyzer.api.naverDataLab'), logo: naverDataLabLogo }
      });
      totalScore += trendScore;
    }
  }

  if (analysis.surroundingInfo) {
    const { hotPlaceCount, subwayStationCount, universityCount, officeBuildingCount } = analysis.surroundingInfo;
    let surroundingScore = 0;
    const conditions = [];

    if (hotPlaceCount > 5) {
      if (hotPlaceCount > 50) surroundingScore += 15;
      else if (hotPlaceCount > 20) surroundingScore += 10;
      else surroundingScore += 5;
      conditions.push(t('waitingAnalyzer.analysis.conditions.surrounding.hotspot', { count: hotPlaceCount }));
    }

    let areaTypeScore = 0;
    let areaType = '';
    if (subwayStationCount > 0) { areaTypeScore = 15; areaType = t('waitingAnalyzer.analysis.conditions.surrounding.stationArea'); }
    if (universityCount > 0 && areaTypeScore < 10) { areaTypeScore = 10; areaType = t('waitingAnalyzer.analysis.conditions.surrounding.universityArea'); }
    if (officeBuildingCount > 5 && areaTypeScore < 10) { areaTypeScore = 10; areaType = t('waitingAnalyzer.analysis.conditions.surrounding.officeArea'); }

    if (areaType) {
      surroundingScore += areaTypeScore;
      conditions.unshift(areaType);
    }

    if (surroundingScore !== 0) {
      details.push({
        factor: t('waitingAnalyzer.analysis.scoreFactors.surrounding'),
        condition: conditions.join(', ') || t('waitingAnalyzer.analysis.conditions.surrounding.normal'),
        score: surroundingScore,
        apiInfo: { name: t('waitingAnalyzer.api.kakaoDevelopers'), logo: kakaoApiLogo }
      });
      totalScore += surroundingScore;
    }
  }

  scoreDetails.value = details;
  generateFinalResult(totalScore);
};

const generateFinalResult = (totalScore: any) => {
  let resultKey = '';
  if (totalScore >= 70) resultKey = 'veryCrowded';
  else if (totalScore >= 50) resultKey = 'crowded';
  else if (totalScore >= 30) resultKey = 'moderate';
  else if (totalScore >= 10) resultKey = 'calm';
  else resultKey = 'quiet';

  result.value = {
    totalScore,
    waitingIndex: t(`waitingAnalyzer.analysis.results.${resultKey}.index`),
    message: t(`waitingAnalyzer.analysis.results.${resultKey}.message`),
    emoji: t(`waitingAnalyzer.analysis.results.${resultKey}.emoji`)
  };
  step.value = 'result';
};

const calculatePopoverWidth = (apiInfo: any): number => {
  // apiInfo 객체나 name 속성이 없으면 기본 너비 200을 반환합니다.
  if (!apiInfo || !apiInfo.name) {
    return 200;
  }

  // 1. Popover 내부 레이아웃의 고정 값들을 정의합니다.
  const internalPadding = 16;  // Popover 자체의 좌우 패딩 합 (padding: 8px * 2)
  const logoWidth = 24;        // 로고 이미지(.api-logo)의 너비
  const gap = 12;              // 로고와 텍스트 사이의 간격(gap)

  // 2. 텍스트의 너비를 추정합니다.
  const text = apiInfo.name as string;
  let estimatedTextWidth = 0;

  // 글자 종류에 따라 너비를 다르게 계산하여 정확도를 높입니다.
  for (const char of text) {
    // 한글 범위(가-힣)에 해당하는 경우
    if (char.match(/[\uac00-\ud7af]/)) {
      estimatedTextWidth += 14; // 한글은 14px로 계산
    } else if (char.match(/[A-Z]/)) {
      estimatedTextWidth += 9; // 대문자는 9px로 계산
    } else {
      estimatedTextWidth += 8;  // 영문 소문자, 숫자, 공백, 특수문자는 8px로 계산
    }
  }

  // 3. 모든 요소의 너비를 합산하여 최종 너비를 계산합니다.
  const calculatedWidth = internalPadding + logoWidth + gap + estimatedTextWidth;

  // 4. 계산된 너비에 약간의 여유분(10px)을 더하고, 10단위로 올림하여 깔끔한 값으로 만듭니다.
  return Math.ceil((calculatedWidth + 10) / 10) * 10;
};


const reset = () => {
  step.value = 'search';
  searchQuery.value = '';
  foundStores.value = [];
  selectedStore.value = null;
  result.value = null;
  scoreDetails.value = [];
  analysis.trendInfo = null;
  analysis.holidayInfo = null;
  fetchRecommendedStores();
  fetchMyArchiveStores();
};
</script>

<template>
  <div class="estimator-container">
    <div class="card">
      <div class="card-header">
        <div style="display: flex; align-items: center; justify-content: center;">
          <h1 class="title">{{ t('waitingAnalyzer.title') }}</h1>
          <div class="icon-flipper">
            <img class="title-icon" :src="Graph" alt="분석 아이콘" />
          </div>
        </div>
        <p class="subtitle">{{ t('waitingAnalyzer.subtitle') }}</p>
      </div>

      <!-- 1. 초기 검색 단계 (수정됨) -->
      <div v-if="step === 'search'" class="card-body search-step-body">
        <!-- 검색 UI를 감싸는 컨테이너 추가 -->
        <div class="search-container">
          <div class="search-form">
            <el-input
                v-model="searchQuery"
                :placeholder="t('waitingAnalyzer.search.placeholder')"
                @keyup.enter="searchStores(searchQuery)"
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

                  <!-- Popover 내용물  -->
                  <div class="tip-accent-themed">
                    <p class="tip-accent-title">
                      <el-icon><InfoFilled /></el-icon>
                      {{ t('waitingAnalyzer.search.tipTitle') }}
                    </p>
                    <i18n-t keypath="waitingAnalyzer.search.tipDescription" tag="p" class="tip-accent-description">
                      <template #limit>
                        <strong>{{ t('waitingAnalyzer.search.limitText') }}</strong>
                      </template>
                    </i18n-t>
                  </div>

                </el-popover>
              </template>
            </el-input>

            <el-button
                @click="searchStores(searchQuery)"
                :disabled="!searchQuery"
                :class="{ 'is-disabled': !searchQuery }"
                class="search-button"
            >
              {{ t('waitingAnalyzer.search.button') }}
            </el-button>
          </div>
        </div>

        <!-- 가게 추천 영역 -->
        <div class="info-section">
          <div class="info-block">
            <div class="info-title-wrapper">
              <img class="highlight-icon" :src="stars" alt="별 아이콘" />
              <h3 class="info-title">{{ t('waitingAnalyzer.recommendations.title') }}</h3>
              <el-button
                  :icon="Refresh"
                  circle
                  size="small"
                  @click="fetchRecommendedStores"
                  :loading="isRecommendationLoading"
                  class="refresh-btn"
              />
            </div>

            <!-- 로딩 중일 때 스켈레톤 UI 표시 -->
            <div v-if="isRecommendationLoading" class="recommend-list skeleton">
              <div class="skeleton-item" v-for="i in 6" :key="i"></div>
            </div>

            <ServiceErrorState
                v-else-if="isRecommendationUnavailable"
                :title="t('waitingAnalyzer.errors.recommendationTitle')"
                :message="t('waitingAnalyzer.errors.recommendationMessage')"
                :show-fallback-action="true"
                :fallback-action-text="t('waitingAnalyzer.errors.showDefaultRecommendations')"
                :retry-action-text="t('waitingAnalyzer.errors.retry')"
                @retry="fetchRecommendedStores"
                @fallback="handleShowDefaultRecommendations"
            />

            <!-- 로딩 완료 후 목록 표시 -->
            <ul v-else class="recommend-list">
              <li
                  v-for="store in recommendedStores"
                  :key="store.name"
                  @click="selectRecommendedStore(store)"
              >
                <span class="store-name">{{ store.name }}</span>
                <span class="store-category">{{ store.category.split(' > ').pop() }}</span>
              </li>
            </ul>
          </div>
        </div>

        <!-- My Archive 섹션 시작 -->
        <div class="info-block">
          <div class="info-title-wrapper">
            <img class="highlight-icon" :src="archive" alt="저장소 아이콘" />
            <h3 class="info-title">{{ t('waitingAnalyzer.archive.title') }}</h3>
          </div>

          <!-- 서비스 장애 시 UI -->
          <div v-if="isArchiveUnavailable" class="service-unavailable-box archive-style">
            <el-icon :size="20"><WarningFilled /></el-icon>
            <span>{{ t('waitingAnalyzer.recommendations.errorMessage2') }}</span>
          </div>

          <el-skeleton :rows="3" animated v-if="isArchiveLoading" class="archive-skeleton" />

          <div v-else class="archive-list">
            <div
                v-for="store in myArchiveStores"
                :key="store.id"
                class="archive-list-item"
            >
              <!-- 정보 영역 -->
              <div class="item-info">
                <span class="item-name">{{ store.name }}</span>
                <span class="item-category">{{ store.category }}</span>
                <span class="item-date">{{ t('waitingAnalyzer.archive.visitDatePrefix') }}{{ store.visitDate }}</span>
              </div>

              <!-- 액션 버튼 영역: 클래스 이름 변경 -->
              <div class="archive-item-actions">
                <el-tooltip :content="t('waitingAnalyzer.archive.tooltipAnalyze')" placement="top">
                  <el-button
                      type="primary"
                      :icon="Search"
                      circle
                      plain
                      @click="selectRecommendedStore(store)"
                      class="action-btn"
                  />
                </el-tooltip>
                <el-tooltip :content="t('waitingAnalyzer.archive.tooltipDetails')" placement="top">
                  <el-button
                      :icon="MoreFilled"
                      circle
                      @click="goToArchiveDetail(store.id)"
                      class="action-btn"
                  />
                </el-tooltip>
              </div>
            </div>
            <el-empty
                v-if="myArchiveStores.length === 0"
                :description="t('waitingAnalyzer.archive.emptyDescription')"
                :image-size="80"
                style="padding: 20px;"
            >
              <template #image>
                <img
                    :src="emptyBox"
                    alt="비어있는 아카이브"
                    class="custom-empty-image"
                />
              </template>
            </el-empty>
          </div>
        </div>
        <!-- My Archive 섹션 끝 -->

      </div>

      <!-- 2. 지점 선택 단계 -->
      <div v-if="step === 'selectStore'" class="card-body">
        <h2 class="step-title">{{ t('waitingAnalyzer.steps.selectStore.title') }}</h2>
        <ul class="store-list">
          <li
              v-for="store in foundStores"
              :key="store.id"
              @click="selectStore(store)"
          >
            <el-text>{{ store.name }}</el-text>
            <span>{{ store.simpleAddress }}</span>
          </li>
        </ul>
        <button class="back-button" @click="reset">{{ t('waitingAnalyzer.steps.selectStore.back') }}</button>
      </div>

      <!-- 방문 시간 선택 단계 -->
      <div v-if="step === 'selectTime'" class="card-body">
        <div class="input-group">
          <h3 class="input-label">{{ t('waitingAnalyzer.steps.selectTime.peopleLabel') }}</h3>
          <el-input-number
              v-model="numberOfPeople"
              :min="1"
              :max="10"
              size="large"
              controls-position="right"
              style="width: 100%;"
          />
        </div>
        <h2 class="step-title">{{ t('waitingAnalyzer.steps.selectTime.timeLabel') }}</h2>
        <button
            class="skip-time-btn"
            @click="selectedTime = 'now'; confirmTimeAndAnalyze()"
        >
          <span>{{ t('waitingAnalyzer.steps.selectTime.skipButton') }}</span>
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
          <button class="back-button" @click="step = 'selectStore'">{{ t('waitingAnalyzer.steps.selectTime.back') }}</button>
          <button class="right-button"@click="confirmTimeAndAnalyze" :disabled="!selectedTime">{{ t('waitingAnalyzer.steps.selectTime.analyze') }}</button>
        </div>
      </div>

      <!-- 3. 데이터 분석 중 (로딩) 단계 -->
      <div v-if="step === 'loading'" class="card-body loading-state">
        <div class="spinner"></div>
        <h2 class="step-title">{{ t('waitingAnalyzer.steps.loading.title', { storeName: selectedStore.name }) }}</h2>
        <p class="loading-message">{{ t('waitingAnalyzer.steps.loading.message') }}</p>
        <div class="progress-list">
          <p :class="{ done: progress.opening }">{{ t('waitingAnalyzer.steps.loading.progress.opening') }}</p>
          <p :class="{ done: progress.weather }">{{ t('waitingAnalyzer.steps.loading.progress.weather') }}</p>
          <p :class="{ done: progress.reviews }">{{ t('waitingAnalyzer.steps.loading.progress.reviews') }}</p>
          <p :class="{ done: progress.holiday }">{{ t('waitingAnalyzer.steps.loading.progress.holiday') }}</p>
          <p :class="{ done: progress.sns }">{{ t('waitingAnalyzer.steps.loading.progress.sns') }}</p>
          <p :class="{ done: progress.surround }">{{ t('waitingAnalyzer.steps.loading.progress.surround') }}</p>
        </div>
      </div>

      <!-- 4. 결과 표시 단계 (대대적 개선) -->
      <div v-if="step === 'result'" class="card-body result-state">
        <!-- 상단 요약 결과 -->
        <div class="result-summary">
          <span class="result-emoji">{{ result.emoji }}</span>
          <div class="result-text">
            <h2 class="result-index">
              <i18n-t keypath="waitingAnalyzer.steps.result.summary" tag="span">
                <template #storeName>{{ selectedStore.name }}</template>
                <template #status><span :class="result.waitingIndex">{{ result.waitingIndex }}</span></template>
              </i18n-t>
            </h2>
            <p class="result-message">{{ result.message }}</p>
          </div>
        </div>

        <!-- 상세 점수 분석 (스크롤 영역) -->
        <div class="score-details">
          <h3 class="details-title">{{ t('waitingAnalyzer.steps.result.detailsTitle') }}</h3>
          <ul class="details-list">
            <li v-for="(detail, index) in scoreDetails" :key="index">
              <!-- 요인 이름과 정보 아이콘을 함께 묶음 -->
              <div class="factor-container">
                <el-popover
                    v-if="detail.apiInfo"
                    placement="left"
                    :width="calculatePopoverWidth(detail.apiInfo)"
                    trigger="click"
                    popper-class="api-info-popover"
                >
                  <!-- Popover를 트리거할 아이콘 -->
                  <template #reference>
                    <el-icon class="info-icon-detail"><InfoFilled /></el-icon>
                  </template>
                  <!-- Popover 내용 -->
                  <div class="api-info-content">
                    <img :src="detail.apiInfo.logo" class="api-logo" alt="API Logo" />
                    <div class="api-text-content">
                      <p class="api-name">{{ detail.apiInfo.name }}</p>
                      <p class="api-description">{{ detail.apiInfo.description }}</p>
                    </div>
                  </div>
                </el-popover>

                <!-- 팩터 텍스트는 Popover 뒤로 이동 -->
                <span class="factor">{{ detail.factor }}</span>
              </div>

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
            <span class="factor">{{ t('waitingAnalyzer.steps.result.totalScoreLabel') }}</span>
            <span class="score">{{ result.totalScore }}</span>
          </div>
          <el-button type="primary" class="reset-button" @click="reset">{{ t('waitingAnalyzer.steps.result.reset') }}</el-button>
        </div>
      </div>

      <!-- 휴무일 예외 처리 -->
      <div v-if="step === 'notAvailable'" class="card-body not-available-state">
        <span class="result-emoji">{{ notAvailableInfo.emoji }}</span>
        <h2 class="result-index">{{ notAvailableInfo.title }}</h2>
        <p class="result-message">{{ notAvailableInfo.message }}</p>

        <!-- 영업시간 정보는 모든 '운영 안 함' 상태에서 유용하므로 그대로 유지 -->
        <div v-if="analysis.openingInfo && analysis.openingInfo.weekdayText" class="opening-hours-closed">
          <h3 class="details-title">
            {{ t('waitingAnalyzer.steps.notAvailable.storeInfoTitle') }}
            <el-popover
                placement="right-end"
                :width="180"
                popper-class="api-info-popover"
            >
              <template #reference>
                <el-icon class="info-icon-detail"><InfoFilled /></el-icon>
              </template>
              <div class="api-info-content">
                <img :src="googleApiLogo" class="api-logo" alt="API Logo" />
                <div class="api-text-content">
                  <p class="api-name">Google Cloud API</p>
                </div>
              </div>
            </el-popover>
          </h3>
          <ul class="hours-list-closed">
            <li
                v-for="(text, index) in analysis.openingInfo.weekdayText"
                :key="index"
                :class="{ 'is-today': (new Date().getDay() === 0 ? 6 : new Date().getDay() - 1) === index }"
            >
              {{ translateWeekdayText(text) }}
            </li>
          </ul>
        </div>

        <button class="reset-button" @click="reset">{{ t('waitingAnalyzer.steps.notAvailable.reset') }}</button>
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
.icon-flipper {
  /* 3D 효과를 위한 원근감 설정 */
  perspective: 1000px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-flipper .title-icon {
  width: 24px; /* 크기 조절 */
  height: 24px;
  margin-left: 6px;
  transform-style: preserve-3d;
  transition: transform 0.6s cubic-bezier(0.68, -0.55, 0.27, 1.55);
  filter: invert(1);
}
html.dark .title-icon {
  /*
    invert(1)은 이미지의 모든 색상을 완전히 반전시킵니다.
    검은색은 흰색으로, 흰색은 검은색으로 바뀝니다.
  */
  filter: invert(0);
}

/* 마우스를 올렸을 때 Y축으로 180도 회전 */
.icon-flipper:hover .title-icon {
  transform: rotateY(180deg);
}

.highlight-icon {
  width: 24px; /* 크기 조절 */
  height: 24px;
  margin-left: 6px;
  margin-right: 4px;
  transform-style: preserve-3d;
  transition: transform 0.6s cubic-bezier(0.68, -0.55, 0.27, 1.55);
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
  max-width: 818px;
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
  display: flex;
  flex-direction: column;
  justify-content: space-between;
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
  border-radius: 4px;
}

.step-title {
  text-align: center; margin-top: 30px; margin-bottom: 20px; font-weight: 500; font-size: 1.1rem; color: var(--el-color-primary); flex-shrink: 0; }
.search-form { display: flex; gap: 8px; margin: auto 0; }
input[type="text"] {
  flex-grow: 1;
  padding: 12px;
  /* border-color를 조금 더 진한 색으로 변경하여 항상 보이게 함 */
  border: 2px solid #ccc;
  border-radius: 6px;
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
  border-top: none;
  margin-top: 0;
  flex-grow: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.info-block {
  margin-top: 24px;
  text-align: center;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.info-title-wrapper {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  margin-bottom: 8px;
}
.info-title {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--el-color-primary);
  margin: 0;
}
.refresh-btn {
  /* 1. 버튼의 기본 스타일을 모두 제거합니다 */
  background: none;
  border: none;
  padding: 4px; /* 클릭 영역을 위해 최소한의 패딩 유지 */
  margin: 0 0 0 4px;
  height: auto; /* Element UI의 고정 높이 제거 */

  /* 2. 아이콘 색상을 주변 텍스트와 비슷하게 맞춰 이질감을 줄입니다 */
  color: var(--el-text-color-secondary);

  /* 3. 부드러운 전환 효과 */
  transition: all 0.3s ease;
}

.refresh-btn:hover {
  /* 4. 마우스를 올렸을 때만 색상과 회전 효과로 상호작용을 유도합니다 */
  color: var(--el-color-primary); /* 메인 색상으로 강조 */
  transform: rotate(180deg) scale(1.1); /* 회전하며 약간 커지는 효과 */
  background-color: var(--el-fill-color-light); /* 아주 연한 배경색으로 클릭 영역 표시 */
}
.service-unavailable-box {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
  height: 120px;
  margin-top: 10px;
  padding: 10px;
  border-radius: 8px;
  background-color: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
  font-size: 0.85rem;
  font-weight: 500;
  border: 1px solid var(--el-color-danger-light-7);
}
/* 저장소 섹션의 장애 박스는 높이가 다르므로 별도 스타일 적용 */
.service-unavailable-box.archive-style {
  height: 165px; /* 저장소 목록의 높이와 맞춤 */
  border-style: dashed;
}
.recommend-list {
  list-style: none;
  padding: 4px 0 0 0;
  margin: 0;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  height: 165px;
  overflow-y: auto;
}
.recommend-list.skeleton {
  height: 165px;
  overflow: hidden;
}
.recommend-list li {
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
  border: 1px solid var(--el-border-color-lighter);
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}
.recommend-list li:hover {
  transform: translateY(-2px);
  box-shadow: var(--el-box-shadow-lighter);
  border-color: var(--el-color-primary-light-5);
}
.store-name {
  font-weight: 600;
  font-size: 0.85rem;
  white-space: normal;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}
.store-category {
  font-size: 0.75rem;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
/* 로딩 스켈레톤 스타일 */
.recommend-list.skeleton {
  gap: 10px;
}
.skeleton-item {
  height: 56px; /* li 아이템의 높이와 유사하게 설정 */
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  border-radius: 8px;
}
@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.archive-skeleton {
  padding: 0 10px;
}
.archive-list {
  flex-grow: 1;
  overflow-y: auto;
  min-height: 0;
  height: 165px;
  max-height: 165px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 2px 4px;
}
.archive-list::-webkit-scrollbar,
.recommend-list::-webkit-scrollbar {
  width: 4px;
}

.archive-list::-webkit-scrollbar-thumb,
.recommend-list::-webkit-scrollbar-thumb {
  background-color: var(--el-border-color-lighter);
  border-radius: 2px;
}

.archive-list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  text-align: left;
  border-bottom: 1px solid var(--el-border-color-extra-light);
}
.archive-list-item:last-child {
  border-bottom: none;
}
.archive-list-item:hover {
  background-color: var(--el-fill-color-light);
}
.item-actions .el-button {
  width: 28px;
  height: 28px;
}
.item-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  overflow: hidden;
}
.item-name {
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-category, .item-date {
  font-size: 0.75rem;
  color: var(--el-text-color-secondary);
}

.archive-item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 0.1;
  transform: scale(0.95); /* 살짝 작게 시작 */
  transition: opacity 0.2s ease-in-out, transform 0.2s ease-in-out;
  pointer-events: none; /* 숨겨져 있을 때 클릭 방지 */
}

.archive-list-item:hover .archive-item-actions {
  opacity: 1;
  transform: scale(1); /* 원래 크기로 */
  pointer-events: auto; /* 클릭 가능하게 */
}

.action-btn {
  /* 크기 */
  width: 28px;
  height: 28px;

  /* 아이콘 크기 */
  font-size: 14px;

  /* Element UI 기본 마진 제거 */
  margin: 0;

  /* 부드러운 전환 효과 */
  transition: all 0.2s ease;
}

/* 버튼에 마우스를 올렸을 때 효과 */
.action-btn:hover {
  transform: scale(1.15) rotate(10deg); /* 살짝 커지면서 약간 회전 */
  box-shadow: var(--el-box-shadow-lighter);
}

.custom-empty-image {
  /* image-size prop 대신 직접 크기를 제어할 수 있습니다. */
  width: 80px;
  /* 예시: 이미지에 회색 필터를 적용하여 톤을 맞춤 */
  filter: grayscale(1);
  opacity: 0.4;
  padding: 0;
}

button {
  padding: 12px 18px;
  background-color: var(--el-color-primary);
  color: var(--el-bg-color);
  border: none;
  border-radius: 6px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
  flex-shrink: 0;
}

button:hover {
  background-color: var(--el-color-primary-light-3);
}

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
.store-list li { margin-top: 4px; padding: 12px 15px; border: 1px solid var(--el-color-primary); border-radius: 6px; margin-bottom: 8px; cursor: pointer; transition: background-color 0.2s, border-color 0.2s, transform 0.2s; display: flex; justify-content: space-between; align-items: center; font-size: 0.9rem; }
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
  border-radius: 6px;
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
  border-radius: 6px;
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
.progress-list { text-align: left; background-color: #fafafa; padding: 10px 15px; border-radius: 6px; }
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
  border-radius: 6px;
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

.opening-hours-closed {
  width: 100%;
  max-width: 400px; /* 너무 넓어지지 않도록 제한 */
  margin: 25px auto 16px;
}

/* 결과 화면의 details-title 스타일을 재활용하거나 새로 정의 */
.opening-hours-closed .details-title {
  font-size: 0.9rem;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--el-text-color-secondary); /* 중앙 정렬된 화면에 맞게 톤 다운 */
}

.hours-list-closed {
  list-style: none;
  padding: 0;
  margin: 0;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  overflow: hidden; /* 자식 요소의 radius를 부모에 맞춤 */
  text-align: left; /* 부모의 text-align: center를 무시하고 좌측 정렬 */
}

.hours-list-closed li {
  padding: 8px 15px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  font-size: 0.8rem;
  color: var(--el-text-color-regular);
}

.hours-list-closed li:last-child {
  border-bottom: none;
}

/* 오늘 요일 하이라이트 스타일 (휴무일이므로 경고/주의 톤으로) */
.hours-list-closed li.is-today {
  background-color: var(--el-color-warning-light-9);
  color: var(--el-color-warning-dark-2);
  font-weight: 700;
}

.api-info-content {
  display: flex;
  align-items: center;
  gap: 12px;
}
.api-logo {
  width: 24px;
  height: 24px;
  object-fit: contain;
  border-radius: 4px;
  flex-shrink: 0;
}
.api-text-content p {
  margin: 0;
}
.api-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}
.info-icon-detail {
  cursor: pointer;
  color: var(--el-color-warning); /* Element Plus의 경고색 변수 사용 */
  font-size: 15px;
  transition: all 0.2s ease;
  /* 클릭 유도를 위해 살짝 떠오르는 효과 추가 */
  vertical-align: middle;
  margin-right: 3px;
}
.info-icon-detail:hover {
  color: var(--el-color-warning-light-3);
  transform: scale(1.15); /* 마우스를 올렸을 때 아이콘 확대 */
}
:global(.el-popper.api-info-popover) {
  /* 기존 스타일 */
  padding: 8px !important;
  border-radius: 8px !important;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: var(--el-box-shadow-light) !important;

  /* ★★★ 원하는 높이 값을 여기에 추가 ★★★ */
  height: 38px;

  /*
    팁: 고정 높이를 설정하면 내부 컨텐츠의 수직 정렬을 위해
    display: flex 와 align-items: center 를 함께 사용하는 것이 좋습니다.
    이렇게 하면 높이가 고정되어도 내용물이 항상 중앙에 위치합니다.
  */
  display: flex;
  align-items: center;
}

.tip-accent-themed {
  /* 폰트를 적용하여 가독성 향상 */
  font-family: 'Noto Sans KR', sans-serif;
  padding: 16px;

  /* 테마의 기본 배경색을 사용 (라이트/다크 모드 자동 전환) */
  background: var(--el-bg-color);
  border-radius: 6px;

  /* 테마의 그림자 스타일을 사용하여 일관성 유지 */
  box-shadow: var(--el-box-shadow-light);

  /* --- 그라디언트 테두리 핵심 로직 --- */
  border-top: 3px solid transparent;

  /*
    1. 첫 번째 그래디언트는 컴포넌트의 배경색을 담당합니다.
    2. 두 번째 그래디언트가 테두리 영역을 채우게 됩니다.
       - var(--primary-color) 와 var(--blue)를 사용해 세련된 색상 조합을 만듭니다.
  */
  background-image: linear-gradient(var(--el-bg-color), var(--el-bg-color)),
  linear-gradient(to right, var(--primary-color), var(--blue));
  background-origin: border-box;
  background-clip: padding-box, border-box;
}

.tip-accent-title {
  display: flex;
  align-items: center;
  margin: 0 0 8px 0;
  font-size: 15px;
  font-weight: 700; /* 좀 더 강조 */

  /* 테마의 기본 텍스트 색상 사용 */
  color: var(--el-text-color-primary);
}

.tip-accent-title .el-icon {
  margin-right: 8px;
  font-size: 18px;

  /* 아이콘 색상을 테두리의 시작 색상과 통일하여 디자인 안정감 부여 */
  color: var(--primary-color);
}

.tip-accent-description {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;

  /* 본문 내용은 일반 텍스트 색상을 사용 */
  color: var(--el-text-color-regular);
}

</style>