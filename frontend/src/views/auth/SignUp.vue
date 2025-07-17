<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, watch, computed, h } from "vue";
import { InfoFilled } from "@element-plus/icons-vue";
import { ElMessageBox, ElCheckbox, ElTag, ElAlert } from 'element-plus';
import type { VNode } from 'vue';
import { defineComponent } from "@vue/runtime-dom";


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

// 동적 객체 생성
const initialData = formFields.reduce((acc, field) => ({ ...acc, [field]: '' }), {}) as any;
const initialRules = formFields.reduce((acc, field) => ({
  ...acc,
  [field]: {
    required: true,
    message: `[${fieldLabels[field]}] 필수조건입니다.`,
    trigger: 'change',
  }
}), {});
const initialVisible = formFields.reduce((acc, field) => ({ ...acc, [field]: false }), {});

// reactive 정의
const state = reactive({
  data: { ...initialData },
  rules: {
    ...initialRules,
  } as any,
  visible: initialVisible as any,
  /* 패스워드 복잡도 상태 */
  complexity: {
    percentage: 0, // 초기값은 0으로 설정
    status: '', // 초기에는 상태 없음
  },
  agreePersonalInfo: false, // (필수) 개인정보 수집 및 이용 동의
  agreeThirdParty: false,
  agreeEtc: false,
  agreeMarketing: false, // (선택) 마케팅 정보 수신 동의 <--- 이 항목을 추가하세요.

  // '전체 동의' 체크박스의 상태
  agreeAll: false,
})

onMounted(() => {
  document.addEventListener('mousemove', mousemoveHandler)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', mousemoveHandler)
})

/**
 * 새로운 규칙에 맞춰 비밀번호 복잡도를 계산하고 state를 업데이트하는 함수
 * @param {string} password - 검사할 비밀번호 문자열
 */
const updatePasswordComplexity = (password: any) => {
  // 비밀번호가 비어있으면 초기 상태로 리셋
  if (!password) {
    state.complexity.percentage = 0;
    state.complexity.status = '';
    return;
  }

  let score = 0;
  const totalChecks = 4; // 총 검사 항목 수

  // --- 규칙 검사 시작 ---

  // 1. 8자리 이상
  if (password.length >= 8) {
    score++;
  }

  // 2. 영문, 숫자, 특수문자를 모두 포함하는지 검사
  const hasEnglish = /[a-zA-Z]/.test(password);
  const hasNumber = /[0-9]/.test(password);
  const hasSpecialChar = /[^a-zA-Z0-9]/.test(password);
  if (hasEnglish && hasNumber && hasSpecialChar) {
    score++;
  }

  // 3. ID와 동일한지 검사 (ID가 존재하고, 비밀번호와 다를 경우)
  if (state.data.userId && password !== state.data.userId) {
    score++;
  } else if (!state.data.userId) {
    // ID 정보가 없는 경우, 이 검사는 통과한 것으로 간주
    score++;
  }

  // 4. 생년월일/전화번호 포함 여부 검사
  let containsPersonalInfo = false;
  // 전화번호 숫자만 추출하여 포함 여부 확인
  if (state.data.phoneNumber) {
    const phoneDigits = state.data.phoneNumber.replace(/\D/g, ''); // '01012345678'
    if (phoneDigits && password.includes(phoneDigits)) {
      containsPersonalInfo = true;
    }
  }
  // 생년월일 숫자만 추출하여 포함 여부 확인 (yyyymmdd, yymmdd, mmdd 등)
  if (state.data.birthdate) {
    const birthDigits = state.data.birthdate.replace(/\D/g, ''); // '19950823'
    if (birthDigits && (password.includes(birthDigits) || password.includes(birthDigits.substring(2)))) {
      containsPersonalInfo = true;
    }
  }

  if (!containsPersonalInfo) {
    score++;
  }

  // --- 점수에 따라 복잡도 상태 업데이트 ---
  // (score / totalChecks) 를 기반으로 percentage를 계산합니다.
  const percentage = Math.floor((score / totalChecks) * 100);
  state.complexity.percentage = percentage;

  if (percentage <= 25) {
    state.complexity.status = 'exception'; // 매우 약함 (빨간색)
  } else if (percentage <= 75) {
    state.complexity.status = 'warning'; // 보통 (노란색)
  } else {
    state.complexity.status = 'success'; // 강함 (초록색)
  }
};

