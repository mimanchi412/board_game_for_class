<template>
  <div class="home-container">
    <!-- 顶部导航栏 -->
    <nav class="navbar">
      <div class="nav-content">
        <div class="nav-brand">
          <div class="logo-icon">
            <el-icon :size="24" color="#fff"><Trophy /></el-icon>
          </div>
          <span class="brand-text">桌游对战平台</span>
        </div>
        
        <div class="nav-right">
          <template v-if="userStore.isAuthenticated">
              <el-dropdown trigger="click" @command="handleCommand">
              <div class="user-profile-trigger">
                <el-avatar :size="36" class="nav-avatar" :src="avatarUrl">
                  {{ (userStore.userInfo?.username || 'U')[0].toUpperCase() }}
                </el-avatar>
                <span class="nav-username">{{ userStore.userInfo?.username || '玩家' }}</span>
                <el-icon class="el-icon--right"><CaretBottom /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile" :icon="Edit">修改资料</el-dropdown-item>
                  <el-dropdown-item command="logout" divided :icon="SwitchButton">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <div class="auth-buttons">
              <el-button link class="nav-btn" @click="navigateToLogin">登录</el-button>
              <el-button type="primary" round class="nav-btn-primary" @click="navigateToRegister">注册</el-button>
            </div>
          </template>
        </div>
      </div>
    </nav>

    <!-- 主内容滚动区 -->
    <div class="main-scroll-area">
      <!-- Hero Banner -->
      <section class="hero-section">
        <div class="hero-content">
          <h1 class="hero-title">智斗无界 · 乐在其中</h1>
          <p class="hero-subtitle">随时随地，与好友开启一场精彩的桌游对决</p>
          <div class="hero-actions" v-if="userStore.isAuthenticated">
            <el-button type="primary" size="large" round class="hero-cta-btn" @click="quickStart">
              <el-icon><VideoPlay /></el-icon> 立即开始
            </el-button>
          </div>
          <div class="hero-actions" v-else>
            <el-button type="primary" size="large" round class="hero-cta-btn" @click="navigateToLogin">
              立即加入
            </el-button>
          </div>
        </div>
      </section>

      <!-- 功能入口区 -->
      <section class="features-section" v-if="userStore.isAuthenticated">
        <div class="section-container">
          <h2 class="section-title">开始游戏</h2>
          <div class="feature-grid">
            <div class="feature-card primary" @click="quickStart">
              <div class="card-icon"><el-icon><Lightning /></el-icon></div>
              <h3>快速匹配</h3>
              <p>随机匹配对手，即刻开战</p>
              <div class="card-hover-bg"></div>
            </div>
            <div class="feature-card success" @click="createRoom">
              <div class="card-icon"><el-icon><Plus /></el-icon></div>
              <h3>创建房间</h3>
              <p>建立专属房间，邀请好友</p>
              <div class="card-hover-bg"></div>
            </div>
            <div class="feature-card warning" @click="showJoinDialog = true">
              <div class="card-icon"><el-icon><Connection /></el-icon></div>
              <h3>加入房间</h3>
              <p>输入房间码，加入对局</p>
              <div class="card-hover-bg"></div>
            </div>
          </div>
        </div>
      </section>

      <!-- 游戏技巧区 -->
      <section class="tips-section">
        <div class="section-container">
          <h2 class="section-title">游戏技巧 & 攻略</h2>
          <div class="tips-grid">
            <div class="tip-card">
              <div class="tip-icon"><el-icon><Medal /></el-icon></div>
              <div class="tip-content">
                <h3>记牌技巧</h3>
                <p>记住关键的大牌（如王、2、A）是否已经打出，可以帮助你判断对手手中的牌力，从而制定最优出牌策略。</p>
              </div>
            </div>
            <div class="tip-card">
              <div class="tip-icon"><el-icon><User /></el-icon></div>
              <div class="tip-content">
                <h3>配合意识</h3>
                <p>作为农民，要学会配合队友。地主的上家要负责顶牌，限制地主出小牌；下家要伺机跑牌，寻找获胜机会。</p>
              </div>
            </div>
            <div class="tip-card">
              <div class="tip-icon"><el-icon><DataAnalysis /></el-icon></div>
              <div class="tip-content">
                <h3>炸弹时机</h3>
                <p>不要轻易使用炸弹，除非能确定回收牌权或者为了翻倍。在关键时刻使用炸弹往往能扭转战局。</p>
              </div>
            </div>
          </div>
        </div>
      </section>
      
      <!-- 页脚 -->
      <footer class="site-footer">
        <p>© 2023 桌游对战平台 - 让快乐更简单</p>
      </footer>
    </div>

    <!-- 加入房间对话框 -->
    <el-dialog
      v-model="showJoinDialog"
      title="加入房间"
      width="360px"
      align-center
      class="custom-dialog"
      :show-close="false"
    >
      <div class="dialog-content">
        <p class="dialog-tip">请输入好友分享的6位房间码</p>
        <el-input
          v-model="joinRoomCode"
          placeholder="000000"
          maxlength="6"
          class="room-code-input"
          oninput="value=value.replace(/[^\d]/g, '')"
        >
          <template #prefix>
            <el-icon><Key /></el-icon>
          </template>
        </el-input>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showJoinDialog = false" round>取消</el-button>
          <el-button type="primary" @click="joinRoom" round :disabled="joinRoomCode.length !== 6">进入房间</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';
