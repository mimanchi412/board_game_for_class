<template>
  <div class="home-container">
    <h1>欢迎来到桌游平台</h1>
    <p>这是游戏大厅页面，您可以在这里创建或加入游戏房间。</p>
    
    <!-- 根据登录状态显示不同内容 -->
    <div v-if="userStore.isAuthenticated" class="game-actions">
      <div class="room-section">
        <h2>游戏房间</h2>
        <div class="room-buttons">
          <el-button type="primary" @click="quickStart">快速开始</el-button>
          <el-button @click="createRoom">创建房间</el-button>
          <el-button @click="showJoinDialog = true">加入房间</el-button>
          <el-button type="danger" @click="logout">退出登录</el-button>
        </div>
        
        <!-- 加入房间对话框 -->
        <el-dialog
          v-model="showJoinDialog"
          title="加入房间"
          width="30%"
        >
          <el-input
            v-model="joinRoomCode"
            placeholder="请输入6位房间码"
            maxlength="6"
            show-word-limit
            oninput="value=value.replace(/[^\d]/g, '')"
            clearable
          ></el-input>
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="showJoinDialog = false">取消</el-button>
              <el-button type="primary" @click="joinRoom">加入</el-button>
            </div>
          </template>
        </el-dialog>
      </div>
    </div>
    
    <!-- 未登录状态 -->
    <div v-else class="auth-actions">
      <div class="action-buttons">
        <el-button type="primary" @click="navigateToLogin">登录</el-button>
        <el-button @click="navigateToRegister">注册</el-button>
      </div>
      <p class="auth-desc">请先登录，体验完整的游戏功能</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';
import { ElMessage } from 'element-plus';
import axios from '../utils/axios';

const router = useRouter();
const userStore = useUserStore();

// 导航到登录页面
const navigateToLogin = () => {
  router.push('/login');
};

// 导航到注册页面
const navigateToRegister = () => {
  router.push('/register');
};

// 加入房间对话框控制
const showJoinDialog = ref(false);
const joinRoomCode = ref('');

// 快速开始游戏
const quickStart = async () => {
  try {
    const result = await axios.post('/api/game/rooms/random/join');
    // 处理随机匹配响应
    if (result.matched && result.room) {
      router.push(`/game-room/${result.room.roomId}`);
      ElMessage.success('匹配成功，正在进入房间...');
    } else {
      ElMessage.info(result.message || '匹配中，等待其他玩家...');
    }
  } catch (error) {
    console.error('快速开始失败：', error);
    ElMessage.error('匹配失败：' + (error.message || '网络错误'));
  }
};

// 创建房间
const createRoom = async () => {
  try {
    const result = await axios.post('/api/game/rooms/custom', {
      roomName: '' // 发送空房间名，后端会处理默认值
    });
    // 正确获取房间ID（从Result对象的data字段中获取）
    router.push(`/game-room/${result.data.roomId}`);
    ElMessage.success(`房间创建成功，房间码：${result.data.roomCode}`);
  } catch (error) {
    console.error('创建房间失败：', error);
    ElMessage.error('创建房间失败：' + (error.message || '网络错误'));
  }
};

// 加入房间
const joinRoom = async () => {
  if (!joinRoomCode.value.trim() || joinRoomCode.value.trim().length !== 6) {
    ElMessage.warning('请输入有效的6位房间码');
    return;
  }
  
  try {
    console.log('准备加入房间，房间码：', joinRoomCode.value.trim());
    console.log('当前用户token：', userStore.token);
    const result = await axios.post('/api/game/rooms/custom/join', {
      roomCode: joinRoomCode.value.trim()
    });
    console.log('加入房间成功，响应：', result);
    // 正确获取房间ID（从Result对象的data字段中获取）
    router.push(`/game-room/${result.data.roomId}`);
    ElMessage.success('加入房间成功');
    showJoinDialog.value = false;
  } catch (error) {
    console.error('加入房间失败：', error);
    ElMessage.error('加入房间失败：' + (error.message || '网络错误'));
  }
};

// 退出登录
const logout = async () => {
  try {
    await userStore.logout();
    router.push('/login');
    ElMessage.success('退出登录成功');
  } catch (error) {
    ElMessage.error('退出登录失败');
  }
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

/* 认证按钮样式 */
.auth-actions {
  margin-top: 2rem;
}

.action-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-bottom: 1rem;
}

.auth-desc {
  font-size: 0.9rem;
  color: #999;
}

/* 游戏操作样式 */
.game-actions {
  margin-top: 2rem;
}

.room-section {
  background-color: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.room-section h2 {
  margin-bottom: 1.5rem;
  color: #333;
}

.room-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-bottom: 2rem;
}

.quick-start {
  margin: 2rem 0;
  padding: 1.5rem;
  background-color: #f0f9eb;
  border-radius: 8px;
}

.quick-start h3 {
  margin-bottom: 1rem;
  color: #52c41a;
}

.quick-desc {
  margin-top: 0.5rem;
  font-size: 0.9rem;
  color: #8c8c8c;
}

.logout-section {
  margin-top: 2rem;
}
</style>