// 'state.data.password' 값이 변경될 때마다 updatePasswordComplexity 함수를 실행
watch(() => state.data.password, (newPassword) => {
  updatePasswordComplexity(newPassword);
});

/**
 * 비밀번호 생성 규칙 툴팁 오픈
 */
const handleClickInfo = () => {
  visible.value = !visible.value;
}

const visible = ref(false)
const position = ref({
  top: 0,
  left: 0,
  bottom: 0,
  right: 0,
})

/**
 * 비밀번호 도움말 클릭 시 position
 * @type {Ref<UnwrapRef<{getBoundingClientRect: (function(): UnwrapRef<{top: number, left: number, bottom: number, right: number}>)}>, UnwrapRef<{getBoundingClientRect: (function(): UnwrapRef<{top: number, left: number, bottom: number, right: number}>)}> | {getBoundingClientRect: (function(): UnwrapRef<{top: number, left: number, bottom: number, right: number}>)}>}
 */
const triggerRef = ref({
  getBoundingClientRect: () => position.value,
})

/**
 * 비밀번호 도움말 클릭 시 마우스 이동 핸들러
 * @param clientX
 * @param clientY
 */
const mousemoveHandler = ({ clientX, clientY }: any) => {
  position.value = DOMRect.fromRect({
    x: clientX,
    y: clientY,
  })
}

/**
 * 필드 포커스 시 에러 메시지 숨기기
 * @param fieldName
 */
const handleFieldFocus = (fieldName: any) => {
  state.visible[fieldName] = false;
}

/**
 * 필드 블러 시 유효성 검사 실행
 * @param fieldName
 */
const handleFieldValidation = (fieldName: any) => {
  formRef.value.validateField(fieldName, (isValid: any, invalidFields: any) => {
    state.visible[fieldName] = !isValid;
  })
}

// 모든 필수 동의 항목의 리스트 (computed를 사용해 동적으로 관리)
const requiredAgreements = computed(() => [
  state.agreePersonalInfo,
  state.agreeThirdParty,
  state.agreeEtc,
]);

/**
 * '전체 동의' 체크박스를 클릭했을 때 실행되는 함수
 * @param {boolean} isChecked - '전체 동의' 체크박스의 새로운 값
 */
const handleAgreeAllChange = (isChecked: any) => {
  // 모든 개별 동의 항목의 상태를 '전체 동의' 상태와 동일하게 맞춰준다.
  state.agreePersonalInfo = isChecked;
  state.agreeThirdParty = isChecked;
  state.agreeEtc = isChecked;
  state.agreeMarketing = isChecked;
};

// 개별 동의 항목들의 상태가 변경되는 것을 감시 (watch)
watch(
    [() => state.agreePersonalInfo, () => state.agreeThirdParty, () => state.agreeEtc, () => state.agreeMarketing],
    (currentValues) => {
      // currentValues 배열에 false가 하나라도 포함되어 있는지 확인
      if (currentValues.includes(false)) {
        // 하나라도 체크 해제되면 '전체 동의'도 해제
        state.agreeAll = false;
      } else {
        // 모든 항목이 체크되었으면 '전체 동의'도 체크
        state.agreeAll = true;
      }
    });

const ReactiveVNode = defineComponent({
  props: {
    // 렌더링할 VNode를 만드는 함수를 prop으로 받습니다.
    renderFn: {
      type: Function,
      required: true,
    },
  },
  setup(props) {
    // setup 함수는 render 함수를 반환합니다.
    // prop으로 받은 함수를 실행하여 최종 VNode를 생성합니다.
    // 이 컴포넌트 덕분에 renderFn 내부에서 사용하는 반응형 데이터가 변경될 때마다
    // UI가 자동으로 다시 렌더링됩니다.
    return () => props.renderFn();
  },
});

