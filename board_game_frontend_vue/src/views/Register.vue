<template>
  <div class="register-container">
    <div class="register-form-wrapper">
      <h2>注册</h2>
      <el-form
        :model="registerForm"
        :rules="registerRules"
        ref="registerFormRef"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="验证码" prop="verificationCode">
          <el-input
            v-model="registerForm.verificationCode"
            placeholder="请输入验证码"
            style="width: 100%;"
          >
            <template #append>
              <el-button
                type="info"
                @click="sendVerificationCode"
                :disabled="countdown > 0 || loading"
                :loading="sendingCode"
              >
                {{ countdown > 0 ? `${countdown}s后重新发送` : '发送验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading">注册</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="login-link">
        已有账号？<a href="#" @click.prevent="navigateToLogin">立即登录</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
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

// 导航到登录页面
const navigateToLogin = () => {
  router.push('/login');
};
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.register-form-wrapper {
  background-color: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

h2 {
  text-align: center;
  margin-bottom: 2rem;
  color: #333;
}

.verification-code-container {
  display: flex;
  align-items: center;
}

.login-link {
  text-align: center;
  margin-top: 1rem;
  color: #666;
}

.login-link a {
  color: #409eff;
  text-decoration: none;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>
