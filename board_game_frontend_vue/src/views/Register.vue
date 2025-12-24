<template>
  <div class="register-container">
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
      <div class="register-card glass-effect">
        <div class="form-header">
          <div class="header-icon-box">
            <el-icon><User /></el-icon>
          </div>
          <h2>欢迎注册</h2>
          <p>加入我们的桌游社区</p>
        </div>
      <el-form
        :model="registerForm"
        :rules="registerRules"
        ref="registerFormRef"
        size="large"
        class="register-form"
      >
        <el-form-item prop="username">
          <el-input 
            v-model="registerForm.username" 
            placeholder="请输入用户名" 
            :prefix-icon="User"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            :prefix-icon="Lock"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            show-password
            :prefix-icon="Lock"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item prop="email">
          <el-input 
            v-model="registerForm.email" 
            placeholder="请输入邮箱" 
            :prefix-icon="Message"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item prop="verificationCode">
          <div class="verification-code-row">
            <el-input
              v-model="registerForm.verificationCode"
              placeholder="验证码"
              :prefix-icon="Key"
              class="code-input"
              :class="'custom-input'"
            />
            <el-button
              type="primary"
              plain
              @click="sendVerificationCode"
              :disabled="countdown > 0 || loading"
              :loading="sendingCode"
              class="code-btn custom-btn"
              round
            >
              {{ countdown > 0 ? `${countdown}s后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading" class="submit-btn" round>立即注册</el-button>
        </el-form-item>
      </el-form>
      <div class="form-footer">
        <el-button link @click="resetForm">重置</el-button>
        <div class="login-link">
          已有账号？<el-link type="primary" @click="navigateToLogin">立即登录</el-link>
          <span class="divider">|</span>
          <el-link type="info" @click="navigateToReset">找回密码</el-link>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Lock, Message, Key, Trophy, ArrowLeft } from '@element-plus/icons-vue';
import axios from '../utils/axios';

const router = useRouter();
const registerFormRef = ref(null);
const loading = ref(false);

// 注册表单数据
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  verificationCode: ''
});

// 注册表单验证规则
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '验证码长度为 6 个字符', trigger: 'blur' }
  ]
};

// 验证码倒计时
const countdown = ref(0);
const sendingCode = ref(false);

// 发送验证码
const sendVerificationCode = () => {
  // 验证邮箱格式
  const emailRule = registerRules.email;
  let emailValid = true;
  let errorMessage = '';
  
  for (const rule of emailRule) {
    if (rule.required && !registerForm.email) {
      emailValid = false;
      errorMessage = rule.message;
      break;
    }
    if (rule.type === 'email') {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(registerForm.email)) {
        emailValid = false;
        errorMessage = rule.message;
        break;
      }
    }
  }
  
  if (!emailValid) {
    ElMessage.warning(errorMessage);
    return;
  }
  
  sendingCode.value = true;
  
  // 调用发送验证码API
  axios.post('/api/auth/send-code', { email: registerForm.email })
    .then((result) => {
      if (result && result.code === 200) {
        ElMessage.success(result.message || '验证码发送成功');
        // 开始倒计时
        startCountdown();
      } else {
        ElMessage.error(result.message || '验证码发送失败');
      }
    })
    .catch((error) => {
      const errorMsg = error.response?.data?.message || error.message || '验证码发送失败';
      ElMessage.error(errorMsg);
    })
    .finally(() => {
      sendingCode.value = false;
    });
};

// 开始倒计时
const startCountdown = () => {
  countdown.value = 60;
  const timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer);
    }
  }, 1000);
};

// 处理注册
const handleRegister = () => {
  registerFormRef.value.validate((valid) => {
    if (valid) {
      loading.value = true;
      // 构造注册请求数据，移除confirmPassword字段
      const registerData = {
        username: registerForm.username,
        password: registerForm.password,
        email: registerForm.email,
        gender: 0, // 默认值，0表示未知
        verificationCode: registerForm.verificationCode // 使用用户输入的验证码
      };
      
      // 调用后端注册接口
      axios.post('/api/auth/register', registerData)
        .then((result) => {
          // 解析后端返回的Result对象
          if (result && result.code === 200) {
            ElMessage.success(result.message || '注册成功');
            // 注册成功后跳转到登录页面
            router.push('/login');
          } else {
            // 显示后端返回的错误信息
            ElMessage.error(result.message || '注册失败，请稍后重试');
          }
        })
        .catch((error) => {
          // 处理网络错误或其他异常
          const errorMsg = error.response?.data?.message || error.message || '注册失败，请稍后重试';
          ElMessage.error(errorMsg);
        })
        .finally(() => {
          loading.value = false;
        });
    } else {
      console.log('表单验证失败');
      return false;
    }
  });
};

// 重置表单
const resetForm = () => {
  registerFormRef.value.resetFields();
};

// 返回首页
const goHome = () => {
  router.push('/');
};

// 导航到登录页面
const navigateToLogin = () => {
  router.push('/login');
};

// 导航到重置密码页面
const navigateToReset = () => {
  router.push('/reset-password');
};
</script>

<style scoped>
.register-container {
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

.register-card {
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

.verification-code-row {
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
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.login-link {
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 6px;
}

.login-link :deep(.el-link) {
  font-weight: bold;
  vertical-align: baseline;
}

.login-link .divider {
  color: #ccc;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