/**
 * 개인정보 수집 및 이용 동의 보기 팝업
 */
const showPrivacyPolicyPopup = () => {
  // 1. 팝업 내부에서만 사용할 반응형 상태를 생성합니다. (기존과 동일)
  const popupState = reactive({
    isAgreedRequired: state.agreePersonalInfo,
    isAgreedMarketing: state.agreeMarketing,
  });

  // 2. 가독성을 높인 VNode를 생성합니다.
  const messageVNode = h(ReactiveVNode, {
    renderFn: () => {
      // --- 디자인 개선을 위한 VNode 재구성 ---

      // 섹션 제목: 팝업 내용이 무엇인지 명확하게 알려줍니다.
      const titleVNode = h('h4',
          { class: 'privacy-section-title' },
          '개인정보 수집·이용 내역'
      );

      // 테이블: '구분' 열을 추가하고 ElTag를 사용해 필수/선택 항목을 시각적으로 강조합니다.
      const policyTableVNode = h('table', { class: 'privacy-table' }, [
        h('thead', null, [
          h('tr', null, [
            h('th', { style: 'width: 15%' }, '구분'),
            h('th', { style: 'width: 28%' }, '수집 목적'),
            h('th', { style: 'width: 27%' }, '수집 항목'),
            h('th', { style: 'width: 30%' }, '보유 및 이용기간'),
          ]),
        ]),
        h('tbody', null, [
          // 필수 항목
          h('tr', null, [
            h('td', null, h(ElTag, { type: 'danger', size: 'small', effect: 'light' }, '필수')),
            h('td', { class: 'retention-period' }, '회원 식별 및 서비스 제공'),
            h('td', { class: 'retention-period' }, '아이디, 비밀번호, 이메일 주소'),
            // 보유 기간 내용을 한 곳으로 모아 명확성을 높입니다.
            h('td', { rowspan: 2, class: 'retention-period' }, '회원 탈퇴 시 즉시 파기. 단, 관련 법령에 따라 보관이 필요한 경우 해당 기간 동안 보존됩니다.'),
          ]),
          // 선택 항목
          h('tr', null, [
            h('td', null, h(ElTag, { type: 'info', size: 'small', effect: 'light' }, '선택')),
            h('td', { class: 'retention-period' }, '마케팅 및 광고 활용'),
            h('td', { class: 'retention-period' }, '연락처, 주소'),
          ]),
        ]),
      ]);

      // 안내 문구: ElAlert를 사용해 동의 거부 권리에 대한 내용을 강조합니다.
      const refusalInfoVNode = h(ElAlert,
          {
            class: 'refusal-info-alert',
            title: '동의 거부 권리 및 불이익 안내',
            type: 'info',
            closable: false,
            showIcon: true,
          },
          () => [
            '귀하는 개인정보 수집 및 이용에 대한 동의를 거부할 권리가 있습니다. ', // 첫 번째 줄 문자열
            h('br'), // <br> 태그에 해당하는 VNode를 삽입하여 줄바꿈
            '다만, 필수 항목에 대한 동의를 거부하실 경우 회원가입 및 관련 서비스 이용이 제한될 수 있습니다.' // 두 번째 줄 문자열
          ]
      );

      // 하단 동의 체크박스 영역: 클래스를 추가하여 스타일링을 용이하게 합니다.
      const agreementFooterVNode = h('div', { class: 'privacy-agreement-footer' }, [
        // 두 개의 체크박스를 담을 컨테이너
        h('div', { class: 'privacy-agreement-items' }, [
          // 필수 동의 체크박스
          h(ElCheckbox as any, {
            modelValue: popupState.isAgreedRequired,
            'onUpdate:modelValue': (newValue: boolean) => { popupState.isAgreedRequired = newValue; },
            size: 'large',
          }, () => [
            h('span', null, '(필수) 개인정보 수집 및 이용에 동의합니다.')
          ]),
          // 선택 동의 체크박스
          h(ElCheckbox as any, {
            modelValue: popupState.isAgreedMarketing,
            'onUpdate:modelValue': (newValue: boolean) => { popupState.isAgreedMarketing = newValue; },
            size: 'large',
            style: {
              marginTop: '8px' // 체크박스 자체에 상단 마진을 주어 간격을 만듭니다.
            },
          }, () => [
            h('span', null, '(선택) 마케팅 정보 수신(이메일, SMS)에 동의합니다.')
          ]),
        ])
      ]);

      // 최종 VNode 조합: 각 VNode를 배열로 묶어 렌더링합니다.
      return h('div', { class: 'privacy-dialog-content' }, [
        h('div', { class: 'privacy-scroll-content' }, [
          titleVNode,
          policyTableVNode,
          refusalInfoVNode,
        ]),
        agreementFooterVNode,
      ]);
    },
  });

  // 3. ElMessageBox 호출 (기존과 동일)
  ElMessageBox.alert(messageVNode, '개인정보 수집 및 이용 동의', {
    confirmButtonText: '확인',
    customClass: 'privacy-policy-message-box-modern',
    dangerouslyUseHTMLString: false,
  })
      .then(() => {
        // '확인' 버튼을 눌렀을 때, 팝업의 두 상태를 모두 메인 state에 반영합니다.
        state.agreePersonalInfo = popupState.isAgreedRequired;
        state.agreeMarketing = popupState.isAgreedMarketing;
      })
      .catch(() => {});
};

