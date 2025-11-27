<template>
  <div class="home-container">
    <h1>欢迎来到桌游平台</h1>
    <p>这是游戏大厅页面，您可以在这里创建或加入游戏房间。</p>
    <div class="action-buttons">
      <el-button type="primary" @click="navigateToLogin">登录</el-button>
      <el-button @click="navigateToRegister">注册</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

// 导航到登录页面
const navigateToLogin = () => {
  router.push('/login');
};

// 导航到注册页面
const navigateToRegister = () => {
  router.push('/register');
};
</script>

<style scoped>
.home-container {
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

.action-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
}
</style>
