<script setup>
import { ref } from 'vue';

const form = ref({
  id: '',
  password: '',
});

</script>

<template>
  <el-card shadow="never">
    <el-form :model="form" ref="form" label-width="0">
      <el-form-item>
        <h2>LOGIN</h2>
      </el-form-item>
      <el-form-item>
        <el-input
            v-model="form.id"
            placeholder="아이디"
            prefix-icon="el-icon-user"
            clearable
            size="large"
            class="el-input--round"
        />
      </el-form-item>
      <el-form-item>
        <el-input
            v-model="form.password"
            type="password"
            placeholder="비밀번호"
            prefix-icon="el-icon-lock"
            clearable
            size="large"
            class="el-input--round"
        />
      </el-form-item>
      <el-form-item>
        <el-button
            type="primary"
            @click="login"
            block
            size="large"
            class="el-button--round el-button--custom"
            style="width: 100%;"
        >
          로그인
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>

</template>

<style scoped>

</style>