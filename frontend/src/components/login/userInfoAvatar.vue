<script setup lang="ts">
import { computed, reactive, ref, onMounted, h } from 'vue';
import { userStore, userState } from '@/store/userStore';
import {CopyDocument, Delete, Female, Male, Setting, UserFilled} from "@element-plus/icons-vue";
import { ElLoading, ElMessage, ElMessageBox } from "element-plus";
import LogOutConfirm from "@/components/MessageBox/LogOutConfirm.vue";
import UserEditForm from '@/components/login/UserEditForm.vue';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import router from "../../../router";
import WithdrawConfirm from "@/components/MessageBox/WithdrawConfirm.vue";
import FinalConfirm from "@/components/MessageBox/FinalConfirm.vue";

const userStoreObj = userStore();

const buttonRef = ref()

// reactive 정의
const state = reactive({
  User: {
    userId      : '' as string,
    userName    : '' as string,
    email       : '' as string,
    phoneNumber : '' as string,
    birthDate   : '' as string,
    genderCode  : '' as string,
    loginTime   : '' as string,
  } as userState,
})

onMounted(async () => {
  state.User = userStoreObj.getUserInfo;

  const match = state.User.loginTime.match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2})/);
  if (match) {
    const [, date, time] = match;
    state.User.loginTime = `${date} ${time}`;
  } else {
    console.error("유효하지 않은 ISO 타임 포맷입니다");
  }
})

const genderIcon = computed(() => (state.User.genderCode === 'M' ? Male : Female));

const formattedPhoneNumber = computed(() => {
  const raw = state.User.phoneNumber?.replace(/\D/g, '') || ''

  if (raw.startsWith('02') && raw.length === 10) {
    // 서울 번호: 02-XXXX-XXXX
    return raw.replace(/(\d{2})(\d{4})(\d{4})/, '$1-$2-$3')
  } else if (raw.length === 11) {
    // 일반 휴대폰: 010-XXXX-XXXX
    return raw.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3')
  } else if (raw.length === 10) {
    // 10자리 일반 번호: 031-XXX-XXXX
    return raw.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3')
  } else {
    return raw // 포맷 불가능한 경우 그대로 출력
  }
})

// pop over 제어 함수
const custromTrigger = () => {
  buttonRef.value = !buttonRef.value
}

const editDialogVisible = ref(false);

// 정보 수정 버튼 클릭 시 다이얼로그를 여는 함수
const openEditDialog = () => {
  editDialogVisible.value = true;
};

// 정보 수정이 성공적으로 완료되었을 때 실행될 함수
const handleUpdateSuccess = () => {
  // 1. 다이얼로그 닫기
  editDialogVisible.value = false;
  // 2. 스토어의 사용자 정보 최신화 (예시: 다시 불러오기)
  // userStoreObj.fetchUserInfo(); // 스토어에 사용자 정보를 다시 요청하는 액션이 있다면 호출
  // 또는, 받은 데이터로 직접 업데이트 할 수도 있습니다.
  console.log('사용자 정보가 업데이트 되었습니다. 상태를 갱신합니다.');
};

/**
 * 로그아웃
 */
const onClickLogOut = async () => {
  try {
    await ElMessageBox.confirm(
        // message에 커스텀 컴포넌트를 렌더링
        h(LogOutConfirm),
        // title은 컴포넌트가 자체적으로 가지고 있으므로 비워둠
        {
          confirmButtonText: '로그아웃',
          cancelButtonText: '취소',

          // --- 스타일링을 위한 옵션 ---
          customClass: 'logout-confirm-box',
          type: '', // 기본 아이콘 숨기기
          showClose: false, // X 닫기 버튼 숨기기
        }
    );

    // '로그아웃' 버튼을 눌렀을 때 실행될 로직

    await Api.post(ApiUrls.LOGOUT, {}, true);

    const loading = ElLoading.service({
      lock: true,
      text: 'Loading',
      background: 'rgba(0, 0, 0, 0.7)',
    })

    setTimeout(() => {
      // 1. 사용자 정보 삭제
      userStore().delUserInfo();
      sessionStorage.clear();

      // 2. 로그인 페이지로 이동
      router.push("/login");

      // 3. 성공 메시지 표시 및 로딩 종료
      ElMessage.success('성공적으로 로그아웃되었습니다.');
      loading.close();
    }, 1000);

  } catch (error) {
    if (error === 'cancel') {
      console.log('로그아웃이 취소되었습니다.');
      ElMessage.info('로그아웃을 취소했습니다.');
    }
  }
}

/**
 * 회원탈퇴
 */
