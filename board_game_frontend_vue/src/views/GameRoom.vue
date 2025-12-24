<template>
  <div class="game-room-container">
    <div class="glass-card">
      <div class="room-header">
        <div class="header-info">
          <h2>游戏大厅</h2>
          <div class="room-tags">
            <el-tag effect="plain" round>ID: {{ roomInfo.roomId || '...' }}</el-tag>
            <el-tag type="success" effect="dark" round size="large" class="code-tag">
              房间码: {{ roomInfo.roomCode || '...' }}
            </el-tag>
          </div>
        </div>
        <el-button type="danger" plain :icon="SwitchButton" @click="leaveRoom" circle title="退出房间" />
      </div>
    
      <div class="room-content">
        <!-- 房间成员列表 -->
        <div class="section-panel members-panel">
          <div class="panel-header">
            <h3><el-icon><User /></el-icon> 玩家列表 ({{ roomInfo.memberIds?.length || 0 }})</h3>
          </div>
          <div class="members-list">
            <div 
              v-for="memberId in roomInfo.memberIds" 
              :key="memberId"
              class="member-card"
              :class="{ 
                'is-host': memberId === roomInfo.ownerId, 
                'is-ready': roomInfo.readyMap[memberId],
                'is-self': String(memberId) === String(userStore.userInfo?.id)
              }"
            >
              <div class="avatar-wrapper">
                <el-avatar :size="48" class="member-avatar">
                  {{ getUserInitials(memberId) }}
                </el-avatar>
                <div v-if="roomInfo.readyMap[memberId]" class="ready-badge"><el-icon><Select /></el-icon></div>
              </div>
              
              <div class="member-info">
                <div class="member-name">
                  {{ getUsername(memberId) }}
                  <el-icon v-if="memberId === roomInfo.ownerId" class="host-icon" title="房主"><Trophy /></el-icon>
                </div>
                <div class="member-status-text">
                  {{ roomInfo.readyMap[memberId] ? '已准备' : '等待中' }}
                </div>
              </div>
            </div>
          </div>
        </div>
      
        <!-- 游戏控制区 -->
        <div class="section-panel controls-panel">
          <div class="panel-header">
            <h3><el-icon><VideoPlay /></el-icon> 操作控制</h3>
          </div>
          
          <div class="control-buttons">
            <el-button 
              :type="isReady ? 'warning' : 'primary'" 
              size="large"
              @click="toggleReady" 
              :loading="loading"
              :disabled="roomInfo.isGameStarted"
              class="main-btn"
              :icon="isReady ? CloseBold : Select"
            >
              {{ isReady ? '取消准备' : '准备就绪' }}
            </el-button>
            
            <el-button 
              type="success" 
              size="large"
              @click="startGame" 
              :loading="loading"
              :disabled="!isHost || !allMembersReady || roomInfo.isGameStarted"
              class="main-btn"
              :icon="VideoPlay"
            >
              开始游戏
            </el-button>
          </div>
          
          <!-- 快速开始按钮 -->
          <div class="quick-start-box" v-if="!roomInfo.isGameStarted">
            <p>等待太久？尝试匹配其他玩家</p>
            <el-button text bg type="info" @click="quickStart">快速匹配</el-button>
          </div>
        </div>
      </div>
      
      <!-- 游戏状态显示 -->
      <div class="game-status-bar" v-if="roomInfo.isGameStarted">
        <el-alert
          title="游戏已开始，正在进入战场..."
          type="success"
          center
          show-icon
          :closable="false"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';

// 辅助函数：获取用户名
const getUsername = (userId) => {
  // 如果用户ID不存在，返回默认值
  if (!userId) {
    return '未知用户';
  }
  // 将数字ID转换为字符串键，因为userMap使用字符串作为键
  const userIdStr = String(userId);
  // 检查用户映射表是否存在该用户
  if (userMap.value[userIdStr]) {
    return userMap.value[userIdStr].nickname || userMap.value[userIdStr].username;
  }
  // 如果没有用户信息，使用用户ID作为默认用户名
  return `用户${userId}`;
};

