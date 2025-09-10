<script setup lang="ts">

import { reactive, ref } from 'vue';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { QuestionFilled, InfoFilled } from "@element-plus/icons-vue";

import publicDataPortalLogo from "@/assets/images/data-logo.jpeg";
import naverApiLogo from "@/assets/images/naver-api-logo.png";
import googleApiLogo from "@/assets/images/google_cloud_logo.png";

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
  opening: false,
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

  const response = await Api.post(ApiUrls.NAVER_STORE_SEARCH, {query: searchQuery.value});
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
})

const notAvailableInfo = reactive({
  emoji: '',
  title: '',
  message: '',
});

/**
 * 블로그 건수 조회
 */
const countReviews = async () => {
  const payload = {
    name: selectedStore.value.name,
    simpleAddress: selectedStore.value.simpleAddress,
    detailAddress: selectedStore.value.simpleAddress,
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
    name: selectedStore.value.name,
    simpleAddress: selectedStore.value.simpleAddress,
    detailAddress: selectedStore.value.simpleAddress,
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
    query: selectedStore.value.name
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
    name: selectedStore.value.name,
    simpleAddress: selectedStore.value.simpleAddress,
    detailAddress: selectedStore.value.simpleAddress,
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
 * 영업시간 파싱 및 현재 상태 판별 헬퍼 함수
 */
const checkBusinessStateForSelectedTime = (openingInfo: any, selectedTimeValue: string) => {
  if (!openingInfo || !openingInfo.weekdayText) {
    return { status: 'UNKNOWN', message: '영업 정보 확인 불가' };
  }

  const now = new Date();
  const dayIndex = now.getDay();
  const todayIndex = (dayIndex === 0) ? 6 : dayIndex - 1;
  const todayHoursText = openingInfo.weekdayText[todayIndex];

  if (!todayHoursText || typeof todayHoursText !== 'string') {
    return { status: 'UNKNOWN', message: '오늘의 영업 정보를 가져올 수 없습니다.' };
  }

  if (todayHoursText.includes('휴무일')) {
    return { status: 'CLOSED_TODAY', message: '오늘은 정기 휴무일입니다.' };
  }

  const colonIndex = todayHoursText.indexOf(':'); // 콜론의 위치를 찾습니다.

  if (colonIndex === -1 || todayHoursText.includes('정보 없음')) {
    return { status: 'UNKNOWN', message: '오늘의 영업 정보를 확인할 수 없습니다.' };
  }

  const timeInfoString = todayHoursText.substring(colonIndex + 1).trim();

  if (!timeInfoString) {
    return { status: 'UNKNOWN', message: '영업 시간 정보를 찾을 수 없습니다.' };
  }

  const hourBlocks = timeInfoString.split(',').map(s => s.trim());

  const parseTimeWithContext = (timeStr: string, contextPrefix: string | null) => {
    const timeRegex = /(오전|오후)?\s*(\d{1,2}):(\d{2})/;
    const match = timeStr.match(timeRegex);
    if (!match) return null;

    let [, prefix, hourStr, minuteStr] = match;
    let hours = parseInt(hourStr, 10);
    const minutes = parseInt(minuteStr, 10);

    prefix = prefix || contextPrefix;

    if (prefix === '오후' && hours !== 12) hours += 12;
    else if (prefix === '오전' && hours === 12) hours = 0;

    return hours * 60 + minutes;
  };

  const operatingPeriods: { start: number; end: number; startText: string; endText: string }[] = [];

  for (const block of hourBlocks) {
    const parts = block.split('~').map(p => p.trim());
    if (parts.length !== 2) continue;

    const [startStr, endStr] = parts;
    const startPrefixMatch = startStr.match(/(오전|오후)/);
    const startContext = startPrefixMatch ? startPrefixMatch[0] : "오전";

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
    return { status: 'UNKNOWN', message: '영업 시간 형식을 분석할 수 없습니다.' };
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
    return { status: 'BEFORE_OPENING', message: `선택하신 시간은 영업 시작 전입니다. (${operatingPeriods[0].startText} 시작)` };
  }

  if (adjustedTargetTime >= lastClosingTime) {
    return { status: 'AFTER_CLOSING', message: '선택하신 시간에는 이미 영업이 종료됩니다.' };
  }

  for (const period of operatingPeriods) {
    if (adjustedTargetTime >= period.start && adjustedTargetTime < period.end) {
      return { status: 'OPERATIONAL', message: '영업 중' };
    }
  }

  for (let i = 0; i < operatingPeriods.length - 1; i++) {
    if (adjustedTargetTime >= operatingPeriods[i].end && adjustedTargetTime < operatingPeriods[i + 1].start) {
      return { status: 'BREAK_TIME', message: `선택하신 시간은 브레이크 타임입니다 (${operatingPeriods[i].endText} ~ ${operatingPeriods[i+1].startText})` };
    }
  }

  return { status: 'UNKNOWN', message: '선택하신 시간의 운영 상태를 확인할 수 없습니다.' };
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
    switch (currentState.status) {
      case 'CLOSED_TODAY':
        notAvailableInfo.emoji = '💤';
        notAvailableInfo.title = '오늘은 휴무일입니다';
        break;
      case 'BREAK_TIME':
        notAvailableInfo.emoji = '☕';
        notAvailableInfo.title = '브레이크 타임입니다';
        break;
      case 'BEFORE_OPENING':
        notAvailableInfo.emoji = '⏳';
        notAvailableInfo.title = '영업 시작 전입니다';
        break;
      case 'AFTER_CLOSING':
        notAvailableInfo.emoji = '🌙';
        notAvailableInfo.title = '영업이 종료되었습니다';
        break;
      default: // 'UNKNOWN' 포함
        notAvailableInfo.emoji = '⚠️';
        notAvailableInfo.title = '운영 상태 확인 불가';
    }
    notAvailableInfo.message = currentState.message;
    step.value = 'notAvailable'; // 통합 '운영 안 함' 상태로 전환
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
    // 영문 요일을 한글로 변환하기 위한 맵
    const dayMap: { [key: string]: string } = {
      MONDAY: '월요일', TUESDAY: '화요일', WEDNESDAY: '수요일',
      THURSDAY: '목요일', FRIDAY: '금요일', SATURDAY: '토요일', SUNDAY: '일요일'
    };

    const { holidayOrWeekend, todayDayOfWeek } = analysis.holidayInfo;
    const dayInKorean = dayMap[todayDayOfWeek] || todayDayOfWeek;

    let timeScore = 0;
    let timeDescription = '';
    let targetHour: number;

    // 사용자가 '시간 미정'을 눌렀으면 현재 시간, 아니면 선택한 시간대의 시작 시간
    if (selectedTime.value === 'now') {
      targetHour = new Date().getHours();
    } else {
      targetHour = parseInt(selectedTime.value.split('-')[0], 10);
    }

    // 시간대에 따른 기본 점수 및 설명 설정
    if (targetHour >= 10 && targetHour < 12) { timeDescription = '오전'; timeScore = 5; }
    else if (targetHour >= 12 && targetHour < 14) { timeDescription = '점심 피크'; timeScore = 15; }
    else if (targetHour >= 14 && targetHour < 17) { timeDescription = '애매한 오후'; timeScore = -10; }
    else if (targetHour >= 17 && targetHour < 21) { timeDescription = '저녁 피크'; timeScore = 20; }
    else { timeDescription = '늦은 저녁'; timeScore = 10; }

    // 주말/공휴일 가중치 적용
    if (holidayOrWeekend) {
      // 피크 시간대에는 더 큰 가점 부여
      if (timeDescription.includes('피크')) {
        timeScore += 15;
      } else {
        timeScore += 10;
      }
    }

    // 금요일 저녁 특별 가중치 (공휴일이 아닌 평일 금요일)
    if (!holidayOrWeekend && todayDayOfWeek === 'FRIDAY' && timeDescription === '저녁 피크') {
      timeScore += 5;
    }

    // 결과 화면에 표시될 최종 텍스트 생성
    let finalCondition = `${dayInKorean} ${timeDescription}`;
    if (holidayOrWeekend && !['토요일', '일요일'].includes(dayInKorean)) {
      finalCondition = `공휴일 ${timeDescription}`; // 평일인데 공휴일인 경우
    }
    // 사용자가 특정 시간대를 선택했다면 괄호로 표시
    if (selectedTime.value !== 'now') {
      const selectedSlot = timeSlots.value.find(slot => slot.value === selectedTime.value);
      if (selectedSlot) finalCondition += ` (${selectedSlot.label})`;
    }

    details.push({
      factor: '시간/요일',
      condition: finalCondition,
      score: timeScore,
      apiInfo: {
        name: '공공데이터포털 (특일 정보)',
        logo: publicDataPortalLogo,
      }
    });
    totalScore += timeScore;
  }

  // 방문 인원수 점수
  if (numberOfPeople.value > 1) { // 3명 이상일 때만 점수 계산 및 표시
    let peopleScore = 0;
    let peopleCondition = `${numberOfPeople.value}명 방문`;

    if (numberOfPeople.value >= 5) {
      peopleScore = 15;
      peopleCondition += ' (단체)';
    } else { // 3-4명인 경우
      peopleScore = 5;
    }

    details.push({
      factor: '방문 인원',
      condition: peopleCondition,
      score: peopleScore
    });
    totalScore += peopleScore;
  }

  // 인지도(리뷰 수) 점수
  if (analysis.reviewCount) {
    let reviewScore = 0;
    if (analysis.reviewCount > 1000) reviewScore = 15;
    else if (analysis.reviewCount > 500) reviewScore = 10;
    else if (analysis.reviewCount > 100) reviewScore = 5;
    if (reviewScore > 0) {
      const formattedCount = new Intl.NumberFormat().format(analysis.reviewCount);

      details.push({
        factor: '인지도(리뷰 수)',
        condition: `리뷰 ${formattedCount}개`,
        score: reviewScore,
        apiInfo: {
          name: '네이버 Developers API',
          logo: naverApiLogo,
        }
      });

      totalScore += reviewScore;
    }
  }

  // 날씨 점수
  if (analysis.weatherInfo) {
    const weather = analysis.weatherInfo;
    const temp = parseInt(weather.temperature, 10);
    let weatherCondition = '';
    let weatherScore = 0;

    // 우선 순위: 강수 > 기온(폭염/한파) > 하늘 상태
    if (weather.precipitation && weather.precipitation !== '없음') {
      weatherCondition = `${weather.precipitation}, ${temp}°C`;
      if (weather.precipitation.includes('비') || weather.precipitation.includes('소나기')) {
        weatherScore = -15; // 비가 오면 외출을 꺼리므로 큰 감점
      } else if (weather.precipitation.includes('눈')) {
        weatherScore = -10; // 눈도 감점 요인
      }
    } else if (temp >= 30) {
      weatherCondition = `폭염 ${temp}°C`;
      weatherScore = -15; // 매우 더운 날씨
    } else if (temp <= 0) {
      weatherCondition = `한파 ${temp}°C`;
      weatherScore = -10; // 매우 추운 날씨
    } else if (weather.sky === '맑음') {
      weatherCondition = `맑음, ${temp}°C`;
      weatherScore = 10;  // 맑고 쾌적한 날씨는 큰 가점
    } else if (weather.sky === '흐림') {
      weatherCondition = `흐림, ${temp}°C`;
      weatherScore = -5;  // 흐린 날은 약간의 감점
    } else if (weather.sky === '구름많음') {
      weatherCondition = `구름많음, ${temp}°C`;
      weatherScore = 0;   // 구름 많은 날은 중립
    }

    // 점수에 영향이 있는 경우에만 상세 내역에 추가
    if (weatherScore !== 0) {
      details.push({
        factor: '현재 날씨',
        condition: weatherCondition,
        score: weatherScore,
        apiInfo: {
          name: '공공데이터포털 (기상청_단기예보)',
          logo: publicDataPortalLogo,
        }
      });
      totalScore += weatherScore;
    }
  }

  // 검색 트렌드 점수
  if (analysis.trendInfo && analysis.trendInfo.length >= 2) {
    const trendData = [...analysis.trendInfo]; // 원본 수정을 피하기 위해 배열 복사
    const now = new Date();9000450
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth() + 1; // getMonth()는 0부터 시작
    const currentDate = now.getDate();

    // 마지막 데이터가 현재 진행 중인 달인지 확인
    const latestData = trendData[trendData.length - 1];
    const [latestYear, latestMonth] = latestData.period.split('-').map(Number);

    let latestRatio = latestData.ratio;

    // 현재 진행 중인 달의 데이터라면 월말 기준으로 예측하여 보정
    if (latestYear === currentYear && latestMonth === currentMonth && currentDate > 1) {
      // 해당 월의 총 일수 구하기
      const daysInMonth = new Date(currentYear, currentMonth, 0).getDate();
      // 일일 평균 ratio 계산
      const dailyAverageRatio = latestData.ratio / currentDate;
      // 월말 예측 ratio 계산
      const projectedRatio = dailyAverageRatio * daysInMonth;

      // 보정된 값으로 업데이트 (최대값은 100을 넘지 않도록)
      latestRatio = Math.min(projectedRatio, 100);
      console.log(`데이터랩 보정: ${latestData.ratio.toFixed(2)} -> ${latestRatio.toFixed(2)} (예측)`);
    }

    const previousRatio = trendData[trendData.length - 2].ratio;
    const change = latestRatio - previousRatio;

    let trendCondition = '';
    let trendScore = 0;

    // 보정된 값을 기준으로 점수 계산
    if (change > 20) {
      trendScore = 15;
      trendCondition = '최근 검색량 급상승';
    } else if (latestRatio > 85) {
      trendScore = 10;
      trendCondition = '최고 수준의 관심도';
    } else if (change > 5) {
      trendScore = 8;
      trendCondition = '관심도 상승 추세';
    } else if (change < -10) {
      trendScore = -5;
      trendCondition = '관심도 하락 추세';
    } else {
      trendScore = 5;
      trendCondition = '꾸준한 관심도 유지';
    }

    if (trendScore !== 0) {
      details.push({ factor: '검색 트렌드', condition: trendCondition, score: trendScore });
      totalScore += trendScore;
    }
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
    message = '약간의 대기가 있을 수 있어요.';
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
          <p :class="{ done: progress.opening }">가게 운영 상태 확인</p>
          <p :class="{ done: progress.weather }">기상청 날씨 정보 수집</p>
          <p :class="{ done: progress.reviews }">네이버 리뷰 및 인지도 분석</p>
          <p :class="{ done: progress.holiday }">공휴일 정보 확인</p>
          <p :class="{ done: progress.sns }">네이버 데이터랩 언급량 확인</p>
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
            <span class="factor">최종 웨이팅 점수</span>
            <span class="score">{{ result.totalScore }}</span>
          </div>
          <el-button type="primary" class="reset-button" @click="reset">새로운 가게 분석하기</el-button>
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
            가게 영업 정보
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
              {{ text }}
            </li>
          </ul>
        </div>

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
  border-radius: 6px;
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
</style>