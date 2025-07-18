<script setup lang="ts">
// 로직은 비워둡니다.
import { ref } from 'vue';
// DocumentCopy 아이콘과 ElMessage 컴포넌트 추가
import { QuestionFilled, DocumentCopy } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

// 탭 상태 관리를 위한 ref (UI 표시용)
const activeTab = ref('email');

// UI 흐름 제어를 위한 상태
const isCodeSent = ref(false); // 인증번호가 전송되었는지 여부
const isIdFound = ref(false);  // 아이디를 찾았는지 여부

// 폼 데이터 (UI 표시용)
const userName = ref('');
const userEmail = ref('');
const authCode = ref('');

// 찾은 아이디 정보 (화면 표시용과 실제 데이터 분리)
const maskedFoundUserId = ref('example***'); // 화면에 표시될 마스킹된 아이디
const fullFoundUserId = ref('example_full_id'); // 실제 API 응답으로 받을 전체 아이디

/**
 * (가상) 인증번호 전송 함수
 * 실제로는 여기서 API를 호출합니다.
 */
const sendAuthCode = () => {
  isCodeSent.value = true;
};

/**
 * (가상) 아이디 찾기 확인 함수
 * 실제로는 여기서 API를 호출합니다.
 */
const findId = () => {
  isIdFound.value = true;
}

/**
 * 클립보드 복사 기능
 * @param {string} text - 복사할 텍스트
 */
const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text);
    ElMessage({
      message: '아이디가 복사되었습니다.',
      type: 'success',
    });
  } catch (err) {
    ElMessage({
      message: '복사에 실패했습니다. 다시 시도해주세요.',
      type: 'error',
    });
    console.error('Failed to copy ID: ', err);
  }
};
</script>

<template>
  <div class="find-id-container">
    <el-card class="find-id-card" shadow="never">
      <!-- 1단계: 아이디 찾기 정보 입력 -->
      <div v-if="!isIdFound">
        <h2 class="title">아이디 찾기</h2>
        <p class="description">가입 시 등록한 정보로 아이디를 찾을 수 있습니다.</p>

        <el-tabs v-model="activeTab" class="find-tabs" stretch>
          <el-tab-pane label="이메일로 찾기" name="email">
            <el-form class="find-form" label-position="top">
              <el-form-item label="이름">
                <el-input
                    v-model="userName"
                    placeholder="가입 시 등록한 이름을 입력하세요."
                    size="large"
                />
              </el-form-item>
              <el-form-item>
                <template #label>
                  <div class="label-with-help">
                    <span>이메일</span>
                    <el-popover placement="right" title="도움말" :width="320" trigger="click">
                      <template #reference>
                        <el-button :icon="QuestionFilled" type="info" link circle class="help-icon-button"/>
                      </template>
                      <div class="popover-content">
                        <p>이메일이 도착하지 않는 경우, 아래 내용을 확인해주세요.</p>
                        <ul>
                          <li>입력하신 이름과 이메일 주소가 가입 시 정보와 일치하는지 확인해주세요.</li>
                          <li>스팸(정크) 메일함으로 자동 분류되지 않았는지 확인해주세요.</li>
                          <li>통신사나 이메일 서비스 제공사의 문제로 수신이 지연될 수 있습니다. 잠시 후 다시 시도해주세요.</li>
                        </ul>
                      </div>
                    </el-popover>
                  </div>
                </template>
                <el-input v-model="userEmail" placeholder="가입 시 등록한 이메일을 입력하세요." size="large" class="input-with-button">
                  <template #append>
                    <el-button type="primary" @click="sendAuthCode">
                      {{ isCodeSent ? '재전송' : '인증번호 전송' }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>

              <!-- 인증번호 입력 필드는 전송 후에만 활성화되며 표시됩니다. -->
              <el-form-item label="인증번호">
                <el-input
                    v-model="authCode"
                    :disabled="!isCodeSent"
                    placeholder="수신된 인증번호를 입력하세요."
                    size="large"
                />
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <el-button type="primary" class="action-button" :disabled="!isCodeSent" @click="findId">
          확인
        </el-button>
      </div>

      <!-- 2단계: 아이디 찾기 결과 (복사하기 기능 추가) -->
      <div v-else class="result-section">
        <p class="result-description">고객님의 아이디 찾기 결과입니다.</p>
        <div class="result-box">
          <span>{{ maskedFoundUserId }}</span>
          <el-button
              :icon="DocumentCopy"
              type="info"
              text
              circle
              class="copy-button"
              @click="copyToClipboard(fullFoundUserId)"
          />
        </div>
        <div class="result-actions">
          <el-button type="default" class="action-button-secondary">비밀번호 재설정</el-button>
          <el-button type="primary" class="action-button-primary">로그인 하기</el-button>
        </div>
      </div>

      <!-- 하단 공통 링크 -->
      <div class="navigation-links">
        <el-button type="info" link>로그인</el-button>
        <el-divider direction="vertical" />
        <el-button type="info" link>비밀번호 찾기</el-button>
      </div>
    </el-card>
  </div>
</template>

<style>
/* Popover 내부 스타일은 scoped로 적용되지 않으므로 일반 style 태그를 사용합니다. */
.popover-content p { margin-top: 0; margin-bottom: 10px; font-size: 14px; color: #303133; }
.popover-content ul { padding-left: 20px; margin: 0; font-size: 13px; color: #606266; }
.popover-content li { margin-bottom: 5px; }
.popover-content li:last-child { margin-bottom: 0; }
</style>

<style scoped>
/* 전체 페이지 레이아웃 */
.find-id-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 100px);
}
/* 아이디 찾기 카드 */
.find-id-card { width: 450px; padding: 16px; box-sizing: border-box; }
.title { font-size: 26px; color: #1f2d3d; text-align: center; margin: 0 0 10px; }
.description { font-size: 15px; color: #8492a6; text-align: center; margin-bottom: 30px; }
/* 탭 스타일 */
.find-tabs { margin-bottom: 20px; }
.find-tabs :deep(.el-tabs__nav-wrap::after) { height: 1px; }
.find-tabs :deep(.el-tabs__item) { font-size: 16px; padding: 0 20px; }
/* 폼 스타일 */
.find-form { margin-top: 15px; }
.find-form .el-form-item { margin-bottom: 18px; }
.find-form :deep(.el-form-item__label) { font-size: 14px; color: #475669; padding-bottom: 6px; line-height: normal; width: 100%; }
.label-with-help { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.help-icon-button { font-size: 16px; }
.help-icon-button:focus { outline: 0; }
.input-with-button :deep(.el-input-group__append) { background-color: transparent; padding: 0; }
.input-with-button :deep(.el-input-group__append .el-button) { border-radius: 0 var(--el-input-border-radius) var(--el-input-border-radius) 0; margin: -1px; }
/* 결과 표시 섹션 */
.result-section { text-align: center; padding: 20px 0; }
.result-description { font-size: 15px; color: #475669; margin-bottom: 20px; }
.result-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f9fafb;
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  padding: 15px 20px;
  margin-bottom: 30px;
}
.result-box span { font-size: 20px; font-weight: 600; color: #303133; letter-spacing: 1px; }
.copy-button { font-size: 20px; }
.result-actions { display: flex; gap: 10px; }
.result-actions .el-button { flex-grow: 1; height: 48px; font-weight: bold; font-size: 16px; }
/* 공통 액션 버튼 */
.action-button { width: 100%; height: 48px; font-weight: bold; font-size: 16px; margin-top: 10px; }
/* 하단 네비게이션 링크 */
.navigation-links { margin-top: 25px; text-align: center; }
</style>