<script setup lang="ts">
/**
 * ========================================
 * 파일명   : OAuthRedirect.vue
 * ----------------------------------------
 * 설명     : OAuth 인증 리다이렉트 페이지
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-11-07
 * ========================================
 */
import { onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { userState, userStore } from '@/store/userStore';

const route = useRoute();
const router = useRouter();

onMounted(async () => {
  const accessToken = route.query.token as string;

  if (accessToken) {
    sessionStorage.setItem('accessToken', accessToken);

    try {
      // 이제 이 API 호출이 성공할 것입니다.
      // ApiUrls.GET_USER가 /api/users/me를 가리키고 있어야 합니다.
      const user = await Api.post(ApiUrls.GET_USER, {}, true);
      const userInfo = user.data as userState;
      sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
      userStore().setUserInfo(userInfo);

      // 성공 시 메인 페이지로 이동
      await router.push("/");

    } catch (error) {
      console.error("사용자 정보 가져오기 실패:", error);

      // [수정] 실패 시 /login 대신 에러 페이지나 다른 경로로 보냅니다.
      // 또는 사용자에게 에러 메시지를 보여주고 아무것도 하지 않을 수 있습니다.
      // alert('로그인 처리 중 오류가 발생했습니다. 다시 시도해주세요.');
      await router.push("/login?error=oauth_failed"); // 쿼리 파라미터로 원인을 전달
    }
  } else {
    console.error("토큰이 없습니다.");
    // 토큰이 없는 경우는 명백한 오류이므로 로그인 페이지로 보냅니다.
    await router.push("/login?error=no_token");
  }
});
</script>
<template>
  <div>
    <p>로그인 처리 중입니다...</p>
  </div>
</template>