// 辅助函数：获取用户头像首字母
const getUserInitials = (userId) => {
  // 如果用户ID不存在，返回默认值
  if (!userId) {
    return 'U';
  }
  const username = getUsername(userId);
  return username.charAt(0).toUpperCase();
};
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';
import { ElMessage } from 'element-plus';
import { User, SwitchButton, Select, CloseBold, VideoPlay, Trophy } from '@element-plus/icons-vue';
import axios from '../utils/axios';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const roomId = computed(() => route.params.roomId);
const loading = ref(false);
const roomInfo = ref({
  roomId: '',
  roomCode: '',
  memberIds: [],
  readyMap: {},
  ownerId: null,
  status: null,
  isHost: false,
  isGameStarted: false
});

// 模拟用户信息映射（实际项目中应该从后端获取）
const userMap = ref({});

// 计算当前用户是否已准备
const isReady = computed(() => {
  if (!roomInfo.value.readyMap || !userStore.userInfo?.id) {
    return false;
  }
  const userId = Number(userStore.userInfo.id);
  // 直接使用数字类型查找准备状态
  return Boolean(roomInfo.value.readyMap[userId]);
});

// 计算是否所有成员都已准备
const allMembersReady = computed(() => {
  const hasEnoughPlayers = roomInfo.value.memberIds.length >= 3;
  if (!hasEnoughPlayers || !roomInfo.value.memberIds || !roomInfo.value.readyMap) {
    return false;
  }
  
  const allReady = roomInfo.value.memberIds.every(memberId => {
    // 直接使用数字类型查找准备状态
    return Boolean(roomInfo.value.readyMap[memberId]);
  });
  
  // 调试日志
  console.log(`房间成员数: ${roomInfo.value.memberIds.length}`);
  console.log(`是否人数足够: ${hasEnoughPlayers}`);
  console.log(`是否所有成员都已准备: ${allReady}`);
  console.log(`完整的readyMap:`, roomInfo.value.readyMap);
  console.log(`完整的memberIds:`, roomInfo.value.memberIds);
  
  return allReady;
});

// 计算是否是房主
const isHost = computed(() => {
  const hasOwnerId = roomInfo.value.ownerId !== null && roomInfo.value.ownerId !== undefined;
  const hasUserId = !!userStore.userInfo?.id;
  
  if (!hasOwnerId || !hasUserId) {
    // 调试日志
    console.log(`房主判断: 缺少必要信息`);
    console.log(`  ownerId: ${roomInfo.value.ownerId}`);
    console.log(`  userStore.userInfo.id: ${userStore.userInfo?.id}`);
    return false;
  }
  
  const ownerId = Number(roomInfo.value.ownerId);
  const userId = Number(userStore.userInfo.id);
  const isHostResult = ownerId === userId;
  
  // 调试日志
  console.log(`房主判断:`);
  console.log(`  ownerId: ${roomInfo.value.ownerId} (数字: ${ownerId})`);
  console.log(`  用户ID: ${userStore.userInfo.id} (数字: ${userId})`);
  console.log(`  是否是房主: ${isHostResult}`);
  
  return isHostResult;
});

// 获取用户信息
const getUserInfoById = async (userId) => {
  // 先检查缓存中是否有用户信息
  const userIdStr = String(userId);
  if (userMap.value[userIdStr]) {
    return userMap.value[userIdStr];
  }
  
  try {
    // 使用新添加的用户信息接口
    const response = await axios.get(`/api/user/info/${userId}`);
    
    // 处理后端返回的格式：{code, message, data, timestamp, success}
    const userInfo = response.success && response.data ? response.data : response;
    
    // 保存到缓存，使用字符串ID作为键
    userMap.value[userIdStr] = userInfo;
    return userInfo;
  } catch (error) {
    console.error(`获取用户${userId}信息失败:`, error);
    // 如果获取失败，返回默认信息
    return { id: userId, username: `用户${userId}` };
  }
};

// 批量获取所有成员的信息
const fetchAllMembersInfo = async () => {
  if (!roomInfo.value.memberIds) {
    return;
  }
  
  try {
    // 使用Promise.all并发获取所有成员信息
    const membersInfo = await Promise.all(
      roomInfo.value.memberIds.map(memberId => getUserInfoById(memberId))
    );
    
    // 构建用户映射表，使用字符串ID作为键以兼容前端显示
    const newUserMap = {};
    membersInfo.forEach(memberInfo => {
      // 添加安全检查，确保memberInfo和memberInfo.id存在
      if (memberInfo && memberInfo.id !== undefined) {
        newUserMap[String(memberInfo.id)] = memberInfo;
      } else {
        console.error('无效的用户信息:', memberInfo);
      }
    });
    
    userMap.value = newUserMap;
  } catch (error) {
    console.error('获取所有成员信息失败:', error);
  }
};

