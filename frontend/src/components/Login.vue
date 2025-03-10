<script setup>
import { Hide } from "@element-plus/icons-vue";
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
          placeholder="사용자ID"
          style="
            height: 40px;
            font-size: 15px;
            margin-bottom: 8px;"/>
      <el-input
          placeholder="비밀번호"
          type="password"
          style="
            height: 40px;
            font-size: 15px;
            margin-bottom: 16px;">
        <template #append>
          <el-button :icon="Hide" />
        </template>
      </el-input>
      <el-button
          type="primary"
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
  <div style="text-align: right; margin-top: 12px;">
    <img style="width: 350px; height: 51px;" src="../assets/images/logo.png" alt="Logo"  />
  </div>
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