const onClickWithdraw = async () => {
  let inputText = '';

  try {
    await ElMessageBox.confirm(
        h(WithdrawConfirm, {
          'onUpdate:text': (value) => { inputText = value; }
        }),
        {
          confirmButtonText: '회원탈퇴',
          cancelButtonText: '취소',
          customClass: 'withdraw-confirm-box',
          showClose: false,
          type: '',

          beforeClose: async (action, instance, done) => {
            // '취소' 버튼을 누르면 즉시 닫습니다.
            if (action !== 'confirm') {
              done();
              return;
            }

            // '회원탈퇴' 버튼을 눌렀을 때, 먼저 입력값 검증
            if (inputText !== '회원탈퇴') {
              ElMessage({
                message: '문자를 정확히 입력해주세요.',
                grouping: true,
                type: 'error',
              });
              return;
            }

            // 중복 클릭을 방지
            instance.confirmButtonLoading = true;
            instance.confirmButtonText = '처리 중...';

            try {
              await ElMessageBox.confirm(
                  h(FinalConfirm),
                  {
                    confirmButtonText: '확인',
                    cancelButtonText: '취소',
                    type: '',
                    customClass: 'final-confirm-box'
                  }
              );

              done();

            } catch (err) { } // 사용자가 '취소'를 누른 경우
            finally {
              instance.confirmButtonLoading = false;
              instance.confirmButtonText = '회원탈퇴';
            }
          },
        }
    );

    // 위 로직이 모두 성공적으로 끝나고 done()이 호출되었을 때만
    // 아래의 실제 탈퇴 처리가 시작.
    const loading = ElLoading.service({
      lock: true,
      text: '회원탈퇴 처리 중...',
      background: 'rgba(0, 0, 0, 0.7)',
    });

    await Api.post(ApiUrls.WITHDRAWN, {}, true);

    setTimeout(() => {
      userStore().delUserInfo();
      sessionStorage.clear();
      localStorage.clear();
      router.push("/login");
      ElMessage.success('회원탈퇴가 성공적으로 처리되었습니다.');
      loading.close();
    }, 1000);

  } catch (error) {
    // 사용자가 첫 번째 창에서 '취소'를 누른 경우
    if (error === 'cancel') { }
    else {
      // API 호출 실패 등 다른 에러 처리
      const loading = ElLoading.service();
      if (loading) loading.close();
    }
  }
}

/**
 * 이메일 복사 함수
 * @param email
 */
const copyEmail = (email: string) => {
  navigator.clipboard.writeText(email)
      .then(() => {
        ElMessage.success('이메일이 복사되었습니다.')
      })
      .catch(() => {
        ElMessage.error('복사에 실패했습니다.')
      })
}

</script>

