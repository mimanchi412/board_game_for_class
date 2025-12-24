<template>
  <div class="profile-container">
    <!-- 顶部导航栏 -->
    <nav class="navbar">
      <div class="nav-content">
        <div class="nav-brand" @click="goHome">
          <div class="logo-icon">
            <el-icon :size="24" color="#fff"><Trophy /></el-icon>
          </div>
          <span class="brand-text">桌游对战平台</span>
        </div>
        <div class="nav-right">
          <el-button link class="nav-back-btn" @click="goHome">
            <el-icon><ArrowLeft /></el-icon> 返回大厅
          </el-button>
        </div>
      </div>
    </nav>

    <div class="main-content">
      <div class="profile-layout">
        <!-- 左侧：用户卡片 -->
        <div class="user-card glass-effect">
          <div class="user-header-bg"></div>
          <div class="user-content">
            <div class="avatar-wrapper">
              <el-avatar :size="100" class="profile-avatar" :src="avatarUrl">
                {{ avatarText }}
              </el-avatar>
              <div class="avatar-upload-trigger">
                <el-upload
                  class="avatar-uploader"
                  action=""
                  :show-file-list="false"
                  :http-request="uploadAvatar"
                  :before-upload="beforeUpload"
                  :disabled="uploading"
                >
                  <el-icon class="camera-icon"><Camera /></el-icon>
                </el-upload>
              </div>
            </div>
            
            <h2 class="user-name">{{ profileForm.nickname || profileForm.username }}</h2>
            <p class="user-email">{{ profileForm.email || '尚未绑定邮箱' }}</p>
            
            <div class="user-stats">
               <div class="stat-item">
                 <span class="stat-value">--</span>
                 <span class="stat-label">胜场</span>
               </div>
               <div class="stat-divider"></div>
               <div class="stat-item">
                 <span class="stat-value">--%</span>
                 <span class="stat-label">胜率</span>
               </div>
            </div>

            <div class="user-actions">
               <el-button type="primary" plain round :loading="avatarLoading" @click="fetchAvatar(true)" size="small">
                <el-icon><Refresh /></el-icon> 刷新头像
              </el-button>
            </div>
          </div>
        </div>

        <!-- 右侧：编辑表单 -->
        <div class="edit-card glass-effect">
          <div class="card-header">
            <h3><el-icon><Edit /></el-icon> 编辑资料</h3>
            <el-button link type="primary" :loading="loading" @click="loadProfile">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>

          <el-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="rules"
            label-position="top"
            class="profile-form"
            size="large"
          >
            <div class="form-row">
              <el-form-item label="用户名" class="half-width">
                <el-input v-model="profileForm.username" disabled :prefix-icon="User" />
              </el-form-item>
              <el-form-item label="邮箱" class="half-width">
                <el-input v-model="profileForm.email" disabled :prefix-icon="Message" />
              </el-form-item>
            </div>

            <div class="form-row">
              <el-form-item label="昵称" prop="nickname" class="half-width">
                <el-input v-model="profileForm.nickname" placeholder="请输入昵称" :prefix-icon="EditPen" />
              </el-form-item>
              <el-form-item label="性别" prop="gender" class="half-width">
                <el-select v-model="profileForm.gender" placeholder="请选择性别" style="width: 100%">
                  <el-option label="保密" :value="0" />
                  <el-option label="男" :value="1" />
                  <el-option label="女" :value="2" />
                </el-select>
              </el-form-item>
            </div>

            <div class="form-row">
               <el-form-item label="生日" class="half-width">
                <el-date-picker
                  v-model="profileForm.birthday"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择日期"
                  style="width: 100%;"
                />
              </el-form-item>
              <el-form-item label="地区" class="half-width">
                <el-input v-model="profileForm.region" placeholder="如：上海" :prefix-icon="Location" />
              </el-form-item>
            </div>

            <el-form-item label="个性签名">
              <el-input
                v-model="profileForm.bio"
                type="textarea"
                :rows="3"
                maxlength="160"
                show-word-limit
                placeholder="写点什么来展示自己..."
              />
            </el-form-item>

            <div class="form-footer">
              <el-button @click="resetForm" :disabled="loading || submitting" round>重置</el-button>
              <el-button type="primary" @click="submit" :loading="submitting" round class="save-btn">保存修改</el-button>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Refresh, Camera, Trophy, Edit, User, Message, EditPen, Location } from '@element-plus/icons-vue';
import { useUserStore } from '../stores/user';
import axios from '../utils/axios';

const router = useRouter();
const userStore = useUserStore();
const profileFormRef = ref(null);
const loading = ref(false);
const submitting = ref(false);
const lastProfile = ref(null);
const avatarUrl = ref('');
const avatarLoading = ref(false);
const uploading = ref(false);
const avatarObjectUrl = ref('');

const profileForm = reactive({
  username: '',
  email: '',
  nickname: '',
  gender: 0,
  bio: '',
  birthday: null,
  region: ''
});

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 32, message: '昵称长度在1-32个字符', trigger: 'blur' }
  ],
  gender: [{ required: true, type: 'number', message: '请选择性别', trigger: 'change' }]
};

const avatarText = computed(() => {
  return (profileForm.nickname || profileForm.username || 'U').charAt(0).toUpperCase();
});

