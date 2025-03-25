<script setup lang="ts">
import {Hide, View} from "@element-plus/icons-vue";
import {computed, reactive, ref} from 'vue'
import {Api} from "@/api/axiosInstance";
import {ApiUrls} from "@/api/apiUrls";
import JSEncrypt from 'jsencrypt';

/**
 * 패스워드는 ref(반응형 변수)로 새로고침하면 사라짐
 * state, pinia에 저장 시 XSS 공격에 취약
 * @type {Ref<UnwrapRef<string>, UnwrapRef<string> | string>}
 */
const userId = ref("");
const password = ref("");

// reactive 정의
const state = reactive({
  isVisible: false,
})

// 패스워드 아이콘 변경
const passwdIcon = computed(() => (state.isVisible ? View : Hide));
const passwdType = computed(() => (state.isVisible ? "text" : "password"));

// 패스워드 입력 모드 전환
const togglePassword = () => {
  state.isVisible = !state.isVisible;
}

/**
 * 패스워드 공개키 받아오기
 * @param password
 */
const encryptPassword = async (password: string): Promise<string> => {
  const { data: publicKey } = await Api.get(ApiUrls.GET_PUBLIC_KEY); // 서버에서 공개 키 받아오기
  const encryptor = new JSEncrypt();
  encryptor.setPublicKey(publicKey);
  return encryptor.encrypt(password) || ''; // RSA 암호화
};

/**
 * 로그인 버튼 클릭 이벤트
 */
const onClickLogin = async () => {

  // 서버에서 공개키 get
  const encryptedPassword = await encryptPassword(password.value);

  const params = {
    userId : userId.value,
    password : encryptedPassword
  }

  await Api.post(ApiUrls.LOGIN, params, true);
}
</script>

<template>
  <el-card
      shadow="never"
      style="width: 350px; height: 370px;">
    <h2 style="font-size: 30px; color: #0D1B2A">
      K.BONS
    </h2>

    <el-form
        style="margin-top: 45px;">
      <div style="text-align: left;">
        <el-checkbox label="로그인정보 기억하기" checked />
      </div>
      <el-input
          v-model="userId"
          placeholder="사용자ID"
          style="
            height: 40px;
            font-size: 15px;
            margin-bottom: 8px;"/>
      <el-input
          v-model="password"
          placeholder="비밀번호"
          :type="passwdType"
          style="
            height: 40px;
            font-size: 15px;
            margin-bottom: 16px;">
        <template #append>
          <el-button @click="togglePassword">
            <el-icon><component :is="passwdIcon" /></el-icon>
          </el-button>
        </template>
      </el-input>
      <el-button
          type="primary"
          @click="onClickLogin"
          style="
            width: 100%;
            height: 45px;
            font-weight: bold;
            font-size: 16px;">
        로그인
      </el-button>
    </el-form>

    <div style="margin-top: 12px;">
      <el-button type="info" link >아이디 찾기</el-button>
      <el-button type="info" link >비밀번호 찾기</el-button>
    </div>
  </el-card>

  <el-card
      shadow="never"
      style="margin-top: 12px; height: 70px;">
    <el-button
          type="primary"
          link
          style="
            font-weight: bold;"
    >
      회원가입
    </el-button>
  </el-card>

  <div style="
    position: fixed;
    bottom: 2%;
    left: 0;
    right: 0;
    font-size: 12px;
    text-align: center;">
    <strong>Copyright</strong> KooBonSang &copy; 2025 All Rights Reserved.
  </div>

</template>

<style scoped>

</style>