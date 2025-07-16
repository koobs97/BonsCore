<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from "vue";
import { InfoFilled } from "@element-plus/icons-vue";


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
const keys = ['userId', 'password', 'email'];
const form = Object.fromEntries(keys.map(k => [k, ref('')]))

// reactive 정의
const state = reactive({
  data: {
    userId: '',
    password: '',
  },
  message: {
    password: {
      required: '비밀번호는 필수조건입니다.',
      noUserId: '비밀번호에 아이디를 포함할 수 없습니다.',
      minLength: '비밀번호는 최소 8자 이상이어야 합니다.',
      maxLength: '비밀번호는 최대 20자 이하이어야 합니다.',
      hasNumber: '비밀번호에 숫자를 포함해야 합니다.',
      hasUpperCase: '비밀번호에 대문자를 포함해야 합니다.',
      hasLowerCase: '비밀번호에 소문자를 포함해야 합니다.',
      hasSpecialChar: '비밀번호에 특수문자를 하나 이상 포함해야 합니다.',
      noWhitespace: '비밀번호에 공백을 포함할 수 없습니다.',
      weakPassword: '너무 쉬운 비밀번호는 사용할 수 없습니다.',
    },
  },
  rules: {
    userId: { required: true, message: '아이디는 필수조건입니다.', trigger: 'blur' },
    password: { required: true, message: '비밀번호는 필수조건입니다.', trigger: 'blur' },
  },
  visible: {
    userId: false,
    password: false,
  }
})

onMounted(() => {
  document.addEventListener('mousemove', mousemoveHandler)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', mousemoveHandler)
})

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

/**
 * 가입하기
 * @returns {Promise<void>}
 */
const onClickSignUp = async () => {

  await formRef.value.validate((valid, fields) => {
    if (valid) {
      console.log('submit!')
    } else {
      console.log('error submit!', fields)

      if('userId' in fields) {
        state.visible.userId = true;
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
        <div style="text-align: left; margin-bottom: 12px;">
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
            content="비밀번호는 필수조건입니다."
            placement="right-start"
            width="250"
            :visible="state.visible.password"
        >
          <template #reference>
            <el-form-item label="비밀번호" prop="password" required>
              <el-input
                  type="password"
                  show-password
                  placeholder="8자 이상 입력해 주세요"
                  clearable
              >
                <template #suffix>
                  <el-icon @click="handleClickInfo" class="cursor-pointer">
                    <InfoFilled />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>
          </template>
        </el-popover>

        <!-- 비밀번호 안내 tooltip (마우스 따라다니는 이벤트) -->
        <el-tooltip
            v-model:visible="visible"
            placement="right"
            effect="light"
            trigger="click"
            virtual-triggering
            :virtual-ref="triggerRef"
        >
          <template #content>
            <div class="password-info">
              <el-text class="password-info-title">
                <el-icon>
                  <InfoFilled />
                </el-icon>
                비밀번호 생성규칙
              </el-text>
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

        <!-- 비밀번호 확인 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            content="비밀번호가 일치하지 않습니다."
            placement="right-start"
            width="250"
            :visible="true"
        >
          <template #reference>
            <el-form-item label="비밀번호 확인" prop="confirmPassword" required>
              <el-input
                  type="password"
                  show-password
                  placeholder="비밀번호를 다시 입력해 주세요"
                  clearable
              />
            </el-form-item>
          </template>
        </el-popover>

        <!-- 이름 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            content="이름은 필수조건입니다."
            placement="right-start"
            width="250"
            :visible="true"
        >
          <template #reference>
            <el-form-item label="이름" prop="name" required>
              <el-input placeholder="실명을 입력해 주세요" clearable />
            </el-form-item>
          </template>
        </el-popover>

        <!-- 이메일 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            content="이메일은 필수조건입니다."
            placement="right-start"
            width="250"
            :visible="true"
        >
          <template #reference>
            <el-form-item label="이메일" prop="email" required>
              <el-input placeholder="example@email.com" clearable />
            </el-form-item>
          </template>
        </el-popover>

        <!-- 전화번호 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            content="전화번호는 필수조건입니다."
            placement="right-start"
            width="250"
            :visible="true"
        >
          <template #reference>
            <el-form-item label="전화번호" prop="phone" required>
              <el-input placeholder="010-1234-5678" clearable />
            </el-form-item>
          </template>
        </el-popover>

        <!-- 생년월일 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            content="생년월일은 필수조건입니다."
            placement="right-start"
            width="250"
            :visible="true"
        >
          <template #reference>
            <el-form-item label="생년월일" prop="phone" required>
              <el-input placeholder="YYYY-MM-DD" clearable />
            </el-form-item>
          </template>
        </el-popover>

        <!-- 성별 -->
        <el-popover
            popper-class="custom-error-popover"
            :popper-style="popoverStyle"
            content="성별은 필수조건입니다."
            placement="right-start"
            width="250"
            :visible="true"
        >
          <template #reference>
            <el-form-item label="성별" prop="gender" required>
              <el-radio-group >
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
            <div style="text-align: left;">
              <el-text tag="b">서비스 이용에 대한 동의</el-text>
            </div>
          </template>
          <template #extra>
            <el-checkbox label="전체동의" value="Value A" />
          </template>

          <el-descriptions-item>
            <template #label>
              <div class="cell-item">
                <el-checkbox label="개인정보수집이용동의" value="Value A" />
              </div>
            </template>
            <el-button link type="info">보기</el-button>
          </el-descriptions-item>
          <el-descriptions-item>
            <template #label>
              <div class="cell-item">
                <el-checkbox label="제3자정보제공동의" value="Value A" />
              </div>
            </template>
            <el-button link type="info">보기</el-button>
          </el-descriptions-item>
          <el-descriptions-item>
            <template #label>
              <div class="cell-item">
                  <el-checkbox label="기타사항" value="Value A" />
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
</style>