/**
 * 제3자 정보 제공 동의 보기 팝업
 */
const showThirdPartyPopup = () => {
  // 1. 팝업 내부에서만 사용할 반응형 상태를 생성합니다.
  const popupState = reactive({
    isAgreed: state.agreeThirdParty, // '제3자 정보 제공 동의' 상태와 연결
  });

  // 2. 가독성을 높인 VNode를 생성합니다.
  const messageVNode = h(ReactiveVNode, {
    renderFn: () => {
      // 섹션 제목
      const titleVNode = h('h4',
          { class: 'privacy-section-title' },
          '개인정보 국외 이전 및 처리 위탁 내역'
      );

      // 테이블 내용: Oracle Cloud에 데이터 처리를 위탁하는 내용으로 변경
      const policyTableVNode = h('table', { class: 'privacy-table' }, [
        h('thead', null, [
          h('tr', null, [
            h('th', { style: { width: '25%' } }, '이전받는 자'),
            h('th', { style: { width: '35%' } }, '이전 목적'),
            h('th', { style: { width: '20%' } }, '개인정보 항목'),
            h('th', { style: { width: '20%' } }, '보유/이용기간'),
          ]),
        ]),
        h('tbody', null, [
          h('tr', null, [
            h('td', null, 'Oracle Corporation (미국)'),
            h('td', { class: 'retention-period' }, '클라우드 데이터베이스 서버 운영 및 데이터의 안전한 저장/관리'),
            h('td', { class: 'retention-period' }, '회원가입 시 수집된 개인정보 일체 (아이디, 비밀번호, 이메일 등)'),
            h('td', { class: 'retention-period' }, '회원 탈퇴 또는 위탁 계약 종료 시까지'),
          ]),
        ]),
      ]);

      // 안내 문구: 클라우드 사용 시나리오에 맞게 수정
      const refusalInfoVNode = h(ElAlert,
          {
            class: 'refusal-info-alert',
            title: '동의 거부 권리 및 불이익 안내',
            type: 'info',
            closable: false,
            showIcon: true,
          },
          () => [
            '귀하는 개인정보의 국외 이전 및 처리 위탁에 대한 동의를 거부할 권리가 있습니다.',
            h('br'),
            '다만, 동의를 거부하실 경우 서비스의 핵심 기능이 동작하지 않으므로 회원가입 및 모든 서비스 이용이 불가능합니다.'
          ]
      );

      // 하단 동의 체크박스 영역: 문구 수정
      const agreementFooterVNode = h('div', { class: 'privacy-agreement-footer' }, [
        h(ElCheckbox as any, {
          modelValue: popupState.isAgreed,
          'onUpdate:modelValue': (newValue: boolean) => { popupState.isAgreed = newValue; },
          size: 'large',
        }, () => [
          h('span', null, '(필수) 위 내용을 모두 확인하였으며, 개인정보의 국외 이전 및 처리 위탁에 동의합니다.')
        ]),
      ]);

      // 최종 VNode 조합
      return h('div', { class: 'privacy-dialog-content' }, [
        h('div', { class: 'privacy-scroll-content' }, [
          titleVNode,
          policyTableVNode,
          refusalInfoVNode,
        ]),
        agreementFooterVNode,
      ]);
    },
  });

  // 3. ElMessageBox 호출: 팝업 제목 수정
  ElMessageBox.alert(messageVNode, '개인정보 국외 이전 및 처리 위탁 동의', {
    confirmButtonText: '확인',
    customClass: 'privacy-policy-message-box-modern',
    dangerouslyUseHTMLString: false,
  })
      .then(() => {
        // '확인' 버튼 클릭 시, 팝업의 상태를 메인 state에 최종 반영
        state.agreeThirdParty = popupState.isAgreed;
      })
      .catch(() => {});
};

