import { defineStore } from 'pinia';
import axios from '../utils/axios';

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    tokenType: localStorage.getItem('tokenType') || '',
    userInfo: null,
    isAuthenticated: !!localStorage.getItem('token'),
    loading: false
  }),

  actions: {
    // 设置token
    setToken(token, tokenType) {
      this.token = token;
      this.tokenType = tokenType;
      this.isAuthenticated = true;
      localStorage.setItem('token', token);
      localStorage.setItem('tokenType', tokenType);
    },

    // 清除token
    clearToken() {
      this.token = '';
      this.tokenType = '';
      this.userInfo = null;
      this.isAuthenticated = false;
      localStorage.removeItem('token');
      localStorage.removeItem('tokenType');
    },

    // 登录
    async login(loginForm) {
      try {
        this.loading = true;
        console.log('发送登录请求，表单数据：', loginForm);
        const response = await axios.post('/api/auth/login', loginForm);
        console.log('登录请求返回的完整响应：', response);
        console.log('响应类型：', typeof response);
        
        // 检查响应是否为null
        if (!response) {
          console.error('登录响应为null');
          throw new Error('登录响应为null');
        }
        
        console.log('登录响应内容：', response);
        
        // 先检查登录是否成功（通过code或success字段判断）
        if (response.code === 200 || response.success === true) {
          // 登录成功，检查是否包含token和tokenType
          if (response.data && response.data.token && response.data.tokenType) {
            this.setToken(response.data.token, response.data.tokenType);
            return response;
          } else {
            console.error('登录响应缺少token或tokenType：', response.data);
            throw new Error('登录响应缺少token信息');
          }
        } else {
          // 登录失败，直接抛出错误
          console.error('登录失败，后端返回错误：', response.message || '登录失败');
          throw new Error(response.message || '登录失败');
        }
      } catch (error) {
        console.error('登录失败：', error);
        throw error;
      } finally {
        this.loading = false;
      }
    },

    // 注册
    async register(registerForm) {
      try {
        this.loading = true;
        // 添加后端要求的gender和verificationCode字段
        const registerData = {
          ...registerForm,
          gender: registerForm.gender || 0, // 默认值，0表示未知
          verificationCode: registerForm.verificationCode || '' // 暂时为空，后续可以实现完整的验证码功能
        };
        
        const response = await axios.post('/api/auth/register', registerData);
        
        // 检查响应是否为null
        if (!response) {
          console.error('注册响应为null');
          throw new Error('注册响应为null');
        }
        
        // 解析后端返回的Result对象
        const result = response.data;
        if (result && result.code === 200) {
          // 注册成功
          return result;
        } else {
          // 注册失败，抛出错误
          throw new Error(result.message || '注册失败');
        }
      } catch (error) {
        console.error('注册失败：', error);
        throw error;
      } finally {
        this.loading = false;
      }
    },

    // 退出登录
    async logout() {
      try {
        this.loading = true;
        await axios.post('/api/auth/logout');
        this.clearToken();
      } catch (error) {
        console.error('退出登录失败:', error);
        // 无论后端是否成功，都清除本地token
        this.clearToken();
      } finally {
        this.loading = false;
      }
    },

    // 获取用户信息
    async getUserInfo() {
      try {
        this.loading = true;
        // 调用后端获取用户信息的接口
        // 注意：axios响应拦截器返回response.data，但后端API返回格式是{code, message, data, timestamp, success}
        // 所以需要从返回的对象中提取data字段作为用户信息
        const response = await axios.get('/api/user/profile');
        
        // 详细日志输出响应内容
        console.log('getUserInfo响应:', response);
        console.log('响应类型:', typeof response);
        console.log('响应结构:', {
          success: response.success,
          data: response.data,
          code: response.code,
          message: response.message
        });
        
        // 确保响应成功且包含data字段
        if (response && response.success && response.data) {
          console.log('提取用户信息:', response.data);
          this.userInfo = response.data;
          return response.data;
        } else if (response && response.data) {
          // 兼容旧格式或异常情况
          console.log('兼容处理：直接使用response.data作为用户信息');
          this.userInfo = response.data;
          return response.data;
        } else {
          throw new Error('获取用户信息失败：响应格式不正确');
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
        // 如果获取用户信息失败，可能是token过期，清除token
        this.clearToken();
        throw error instanceof Error ? error : new Error(error);
      } finally {
        this.loading = false;
      }
    },

    // 更新个人资料
    async updateProfile(profileData) {
      try {
        this.loading = true;
        const payload = {
          nickname: profileData.nickname,
          gender: profileData.gender,
          bio: profileData.bio || null,
          birthday: profileData.birthday || null,
          region: profileData.region || null
        };
        const response = await axios.put('/api/user/profile', payload);
        if (response && response.code === 200) {
          this.userInfo = response.data;
          return response.data;
        }
        throw new Error(response?.message || '更新个人资料失败');
      } catch (error) {
        console.error('更新个人资料失败:', error);
        throw error instanceof Error ? error : new Error(error);
      } finally {
        this.loading = false;
      }
    }
  }
});