import { ElMessage } from 'element-plus';
import { 
  VideoPlay, Plus, Connection, SwitchButton, User, Key, Trophy, 
  DataAnalysis, CaretBottom, Edit, Medal, Lightning 
} from '@element-plus/icons-vue';
import axios from '../utils/axios';

const router = useRouter();
const userStore = useUserStore();

const avatarUrl = ref('');
let avatarObjectUrl = '';

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

// 处理下拉菜单命令
const handleCommand = (command) => {
  if (command === 'logout') {
    logout();
  } else if (command === 'profile') {
    router.push('/profile');
  }
};

// 快速开始游戏
const quickStart = async () => {
  try {
    const result = await axios.post('/api/game/rooms/random/join');
    const data = result?.data || result; // 兼容拦截器返回
    const matched = data?.matched;
    const room = data?.room || data;
    if (matched && room?.roomId) {
      ElMessage.success(data?.message || '匹配成功，正在进入房间...');
      router.push(`/game-room/${room.roomId}`);
      return;
    }
    if (room?.roomId) {
      ElMessage.info(data?.message || '匹配中，先进入房间等待其他玩家');
      router.push(`/game-room/${room.roomId}`);
      return;
    }
    ElMessage.info(data?.message || '匹配中，等待其他玩家...');
  } catch (error) {
    console.error('快速开始失败：', error);
    ElMessage.error('匹配失败：' + (error.message || '网络错误'));
  }
};

// 创建房间
const createRoom = async () => {
  try {
    const res = await axios.post('/api/game/rooms/custom', {
      roomName: '' // 发送空房间名，后端会处理默认值
    });
    const data = res?.data || res;
    if (!data?.roomId) {
      throw new Error('创建房间返回数据异常');
    }
    router.push(`/game-room/${data.roomId}`);
    ElMessage.success(`房间创建成功，房间码：${data.roomCode || '****'}`);
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
    const res = await axios.post('/api/game/rooms/custom/join', {
      roomCode: joinRoomCode.value.trim()
    });
    const data = res?.data || res;
    console.log('加入房间成功，响应：', res);
    // 正确获取房间ID（从Result对象的data字段中获取）
    if (data?.roomId) {
      router.push(`/game-room/${data.roomId}`);
      ElMessage.success('加入房间成功');
    } else {
      throw new Error('加入房间返回数据异常');
    }
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
    if (avatarObjectUrl) {
      URL.revokeObjectURL(avatarObjectUrl);
      avatarObjectUrl = '';
    }
    avatarUrl.value = '';
    router.push('/login');
    ElMessage.success('退出登录成功');
  } catch (error) {
    ElMessage.error('退出登录失败');
  }
};