/**
 * 기타 사항 동의 보기 팝업
 */
const showEtcPopup = () => {
  // 1. 팝업 내부에서만 사용할 반응형 상태를 생성합니다.
  const popupState = reactive({
    isAgreed: state.agreeEtc, // '기타 사항 동의' 상태와 연결
  });

  // 2. 가독성을 높인 VNode를 생성합니다.
  const messageVNode = h(ReactiveVNode, {
    renderFn: () => {
      // 아이콘을 사용하기 위해 InfoFilled 컴포넌트를 h() 함수로 감쌉니다.
      const iconVNode = (component: any) => h('div', { class: 'etc-icon-wrapper' }, h(component, { class: 'etc-icon' }));

      // --- 각 섹션을 VNode로 정의합니다. 내용은 주석을 참고하여 자유롭게 채우시면 됩니다. ---

      // 첫 번째 섹션
      const sectionOne = h('div', { class: 'etc-section' }, [
        h('div', { class: 'etc-section-header' }, [
          iconVNode(InfoFilled),
          h('h5', { class: 'etc-section-title' }, '첫 번째 주요 정책')
        ]),
        h('p', { class: 'etc-section-content' },
            '이곳에 첫 번째 주요 정책 또는 기타 안내 사항에 대한 내용을 상세하게 기술합니다. 필요에 따라 여러 문단으로 구성할 수 있습니다. 사용자가 꼭 알아야 할 중요한 정보를 명확하고 간결하게 전달하는 것이 좋습니다.'
        ),
      ]);

      // 두 번째 섹션
      const sectionTwo = h('div', { class: 'etc-section' }, [
        h('div', { class: 'etc-section-header' }, [
          iconVNode(InfoFilled),
          h('h5', { class: 'etc-section-title' }, '두 번째 고려 사항')
        ]),
        h('p', { class: 'etc-section-content' },
            '이곳에는 두 번째 안내 사항을 작성합니다. 예를 들어, 서비스 이용 규칙, 콘텐츠 저작권 정책, 혹은 분쟁 해결 절차 등에 대한 내용을 포함할 수 있습니다.'
        ),
        // 필요하다면 목록(ul/li)을 추가하여 가독성을 높일 수 있습니다.
        h('ul', { class: 'etc-list' }, [
          h('li', null, '항목 1: 관련된 세부 규칙이나 가이드라인을 명시합니다.'),
          h('li', null, '항목 2: 사용자가 준수해야 할 또 다른 중요한 사항입니다.'),
        ]),
      ]);

      // 하단 동의 체크박스 영역
      const agreementFooterVNode = h('div', { class: 'privacy-agreement-footer' }, [
        h(ElCheckbox as any, {
          modelValue: popupState.isAgreed,
          'onUpdate:modelValue': (newValue: boolean) => { popupState.isAgreed = newValue; },
          size: 'large',
        }, () => [
          h('span', null, '(선택) 위 기타 사항을 모두 확인하였으며, 내용에 동의합니다.')
        ]),
      ]);

      // 최종 VNode 조합
      return h('div', { class: 'privacy-dialog-content' }, [
        h('div', { class: 'privacy-scroll-content' }, [
          // 생성한 섹션들을 배열 안에 나열합니다.
          sectionOne,
          sectionTwo,
        ]),
        agreementFooterVNode,
      ]);
    },
  });

  // 3. ElMessageBox 호출
  ElMessageBox.alert(messageVNode, '기타 사항 안내 및 동의', {
    confirmButtonText: '확인',
    customClass: 'privacy-policy-message-box-modern',
    dangerouslyUseHTMLString: false,
  })
      .then(() => {
        // '확인' 버튼 클릭 시, 팝업의 상태를 메인 state에 최종 반영
        state.agreeEtc = popupState.isAgreed;
      })
      .catch(() => {});
};

