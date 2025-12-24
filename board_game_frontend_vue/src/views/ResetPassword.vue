<template>
  <div class="reset-container">
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
      <div class="reset-card glass-effect">
        <div class="form-header">
          <div class="header-icon-box">
            <el-icon><Lock /></el-icon>
          </div>
          <h2>重置密码</h2>
          <p>输入邮箱获取验证码并设置新密码</p>
        </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        class="reset-form"
      >
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入注册邮箱"
            :prefix-icon="Message"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item prop="usernameOrEmail">
          <el-input
            v-model="form.usernameOrEmail"
            placeholder="用户名或邮箱，用于校验"
            :prefix-icon="User"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item prop="verificationCode">
          <div class="verification-row">
            <el-input
              v-model="form.verificationCode"
              placeholder="邮箱验证码"
              :prefix-icon="Key"
              class="code-input"
              :class="'custom-input'"
            />
            <el-button
              type="primary"
              plain
              @click="sendCode"
              :disabled="countdown > 0"
              :loading="sendingCode"
              class="code-btn custom-btn"
              round
            >
              {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
            :prefix-icon="Lock"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
            :prefix-icon="Lock"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit" :loading="loading" class="submit-btn" round>重置密码</el-button>
        </el-form-item>
      </el-form>
      <div class="form-footer">
        <el-button link @click="navigateLogin">返回登录</el-button>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Lock, Message, Key, Trophy, ArrowLeft } from '@element-plus/icons-vue';
import axios from '../utils/axios';

const router = useRouter();
const formRef = ref(null);
const loading = ref(false);
const sendingCode = ref(false);
const countdown = ref(0);
let timer = null;

const form = reactive({
  email: '',
  usernameOrEmail: '',
  verificationCode: '',
  newPassword: '',
  confirmPassword: ''
});

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  usernameOrEmail: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '验证码长度为6位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

const goHome = () => {
  router.push('/');
};

const navigateLogin = () => {
  router.push('/login');
};

const startCountdown = () => {
  countdown.value = 60;
  timer = setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0) {
      clearInterval(timer);
      timer = null;
    }
  }, 1000);
};

const sendCode = () => {
  formRef.value.validateField('email', async (valid) => {
    if (valid) return;
    sendingCode.value = true;
    try {
      const res = await axios.post('/api/auth/send-code', { email: form.email });
      if (res && res.code === 200) {
        ElMessage.success(res.message || '验证码发送成功');
        startCountdown();
      } else {
        ElMessage.error(res?.message || '验证码发送失败');
      }
    } catch (error) {
      ElMessage.error(error?.message || '验证码发送失败');
    } finally {
      sendingCode.value = false;
    }
  });
};

const submit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      const payload = {
        usernameOrEmail: form.usernameOrEmail || form.email,
        verificationCode: form.verificationCode,
        newPassword: form.newPassword
      };
      const res = await axios.post('/api/auth/reset-password', payload);
      if (res && res.code === 200) {
        ElMessage.success(res.message || '密码重置成功，请使用新密码登录');
        router.push('/login');
      } else {
        ElMessage.error(res?.message || '密码重置失败');
      }
    } catch (error) {
      ElMessage.error(error?.message || '密码重置失败');
    } finally {
      loading.value = false;
    }
  });
};

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
});
</script>

<style scoped>
.reset-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #4b6cb7 0%, #182848 100%);
  background-size: cover;
  flex-direction: column;
}

/* Navbar */
.navbar {
  height: 64px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
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
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-text {
  font-size: 20px;
  font-weight: bold;
  color: #fff;
}

.nav-back-btn {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
}

.nav-back-btn:hover {
  color: #fff;
}

/* Main Content */
.main-content {
  margin-top: 64px;
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 20px;
}

.reset-card {
  background: rgba(255, 255, 255, 0.9);
  padding: 40px 50px;
  border-radius: 24px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 480px;
  backdrop-filter: blur(20px);
  animation: fadeInUp 0.6s ease-out;
}

.form-header {
  text-align: center;
  margin-bottom: 40px;
}

.header-icon-box {
  width: 60px;
  height: 60px;
  background: #e9f3ff;
  color: #409eff;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 28px;
  margin: 0 auto 15px;
}

.form-header h2 {
  margin: 0 0 8px;
  color: #333;
  font-size: 26px;
  font-weight: 700;
}

.form-header p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.custom-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 4px 15px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.custom-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.verification-row {
  display: flex;
  width: 100%;
  gap: 10px;
}

.code-input {
  flex: 1;
}

.code-btn {
  width: 120px;
  border-radius: 12px;
}

.submit-btn {
  width: 100%;
  font-size: 18px;
  padding: 22px 0;
  border-radius: 12px;
  font-weight: bold;
  margin-top: 10px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.form-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
