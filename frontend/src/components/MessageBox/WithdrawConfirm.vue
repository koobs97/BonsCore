<template>
  <div class="withdraw-container">
    <h3>회원탈퇴 안내</h3>
    <p class="subtitle">정말로 탈퇴하시겠습니까?</p>

    <div class="warning-box">
      <!-- 아이콘을 담을 별도의 div 생성 -->
      <div class="icon-wrapper">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"></path>
        </svg>
      </div>
      <!-- 텍스트 컨텐츠를 담을 별도의 div 생성 -->
      <div class="content-wrapper">
        <p>회원탈퇴 시 아래의 정보가 영구적으로 삭제되며, 복구할 수 없습니다.</p>
        <ul>
          <li>- 계정 정보 및 모든 개인 데이터</li>
          <li>- 작성한 게시물, 댓글 등 모든 활동 기록</li>
        </ul>
      </div>
    </div>

    <p class="final-confirm-text">
      회원탈퇴를 계속 진행하시려면,<br/>아래 입력칸에 '<b>회원탈퇴</b>'를 정확히 입력해주세요.
    </p>

    <el-input
        v-model="confirmText"
        @input="onInput"
        placeholder="'회원탈퇴'를 입력하세요"
        size="large"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const confirmText = ref('');

const emit = defineEmits(['update:text']);

// 이벤트가 발생할 때마다 부모에게 값을 전달하는 함수
const onInput = (value: string) => {
  emit('update:text', value);
}
</script>

<style scoped>
/* ----------------------------------------- */
/* 1. 컴포넌트 테마 변수 정의 (라이트/다크)    */
/* ----------------------------------------- */

.withdraw-container {
  /* 라이트 모드 기본 색상 변수 */
  --withdraw-title-color: var(--el-text-color-primary, #2c3e50);
  --withdraw-subtitle-color: var(--el-text-color-secondary, #8492a6);
  --withdraw-text-color: var(--el-text-color-regular, #5a687c);

  --withdraw-warning-bg: var(--el-color-danger-light-9, #fef0f0);
  --withdraw-warning-border: var(--el-color-danger, #f56c6c);
  --withdraw-warning-icon-color: var(--el-color-danger, #f56c6c);
  --withdraw-warning-title-color: var(--el-color-danger-dark-2, #d33a3a);
  --withdraw-warning-list-color: var(--el-color-danger-light-3, #e57474);
}

/* html에 .dark 클래스가 있을 때 적용될 색상 변수 */
/* :deep()을 사용하여 상위 html 태그를 참조합니다. */
:deep(html.dark) .withdraw-container {
  --withdraw-title-color: #EAECEF;
  --withdraw-subtitle-color: #a8abb2;
  --withdraw-text-color: #c0c4cc;

  --withdraw-warning-bg: rgba(245, 108, 108, 0.1); /* 어두운 배경에 맞는 붉은 톤 */
  --withdraw-warning-border: #f56c6c;
  --withdraw-warning-icon-color: #f56c6c;
  --withdraw-warning-title-color: #f89898;
  --withdraw-warning-list-color: #fab6b6;
}


/* ----------------------------------------- */
/* 2. 변수를 사용한 실제 스타일 적용           */
/* ----------------------------------------- */

.withdraw-container {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  text-align: center;
  padding: 0;
}

/* 메인 타이틀 */
h3 {
  font-size: 24px;
  font-weight: 700;
  color: var(--withdraw-title-color);
  margin-top: 0;
  margin-bottom: 8px;
}

/* 서브 타이틀 */
.subtitle {
  font-size: 16px;
  color: var(--withdraw-subtitle-color);
  margin-bottom: 24px;
}

/* --- 경고 박스 디자인 --- */
.warning-box {
  display: flex;
  align-items: flex-start;
  background-color: var(--withdraw-warning-bg);
  border-left: 5px solid var(--withdraw-warning-border);
  border-radius: 4px;
  padding: 16px;
  margin-bottom: 28px;
  text-align: left;
  transition: background-color 0.3s, border-color 0.3s; /* 부드러운 전환 효과 */
}

/* 아이콘을 감싸는 래퍼 */
.icon-wrapper {
  margin-right: 12px;
  flex-shrink: 0;
  color: var(--withdraw-warning-icon-color);
  transition: color 0.3s;
}

.icon-wrapper svg {
  width: 24px;
  height: 24px;
  position: relative;
  top: -1px;
}

/* 텍스트 컨텐츠를 감싸는 래퍼 */
.content-wrapper {
  flex-grow: 1;
}

.content-wrapper p {
  margin: 0 0 12px 0;
  font-weight: 600;
  color: var(--withdraw-warning-title-color);
  font-size: 15px;
  line-height: 1.5;
  transition: color 0.3s;
}

.content-wrapper ul {
  list-style-type: none;
  padding-left: 0;
  margin-top: 8px;
  margin-bottom: 0;
  color: var(--withdraw-warning-list-color);
  font-size: 14px;
  transition: color 0.3s;
}

.content-wrapper ul li {
  margin-bottom: 2px;
  line-height: 1.6;
}

/* 최종 확인 텍스트 */
.final-confirm-text {
  font-size: 15px;
  color: var(--withdraw-text-color);
  margin-bottom: 12px;
  line-height: 1.6;
  transition: color 0.3s;
}

/* 최종 확인 텍스트의 굵은 글씨 스타일 */
.final-confirm-text b {
  color: var(--withdraw-warning-border);
  font-weight: 700;
}

.el-input {
  margin-top: 0;
}
</style>