// 获取房间详情
const fetchRoomInfo = async () => {
  try {
    loading.value = true;
    const response = await axios.get(`/api/game/rooms/${roomId.value}`);
    
    // 深拷贝后端返回的数据，确保引用发生变化，触发计算属性重新计算
    const newRoomInfo = JSON.parse(JSON.stringify(response.data));
    
    // 添加/更新前端需要的属性
    newRoomInfo.isGameStarted = newRoomInfo.status === 'PLAYING';
    
    // 确保readyMap存在
    if (!newRoomInfo.readyMap) {
      newRoomInfo.readyMap = {};
    } else {
      // 确保所有值都是布尔类型
      const normalizedReadyMap = {};
      Object.keys(newRoomInfo.readyMap).forEach(key => {
        normalizedReadyMap[key] = Boolean(newRoomInfo.readyMap[key]);
      });
      newRoomInfo.readyMap = normalizedReadyMap;
    }
    
    // 确保memberIds存在且为数组
    if (!Array.isArray(newRoomInfo.memberIds)) {
      newRoomInfo.memberIds = [];
    }
    
    // 更新roomInfo.value，触发计算属性重新计算
    roomInfo.value = newRoomInfo;
    
    // 调试日志 - 游戏状态更新
    console.log(`游戏状态更新:`);
    console.log(`  房间状态: ${roomInfo.value.status}`);
    console.log(`  isGameStarted: ${roomInfo.value.isGameStarted}`);
    
    // 获取所有成员的信息
    await fetchAllMembersInfo();
    
    // 日志输出，便于调试
    console.log('房间信息:', roomInfo.value);
    console.log('当前用户:', userStore.userInfo);
    console.log('是否是房主:', isHost.value);
    console.log('所有成员是否已准备:', allMembersReady.value);
    
    // 按钮状态综合日志
    console.log('开始游戏按钮状态检查:');
    console.log('  !isHost:', !isHost.value);
    console.log('  !allMembersReady:', !allMembersReady.value);
    console.log('  roomInfo.isGameStarted:', roomInfo.value.isGameStarted);
    console.log('  按钮是否禁用:', !isHost.value || !allMembersReady.value || roomInfo.value.isGameStarted);
    
    // 如果游戏已开始，跳转到游戏界面
    if (roomInfo.value.isGameStarted) {
      // 跳转到游戏界面
      router.push(`/game-play/${roomId.value}`);
    }
  } catch (error) {
    ElMessage.error('获取房间信息失败：' + error.response?.data?.message || '网络错误');
    // 如果获取房间信息失败，返回首页
    router.push('/');
  } finally {
    loading.value = false;
  }
};

// 准备/取消准备
const toggleReady = async () => {
  try {
    loading.value = true;
    
    // 调用后端API
    await axios.post(`/api/game/rooms/${roomId.value}/ready`, {
      ready: !isReady.value
    });
    
    // 重新获取房间信息
    await fetchRoomInfo();
    
    ElMessage.success(isReady.value ? '取消准备成功' : '准备成功');
  } catch (error) {
    console.error('准备状态切换失败:', error);
    ElMessage.error('操作失败：' + error.response?.data?.message || '网络错误');
  } finally {
    loading.value = false;
  }
};

// 开始游戏
const startGame = async () => {
  try {
    loading.value = true;
    await axios.post(`/api/game/rooms/${roomId.value}/start`);
    // 重新获取房间信息
    await fetchRoomInfo();
    ElMessage.success('游戏开始！');
  } catch (error) {
    ElMessage.error('开始游戏失败：' + error.response?.data?.message || '网络错误');
  } finally {
    loading.value = false;
  }
};

// 退出房间
const leaveRoom = async () => {
  try {
    console.log('开始退出房间，房间ID:', roomId.value);
    loading.value = true;
    // 调用后端退出房间接口
    await axios.post(`/api/game/rooms/${roomId.value}/leave`);
    console.log('后端接口调用成功，准备跳转到首页');
    router.push('/');
    ElMessage.success('已退出房间');
  } catch (error) {
    console.error('退出房间失败：', error);
    console.log('接口调用失败，准备跳转到首页');
    // 即使调用失败也返回首页
    router.push('/');
    ElMessage.success('已退出房间');
  } finally {
    loading.value = false;
    console.log('退出房间方法执行完毕');
  }
};