const fetchAvatar = async () => {
  if (!userStore.isAuthenticated) return;
  try {
    const blob = await axios.get('/api/user/avatar', { responseType: 'blob' });
    if (blob instanceof Blob) {
      if (avatarObjectUrl) URL.revokeObjectURL(avatarObjectUrl);
      avatarObjectUrl = URL.createObjectURL(blob);
      avatarUrl.value = avatarObjectUrl;
    }
  } catch (error) {
    // 头像不存在或请求异常时保持文字头像
    avatarUrl.value = '';
  }
};

onMounted(async () => {
  if (userStore.isAuthenticated && !userStore.userInfo) {
    try {
      await userStore.getUserInfo();
    } catch (error) {
      console.error('获取用户信息失败:', error);
      ElMessage.error('登录已过期，请重新登录');
      router.push('/login');
    }
  }
  await fetchAvatar();
});

onBeforeUnmount(() => {
  if (avatarObjectUrl) {
    URL.revokeObjectURL(avatarObjectUrl);
    avatarObjectUrl = '';
  }
});

</script>

<style scoped>
.home-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 防止双重滚动条 */
}

/* Navbar */
.navbar {
  height: 64px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 100;
}

.nav-content {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-text {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.user-profile-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 20px;
  transition: background 0.3s;
}

.user-profile-trigger:hover {
  background: rgba(0, 0, 0, 0.05);
}

.nav-avatar {
  background: #667eea;
  font-weight: bold;
}

.nav-username {
  font-size: 14px;
  font-weight: bold;
  color: #333;
}

.nav-btn {
  font-size: 16px;
}

/* Main Scroll Area */
.main-scroll-area {
  margin-top: 64px;
  flex: 1;
  overflow-y: auto;
}

/* Hero Section */
.hero-section {
  background: linear-gradient(135deg, #4b6cb7 0%, #182848 100%);
  padding: 80px 20px;
  text-align: center;
  color: white;
}

.hero-title {
  font-size: 48px;
  margin: 0 0 20px;
  font-weight: 800;
  letter-spacing: 2px;
}

.hero-subtitle {
  font-size: 18px;
  opacity: 0.9;
  margin: 0 0 40px;
  font-weight: 300;
}

.hero-cta-btn {
  padding: 20px 40px;
  font-size: 18px;
  font-weight: bold;
}

/* Section Common */
.section-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 20px;
}

.section-title {
  text-align: center;
  font-size: 32px;
  color: #333;
  margin: 0 0 50px;
  font-weight: bold;
}

/* Feature Grid */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
}

.feature-card {
  background: white;
  border-radius: 20px;
  padding: 40px 30px;
  text-align: center;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
}

.feature-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.card-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  margin: 0 auto 20px;
}

.primary .card-icon { background: #ecf5ff; color: #409eff; }
.success .card-icon { background: #f0f9eb; color: #67c23a; }
.warning .card-icon { background: #fdf6ec; color: #e6a23c; }

.feature-card h3 {
  font-size: 20px;
  margin: 0 0 10px;
  color: #333;
}

.feature-card p {
  color: #999;
  margin: 0;
}

/* Tips Section */
.tips-section {
  background: white;
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
}

.tip-card {
  display: flex;
  gap: 20px;
  padding: 30px;
  background: #f8f9fa;
  border-radius: 16px;
  transition: transform 0.3s;
}

.tip-card:hover {
  transform: translateY(-5px);
  background: #fff;
  box-shadow: 0 10px 30px rgba(0,0,0,0.05);
}

.tip-icon {
  font-size: 32px;
  color: #409eff;
}

.tip-content h3 {
  margin: 0 0 10px;
  font-size: 18px;
  color: #333;
}

.tip-content p {
  margin: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

/* Footer */
.site-footer {
  background: #333;
  color: #999;
  text-align: center;
  padding: 40px 0;
  margin-top: auto;
}

/* Dialog Styles */
.dialog-content {
  padding: 20px 0;
  text-align: center;
}

.dialog-tip {
  margin-bottom: 15px;
  color: #666;
}

.room-code-input :deep(.el-input__wrapper) {
  border-radius: 24px;
  padding: 5px 15px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.room-code-input :deep(.el-input__inner) {
  text-align: center;
  font-size: 20px;
  letter-spacing: 4px;
  font-weight: bold;
}
</style>