/**
 * 가입하기
 * @returns {Promise<void>}
 */
const onClickSignUp = async () => {

  await formRef.value.validate((valid: any, fields: any) => {
    if (valid) {
      console.log('submit!')
    } else {
      for (const fieldName in fields) {
        // state.visible 객체에 해당 필드명과 일치하는 키가 있는지 확인 후, 값을 true로 변경
        if (Object.prototype.hasOwnProperty.call(state.visible, fieldName)) {
          state.visible[fieldName] = true;
        }
      }

    }
  })
}

</script>

<template>
  <div class="signup-container">

    <!-- 회원가입 card -->
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
              <el-input
                  placeholder="6~12자의 영문/숫자 조합"
                  clearable
                  class="input-with-button"
                  v-model="state.data.userId"
                  ref="userId"
                  @blur="() => handleFieldValidation('userId')"
              >
                <template #append>
                  <el-button type="primary">중복 체크</el-button>
                </template>
              </el-input>
            </el-form-item>
          </template>
        </el-popover>

        <!-- 비밀번호 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            :content="state.rules.password.message"
            placement="right-start"
            width="250"
            :visible="state.visible.password"
        >
          <template #reference>
            <el-form-item label="비밀번호" prop="password" required>
              <el-input
                  v-model="state.data.password"
                  type="password"
                  show-password
                  placeholder="8자 이상 입력해 주세요"
                  clearable
                  ref="password"
                  @blur="() => handleFieldValidation('password')"
              >
              </el-input>
            </el-form-item>
          </template>
        </el-popover>

        <!-- 패스워드 복잡도 -->
        <el-form-item>
          <div style="display: flex; align-items: center; width: 100%; max-width: 500px; margin-bottom: 20px; gap: 8px">
            <el-tag
                plain
                class="password-comple-tag"
            >
              <el-icon
                  @click="handleClickInfo"
                  class="password-comple-tag-icon"
              >
                <InfoFilled />
              </el-icon>
              <el-text class="password-comple-text">
                비밀번호 복잡도
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
        <!-- 패스워드 복잡도 -->

        <!-- 비밀번호 안내 tooltip (마우스 따라다니는 이벤트) -->
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
              <el-text class="password-info-title">
                비밀번호 생성규칙
              </el-text>
              <el-divider class="password-info-divider"></el-divider>
              <el-alert title="비밀번호 생성 관련 안내" type="info" show-icon :closable="false" />

              <el-divider class="password-info-divider"></el-divider>
              <el-tag class="password-info-content">1. 8자리 이상</el-tag>
              <el-tag class="password-info-content">2. 영문/특수기호/숫자 전부 포함</el-tag>
              <el-tag class="password-info-content">3. ID와 동일하게 사용불가</el-tag>
              <el-tag class="password-info-content">4. 생년월일/전화번호 포함 불가</el-tag>
              <el-divider class="password-info-divider"></el-divider>
              <el-tag class="password-info-footer">생성규칙에 맞춰 비밀번호를 생성해주세요</el-tag>
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
                  placeholder="실명을 입력해 주세요"
                  clearable
                  ref="userName"
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
                  placeholder="example@email.com"
                  clearable
                  ref="email"
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
                  placeholder="010-1234-5678"
                  clearable
                  ref="phoneNumber"
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
                  placeholder="YYYY-MM-DD"
                  clearable
                  ref="birthDate"
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
              <el-radio-group
                  v-model="state.data.genderCode"
                  ref="genderCode"
                  @change="() => handleFieldValidation('genderCode')"
              >
                <el-radio-button label="male">남자</el-radio-button>
                <el-radio-button label="female">여자</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </template>
        </el-popover>
      </el-form>

      <!-- 구분선 -->
      <el-divider />

      <!-- 이용약관동의 -->
      <div style="margin-top: 24px;">
        <el-descriptions
            class="margin-top"
            :column="2"
            :size="'small'"
            border
        >
          <template #title>
            <div class="text-title2">
              <el-text tag="b">서비스 이용에 대한 동의</el-text>
            </div>
          </template>
          <template #extra>
            <!-- '전체 동의' 체크박스 -->
            <!-- v-model 대신 :model-value와 @change를 명시적으로 사용해도 됩니다. -->
            <el-checkbox
                v-model="state.agreeAll"
                @change="handleAgreeAllChange"
                label="전체동의"
            />
          </template>

          <!-- 개별 동의 항목들 -->
          <el-descriptions-item>
            <template #label>
              <div class="cell-item">
                <!-- 각 항목을 state의 개별 속성과 v-model로 바인딩 -->
                <el-checkbox v-model="state.agreePersonalInfo" label="개인정보수집이용동의" />
              </div>
            </template>
            <el-button link type="info" @click="showPrivacyPolicyPopup">보기</el-button>
          </el-descriptions-item>

          <el-descriptions-item>
            <template #label>
              <div class="cell-item">
                <el-checkbox v-model="state.agreeThirdParty" label="제3자정보제공동의" />
              </div>
            </template>
            <el-button link type="info" @click="showThirdPartyPopup">보기</el-button>
          </el-descriptions-item>

          <el-descriptions-item>
            <template #label>
              <div class="cell-item">
                <el-checkbox v-model="state.agreeEtc" label="기타사항" />
              </div>
            </template>
            <div style="text-align: right;">
              <el-button link type="info" @click="showEtcPopup">보기</el-button>
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <el-button
            type="primary"
            class="signup-button"
            @click="onClickSignUp"
        >
          가입하기
        </el-button>

      </div>
    </el-card>
  </div>
