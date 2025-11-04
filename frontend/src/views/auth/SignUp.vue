<script setup lang="ts">
/**
 * ========================================
 * 파일명   : SignUp.vue
 * ----------------------------------------
 * 설명     : 회원가입
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-07-10
 * ========================================
 */
import { computed, h, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { Check, Close, InfoFilled } from "@element-plus/icons-vue";
import { ElAlert, ElCheckbox, ElLoading, ElMessage, ElMessageBox, ElTag } from 'element-plus';
import { defineComponent } from "@vue/runtime-dom";
import { useRouter } from "vue-router";
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { userStore } from "@/store/userStore";
import { Common } from '@/common/common';
import { Dialogs } from "@/common/dialogs";

// router
const router = useRouter();

// 알림 popover style 속성
const popoverStyle = {
  padding: '6px 12px',
  fontSize: '12px',
  lineHeight: 1.6,
  borderRadius: '4px',
  background: '#f8f9fa',
  color: '#e53e3e',
  fontWeight: 'bold',
  border: '1px solid #e53e3e',
  boxShadow: 'none',
};

// focus 용 ref
const formRef = ref();

// 필드 목록 정의
const formFields = ['userId', 'password', 'userName', 'email', 'phoneNumber', 'birthDate', 'genderCode'];
const fieldLabels = {
  userId: '아이디',
  password: '비밀번호',
  userName: '이름',
  email: '이메일',
  phoneNumber: '전화번호',
  birthDate: '생년월일',
  genderCode: '성별',
} as any;

// 한글 조사(을/를)를 결정하는 헬퍼 함수
const getPostposition = (word: string, josa1: string, josa2: string) => {
  if (!word) return '';
  const lastChar = word[word.length - 1];
  if (lastChar.match(/[가-힣]/)) {
    const hasJongseong = (lastChar.charCodeAt(0) - 0xAC00) % 28 > 0;
    return hasJongseong ? josa1 : josa2;
  }
  return josa1;
};

// 동적 객체 생성
const initialData = formFields.reduce((acc, field) => ({ ...acc, [field]: '' }), {}) as any;
const initialRules = formFields.reduce((acc, field) => {
  let message = '';
  if (field === 'genderCode') {
    message = `${fieldLabels[field]}${getPostposition(fieldLabels[field], '을', '를')} 선택해주세요.`;
  } else {
    message = `${fieldLabels[field]}${getPostposition(fieldLabels[field], '을', '를')} 입력해주세요.`;
  }
  return { ...acc, [field]: { required: true, message: message, trigger: 'blur' } };
}, {});
const initialVisible = formFields.reduce((acc, field) => ({ ...acc, [field]: false }), {});

// reactive 정의
const state = reactive({
  data: { ...initialData },
  rules: { ...initialRules } as any,
  visible: initialVisible as any,
  complexity: { percentage: 0, status: '' },
  agreePersonalInfo: false,
  agreeThirdParty: false,
  agreeEtc: false,
  agreeMarketing: false,
  agreeAll: false,
  showAgreementError: false,
  userIdCheckStatus: '' as '' | 'success' | 'error',
  userEmailCheckStatus: '' as '' | 'success' | 'error',
  isPasswordPwned: false, // 비밀번호 유출 여부 상태
  isCheckingPwned: false, // 유출 여부 확인 중 로딩 상태
});

// 화면진입 시
onMounted(() => {
  document.addEventListener('mousemove', mousemoveHandler)
})

// 화면이탈 시
onUnmounted(() => {
  document.removeEventListener('mousemove', mousemoveHandler)
})

/**
 * 비밀번호 복잡도 계산
 * @param password
 */
const updatePasswordComplexity = (password: string) => {
  if (!password) {
    state.complexity.percentage = 0;
    state.complexity.status = '';
    return;
  }

  let score = 0;
  const totalChecks = 6; // 총 검사 항목 6개

  // --- 필수 항목 (4개) ---
  // 1. 8자리 이상
  if (password.length >= 8) score++;

  // 2. 영문/특수기호/숫자 전부 포함
  const hasEnglish = /[a-zA-Z]/.test(password);
  const hasNumber = /[0-9]/.test(password);
  const hasSpecialChar = /[^a-zA-Z0-9]/.test(password);
  if (hasEnglish && hasNumber && hasSpecialChar) score++;

  // 3. ID와 동일하게 사용 불가
  if (state.data.userId && password !== state.data.userId) score++;
  else if (!state.data.userId) score++; // ID가 없으면 통과

  // 4. 생년월일/전화번호 포함 불가
  let containsPersonalInfo = false;
  if (state.data.phoneNumber) {
    const phoneDigits = state.data.phoneNumber.replace(/\D/g, '');
    if (phoneDigits && password.includes(phoneDigits)) containsPersonalInfo = true;
  }
  if (state.data.birthDate) {
    const birthDigits = state.data.birthDate.replace(/\D/g, '');
    if (birthDigits && (password.includes(birthDigits) || (birthDigits.length > 6 && password.includes(birthDigits.substring(2))))) {
      containsPersonalInfo = true;
    }
  }
  if (!containsPersonalInfo) score++;

  // --- 권장 항목 (2개) ---
  // 5. 3자리 순차적인 숫자/문자 사용 금지
  let hasSequential = false;
  for (let i = 0; i < password.length - 2; i++) {
    const c1 = password.charCodeAt(i);
    const c2 = password.charCodeAt(i + 1);
    const c3 = password.charCodeAt(i + 2);
    // 오름차순 (e.g., 123, abc)
    if (c2 === c1 + 1 && c3 === c2 + 1) hasSequential = true;
    // 내림차순 (e.g., 321, cba)
    if (c2 === c1 - 1 && c3 === c2 - 1) hasSequential = true;
  }
  if (!hasSequential) score++;

  // 6. 대/소문자 모두 포함
  const hasLowerCase = /[a-z]/.test(password);
  const hasUpperCase = /[A-Z]/.test(password);
  if (hasLowerCase && hasUpperCase) score++;

  const percentage = Math.floor((score / totalChecks) * 100);
  state.complexity.percentage = percentage;

  // 필수 조건(앞 4개) 충족 여부 확인
  const isRequiredMet = password.length >= 8 &&
      (hasEnglish && hasNumber && hasSpecialChar) &&
      (state.data.userId ? password !== state.data.userId : true) &&
      !containsPersonalInfo;

  if (!isRequiredMet || percentage < 67) { // 필수 조건 미충족 또는 점수 4/6 미만
    state.complexity.status = 'exception';
  } else if (percentage < 100) { // 4/6 또는 5/6 충족
    state.complexity.status = 'warning';
  } else { // 모든 조건(6/6) 충족
    state.complexity.status = 'success';
  }
};

watch(() => state.data.password, (newPassword) => {
  updatePasswordComplexity(newPassword);
});

// 다른 정보 변경 시에도 비밀번호 복잡도 재계산
watch(() => [state.data.userId, state.data.birthDate, state.data.phoneNumber], () => {
  updatePasswordComplexity(state.data.password);
});

/**
 * 비밀번호 규칙 팝오버 띄우기
 */
const handleClickInfo = () => {
  visible.value = !visible.value;
}
const visible = ref(false)
const position = ref({ top: 0, left: 0, bottom: 0, right: 0, })
const triggerRef = ref({
  getBoundingClientRect: () => position.value,
})
const mousemoveHandler = ({ clientX, clientY }: any) => {
  position.value = DOMRect.fromRect({ x: clientX, y: clientY, })
}
/****** 팝오버 trigger 관련 함수들 */

/**
 * 입력란에 마우스 포커스 시 이벤트
 * @param fieldName
 */
const handleFieldFocus = (fieldName: any) => {
  state.visible[fieldName] = false;
  if (fieldName === 'userId') {
    // 사용자가 아이디를 다시 수정하려고 할 때, 기본 메시지로 되돌리고 상태 아이콘을 숨깁니다.
    state.rules.userId.message = `${fieldLabels.userId}${getPostposition(fieldLabels.userId, '을', '를')} 입력해주세요.`;
    state.userIdCheckStatus = '';
  }
  if(fieldName === 'email') {
    state.userEmailCheckStatus = '';
  }
  // 비밀번호 필드에 포커스 시, 기존 유효성 검사 메시지 초기화
  if (fieldName === 'password') {
    state.rules.password.message = '비밀번호를 입력해주세요.';
  }
}

/**
 * 비밀번호 유출 여부를 확인하는 API를 호출한다.
 */
const checkPwnedPassword = async () => {
  if (!state.data.password || state.complexity.status === 'exception') {
    state.isPasswordPwned = false;
    return;
  }

  state.isCheckingPwned = true;
  state.isPasswordPwned = false; // 검사 시작 시 초기화

  try {
    // 비밀번호를 암호화하여 전송 (기존 Common.encryptPassword 사용)
    const encryptedPassword = await Common.encryptPassword(state.data.password);
    const response = await Api.post(ApiUrls.CHECK_PWNED_PASSWORD, { password: encryptedPassword });

    if (response.data) {
      state.isPasswordPwned = true;
      // 유효성 검사 규칙에 에러 메시지를 동적으로 설정
      state.rules.password.message = '이 비밀번호는 유출된 이력이 있습니다.<br>다른 비밀번호를 사용해주세요.';
      state.visible.password = true; // 에러 팝오버 표시
    } else {
      state.isPasswordPwned = false;
    }
  } catch (error) {
    state.isPasswordPwned = false;
  } finally {
    state.isCheckingPwned = false;
  }
};

/**
 * validation 체크
 * @param fieldName
 */
const handleFieldValidation = (fieldName: any) => {
  formRef.value.validateField(fieldName, async (isValid: any, invalidFields: any) => {
    if (!isValid) {
      state.visible[fieldName] = true;
      if (fieldName === 'password') {
        // 커스텀 validator에서 생성된 에러 메시지를 rules 객체에 반영
        state.rules.password.message = invalidFields[fieldName][0].message;
      }
    } else {
      state.visible[fieldName] = false;
    }

    if (fieldName === 'userId' && !isValid) {
      state.userIdCheckStatus = 'error';
    }
    if(fieldName === 'email' && isValid) {
      const param = {
        email: state.data.email
      }
      const response = await Api.post(ApiUrls.CHECK_EMAIL, param);
      if (response.data) {
        ElMessage({
          message: '사용할 수 없는 이메일입니다.',
          grouping: true,
          type: 'error',
        })
        state.userEmailCheckStatus = 'error';
      } else {
        ElMessage.success('사용 가능한 이메일입니다.');
        state.userEmailCheckStatus = 'success';
      }

    }
    // 비밀번호 필드 유효성 검사 통과 후, 유출 여부 검사 실행
    if (fieldName === 'password' && isValid) {
      await checkPwnedPassword();
    }
  })
}

/**
 * 약관 동의 여부 계산
 * 회원가입 누를 시 약관 동의 여부 체크
 */
const requiredAgreements = computed(() => [
  state.agreePersonalInfo,
  state.agreeThirdParty,
  state.agreeEtc,
]);

/**
 * 전체동의
 * @param isChecked
 */
const handleAgreeAllChange = (isChecked: any) => {
  state.agreePersonalInfo = isChecked;
  state.agreeThirdParty = isChecked;
  state.agreeEtc = isChecked;
  state.agreeMarketing = isChecked;
};

// 동의 관련 watch 함수
watch(
    [
      () => state.agreePersonalInfo,
      () => state.agreeThirdParty,
      () => state.agreeEtc,
      () => state.agreeMarketing
    ],
    (currentValues) => {
      state.agreeAll = !currentValues.includes(false);
    }
);

// 약관 보기 항목에 사용되는 함수
const ReactiveVNode = defineComponent({
  props: { renderFn: { type: Function, required: true } },
  setup(props) { return () => props.renderFn(); },
});

/**
 * 개인정보수집이용동의 [보기]
 */
const showPrivacyPolicyPopup = () => {
  const popupState = reactive({ isAgreedRequired: state.agreePersonalInfo, isAgreedMarketing: state.agreeMarketing });
  const messageVNode = h(ReactiveVNode, {
    renderFn: () => h('div', { class: 'privacy-dialog-content' }, [
      h('div', { class: 'privacy-scroll-content' }, [
        h('h4', { class: 'privacy-section-title' }, '개인정보 수집·이용 내역'),
        h('table', { class: 'privacy-table' }, [
          h('thead', null, [h('tr', null, [h('th', { style: 'width: 15%' }, '구분'), h('th', { style: 'width: 28%' }, '수집 목적'), h('th', { style: 'width: 27%' }, '수집 항목'), h('th', { style: 'width: 30%' }, '보유 및 이용기간')])]),
          h('tbody', null, [
            h('tr', null, [h('td', null, h(ElTag, { type: 'danger', size: 'small', effect: 'light' }, () => '필수')), h('td', { class: 'retention-period' }, '회원 식별 및 서비스 제공'), h('td', { class: 'retention-period' }, '아이디, 비밀번호, 이메일 주소'), h('td', { rowspan: 2, class: 'retention-period' }, '회원 탈퇴 시 즉시 파기. 단, 관련 법령에 따라 보관이 필요한 경우 해당 기간 동안 보존됩니다.')]),
            h('tr', null, [h('td', null, h(ElTag, { type: 'info', size: 'small', effect: 'light' }, () => '선택')), h('td', { class: 'retention-period' }, '마케팅 및 광고 활용'), h('td', { class: 'retention-period' }, '연락처, 주소')])
          ]),
        ]),
        h(ElAlert, { class: 'refusal-info-alert', title: '동의 거부 권리 및 불이익 안내', type: 'info', closable: false, showIcon: true }, () => ['귀하는 개인정보 수집 및 이용에 대한 동의를 거부할 권리가 있습니다. ', h('br'), '다만, 필수 항목에 대한 동의를 거부하실 경우 회원가입 및 관련 서비스 이용이 제한될 수 있습니다.'])
      ]),
      h('div', { class: 'privacy-agreement-footer' }, [
        h('div', { class: 'privacy-agreement-items' }, [
          h(ElCheckbox as any, { modelValue: popupState.isAgreedRequired, 'onUpdate:modelValue': (v: boolean) => { popupState.isAgreedRequired = v; }, size: 'large' }, () => [h('span', null, '(필수) 개인정보 수집 및 이용에 동의합니다.')]),
          h(ElCheckbox as any, { modelValue: popupState.isAgreedMarketing, 'onUpdate:modelValue': (v: boolean) => { popupState.isAgreedMarketing = v; }, size: 'large' }, () => [h('span', null, '(선택) 마케팅 정보 수신(이메일, SMS)에 동의합니다.')])
        ])
      ])
    ]),
  });
  ElMessageBox.alert(messageVNode, '개인정보 수집 및 이용 동의', { confirmButtonText: '확인', customClass: 'privacy-policy-message-box-modern', dangerouslyUseHTMLString: false })
      .then(() => { state.agreePersonalInfo = popupState.isAgreedRequired; state.agreeMarketing = popupState.isAgreedMarketing; }).catch(() => {});
};

/**
 * 제3자정보제공동의 [보기]
 */
const showThirdPartyPopup = () => {
  const popupState = reactive({ isAgreed: state.agreeThirdParty });
  const messageVNode = h(ReactiveVNode, {
    renderFn: () => h('div', { class: 'privacy-dialog-content' }, [
      h('div', { class: 'privacy-scroll-content' }, [
        h('h4', { class: 'privacy-section-title' }, '개인정보 국외 이전 및 처리 위탁 내역'),
        h('table', { class: 'privacy-table' }, [
          h('thead', null, [h('tr', null, [h('th', { style: { width: '25%' } }, '이전받는 자'), h('th', { style: { width: '35%' } }, '이전 목적'), h('th', { style: { width: '20%' } }, '개인정보 항목'), h('th', { style: { width: '20%' } }, '보유/이용기간')])]),
          h('tbody', null, [h('tr', null, [h('td', null, 'Oracle Corporation (미국)'), h('td', { class: 'retention-period' }, '클라우드 데이터베이스 서버 운영 및 데이터의 안전한 저장/관리'), h('td', { class: 'retention-period' }, '회원가입 시 수집된 개인정보 일체 (아이디, 비밀번호, 이메일 등)'), h('td', { class: 'retention-period' }, '회원 탈퇴 또는 위탁 계약 종료 시까지')])])
        ]),
        h(ElAlert, { class: 'refusal-info-alert', title: '동의 거부 권리 및 불이익 안내', type: 'info', closable: false, showIcon: true }, () => ['귀하는 개인정보의 국외 이전 및 처리 위탁에 대한 동의를 거부할 권리가 있습니다.', h('br'), '다만, 동의를 거부하실 경우 서비스의 핵심 기능이 동작하지 않으므로 회원가입 및 모든 서비스 이용이 불가능합니다.'])
      ]),
      h('div', { class: 'privacy-agreement-footer' }, [h(ElCheckbox as any, { modelValue: popupState.isAgreed, 'onUpdate:modelValue': (v: boolean) => { popupState.isAgreed = v; }, size: 'large' }, () => [h('span', null, '(필수) 위 내용을 모두 확인하였으며, 개인정보의 국외 이전 및 처리 위탁에 동의합니다.')])])
    ]),
  });
  ElMessageBox.alert(messageVNode, '개인정보 국외 이전 및 처리 위탁 동의', { confirmButtonText: '확인', customClass: 'privacy-policy-message-box-modern', dangerouslyUseHTMLString: false })
      .then(() => { state.agreeThirdParty = popupState.isAgreed; }).catch(() => {});
};

/**
 * 기타사항 [보기]
 */
const showEtcPopup = () => {
  const popupState = reactive({ isAgreed: state.agreeEtc });
  const iconVNode = (component: any) => h('div', { class: 'etc-icon-wrapper' }, h(component, { class: 'etc-icon' }));
  const messageVNode = h(ReactiveVNode, {
    renderFn: () => h('div', { class: 'privacy-dialog-content' }, [
      h('div', { class: 'privacy-scroll-content' }, [
        h('div', { class: 'etc-section' }, [h('div', { class: 'etc-section-header' }, [iconVNode(InfoFilled), h('h5', { class: 'etc-section-title' }, '첫 번째 주요 정책')]), h('p', { class: 'etc-section-content' }, '이곳에 첫 번째 주요 정책 또는 기타 안내 사항에 대한 내용을 상세하게 기술합니다. 필요에 따라 여러 문단으로 구성할 수 있습니다. 사용자가 꼭 알아야 할 중요한 정보를 명확하고 간결하게 전달하는 것이 좋습니다.')]),
        h('div', { class: 'etc-section' }, [h('div', { class: 'etc-section-header' }, [iconVNode(InfoFilled), h('h5', { class: 'etc-section-title' }, '두 번째 고려 사항')]), h('p', { class: 'etc-section-content' }, '이곳에는 두 번째 안내 사항을 작성합니다. 예를 들어, 서비스 이용 규칙, 콘텐츠 저작권 정책, 혹은 분쟁 해결 절차 등에 대한 내용을 포함할 수 있습니다.'), h('ul', { class: 'etc-list' }, [h('li', null, '항목 1: 관련된 세부 규칙이나 가이드라인을 명시합니다.'), h('li', null, '항목 2: 사용자가 준수해야 할 또 다른 중요한 사항입니다.')])]),
      ]),
      h('div', { class: 'privacy-agreement-footer' }, [h(ElCheckbox as any, { modelValue: popupState.isAgreed, 'onUpdate:modelValue': (v: boolean) => { popupState.isAgreed = v; }, size: 'large' }, () => [h('span', null, '(필수) 위 기타 사항을 모두 확인하였으며, 내용에 동의합니다.')])])
    ]),
  });
  ElMessageBox.alert(messageVNode, '기타 사항 안내 및 동의', { confirmButtonText: '확인', customClass: 'privacy-policy-message-box-modern', dangerouslyUseHTMLString: false })
      .then(() => { state.agreeEtc = popupState.isAgreed; }).catch(() => {});
};

/**
 * 중복 체크: 아이디 필드만 검사
 */
const onClickCheckId = () => {
  formRef.value.validateField('userId', async (isValid: boolean) => {
    if (isValid) {
      const response = await Api.post(ApiUrls.CHECK_ID, { userId : state.data.userId }, true);
      if (response.data) {
        ElMessage({ message: '이미 사용 중인 아이디입니다.', grouping: true, type: 'error' })
        state.userIdCheckStatus = 'error';
      } else {
        ElMessage({ message: '사용 가능한 아이디입니다.', grouping: true, type: 'success' });
        state.visible.userId = false;
        state.userIdCheckStatus = 'success';
      }
    } else {
      state.visible.userId = true;
      state.userIdCheckStatus = 'error';
    }
  });
}

/**
 * 가입하기: 전체 필드 검사 및 모든 오류 메시지 표시
 */
const onClickSignUp = async () => {
  // 모든 팝오버를 우선 숨겨서 깨끗한 상태에서 검사 시작
  for (const key in state.visible) {
    state.visible[key] = false;
  }

  let isFormValid = false;
  try {
    // 폼 전체 유효성 검사
    await formRef.value.validate();
    isFormValid = true;
  } catch (invalidFields: any) {
    isFormValid = false;
    // 유효성 검사에 실패한 필드 객체를 순회
    for (const fieldName in invalidFields) {
      if (Object.prototype.hasOwnProperty.call(state.visible, fieldName)) {
        // 실패한 필드에 해당하는 visible 상태를 true로 변경하여 팝오버 표시
        state.visible[fieldName] = true;
        // 실패한 비밀번호 필드의 에러 메시지를 rules에 업데이트
        if (fieldName === 'password' && invalidFields.password) {
          state.rules.password.message = invalidFields.password[0].message;
        }
        // 아이디 필드가 유효하지 않으면 상태 아이콘도 업데이트
        if (fieldName === 'userId') {
          state.userIdCheckStatus = 'error';
        }
      }
    }
  }

  // 유출된 비밀번호인지 최종 확인
  if (state.isPasswordPwned) {
    state.visible['password'] = true;
    state.rules.password.message = '이 비밀번호는 유출된 이력이 있습니다.<br>다른 비밀번호를 사용해주세요.';
    ElMessage({ message: '안전하지 않은 비밀번호입니다. 다른 비밀번호를 사용해주세요.', grouping: true, type: 'error' })
    return;
  }

  // 비밀번호 생성규칙 검사
  if(state.complexity.status === 'exception') {
    state.visible['password'] = true;
    state.rules.password.message = '비밀번호 생성규칙을 확인해주세요.';
    return;
  }

  // 약관 동의 여부 검사
  const allRequiredAgreed = requiredAgreements.value.every(agreed => agreed);
  state.showAgreementError = !allRequiredAgreed;

  // 최종 제출 조건 확인
  if (isFormValid && allRequiredAgreed) {
    console.log('submit!');

    // 아이디 유효성 재검사
    const response = await Api.post(ApiUrls.CHECK_ID, { userId : state.data.userId }, true);
    if (response.data) {
      ElMessage({ message: '이미 사용 중인 아이디입니다.', grouping: true, type: 'error' })
      state.userIdCheckStatus = 'error';
      return;
    }
    else {
      state.userIdCheckStatus = 'success';
    }

    const response2 = await Api.post(ApiUrls.CHECK_EMAIL, { email: state.data.email });
    if (response2.data) {
      ElMessage({ message: '사용할 수 없는 이메일입니다.', grouping: true, type: 'error' })
      state.userEmailCheckStatus = 'error';
      return;
    }
    else {
      state.userEmailCheckStatus = 'success';
    }

    // 아이디 및 이메일이 유효할 때
    if(state.userEmailCheckStatus === 'success' && state.userIdCheckStatus === 'success') {
      try {

        await Dialogs.customConfirm(
            '회원가입 확인',
            '입력하신 정보로 회원가입을 완료하시겠습니까?',
            '가입하기',
            '취소',
            '485px',
        );

        // 서버에서 공개키 get
        const encryptedPassword = await Common.encryptPassword(state.data.password);

        const params = {
          userId : state.data.userId,
          password : encryptedPassword,
          userName : state.data.userName,
          email : state.data.email,
          phoneNumber : state.data.phoneNumber.replaceAll("-", ""),
          birthDate : state.data.birthDate.replaceAll("-", ""),
          genderCode : state.data.genderCode,
          termsAgree1: state.agreePersonalInfo ? 'Y' : 'N',
          termsAgree2: state.agreeThirdParty ? 'Y' : 'N',
          termsAgree3: state.agreeEtc ? 'Y' : 'N',
          termsAgree4: state.agreeMarketing ? 'Y' : 'N',
        }

        await Api.post(ApiUrls.SIGN_UP, params);

        const loading = ElLoading.service({
          lock: true,
          text: 'Loading',
          background: 'rgba(0, 0, 0, 0.7)',
        })
        ElMessage.success('회원가입이 완료되었습니다.');

        setTimeout(()=>{
          userStore().delUserInfo();
          sessionStorage.clear();
          router.push("/login");
          loading.close();
        }, 1000);
      } catch (action) {
        if (action === 'cancel') {
          ElMessage.info('회원가입을 취소했습니다.');
        }
      }
    }

  } else {
    ElMessage({ message: '입력 정보를 확인하고 필수 약관에 동의해주세요.', grouping: true, type: 'error' })
  }
};

/**
 * 로그인 화면으로 이동
 */
const onClickToOpenLogin = () => {
  router.push("/login");
}

// 생년월일 형식 자동 변환 함수
const formatBirthDate = (value: string) => {
  // 숫자 이외의 문자 제거
  let cleaned = value.replace(/\D/g, '');

  // yyyy-mm-dd 형식으로 하이픈 추가
  if (cleaned.length > 4) {
    cleaned = cleaned.slice(0, 4) + '-' + cleaned.slice(4);
  }
  if (cleaned.length > 7) {
    cleaned = cleaned.slice(0, 7) + '-' + cleaned.slice(7, 9);
  }

  // v-model과 연결된 데이터 업데이트 (최대 10자)
  state.data.birthDate = cleaned.slice(0, 10);
};
</script>

<template>
  <div class="signup-container">
    <el-card class="signup-card" shadow="never">
      <template #header>
        <div class="card-header">회원가입</div>
      </template>

      <el-form
          ref="formRef"
          :model="state.data"
          :rules="state.rules"
          :show-message="false"
          label-position="left"
          class="signup-form"
          label-width="120px"
          @submit.prevent
      >
        <div class="text-title1">
          <el-text tag="b">개인정보입력</el-text>
        </div>

        <!-- 아이디 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            :content="state.rules.userId.message"
            placement="right-start"
            width="250"
            :visible="state.visible.userId"
        >
          <template #reference>
            <el-form-item label="아이디" prop="userId" required>
              <div class="id-input-wrapper">
                <el-input
                    placeholder="6~12자의 영문/숫자 조합"
                    clearable
                    class="input-with-button"
                    v-model="state.data.userId"
                    v-byte-limit="50"
                    @focus="() => handleFieldFocus('userId')"
                    @blur="() => handleFieldValidation('userId')"
                >
                  <template #append>
                    <el-button
                        type="primary"
                        @click.prevent="onClickCheckId"
                        class="non-outline">중복 체크</el-button>
                  </template>
                </el-input>

                <div :class="['id-check-indicator', state.userIdCheckStatus && `is-${state.userIdCheckStatus}`]">
                  <el-button
                      v-if="state.userIdCheckStatus === 'success'"
                      type="success" :icon="Check"
                      circle
                      size="small"
                  />
                  <el-button
                      v-if="state.userIdCheckStatus === 'error'"
                      type="danger"
                      :icon="Close"
                      circle
                      size="small"
                  />
                </div>

              </div>
            </el-form-item>
          </template>
        </el-popover>

        <!-- 비밀번호 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            placement="right-start"
            width="250"
            :visible="state.visible.password"
        >
          <div v-html="state.rules.password.message"></div>
          <template #reference>
            <el-form-item label="비밀번호" prop="password" required>
              <el-input
                  v-model="state.data.password"
                  type="password"
                  show-password
                  v-byte-limit="50"
                  placeholder="8자 이상 입력해 주세요"
                  clearable
                  @focus="() => handleFieldFocus('password')"
                  @blur="() => handleFieldValidation('password')"
              />
            </el-form-item>
          </template>
        </el-popover>

        <!-- 비밀번호 복잡도 -->
        <el-form-item>
          <div class="password-comple-area">
            <el-tag plain class="password-comple-tag">
              <el-icon
                  @click="handleClickInfo"
                  class="password-comple-tag-icon">
                <InfoFilled />
              </el-icon>
              <el-text class="password-comple-text">비밀번호 복잡도</el-text>
            </el-tag>
            <el-progress
                style="width: 100%;"
                :text-inside="true"
                :stroke-width="18"
                :percentage="state.complexity.percentage"
                :status="state.complexity.status"
            />
          </div>
        </el-form-item>

        <!-- 비밀번호 안내 tooltip -->
        <el-tooltip
            v-model:visible="visible"
            placement="bottom-end"
            effect="light"
            trigger="click"
            virtual-triggering
            :virtual-ref="triggerRef"
        >
          <template #content>
            <div class="password-info">
              <el-text class="password-info-title">비밀번호 생성규칙</el-text>
              <el-divider class="password-info-divider" />
              <el-alert
                  title="비밀번호 생성 관련 안내"
                  type="info"
                  show-icon
                  :closable="false"
              />
              <el-divider class="password-info-divider" />
              <el-descriptions
                  :column="1"
                  size="small"
                  border
              >
                <el-descriptions-item label-class-name="my-label" label="필수">8자리 이상</el-descriptions-item>
                <el-descriptions-item label-class-name="my-label" label="필수">영문/특수기호/숫자 전부 포함</el-descriptions-item>
                <el-descriptions-item label-class-name="my-label" label="필수">ID와 동일하게 사용불가</el-descriptions-item>
                <el-descriptions-item label-class-name="my-label" label="필수">생년월일/전화번호 포함 불가</el-descriptions-item>
                <el-descriptions-item label="권장">3자리의 순차적인 숫자 사용 금지</el-descriptions-item>
                <el-descriptions-item label="권장">대문자(영문) 소문자(영문)이 한개 이상 포함</el-descriptions-item>
              </el-descriptions>
              <el-divider class="password-info-divider" />
            </div>
          </template>
        </el-tooltip>

        <!-- 이름 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            :content="state.rules.userName.message"
            placement="right-start"
            width="250"
            :visible="state.visible.userName"
        >
          <template #reference>
            <el-form-item label="이름" prop="userName" required>
              <el-input
                  v-model="state.data.userName"
                  v-byte-limit="100"
                  placeholder="실명을 입력해 주세요"
                  clearable @focus="() => handleFieldFocus('userName')"
                  @blur="() => handleFieldValidation('userName')"
              />
            </el-form-item>
          </template>
        </el-popover>

        <!-- 이메일 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            :content="state.rules.email.message"
            placement="right-start"
            width="250"
            :visible="state.visible.email"
        >
          <template #reference>
            <el-form-item label="이메일" prop="email" required>
              <el-input
                  v-model="state.data.email"
                  v-byte-limit="250"
                  placeholder="example@email.com"
                  clearable @focus="() => handleFieldFocus('email')"
                  @blur="() => handleFieldValidation('email')"
              />
            </el-form-item>
          </template>
        </el-popover>

        <!-- 전화번호 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            :content="state.rules.phoneNumber.message"
            placement="right-start"
            width="250"
            :visible="state.visible.phoneNumber"
        >
          <template #reference>
            <el-form-item label="전화번호" prop="phoneNumber" required>
              <el-input
                  v-model="state.data.phoneNumber"
                  v-byte-limit="20"
                  placeholder="010-1234-5678"
                  clearable @focus="() => handleFieldFocus('phoneNumber')"
                  @blur="() => handleFieldValidation('phoneNumber')"
              />
            </el-form-item>
          </template>
        </el-popover>

        <!-- 생년월일 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            :content="state.rules.birthDate.message"
            placement="right-start"
            width="250"
            :visible="state.visible.birthDate"
        >
          <template #reference>
            <el-form-item label="생년월일" prop="birthDate" required>
              <el-input
                  v-model="state.data.birthDate"
                  v-byte-limit="10"
                  placeholder="YYYY-MM-DD"
                  clearable
                  @input="formatBirthDate"
                  @focus="() => handleFieldFocus('birthDate')"
                  @blur="() => handleFieldValidation('birthDate')"
              />
            </el-form-item>
          </template>
        </el-popover>

        <!-- 성별 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            :content="state.rules.genderCode.message"
            placement="right-start"
            width="250"
            :visible="state.visible.genderCode"
        >
          <template #reference>
            <el-form-item label="성별" prop="genderCode" required>
              <div class="my-radio-group">
                <el-radio-group
                    v-model="state.data.genderCode"
                    @change="() => handleFieldValidation('genderCode')"
                >
                  <el-radio-button label="M">남자</el-radio-button>
                  <el-radio-button label="F">여자</el-radio-button>
                </el-radio-group>
              </div>
            </el-form-item>
          </template>
        </el-popover>
      </el-form>

      <el-divider />

      <div style="margin-top: 24px;">
        <el-descriptions class="margin-top" :column="2" :size="'small'" border>
          <template #title>
            <div class="text-title2"><el-text tag="b">서비스 이용에 대한 동의</el-text></div>
          </template>
          <template #extra>
            <el-checkbox v-model="state.agreeAll" @change="handleAgreeAllChange" label="전체동의" />
          </template>
          <el-descriptions-item>
            <template #label><div class="cell-item"><el-checkbox v-model="state.agreePersonalInfo" label="(필수) 개인정보수집이용동의" /></div></template>
            <el-button link type="info" @click="showPrivacyPolicyPopup" class="non-outline">보기</el-button>
          </el-descriptions-item>
          <el-descriptions-item>
            <template #label><div class="cell-item"><el-checkbox v-model="state.agreeThirdParty" label="(필수) 제3자정보제공동의" /></div></template>
            <el-button link type="info" @click="showThirdPartyPopup" class="non-outline">보기</el-button>
          </el-descriptions-item>
          <el-descriptions-item>
            <template #label><div class="cell-item"><el-checkbox v-model="state.agreeEtc" label="(필수) 기타사항" /></div></template>
            <div style="text-align: right;">
              <el-button link type="info" @click="showEtcPopup" class="non-outline">보기</el-button>
            </div>
          </el-descriptions-item>
        </el-descriptions>
        <div style="height: 50px;">
          <el-alert
              title="필수 이용약관에 모두 동의해주세요." type="error" :closable="false"
              show-icon :class="['agreement-error-alert', { 'is-visible': state.showAgreementError }]"
          />
        </div>
        <el-button type="primary" class="signup-button" @click="onClickSignUp">가입하기</el-button>
      </div>
    </el-card>

    <el-card class="signup-prompt-card" shadow="never">
      <el-text>이미 계정이 있으신가요?</el-text>
      <el-button type="primary" link class="signup-link" @click="onClickToOpenLogin">로그인</el-button>
    </el-card>
  </div>
</template>

<style>
.privacy-section-title { font-size: 16px; font-weight: 600; color: var(--el-text-color-primary); margin: 0 0 16px 0; }
.privacy-table .retention-period { font-size: 13px; color: #606266; line-height: 1.5; }
.privacy-table td .el-tag { display: flex; justify-content: center; align-items: center; margin: 0 auto; }
.refusal-info-alert { margin-top: 20px; }
.refusal-info-alert .el-alert__description { font-size: 13px !important; }
.privacy-agreement-footer .el-checkbox.el-checkbox--large { height: auto }
.privacy-agreement-footer .el-checkbox__label { white-space: normal; line-height: 1.5; color: var(--el-text-color-primary); font-weight: 500; }
.privacy-policy-message-box-modern { max-width: 830px; width: 90%; border-radius: 8px; padding: 0; }
.privacy-policy-message-box-modern .el-message-box__header { padding: 18px 24px; border-bottom: 1px solid #ebeef5; }
.privacy-policy-message-box-modern .el-message-box__title { font-size: 18px; font-weight: 600; color: var(--el-text-color-primary); }
.privacy-policy-message-box-modern .el-message-box__content { padding: 0; }
.privacy-policy-message-box-modern .el-message-box__btns { padding: 10px 24px 18px; }
.privacy-dialog-content { display: flex; flex-direction: column; max-height: 60vh; }
.privacy-scroll-content { flex-grow: 1; overflow-y: auto; padding: 20px 24px; }
.privacy-table { width: 100%; border-collapse: collapse; font-size: 14px; text-align: left; margin-bottom: 20px; }
.privacy-table th, .privacy-table td { border: 1px solid #ebeef5; padding: 12px 14px; vertical-align: middle; }
.privacy-table thead { background-color: var(--agree-info-color); font-weight: 600; color: #606266; }
.privacy-table td { color: #303133; line-height: 1.6; }
.refusal-info { font-size: 13px; color: #909399; line-height: 1.6; margin: 0; }
.privacy-agreement-footer { flex-shrink: 0; padding: 16px 24px; background-color: var(--agree-info-color); border-top: 1px solid #ebeef5; }
.etc-section { margin-bottom: 24px; }
.etc-section-header { display: flex; align-items: center; margin-bottom: 12px; }
.etc-icon-wrapper { display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; width: 24px; height: 24px; margin-right: 8px; border-radius: 50%; background-color: #eef2ff; }
.etc-icon { font-size: 14px; color: #4338ca; }
.etc-section-title { font-size: 16px; font-weight: 600; color: var(--el-text-color-primary); margin: 0; }
.etc-section-content { font-size: 14px; line-height: 1.7; color: #606266; padding-left: 32px; margin: 0; }
.etc-list { padding-left: 52px; margin-top: 10px; font-size: 14px; color: #606266; }
.etc-list li { margin-bottom: 6px; }
</style>

<style scoped>
.id-input-wrapper {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 8px;
}
.id-input-wrapper .el-input {
  flex-grow: 1;
}
.id-check-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  flex-shrink: 0;
  background-color: transparent;
  border: 1px solid transparent;
  transition: all 0.2s ease-in-out;
  opacity: 0;
}
.id-check-indicator.is-success,
.id-check-indicator.is-error {
  opacity: 1;
}
.id-check-indicator.is-success {
  background-color: var(--el-color-success-light-9);
  border-color: var(--el-color-success-light-5);
}
.id-check-indicator.is-error {
  background-color: var(--el-color-error-light-9);
  border-color: var(--el-color-error-light-5);
}
.id-check-indicator .el-icon {
  font-size: 18px;
  font-weight: bold;
}
.id-check-indicator.is-success .el-icon {
  color: var(--el-color-success);
}
.id-check-indicator.is-error .el-icon {
  color: var(--el-color-error);
}
.signup-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 70vh;
  padding: 50px 20px;
}
.signup-card {
  width: 100%;
  max-width: 700px;
  height: 950px;
}
.card-header {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  letter-spacing: 1px;
  font-size: 20px;
  text-align: center;
}
.text-title1, .text-title2 {
  text-align: left;
  margin-bottom: 12px;
}
.signup-form .el-form-item {
  margin-bottom: 8px;
}
.signup-form :deep(.el-form-item__label) {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}
.signup-button {
  width: 100%;
  height: 48px;
  font-weight: bold;
  font-size: 16px;
}
:deep(.el-popper[data-popper-placement^=right]>.el-popper__arrow:before) {
  border-right-color: #e53e3e !important;
  border-top-color: #e53e3e !important;
}
.password-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.password-comple-area {
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 500px;
  margin-bottom: 20px;
  gap: 8px;
}
.password-comple-tag {
  color: #4527A0;
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 2px 5px 4px 4px;
}
.password-comple-tag-icon {
  cursor: pointer;
  font-size: 12px;
  position: relative;
  top: 1px;
}
.password-comple-text {
  font-size: 11px;
  font-weight: bold;
}
.password-info-title {
  font-weight: bold;
}
.password-info-content {
  margin-bottom: 4px;
  width: 275px;
  justify-content: left;
  color: #4527A0;
}
.password-info-divider {
  margin-bottom: 4px;
  margin-top: 4px;
}
.password-info-footer {
  margin-bottom: 4px;
  width: 275px;
  justify-content: left;
  background-color: #F5F5F5;
  color: #212121;
}
::v-deep(.el-progress-bar__innerText) {
  font-size: 10px !important;
  padding-bottom: 2px !important;
}
.signup-prompt-card {
  width: 100%;
  max-width: 700px;
  margin-top: 12px;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 4px 4px;
  box-sizing: border-box;
  height: 50px;
}
.non-outline {
  outline: 0;
  color: var(--el-color-info) !important;
}
.signup-prompt-card :deep(.el-card__body) {
  padding: 8px 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}
.agreement-error-alert {
  visibility: hidden;
  opacity: 0;
  margin-top: 12px;
  margin-bottom: 12px;
  height: 0;
  padding-top: 0;
  padding-bottom: 0;
  border-width: 0;
  transition: opacity 0.3s ease, height 0.3s ease, padding 0.3s ease, border-width 0.3s ease;
  overflow: hidden;
}
.agreement-error-alert.is-visible {
  visibility: visible;
  opacity: 1;
  height: 36px;
  padding-top: 8px;
  padding-bottom: 8px;
  border-width: 1px;
}
.signup-link {
  color: var(--el-color-primary) !important;
}
.my-radio-group :deep(.el-radio-button),
.my-radio-group :deep(.el-radio-button.is-active .el-radio-button__inner) {
  /* 여기에 선택된 버튼 자체의 스타일을 적용합니다. */
  color: var(--el-bg-color);
}
</style>