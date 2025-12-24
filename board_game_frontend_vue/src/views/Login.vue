<template>
  <div class="login-container">
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
      <div class="login-card glass-effect">
        <div class="form-header">
          <div class="header-icon-box">
            <el-icon><User /></el-icon>
          </div>
          <h2>欢迎回来</h2>
          <p>登录您的账号以继续</p>
        </div>
      <el-form
        :model="loginForm"
        :rules="loginRules"
        ref="loginFormRef"
        size="large"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="请输入用户名" 
            :prefix-icon="User"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            :prefix-icon="Lock"
            class="custom-input"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="userStore.loading" class="submit-btn" round>立即登录</el-button>
        </el-form-item>
      </el-form>
      <div class="form-footer">
        <el-button link @click="resetForm">重置</el-button>
        <div class="register-link">
          还没有账号？<el-link type="primary" @click="navigateToRegister">立即注册</el-link>
          <span class="divider">|</span>
          <el-link type="info" @click="navigateToReset">忘记密码？</el-link>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Trophy, ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter();
const loginFormRef = ref(null);
const userStore = useUserStore();

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: ''
});

// 登录表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
};

// 处理登录
const handleLogin = () => {
  loginFormRef.value.validate((valid) => {
    if (valid) {
      userStore.loading = true
      // 调用后端登录接口
      userStore.login(loginForm)
        .then((response) => {
          ElMessage.success('登录成功')
          // 登录成功后跳转到首页
          router.push('/')
        })
        .catch((error) => {
          ElMessage.error(error.response?.data?.message || '登录失败，请稍后重试')
        })
        .finally(() => {
          userStore.loading = false
        })
    } else {
      console.log('表单验证失败');
      return false;
    }
  });
};

// 重置表单
const resetForm = () => {
  loginFormRef.value.resetFields();
};

// 返回首页
const goHome = () => {
  router.push('/');
};

// 导航到注册页面
const navigateToRegister = () => {
  router.push('/register');
};

// 导航到重置密码页面
const navigateToReset = () => {
  router.push('/reset-password');
};
</script>

<style scoped>
.login-container {
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

.login-card {
  background: rgba(255, 255, 255, 0.9);
  padding: 40px 50px;
  border-radius: 24px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 450px;
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

.register-link {
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 6px;
}

.register-link :deep(.el-link) {
  font-weight: bold;
  vertical-align: baseline;
}

.register-link .divider {
  color: #ccc;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