</template>

<!--
  ElMessageBox는 App 루트에 생성되므로, scoped 스타일은 적용되지 않습니다.
  전역 스타일로 정의해야 합니다.
-->
<style>
.privacy-section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0; /* 제목과 테이블 사이 간격 */
}

/* 테이블의 '보유 기간' 셀 스타일 */
.privacy-table .retention-period {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

/* ElTag를 가운데 정렬하기 위한 스타일 */
.privacy-table td .el-tag {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 auto;
}

/* 동의 거부 안내 Alert 스타일 */
.refusal-info-alert {
  margin-top: 20px; /* 테이블과의 간격 */
}
.refusal-info-alert .el-alert__description {
  font-size: 13px !important; /* Element Plus 기본 스타일 덮어쓰기 */
}

/* 하단 동의 체크박스 사이즈 조정 */
.privacy-agreement-footer .el-checkbox.el-checkbox--large {
  height: auto; /* 라벨이 여러 줄일 때를 대비 */
}

/* 하단 동의 체크박스 라벨의 줄바꿈 처리 및 폰트 강조 */
.privacy-agreement-footer .el-checkbox__label {
  white-space: normal;
  line-height: 1.5;
  color: #303133;
  font-weight: 500;
}
/* MessageBox 자체의 스타일링 */
.privacy-policy-message-box-modern {
  max-width: 830px;
  width: 90%;
  border-radius: 10px;
  padding: 0; /* 내부에서 패딩을 제어하기 위해 초기화 */
}

.privacy-policy-message-box-modern .el-message-box__header {
  padding: 18px 24px;
  border-bottom: 1px solid #ebeef5;
}

.privacy-policy-message-box-modern .el-message-box__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.privacy-policy-message-box-modern .el-message-box__content {
  padding: 0; /* 내부에서 패딩을 제어하기 위해 초기화 */
}

.privacy-policy-message-box-modern .el-message-box__btns {
  padding: 10px 24px 18px;
}

/* 우리가 VNode로 만든 컨텐츠의 내부 스타일링 */
.privacy-dialog-content {
  display: flex;
  flex-direction: column;
  max-height: 60vh; /* 뷰포트 높이의 60%를 최대로 하여 너무 길어지지 않게 함 */
}

.privacy-scroll-content {
  flex-grow: 1;
  overflow-y: auto; /* 내용이 길어지면 이 부분만 스크롤됨 */
  padding: 20px 24px;
}

/* 표 스타일 */
.privacy-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  text-align: left;
  margin-bottom: 20px;
}

