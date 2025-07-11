<script setup>

const popoverStyle = {
  padding: '6px 12px', // 가로 패딩을 살짝 넓혀 가독성 확보
  fontSize: '12px',
  lineHeight: 1.6,
  borderRadius: '4px',
  background: '#f8f9fa',
  color: '#e53e3e',
  fontWeight: 'bold',
  border: '1px solid #dee2e6',
  boxShadow: 'none',
};

</script>

<template>
  <div class="signup-container">
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
        <el-form-item label="아이디" prop="id">
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

        <!-- 비밀번호 -->
        <el-form-item label="비밀번호" prop="password">
          <el-popover
              :popper-style="popoverStyle"
              content="비밀번호는 필수조건입니다."
              placement="right-start"
              width="250"
              :visible="true"
          >
            <template #reference>
              <el-input
                  type="password"
                  show-password
                  placeholder="8자 이상 입력해 주세요"
                  clearable
              />
            </template>
          </el-popover>
        </el-form-item>

        <!-- 비밀번호 확인 -->
        <el-form-item label="비밀번호 확인" prop="confirmPassword">
          <el-popover
              :popper-style="popoverStyle"
              content="비밀번호가 일치하지 않습니다."
              placement="right-start"
              width="250"
              :visible="true"
          >
            <template #reference>
              <el-input
                  type="password"
                  show-password
                  placeholder="비밀번호를 다시 입력해 주세요"
                  clearable
              />
            </template>
          </el-popover>
        </el-form-item>

        <!-- 이름 -->
        <el-form-item label="이름" prop="name">
          <el-input placeholder="실명을 입력해 주세요" clearable />
        </el-form-item>

        <!-- 이메일 -->
        <el-form-item label="이메일" prop="email">
          <el-input placeholder="example@email.com" clearable />
        </el-form-item>

        <!-- 전화번호 -->
        <el-form-item label="전화번호" prop="phone">
          <el-input placeholder="010-1234-5678" clearable />
        </el-form-item>

        <!-- 성별 -->
        <el-form-item label="성별" prop="gender">
          <el-radio-group>
            <el-radio label="male">남자</el-radio>
            <el-radio label="female">여자</el-radio>
          </el-radio-group>
        </el-form-item>

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
            <div style="text-align: left; margin-bottom: 12px;">
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
.custom-popover {
  padding: 12px 16px;
  color: #e0e0e0; /* 밝은 회색 텍스트 */
  font-size: 13px;
  line-height: 1.6;
  border: 1px solid #444; /* 테두리 살짝 추가 */
  border-radius: 8px;
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.3);

  /* 고급스러운 다크 그레이 배경 */
  background: #2c3e50;
}
</style>