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
import { Check, Close, InfoFilled, ArrowLeft } from "@element-plus/icons-vue";
import { ElAlert, ElCheckbox, ElLoading, ElMessage, ElMessageBox, ElTag } from 'element-plus';
import { defineComponent } from "@vue/runtime-dom";
import { useRouter } from "vue-router";
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { userStore } from "@/store/userStore";
import { Common } from '@/common/common';
import { Dialogs } from "@/common/dialogs";
import { useI18n } from "vue-i18n";

// i18n
const { t, locale } = useI18n();

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
  userId: t('emailSignup.labels.userId'),
  password: t('emailSignup.labels.password'),
  userName: t('emailSignup.labels.userName'),
  email: t('emailSignup.labels.email'),
  phoneNumber: t('emailSignup.labels.phoneNumber'),
  birthDate: t('emailSignup.labels.birthDate'),
  gender: t('emailSignup.labels.gender'),
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
const initialVisible = formFields.reduce((acc, field) => ({ ...acc, [field]: false }), {});

// reactive 정의
const state = reactive({
  data: { ...initialData },
  rules: {} as any,
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

// 언어 변경 시 유효성 검사 메시지를 업데이트하는 함수
const updateValidationMessages = () => {
  const newRules = {} as any;
  formFields.forEach(field => {
    // 1. 번역된 라벨 가져오기 (예: "아이디" or "User ID")
    const label = t(`emailSignup.labels.${field}`);

    // 2. 최종적으로 {field}에 들어갈 값 만들기
    let finalFieldLabel = label;

    // 3. 현재 언어가 'ko'일 때만 조사를 붙임
    if (locale.value === 'ko') {
      const particle = getPostposition(label, '을', '를');
      finalFieldLabel = label + particle;
    }

    // 4. 규칙 타입 결정 (입력 or 선택)
    const ruleType = field === 'genderCode' ? 'select' : 'enter';
    const message = t(`emailSignup.rules.${ruleType}`, { field: finalFieldLabel });

    // 5. 최종 규칙 객체 생성
    newRules[field] = {
      required: true,
      message: message,
      trigger: 'blur'
    };
  });
  state.rules = newRules;
};

// 컴포넌트가 마운트될 때, 그리고 언어(locale)가 변경될 때마다 유효성 규칙을 다시 생성
watch(locale, updateValidationMessages, { immediate: true });

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
  if (fieldName === 'userId' || fieldName === 'password') {
    // watch가 이미 rule을 관리하므로, 여기서는 단순히 상태만 초기화
    updateValidationMessages(); // 간단하게 전체 규칙을 다시 생성하여 메시지를 원본으로 되돌림
    state.userIdCheckStatus = (fieldName === 'userId') ? '' : state.userIdCheckStatus;
  }
  if(fieldName === 'email') {
    state.userEmailCheckStatus = '';
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
      state.rules.password.message = t('emailSignup.rules.pwnedPassword');
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
        ElMessage({ message: t('emailSignup.messages.emailInUse'), grouping: true, type: 'error' });
        state.userEmailCheckStatus = 'error';
      } else {
        ElMessage.success(t('emailSignup.messages.emailAvailable'));
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
        h('h4', { class: 'privacy-section-title' }, t('emailSignup.dialogs.privacySectionTitle')),
        h('table', { class: 'privacy-table' }, [
          h('thead', null, [h('tr', null, [
            h('th', { style: 'width: 15%' }, t('emailSignup.dialogs.colCategory')),
            h('th', { style: 'width: 28%' }, t('emailSignup.dialogs.colPurpose')),
            h('th', { style: 'width: 27%' }, t('emailSignup.dialogs.colItems')),
            h('th', { style: 'width: 30%' }, t('emailSignup.dialogs.colRetention'))
          ])]),
          h('tbody', null, [
            h('tr', null, [
              h('td', null, h(ElTag, { type: 'danger', size: 'small', effect: 'light' }, () => t('emailSignup.dialogs.rowRequired'))),
              h('td', { class: 'retention-period' }, t('emailSignup.dialogs.purposeRequired')),
              h('td', { class: 'retention-period' }, t('emailSignup.dialogs.itemsRequired')),
              h('td', { rowspan: 2, class: 'retention-period' }, t('emailSignup.dialogs.retentionInfo'))
            ]),
            h('tr', null, [
              h('td', null, h(ElTag, { type: 'info', size: 'small', effect: 'light' }, () => t('emailSignup.dialogs.rowOptional'))),
              h('td', { class: 'retention-period' }, t('emailSignup.dialogs.purposeOptional')),
              h('td', { class: 'retention-period' }, t('emailSignup.dialogs.itemsOptional'))
            ])
          ]),
        ]),
        h(ElAlert, { class: 'refusal-info-alert', title: t('emailSignup.dialogs.refusalTitle'), type: 'info', closable: false, showIcon: true }, () => t('emailSignup.dialogs.refusalContentPrivacy'))
      ]),
      h('div', { class: 'privacy-agreement-footer' }, [
        h('div', { class: 'privacy-agreement-items' }, [
          h(ElCheckbox as any, { modelValue: popupState.isAgreedRequired, 'onUpdate:modelValue': (v: boolean) => { popupState.isAgreedRequired = v; }, size: 'large' }, () => [h('span', null, t('emailSignup.dialogs.agreeRequired'))]),
          h(ElCheckbox as any, { modelValue: popupState.isAgreedMarketing, 'onUpdate:modelValue': (v: boolean) => { popupState.isAgreedMarketing = v; }, size: 'large' }, () => [h('span', null, t('emailSignup.dialogs.agreeMarketing'))])
        ])
      ])
    ]),
  });
  ElMessageBox.alert(messageVNode, t('emailSignup.dialogs.privacyTitle'), { confirmButtonText: t('emailSignup.buttons.ok'), customClass: 'privacy-policy-message-box-modern', dangerouslyUseHTMLString: false })
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
        h('h4', { class: 'privacy-section-title' }, t('emailSignup.dialogs.thirdPartySectionTitle')),
        h('table', { class: 'privacy-table' }, [
          h('thead', null, [h('tr', null, [
            h('th', { style: { width: '25%' } }, t('emailSignup.dialogs.colRecipient')),
            h('th', { style: { width: '35%' } }, t('emailSignup.dialogs.colPurpose')),
            h('th', { style: { width: '20%' } }, t('emailSignup.dialogs.colItems')),
            h('th', { style: { width: '20%' } }, t('emailSignup.dialogs.colRetention'))
          ])]),
          h('tbody', null, [h('tr', null, [
            h('td', null, t('emailSignup.dialogs.recipientName')),
            h('td', { class: 'retention-period' }, t('emailSignup.dialogs.transferPurpose')),
            h('td', { class: 'retention-period' }, t('emailSignup.dialogs.transferItems')),
            h('td', { class: 'retention-period' }, t('emailSignup.dialogs.transferRetention'))
          ])])
        ]),
        h(ElAlert, { class: 'refusal-info-alert', title: t('emailSignup.dialogs.refusalTitle'), type: 'info', closable: false, showIcon: true }, () => t('emailSignup.dialogs.refusalContentThirdParty'))
      ]),
      h('div', { class: 'privacy-agreement-footer' }, [h(ElCheckbox as any, { modelValue: popupState.isAgreed, 'onUpdate:modelValue': (v: boolean) => { popupState.isAgreed = v; }, size: 'large' }, () => [h('span', null, t('emailSignup.dialogs.agreeThirdParty'))])])
    ]),
  });
  ElMessageBox.alert(messageVNode, t('emailSignup.dialogs.thirdPartyTitle'), { confirmButtonText: t('emailSignup.buttons.ok'), customClass: 'privacy-policy-message-box-modern', dangerouslyUseHTMLString: false })
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
        h('div', { class: 'etc-section' }, [h('div', { class: 'etc-section-header' }, [iconVNode(InfoFilled), h('h5', { class: 'etc-section-title' }, t('emailSignup.dialogs.etcSection1Title'))]), h('p', { class: 'etc-section-content' }, t('emailSignup.dialogs.etcSection1Content'))]),
        h('div', { class: 'etc-section' }, [h('div', { class: 'etc-section-header' }, [iconVNode(InfoFilled), h('h5', { class: 'etc-section-title' }, t('emailSignup.dialogs.etcSection2Title'))]), h('p', { class: 'etc-section-content' }, t('emailSignup.dialogs.etcSection2Content')), h('ul', { class: 'etc-list' }, [h('li', null, t('emailSignup.dialogs.etcListItem1')), h('li', null, t('emailSignup.dialogs.etcListItem2'))])]),
      ]),
      h('div', { class: 'privacy-agreement-footer' }, [h(ElCheckbox as any, { modelValue: popupState.isAgreed, 'onUpdate:modelValue': (v: boolean) => { popupState.isAgreed = v; }, size: 'large' }, () => [h('span', null, t('emailSignup.dialogs.agreeEtc'))])])
    ]),
  });
  ElMessageBox.alert(messageVNode, t('emailSignup.dialogs.etcTitle'), { confirmButtonText: t('emailSignup.buttons.ok'), customClass: 'privacy-policy-message-box-modern', dangerouslyUseHTMLString: false })
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
        ElMessage({ message: t('emailSignup.messages.idInUse'), grouping: true, type: 'error' })
        state.userIdCheckStatus = 'error';
      } else {
        ElMessage({ message: t('emailSignup.messages.idAvailable'), grouping: true, type: 'success' });
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
    state.rules.password.message = t('emailSignup.rules.pwnedPassword');
    ElMessage({ message: t('emailSignup.messages.unsafePassword'), grouping: true, type: 'error' })
    return;
  }

  // 비밀번호 생성규칙 검사
  if(state.complexity.status === 'exception') {
    state.visible['password'] = true;
    state.rules.password.message = t('emailSignup.rules.checkPasswordPolicy');
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
      ElMessage({ message: t('emailSignup.messages.idInUse'), grouping: true, type: 'error' })
      state.userIdCheckStatus = 'error';
      return;
    }
    else {
      state.userIdCheckStatus = 'success';
    }

    const response2 = await Api.post(ApiUrls.CHECK_EMAIL, { email: state.data.email });
    if (response2.data) {
      ElMessage({ message: t('emailSignup.messages.emailInUse'), grouping: true, type: 'error' })
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
            t('emailSignup.dialogs.confirmSignUpTitle'),
            t('emailSignup.dialogs.confirmSignUpMessage'),
            t('emailSignup.buttons.signUp'),
            t('emailSignup.buttons.cancel'),
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
        ElMessage.success(t('emailSignup.messages.signUpSuccess'));

        setTimeout(()=>{
          userStore().delUserInfo();
          sessionStorage.clear();
          router.push("/login");
          loading.close();
        }, 1000);
      } catch (action) {
        if (action === 'cancel') {
          ElMessage.info(t('emailSignup.messages.signUpCancelled'));
        }
      }
    }

  } else {
    ElMessage({ message: t('emailSignup.messages.validationError'), grouping: true, type: 'error' })
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

      <div class="card-header">
        <el-button :icon="ArrowLeft" text circle @click="router.back()" />
        <h2 class="main-title">{{ t('emailSignup.header') }}</h2>
        <div style="width: 40px;"></div>
      </div>

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
          <el-text tag="b">{{ t('emailSignup.title') }}</el-text>
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
            <el-form-item :label="t('emailSignup.labels.userId')" prop="userId" required>
              <div class="id-input-wrapper">
                <el-input
                    :placeholder="t('emailSignup.placeholders.userId')"
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
                        class="non-outline"
                    >
                      {{ t('emailSignup.buttons.checkDuplicate') }}
                    </el-button>
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
            <el-form-item :label="t('emailSignup.labels.password')" prop="password" required>
              <el-input
                  v-model="state.data.password"
                  type="password"
                  show-password
                  v-byte-limit="50"
                  :placeholder="t('emailSignup.placeholders.password')"
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
              <el-text
                  class="password-comple-text"
              >
                {{ t('emailSignup.passwordComplexity.title') }}
              </el-text>
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
              <el-text class="password-info-title">{{ t('emailSignup.passwordComplexity.policyTitle') }}</el-text>
              <el-divider class="password-info-divider" />
              <el-alert
                  :title="t('emailSignup.passwordComplexity.policyAlert')"
                  type="info"
                  show-icon
                  :closable="false"
              />
              <el-divider class="password-info-divider" />
              <el-descriptions :column="1" size="small" border >
                <el-descriptions-item
                    label-class-name="my-label"
                    :label="t('emailSignup.passwordComplexity.required')"
                >
                  {{ t('emailSignup.passwordComplexity.rule1') }}
                </el-descriptions-item>
                <el-descriptions-item
                    label-class-name="my-label"
                    :label="t('emailSignup.passwordComplexity.required')"
                >
                  {{ t('emailSignup.passwordComplexity.rule2') }}
                </el-descriptions-item>
                <el-descriptions-item
                    label-class-name="my-label"
                    :label="t('emailSignup.passwordComplexity.required')"
                >
                  {{ t('emailSignup.passwordComplexity.rule3') }}
                </el-descriptions-item>
                <el-descriptions-item
                    label-class-name="my-label"
                    :label="t('emailSignup.passwordComplexity.required')"
                >
                  {{ t('emailSignup.passwordComplexity.rule4') }}
                </el-descriptions-item>
                <el-descriptions-item
                    :label="t('emailSignup.passwordComplexity.recommended')"
                >
                  {{ t('emailSignup.passwordComplexity.rule5') }}
                </el-descriptions-item>
                <el-descriptions-item
                    :label="t('emailSignup.passwordComplexity.recommended')"
                >
                  {{ t('emailSignup.passwordComplexity.rule6') }}
                </el-descriptions-item>
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
            <el-form-item :label="t('emailSignup.labels.userName')" prop="userName" required>
              <el-input
                  v-model="state.data.userName"
                  v-byte-limit="100"
                  :placeholder="t('emailSignup.placeholders.userName')"
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
            <el-form-item :label="t('emailSignup.labels.email')" prop="email" required>
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
            <el-form-item :label="t('emailSignup.labels.phoneNumber')" prop="phoneNumber" required>
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
            <el-form-item :label="t('emailSignup.labels.birthDate')" prop="birthDate" required>
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
            <el-form-item :label="t('emailSignup.labels.genderCode')" prop="genderCode" required>
              <div class="my-radio-group">
                <el-radio-group
                    v-model="state.data.genderCode"
                    @change="() => handleFieldValidation('genderCode')"
                >
                  <el-radio-button label="M">{{ t('emailSignup.labels.genderMale') }}</el-radio-button>
                  <el-radio-button label="F">{{ t('emailSignup.labels.genderFemale') }}</el-radio-button>
                </el-radio-group>
              </div>
            </el-form-item>
          </template>
        </el-popover>
      </el-form>

      <el-divider />

      <div style="margin-top: 24px;">
        <el-descriptions
            class="margin-top"
            :column="2"
            :size="'small'"
            border
        >
          <template #title>
            <div class="text-title2">
              <el-text tag="b">
                {{ t('emailSignup.agreements.title') }}
              </el-text>
            </div>
          </template>

          <template #extra>
            <el-checkbox
                v-model="state.agreeAll"
                @change="handleAgreeAllChange"
                :label="t('emailSignup.agreements.agreeAll')"
            />
          </template>

          <el-descriptions-item>
            <template #label>
              <div class="cell-item">
                <el-checkbox
                    v-model="state.agreePersonalInfo"
                    :label="t('emailSignup.agreements.privacyPolicy')"
                />
              </div>
            </template>
            <el-button
                link
                type="info"
                @click="showPrivacyPolicyPopup"
                class="non-outline"
            >
              {{ t('emailSignup.buttons.viewDetails') }}
            </el-button>
          </el-descriptions-item>

          <el-descriptions-item>
            <template #label>
              <div class="cell-item">
                <el-checkbox
                    v-model="state.agreeThirdParty"
                    :label="t('emailSignup.agreements.thirdParty')"
                />
              </div>
            </template>
            <el-button
                link
                type="info"
                @click="showThirdPartyPopup"
                class="non-outline"
            >
              {{ t('emailSignup.buttons.viewDetails') }}
            </el-button>
          </el-descriptions-item>

          <el-descriptions-item>
            <template #label>
              <div class="cell-item">
                <el-checkbox
                    v-model="state.agreeEtc"
                    :label="t('emailSignup.agreements.etc')"
                />
              </div>
            </template>
            <div style="text-align: right;">
              <el-button
                  link
                  type="info"
                  @click="showEtcPopup"
                  class="non-outline"
              >
                {{ t('emailSignup.buttons.viewDetails') }}
              </el-button>
            </div>
          </el-descriptions-item>

        </el-descriptions>
        <div style="height: 50px;">
          <el-alert
              :title="t('emailSignup.messages.agreeToRequiredTerms')"
              type="error"
              :closable="false"
              show-icon
              :class="['agreement-error-alert', { 'is-visible': state.showAgreementError }]"
          />
        </div>
        <el-button
            type="primary"
            class="signup-button"
            @click="onClickSignUp"
        >
          {{ t('emailSignup.buttons.signUp') }}
        </el-button>
      </div>
    </el-card>

    <el-card
        class="signup-prompt-card"
        shadow="never"
    >
      <el-text>
        {{ t('emailSignup.prompt') }}
      </el-text>
      <el-button
          type="primary"
          link
          class="signup-link"
          @click="onClickToOpenLogin"
      >
        {{ t('emailSignup.login') }}
      </el-button>
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
.privacy-policy-message-box-modern .el-message-box__title { font-size: 18px; font-weight: 600; color: var(--el-text-color-primary); padding: 20px; }
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

  padding: 50px 20px;
}
.signup-card {
  width: 100%;
  max-width: 700px;

}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.main-title {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  font-size: 20px;
  text-align: center;
  color: var(--el-text-color-primary);
  margin: 0;
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
  margin-left: 2px;
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