.privacy-table th,
.privacy-table td {
  border: 1px solid #ebeef5;
  padding: 12px 14px;
  vertical-align: middle;
}

.privacy-table thead {
  background-color: #f5f7fa;
  font-weight: 600;
  color: #606266;
}

.privacy-table td {
  color: #303133;
  line-height: 1.6;
}

/* 동의 거부 안내 문구 스타일 */
.refusal-info {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  margin: 0;
}

/* 하단 동의 체크박스 영역 스타일 */
.privacy-agreement-footer {
  flex-shrink: 0; /* 스크롤 되어도 크기가 줄어들지 않음 */
  padding: 16px 24px;
  background-color: #f5f7fa;
  border-top: 1px solid #ebeef5;
}

/* 하단 동의 체크박스 라벨의 줄바꿈 처리 */
.privacy-agreement-footer .el-checkbox__label {
  white-space: normal;
  line-height: 1.5;
  color: #303133;
  font-weight: 500;
}

/* 기타사항 */
.etc-section {
  margin-bottom: 24px; /* 섹션 간의 간격 */
}

/* 섹션 헤더 (아이콘 + 제목) */
.etc-section-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

/* 아이콘을 감싸는 래퍼 */
.etc-icon-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  margin-right: 8px;
  border-radius: 50%;
  background-color: #eef2ff; /* 아이콘 배경색 */
}

/* 아이콘 스타일 */
.etc-icon {
  font-size: 14px;
  color: #4338ca; /* 아이콘 색상 */
}

/* 섹션 제목 스타일 */
.etc-section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

/* 섹션 내용 스타일 */
.etc-section-content {
  font-size: 14px;
  line-height: 1.7;
  color: #606266;
  padding-left: 32px; /* 아이콘 너비만큼 들여쓰기하여 정렬 */
  margin: 0;
}

/* 목록 스타일 */
.etc-list {
  padding-left: 52px; /* 들여쓰기 추가 */
  margin-top: 10px;
  font-size: 14px;
  color: #606266;
}

.etc-list li {
  margin-bottom: 6px;
}
</style>

<style scoped>
.signup-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
  padding: 20px;
}
.signup-card {
  width: 100%;
  max-width: 500px;
}
.card-header {
  font-size: 20px;
  font-weight: bold;
  text-align: center;
}
.text-title1 {
  text-align: left;
  margin-bottom: 12px;
}
.text-title2 {
  text-align: left;
  margin-bottom: 12px;
}
.signup-form .el-form-item {
  margin-bottom: 8px; /* 모든 el-form-item의 아래쪽 간격을 6px로 설정 */
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
  margin-top: 50px;
}
/* el-popper 스타일 문제를 해결하기 위해 :deep() 선택자 사용 */
:deep(.el-popper[data-popper-placement^=right]>.el-popper__arrow:before) {
  border-right-color: #e53e3e !important;
  border-top-color: #e53e3e !important;
}
.password-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
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
</style>