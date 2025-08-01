<script setup lang="ts">
import { computed, reactive, ref, onMounted, h } from 'vue';
import { userStore, userState } from '@/store/userStore';
import { CopyDocument, Female, Male, Setting, UserFilled } from "@element-plus/icons-vue";
import { ElLoading, ElMessage, ElMessageBox } from "element-plus";
import LogOutConfirm from "@/components/MessageBox/LogOutConfirm.vue";
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import router from "../../../router";

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

onMounted(() => {
  state.User = userStoreObj.getUserInfo;
  console.log(state.User)

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
          customClass: 'logout-confirm-box', // CSS에서 사용할 클래스
          type: '', // 기본 아이콘 숨기기
          showClose: false, // X 닫기 버튼 숨기기
        }
    );

    // '로그아웃' 버튼을 눌렀을 때 실행될 로직

    await Api.post(ApiUrls.LOGOUT, {}, true);

    ElLoading.service({
      lock: true,
      text: 'Loading',
      background: 'rgba(0, 0, 0, 0.7)',
    })
    ElMessage.success('성공적으로 로그아웃되었습니다.');

    setTimeout(()=>{
      userStore().delUserInfo();
      sessionStorage.clear();
      router.push("/login");
    }, 1000);

  } catch (error) {
    if (error === 'cancel') {
      console.log('로그아웃이 취소되었습니다.');
      ElMessage.info('로그아웃을 취소했습니다.');
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
  <el-card class="custom-el-card" shadow="never" style="width: 230px;">
    <template #header>
      <div style="height: 60px; display: flex; align-items: center;">
        <div style="display: flex; justify-content: flex-start; padding: 0 0 0 8px;">


          <el-popover
              :width="300"
              v-model:visible="buttonRef"
              trigger="manual"
              placement="right-start"
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
                        background: white;
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
                        style="margin: 0 0 0 0;"
                        src='https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
                    />
                  </div>
                  <div style="margin: 0 0 0 0;">
                    <el-text tag="mark" style="font-weight: bold;">@{{ state.User.userName }}</el-text>
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
                    <el-button icon="EditPen" style="font-size: 12px; width: 90px; height: 30px; margin: 12px 2px 0 0;">정보수정</el-button>
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
      <el-tag type="info" effect="Light" style="margin-left: 4px; width: 82px;">로그인일시</el-tag>
      <el-tag type="info" effect="Light" style="margin-left: 4px; width: 136px;">{{ state.User.loginTime }}</el-tag>
    </div>
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