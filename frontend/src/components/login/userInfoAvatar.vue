<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue';
import { userStore, userState } from '@/store/userStore';
import { Female, Male } from "@element-plus/icons-vue";

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



</script>

<template>
  <el-card class="custom-el-card" shadow="never" style="height: 90px; width: 230px; margin: 0 0 4px 4px; position: relative;">
    <template #header>
      <div style="height: 60px; display: flex; align-items: center;">
        <div style="display: flex; justify-content: flex-start; padding: 0 0 0 8px;">


          <el-popover
              :width="300"
              :virtual-ref="buttonRef"
              trigger="click"
              placement="right-start"
              popper-style="box-shadow: rgb(14 18 22 / 35%) 0px 10px 38px -10px, rgb(14 18 22 / 20%) 0px 10px 20px -15px; padding: 12px;"
          >
            <template #reference>
              <el-avatar shape="square" :size="40">
                <el-text style="font-size: 12px; color: white">
                  {{ state.User.userName.slice(-2) }}
                </el-text>
              </el-avatar>
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
                      <el-text style="font-weight: bold; font-size: 11px;">
                        {{ state.User.email }}
                      </el-text>
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
                    <el-button icon="Promotion" style="font-size: 12px; width: 90px; height: 30px; margin: 12px 0 0 2px;">로그아웃</el-button>
                  </div>
                </div>
              </template>
            </el-card>
          </el-popover>

        </div>
      </div>
    </template>
    <div style="text-align: left;">
      <el-tag style="margin-left: 8px;">로그인일시</el-tag>
      <el-tag style="margin-left: 4px;">{{ state.User.loginTime }}</el-tag>
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