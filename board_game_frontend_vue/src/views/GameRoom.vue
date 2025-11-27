<template>
  <div class="game-room-container">
    <h1>游戏房间 #{{ roomId }}</h1>
    <p>这是游戏房间页面，您可以在这里进行游戏。</p>
    <el-button @click="navigateToHome">返回大厅</el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { defineProps } from 'vue';

const router = useRouter();

// 接收路由参数
const props = defineProps({
  roomId: {
    type: String,
    required: true
  }
});

// 导航回首页
const navigateToHome = () => {
  router.push('/');
};
</script>

<style scoped>
.game-room-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
  text-align: center;
}

h1 {
  color: #333;
  margin-bottom: 1rem;
}

p {
  color: #666;
  margin-bottom: 2rem;
}
</style>
