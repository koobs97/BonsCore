<script setup>

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

</script>

<template>
  <div class="signup-container">

    <!-- 회원가입 card -->
    <el-card class="signup-card" shadow="never">

      <template #header>
        <div class="card-header">회원가입</div>
      </template>

      <el-form
          ref="ruleFormRef"
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
            content="아이디는 필수조건입니다."
            placement="right-start"
            width="250"
            :visible="true"
        >
          <template #reference>
            <el-form-item label="아이디" prop="id" required>
              <el-input
                  placeholder="6~12자의 영문/숫자 조합"
                  clearable
                  class="input-with-button"
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
            :visible="true"
        >
          <template #reference>
            <el-form-item label="비밀번호" prop="password" required>
              <el-input
                  type="password"
                  show-password
                  placeholder="8자 이상 입력해 주세요"
                  clearable
              />
            </el-form-item>
          </template>
        </el-popover>

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
</style>