// 快速开始游戏
const quickStart = async () => {
  try {
    loading.value = true;
    const response = await axios.post('/api/game/rooms/random/join');
    const { roomId } = response.data;
    router.push(`/game-room/${roomId}`);
    ElMessage.success('正在为您匹配玩家...');
  } catch (error) {
    ElMessage.error('匹配失败：' + error.response?.data?.message || '网络错误');
  } finally {
    loading.value = false;
  }
};

// 定义定时器变量
let roomInfoTimer = null;

// 监听游戏开始状态变化
watch(
  () => roomInfo.value.isGameStarted,
  (newVal) => {
    if (newVal) {
      console.log('游戏已开始，导航到游戏界面');
      router.push(`/game-play/${roomId.value}`);
    }
  },
  { immediate: true }
);

// 组件挂载时获取房间信息
onMounted(async () => {
  // 调试用户认证状态
  console.log('onMounted开始:');
  console.log('  isAuthenticated:', userStore.isAuthenticated);
  console.log('  token:', userStore.token);
  console.log('  userInfo:', userStore.userInfo);
  
  // 强制获取用户信息，无论isAuthenticated状态如何
  if (!userStore.userInfo) {
    try {
      console.log('准备调用getUserInfo()');
      await userStore.getUserInfo();
      console.log('获取用户信息成功:', userStore.userInfo);
      console.log('用户ID类型:', typeof userStore.userInfo?.id);
      console.log('用户ID值:', userStore.userInfo?.id);
    } catch (error) {
      console.error('获取用户信息失败:', error);
      console.error('错误详情:', error.response?.data);
      console.error('错误配置:', {
        status: error.response?.status,
        headers: error.response?.headers,
        config: error.config
      });
      // 如果获取用户信息失败，可能是token过期，清除token并返回登录页
      userStore.clearToken();
      router.push('/login');
      ElMessage.error('请重新登录');
      return;
    }
  } else {
    console.log('用户信息已存在:', userStore.userInfo);
    console.log('用户ID类型:', typeof userStore.userInfo?.id);
    console.log('用户ID值:', userStore.userInfo?.id);
  }
  
  await fetchRoomInfo();
  
  // 设置定时器定期刷新房间信息
  roomInfoTimer = setInterval(async () => {
    try {
      await fetchRoomInfo();
    } catch (error) {
      console.error('定时刷新房间信息失败:', error);
    }
  }, 1000);
});

// 组件卸载时清除定时器
onUnmounted(() => {
  if (roomInfoTimer) {
    clearInterval(roomInfoTimer);
    roomInfoTimer = null;
  }
});
</script>

<style scoped>
.game-room-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.glass-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 30px;
  width: 100%;
  max-width: 900px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  margin-top: 20px;
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.header-info h2 {
  margin: 0;
  color: #333;
  font-size: 24px;
  margin-bottom: 10px;
}

.room-tags {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.code-tag {
  font-weight: bold;
  letter-spacing: 1px;
}

.room-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.section-panel {
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.panel-header h3 {
  margin: 0 0 20px;
  color: #333;
  font-size: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.member-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.member-card.is-self {
  border-color: #a0cfff;
}

.member-card.is-ready {
  background: #f0f9eb;
  border-color: #e1f3d8;
}

.avatar-wrapper {
  position: relative;
}

.member-avatar {
  background: #667eea;
  font-weight: bold;
  font-size: 18px;
}

.ready-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  background: #67c23a;
  color: white;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  border: 2px solid white;
}

.member-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.member-name {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  display: flex;
  align-items: center;
  gap: 6px;
}

.host-icon {
  color: #e6a23c;
}

.member-status-text {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.control-buttons {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 25px;
}

.main-btn {
  width: 100%;
  margin-left: 0 !important;
  height: 44px;
}

.quick-start-box {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.quick-start-box p {
  margin: 0 0 10px;
  color: #909399;
  font-size: 12px;
}

.game-status-bar {
  margin-top: 20px;
}

@media (max-width: 768px) {
  .room-content {
    grid-template-columns: 1fr;
  }
  
  .glass-card {
    padding: 20px;
  }
}
</style>