const applyProfileData = (info = {}) => {
  const genderValue = Number(info.gender);
  profileForm.username = info.username || '';
  profileForm.email = info.email || '';
  profileForm.nickname = info.nickname || info.username || '';
  profileForm.gender = Number.isNaN(genderValue) ? 0 : genderValue;
  profileForm.bio = info.bio || '';
  profileForm.birthday = info.birthday || null;
  profileForm.region = info.region || '';
  lastProfile.value = { ...info };
};

const loadProfile = async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login');
    return;
  }
  loading.value = true;
  try {
    const data = await userStore.getUserInfo();
    applyProfileData(data || userStore.userInfo || {});
    await fetchAvatar(true);
  } catch (error) {
    ElMessage.error(error?.message || '获取个人资料失败');
    if (!userStore.isAuthenticated) {
      router.push('/login');
    }
  } finally {
    loading.value = false;
  }
};

const submit = () => {
  profileFormRef.value.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      const updatedProfile = await userStore.updateProfile({
        nickname: profileForm.nickname,
        gender: profileForm.gender,
        bio: profileForm.bio,
        birthday: profileForm.birthday || null,
        region: profileForm.region
      });
      ElMessage.success('资料更新成功');
      applyProfileData(updatedProfile || userStore.userInfo || {});
    } catch (error) {
      ElMessage.error(error?.message || '更新失败');
    } finally {
      submitting.value = false;
    }
  });
};

const resetForm = () => {
  if (lastProfile.value) {
    applyProfileData(lastProfile.value);
  }
};

const goHome = () => {
  router.push('/');
};

const beforeUpload = (file) => {
  const allowed = ['image/png', 'image/jpeg', 'image/webp'];
  const maxSize = 5 * 1024 * 1024;
  if (!allowed.includes(file.type)) {
    ElMessage.error('仅支持 PNG/JPEG/WebP 格式');
    return false;
  }
  if (file.size > maxSize) {
    ElMessage.error('头像大小不能超过 5MB');
    return false;
  }
  return true;
};

const fetchAvatar = async (silent = false) => {
  if (!userStore.isAuthenticated) return;
  avatarLoading.value = !silent;
  try {
    const blob = await axios.get('/api/user/avatar', { responseType: 'blob' });
    if (blob instanceof Blob) {
      if (avatarObjectUrl.value) {
        URL.revokeObjectURL(avatarObjectUrl.value);
      }
      avatarObjectUrl.value = URL.createObjectURL(blob);
      avatarUrl.value = avatarObjectUrl.value;
    }
  } catch (error) {
    // 后端不存在头像时会返回404，这里静默处理
    avatarUrl.value = '';
  } finally {
    avatarLoading.value = false;
  }
};

const uploadAvatar = async (option) => {
  const file = option.file;
  uploading.value = true;
  try {
    const formData = new FormData();
    formData.append('file', file);
    await axios.post('/api/user/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    ElMessage.success('头像更新成功');
    await fetchAvatar(true);
    option?.onSuccess?.();
  } catch (error) {
    const message = error?.message || '头像上传失败';
    ElMessage.error(message);
    option?.onError?.(error);
  } finally {
    uploading.value = false;
  }
};

onMounted(() => {
  loadProfile();
});

onBeforeUnmount(() => {
  if (avatarObjectUrl.value) {
    URL.revokeObjectURL(avatarObjectUrl.value);
  }
});
</script>

<style scoped>
.profile-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
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
  cursor: pointer;
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

.nav-back-btn {
  font-size: 16px;
  color: #666;
}

.nav-back-btn:hover {
  color: #409eff;
}

/* Main Content */
.main-content {
  margin-top: 64px;
  padding: 40px 20px;
  flex: 1;
  background: linear-gradient(180deg, #eef1f5 0%, #f5f7fa 100%);
}

.profile-layout {
  max-width: 1000px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
}

/* Glass Effect Cards */
.glass-effect {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.8);
  overflow: hidden;
}

/* User Card */
.user-card {
  display: flex;
  flex-direction: column;
  position: relative;
}

.user-header-bg {
  height: 100px;
  background: linear-gradient(135deg, #4b6cb7 0%, #182848 100%);
}

.user-content {
  padding: 0 24px 30px;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: -50px;
}

.avatar-wrapper {
  position: relative;
  margin-bottom: 16px;
}

.profile-avatar {
  background: #667eea;
  font-size: 36px;
  font-weight: bold;
  border: 4px solid white;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.avatar-upload-trigger {
  position: absolute;
  bottom: 0;
  right: 0;
  background: white;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  cursor: pointer;
  transition: all 0.2s;
}

.avatar-upload-trigger:hover {
  background: #f0f2f5;
  transform: scale(1.1);
}

.camera-icon {
  color: #666;
  font-size: 16px;
}

.user-name {
  margin: 0 0 4px;
  font-size: 22px;
  color: #333;
  font-weight: bold;
}

.user-email {
  margin: 0 0 24px;
  color: #666;
  font-size: 14px;
}

.user-stats {
  display: flex;
  width: 100%;
  justify-content: center;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 0;
  background: #f8f9fa;
  border-radius: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 20px;
}

.stat-value {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 24px;
  background: #e4e7ed;
}

/* Edit Card */
.edit-card {
  padding: 30px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-row {
  display: flex;
  gap: 20px;
}

.half-width {
  flex: 1;
}

.form-footer {
  margin-top: 30px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.save-btn {
  padding-left: 30px;
  padding-right: 30px;
}

@media (max-width: 768px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
  
  .form-row {
    flex-direction: column;
    gap: 0;
  }
}
</style>
