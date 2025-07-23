<template>
  <el-container class="main-container">
    <!-- 1. 상단 헤더 -->
    <el-header class="main-header">
      <div class="header-content">
        <!-- 로고 -->
        <div class="logo">
          <el-icon><Stopwatch /></el-icon>
          <span>웨이팅 레이더</span>
        </div>

        <!-- 중앙 검색창 -->
        <div class="search-bar">
          <el-input
              v-model="searchInput"
              placeholder="맛집, 지역을 검색하여 웨이팅 정보를 확인하세요"
              :prefix-icon="Search"
              clearable
          />
        </div>

        <!-- 우측 메뉴 -->
        <el-menu mode="horizontal" :ellipsis="false" class="right-menu">
          <el-menu-item index="1">
            <el-icon><Location /></el-icon>
            내 주변
          </el-menu-item>
          <el-menu-item index="2">
            <el-badge is-dot class="item">
              <el-icon><Bell /></el-icon>
            </el-badge>
          </el-menu-item>
          <el-sub-menu index="3">
            <template #title>
              <el-avatar :icon="UserFilled" :size="30" />
            </template>
            <el-menu-item index="3-1">내 정보</el-menu-item>
            <el-menu-item index="3-2">내 제보 관리</el-menu-item>
            <el-menu-item index="3-3">저장한 맛집</el-menu-item>
            <el-menu-item index="3-4">로그아웃</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </div>
    </el-header>

    <!-- 2. 메인 콘텐츠 -->
    <el-main class="main-content">
      <!-- 섹션 1: 실시간 인기 맛집 -->
      <section class="content-section">
        <h2 class="section-title">
          <el-icon><HotWater /></el-icon> 실시간 인기 맛집
        </h2>
        <p class="section-description">지금 가장 많은 사람들이 확인하는 곳이에요!</p>
        <el-row :gutter="20">
          <el-col :span="8" v-for="item in hotRestaurants" :key="item.id">
            <el-card shadow="hover" class="restaurant-card">
              <img :src="item.imageUrl" class="restaurant-image"/>
              <div class="card-body">
                <div class="card-header">
                  <h3>{{ item.name }}</h3>
                  <el-tag size="small">{{ item.category }}</el-tag>
                </div>
                <div class="waiting-info">
                  <el-tag :type="item.status === 'available' ? 'success' : 'warning'" effect="dark">
                    {{ item.status === 'available' ? '바로 입장' : `현재 ${item.waitingTeams}팀 대기` }}
                  </el-tag>
                  <span v-if="item.status !== 'available'">예상 대기 시간: 약 {{ item.estimatedTime }}분</span>
                </div>
                <div class="update-info">
                  <el-icon><Refresh /></el-icon> {{ item.lastUpdate }}분 전 제보
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </section>

      <!-- 섹션 2: 시간대별 웨이팅 예측 -->
      <section class="content-section">
        <h2 class="section-title">
          <el-icon><DataLine /></el-icon> 시간대별 웨이팅 예측
        </h2>
        <p class="section-description">금요일 저녁 7시, 이곳은 붐빌 확률이 높아요.</p>
        <el-row :gutter="20">
          <el-col :span="8" v-for="item in predictedRestaurants" :key="item.id">
            <el-card shadow="hover" class="restaurant-card">
              <div class="prediction-body">
                <div class="card-header">
                  <h3>{{ item.name }}</h3>
                  <el-tag size="small" type="info">{{ item.category }}</el-tag>
                </div>
                <div class="prediction-info">
                  <p><strong>{{ item.time }}</strong> 평균 <strong>{{ item.avgWaitTime }}분</strong> 대기</p>
                  <span>(최대 {{ item.maxWaitTime }}분)</span>
                </div>
                <div class="tip-info">
                  <el-icon><Memo /></el-icon> "주차 팁: 가게 앞은 불가, 공영 주차장 이용 필수"
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </section>

      <!-- 섹션 3: 최신 웨이팅 꿀팁 게시판 -->
      <section class="content-section">
        <h2 class="section-title">
          <el-icon><Comment /></el-icon> 최신 웨이팅 꿀팁
        </h2>
        <p class="section-description">기다리는 시간을 줄여주는 사용자들의 생생한 정보!</p>
        <el-table :data="waitingTips" stripe style="width: 100%">
          <el-table-column prop="restaurant" label="맛집 이름" width="180" />
          <el-table-column prop="tip" label="꿀팁 내용" />
          <el-table-column prop="author" label="작성자" width="120" />
          <el-table-column prop="likes" label="도움됐어요" width="120">
            <template #default="scope">
              <el-button type="primary" link :icon="Pointer" size="small">{{ scope.row.likes }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

    </el-main>
  </el-container>
</template>

<script setup>
import { ref } from 'vue';
import {
  Search,
  UserFilled,
  Bell,
  Location,
  Stopwatch,
  HotWater,
  Refresh,
  DataLine,
  Memo,
  Comment,
  Pointer
} from '@element-plus/icons-vue';

// --- 임시 데이터 (하드코딩) ---
// 나중에는 이 부분들이 API 호출로 대체됩니다.

const searchInput = ref('');

// 실시간 인기 맛집 데이터
const hotRestaurants = ref([
  { id: 1, name: '오복수산', category: '일식', imageUrl: 'https://via.placeholder.com/400x250/FFC107/000000?text=Obok+Susan', status: 'waiting', waitingTeams: 12, estimatedTime: 45, lastUpdate: 3 },
  { id: 2, name: '런던 베이글 뮤지엄', category: '베이커리', imageUrl: 'https://via.placeholder.com/400x250/8BC34A/FFFFFF?text=London+Bagel', status: 'waiting', waitingTeams: 32, estimatedTime: 90, lastUpdate: 5 },
  { id: 3, name: '카츠바이콘반', category: '돈카츠', imageUrl: 'https://via.placeholder.com/400x250/E91E63/FFFFFF?text=Katsu', status: 'available', waitingTeams: 0, estimatedTime: 0, lastUpdate: 10 },
]);

// 시간대별 예측 데이터
const predictedRestaurants = ref([
  { id: 1, name: '고든램지 버거', category: '버거', time: '토요일 13시', avgWaitTime: 65, maxWaitTime: 120 },
  { id: 2, name: '다운타우너 안국', category: '버거', time: '금요일 19시', avgWaitTime: 30, maxWaitTime: 50 },
  { id: 3, name: '카페 노티드', category: '디저트', time: '일요일 15시', avgWaitTime: 25, maxWaitTime: 40 },
]);

// 웨이팅 꿀팁 데이터
const waitingTips = ref([
  { id: 1, restaurant: '런던 베이글 뮤지엄', tip: '테이블링 원격 줄서기보다 현장 대기가 더 빠를 때도 있어요!', author: '베이글러버', likes: 15 },
  { id: 2, restaurant: '카츠바이콘반', tip: '오픈 30분 전에 도착하면 첫 타임에 바로 입장 가능합니다.', author: '미식가', likes: 22 },
  { id: 3, restaurant: '오복수산', tip: '2명이라면 바(bar) 자리를 요청하면 더 빨리 앉을 수 있어요.', author: '해산물킬러', likes: 8 },
]);
</script>

<style scoped>
.main-container {
  background-color: #f4f5f7; /* 전체 배경색 */
}

.main-header {
  background-color: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  position: sticky;
  top: 0;
  z-index: 1000;
  padding: 0 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  max-width: 1200px;
}

.logo {
  display: flex;
  align-items: center;
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
}
.logo .el-icon {
  margin-right: 8px;
}

.search-bar {
  flex-grow: 1;
  max-width: 500px;
  margin: 0 40px;
}

.right-menu {
  border-bottom: none;
}
.right-menu .el-menu-item, .right-menu .el-sub-menu {
  background-color: transparent !important;
}
.right-menu .el-badge {
  display: flex;
  align-items: center;
  height: 100%;
}


.main-content {
  max-width: 1200px;
  margin: 20px auto;
  padding: 20px;
}

.content-section {
  background-color: #ffffff;
  padding: 24px;
  border-radius: 8px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.06);
}

.section-title {
  font-size: 22px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}
.section-title .el-icon {
  margin-right: 8px;
  color: #409EFF;
}

.section-description {
  color: #909399;
  margin-bottom: 20px;
}

.restaurant-card {
  border-radius: 8px;
}
.restaurant-card .card-body {
  padding: 14px;
}
.restaurant-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.restaurant-card .card-header h3 {
  margin: 0;
  font-size: 16px;
}

.restaurant-image {
  width: 100%;
  height: 150px;
  object-fit: cover;
  display: block;
}

.waiting-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  margin-bottom: 10px;
  color: #606266;
}
.waiting-info .el-tag {
  font-weight: bold;
}
.update-info {
  font-size: 12px;
  color: #909399;
  text-align: right;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.update-info .el-icon {
  margin-right: 4px;
}

.prediction-body {
  padding: 14px;
}
.prediction-info {
  font-size: 14px;
  background-color: #f4f5f7;
  padding: 10px;
  border-radius: 4px;
  margin: 10px 0;
}
.prediction-info p { margin: 0; }
.prediction-info span { font-size: 12px; color: #909399; }

.tip-info {
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
}
.tip-info .el-icon {
  margin-right: 5px;
}
</style>