<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, watch, computed, h } from "vue";
import { InfoFilled } from "@element-plus/icons-vue";
import { ElMessageBox, ElCheckbox } from 'element-plus';
import type { VNode } from 'vue';


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
};

// 동적 객체 생성
const initialData = formFields.reduce((acc, field) => ({ ...acc, [field]: '' }), {});
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
    password: { // required 외에 다른 validator를 추가할 때
      // validator: customValidator,
      ...initialRules.password, // 기본 required 규칙은 유지
      // 다른 규칙 추가...
    },
  },
  visible: initialVisible,
  /* 패스워드 복잡도 상태 */
  complexity: {
    percentage: 0, // 초기값은 0으로 설정
    status: '', // 초기에는 상태 없음
  },
  // 개별 동의 항목의 체크 상태
  agreePersonalInfo: false,
  agreeThirdParty: false,
  agreeEtc: false,

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
const updatePasswordComplexity = (password) => {
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
const mousemoveHandler = ({ clientX, clientY }) => {
  position.value = DOMRect.fromRect({
    x: clientX,
    y: clientY,
  })
}

/**
 * 필드 포커스 시 에러 메시지 숨기기
 * @param fieldName
 */
const handleFieldFocus = (fieldName) => {
  state.visible[fieldName] = false;
}

/**
 * 필드 블러 시 유효성 검사 실행
 * @param fieldName
 */
const handleFieldValidation = (fieldName) => {
  formRef.value.validateField(fieldName, (isValid, invalidFields) => {
    state.visible[fieldName] = !isValid;
    if (!isValid) {
      // rules에 정의된 메시지를 state에 저장
      state.message[fieldName] = invalidFields[fieldName][0].message;
    }
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
const handleAgreeAllChange = (isChecked) => {
  // 모든 개별 동의 항목의 상태를 '전체 동의' 상태와 동일하게 맞춰준다.
  state.agreePersonalInfo = isChecked;
  state.agreeThirdParty = isChecked;
  state.agreeEtc = isChecked;
};

// 개별 동의 항목들의 상태가 변경되는 것을 감시 (watch)
watch(requiredAgreements, (currentValues) => {
  // currentValues 배열에 false가 하나라도 포함되어 있는지 확인
  if (currentValues.includes(false)) {
    // 하나라도 체크 해제되면 '전체 동의'도 해제
    state.agreeAll = false;
  } else {
    // 모든 항목이 체크되었으면 '전체 동의'도 체크
    state.agreeAll = true;
  }
});

const showPrivacyPolicyPopup = () => {
  // 표 형식으로 약관 내용을 구조화하여 가독성 향상
  const policyTableVNode = h('table', { class: 'privacy-table' }, [
    h('thead', null, [
      h('tr', null, [
        h('th', null, '수집 목적'),
        h('th', null, '수집 항목'),
        h('th', null, '보유 기간'),
      ]),
    ]),
    h('tbody', null, [
      h('tr', null, [
        h('td', null, '회원 식별 및 서비스 제공'),
        h('td', null, '아이디, 비밀번호, 이메일 주소'),
        h('td', { rowspan: 2 }, '회원 탈퇴 시 즉시 파기 (단, 관계 법령에 따라 보존 필요 시 해당 기간까지 보관)'),
      ]),
      h('tr', null, [
        h('td', null, '마케팅 및 광고 활용 (선택)'),
        h('td', null, '연락처, 주소'),
      ]),
    ]),
  ]);

  // 동의 거부 관련 안내 문구
  const refusalInfoVNode = h('p', { class: 'refusal-info' },
      '※ 귀하는 위 동의를 거부할 권리가 있으나, 필수 항목에 대한 동의 거부 시 서비스 이용이 제한될 수 있습니다.'
  );

  // 하단 동의 체크박스 영역
  const agreementFooterVNode = h('div', { class: 'privacy-agreement-footer' }, [
    h(ElCheckbox, {
      modelValue: state.agreePersonalInfo,
      'onUpdate:modelValue': (newValue: boolean) => {
        state.agreePersonalInfo = newValue;
      },
      label: '위 내용을 모두 확인하였으며, 개인정보 수집 및 이용에 동의합니다.',
      size: 'default', // 팝업 안에서는 default 사이즈가 더 잘 어울립니다.
    }),
  ]);

  // 위의 모든 VNode를 조합하여 최종 메시지 구성
  const messageVNode: VNode = h('div', { class: 'privacy-dialog-content' }, [
    h('div', { class: 'privacy-scroll-content' }, [
      policyTableVNode,
      refusalInfoVNode,
    ]),
    agreementFooterVNode,
  ]);

  // ElMessageBox 호출
  ElMessageBox.alert(messageVNode, '개인정보 수집 및 이용 동의', {
    confirmButtonText: '확인',
    // 이전 클래스와 충돌을 피하기 위해 새 클래스 이름 사용
    customClass: 'privacy-policy-message-box-modern',
    dangerouslyUseHTMLString: false, // VNode를 사용하므로 이 옵션은 false여야 합니다.
  }).catch(() => {});
};

/**
 * 가입하기
 * @returns {Promise<void>}
 */
const onClickSignUp = async () => {

  await formRef.value.validate((valid, fields) => {
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
            <el-button link type="info">보기</el-button>
          </el-descriptions-item>

          <el-descriptions-item>
            <template #label>
              <div class="cell-item">
                <el-checkbox v-model="state.agreeEtc" label="기타사항" />
              </div>
            </template>
            <div style="text-align: right;">
              <el-button link type="info">보기</el-button>
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
/* MessageBox 자체의 스타일링 */
.privacy-policy-message-box-modern {
  max-width: 680px;
  width: 90%;
  border-radius: 12px;
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
  padding: 2px 4px 4px 4px;
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