<template>
  <el-card class="custom-el-card" shadow="never" style="width: 230px; box-shadow: 0 4px 12px rgba(108, 92, 231, 0.05);">
    <template #header>
      <div style="height: 60px; display: flex; align-items: center;">
        <div style="display: flex; justify-content: flex-start; padding: 0 0 0 8px;">

          <el-popover
              :width="338"
              :visible="buttonRef"
              placement="right-start"
              persistent
              trigger="manual"
              popper-style="box-shadow: rgb(14 18 22 / 35%) 0px 10px 38px -10px, rgb(14 18 22 / 20%) 0px 10px 20px -15px; padding: 12px;"
          >
            <template #reference>
              <div style="display: inline-flex; align-items: center;">
                <!-- 메인 아바타 -->
                <div style="position: relative; width: 40px; height: 40px;">
                  <el-avatar shape="square" :size="40">
                    <el-icon style="font-size: 24px;">
                      <UserFilled />
                    </el-icon>
                  </el-avatar>

                  <!-- 설정 아이콘 -->
                  <el-icon
                      style="
                        position: absolute;
                        bottom: -4px;
                        right: -4px;
                        background: var(--el-bg-color);
                        border-radius: 50%;
                        font-size: 14px;
                        padding: 2px;
                        box-shadow: 0 0 2px rgba(0,0,0,0.2);
                        cursor: pointer;
                      "
                      @click="custromTrigger"
                  >
                    <Setting />
                  </el-icon>
                </div>

                <!-- 이름/이메일 세로 배치 -->
                <div
                    style="
                      display: flex;
                      flex-direction: column;
                      justify-content: flex-start;
                      margin-left: 8px;
                      user-select: none;
                      min-width: 150px;
                      text-align: left;
                    "
                >
                  <!-- 이름 -->
                  <div style="font-weight: bold; font-size: 14px; user-select: text;">
                    {{ state.User.userName }}
                  </div>

                  <!-- 이메일 -->
                  <div style="font-size: 12px; color: #666; user-select: text; margin-top: 2px;">
                    {{ state.User.email }}
                  </div>
                </div>
              </div>
            </template>

            <el-card shadow="never">
              <template #default>
                <div
                    style="text-align: center;"
                >
                  <div style="margin-bottom: 6px;">
                    <el-avatar
                        :size="85"
                        style="margin: 0 0 0 0; border: 3px solid #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.1);"
                        src='https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                    />
                  </div>
                  <div style="display: flex; align-items: center; justify-content: center; margin-top: 6px; margin-left: 32px;">
                    <el-text tag="mark" style="font-weight: bold; color: var(--el-overlay-color); font-size: 14px;">
                      @{{ state.User.userName }}
                    </el-text>
                    <el-tooltip content="회원탈퇴" placement="right-end" :hide-after="0">
                      <el-button
                          type="danger"
                          :icon="Delete"
                          text
                          bg
                          circle
                          @click.stop="onClickWithdraw"
                          style="width: 24px; height: 24px; margin-left: 8px; outline: none;"
                      />
                    </el-tooltip>
                  </div>
                  <el-descriptions
                      style="margin-top: 12px;"
                      :column="1"
                      size="small"
                      border
                  >
                    <el-descriptions-item>
                      <template #label>
                        <div>
                          ID
                        </div>
                      </template>
                      <el-text style="font-weight: bold; font-size: 11px;">
                        {{ state.User.userId }}
                      </el-text>
                    </el-descriptions-item>

                    <el-descriptions-item>
                      <template #label>
                        <div>
                          유저명
                        </div>
                      </template>
                      <el-text style="font-weight: bold; font-size: 11px;">
                        {{ state.User.userName }}
                      </el-text>
                    </el-descriptions-item>

                    <el-descriptions-item>
                      <template #label>
                        <div class="cell-item">
                          이메일
                        </div>
                      </template>
                      <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
                        <el-text style="font-weight: bold; font-size: 11px;">
                          {{ state.User.email }}
                        </el-text>
                        <el-tag type="success" style="width: 24px;" @click="copyEmail(state.User.email)">
                          <el-icon><CopyDocument /></el-icon>
                        </el-tag>
                      </div>
                    </el-descriptions-item>

                    <el-descriptions-item>
                      <template #label>
                        <div class="cell-item">
                          전화번호
                        </div>
                      </template>
                      <el-text style="font-weight: bold; font-size: 11px;">
                        {{ formattedPhoneNumber }}
                      </el-text>
                    </el-descriptions-item>

                    <el-descriptions-item>
                      <template #label>
                        <div class="cell-item">
                          성별
                        </div>
                      </template>
                      <el-tag style="width: 24px;">
                        <el-icon><genderIcon /></el-icon>
                      </el-tag>
                    </el-descriptions-item>

                  </el-descriptions>
                  <div>
                    <el-button
                        icon="EditPen"
                        style="font-size: 12px; width: 90px; height: 30px; margin: 12px 2px 0 0;"
                        @click="openEditDialog"
                    >
                      정보수정
                    </el-button>
                    <el-button
                        icon="Promotion"
                        style="font-size: 12px; width: 90px; height: 30px; margin: 12px 2px 0 0;"
                        @click="onClickLogOut"
                    >로그아웃</el-button>
                  </div>
                </div>
              </template>
            </el-card>
          </el-popover>

        </div>
      </div>
    </template>
    <div style="text-align: left; margin-top: 2px; height: 30px;">
      <el-tag type="info" effect="light" style="margin-left: 4px; width: 82px;">로그인일시</el-tag>
      <el-tag type="info" effect="light" style="margin-left: 4px; width: 136px;">{{ state.User.loginTime }}</el-tag>
    </div>
    <UserEditForm
        v-model:visible="editDialogVisible"
        :user-data="state.User"
        @update-success="handleUpdateSuccess"
    />
  </el-card>
</template>

<style scoped>
.el-card {
  --el-card-border-color: var(--el-border-color-light);
  --el-card-border-radius: 4px;
  --el-card-padding: 12px;
  --el-card-bg-color: var(--el-fill-color-blank);
  background-color: var(--el-card-bg-color);
  border: 1px solid var(--el-card-border-color);
  border-radius: var(--el-card-border-radius);
  color: var(--el-text-color-primary);
  overflow: hidden;
  transition: var(--el-transition-duration);
}

</style>
<style>
.withdraw-confirm-box.el-message-box {
  width: 570px !important;
  padding: 20px;
  border-radius: 4px;
}

/* 커스텀 컴포넌트에 자체 헤더가 있으므로 기본 헤더는 숨김 */
.withdraw-confirm-box .el-message-box__header {
  display: none;
}

/* 컨텐츠 영역의 불필요한 패딩 제거 */
.withdraw-confirm-box .el-message-box__content {
  padding: 0;
}
</style>