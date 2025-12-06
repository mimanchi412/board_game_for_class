import axios from 'axios';
import { useUserStore } from '../stores/user';

// 创建axios实例
const instance = axios.create({
  baseURL: 'http://localhost:8080', // 后端API基础URL
  timeout: 10000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json',
  }
});

// 请求拦截器
instance.interceptors.request.use(
  config => {
    // 从Pinia store获取token
    const userStore = useUserStore();
    
    // 添加请求调试日志
    console.log(`发送请求: ${config.url}`);
    console.log(`当前token: ${userStore.token}`);
    console.log(`当前tokenType: ${userStore.tokenType}`);
    
    if (userStore.token) {
      config.headers['Authorization'] = `${userStore.tokenType} ${userStore.token}`;
      console.log(`已添加Authorization头: ${config.headers['Authorization']}`);
    } else {
      console.log('未添加Authorization头，token不存在');
    }
    
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器
instance.interceptors.response.use(
  response => {
    // 直接返回响应数据
    return response.data;
  },
  error => {
    // 统一处理错误
    let errorMessage = '请求失败，请稍后重试';
    console.error('请求错误详情:', error);
    if (error.response) {
      // 服务器返回了错误状态码
      const { status, data, config } = error.response;
      console.error(`请求失败: ${config.url}，状态码: ${status}，响应数据:`, data);
      switch (status) {
        case 400:
          errorMessage = data.message || '请求参数错误';
          break;
        case 401:
          errorMessage = '未授权，请重新登录';
          // 处理登录过期逻辑，清除token并跳转到登录页
          const userStore = useUserStore();
          console.error('401错误，当前token:', userStore.token);
          userStore.clearToken();
          // 使用window.location.href跳转，避免在拦截器中使用router导致的问题
          window.location.href = '/login';
          break;
        case 403:
          errorMessage = '拒绝访问';
          break;
        case 404:
          errorMessage = '请求的资源不存在';
          break;
        case 500:
          errorMessage = '服务器内部错误';
          break;
        default:
          errorMessage = data.message || `请求失败，状态码：${status}`;
      }
    } else if (error.request) {
      // 请求已发出，但没有收到响应
      errorMessage = '服务器无响应，请检查网络连接';
      console.error('请求发出但无响应:', error.request);
    } else {
      // 请求配置出错
      console.error('请求配置错误:', error.message);
    }
    // 可以在这里使用Element Plus的Message组件显示错误信息
    console.error('请求错误:', errorMessage);
    return Promise.reject(errorMessage);
  }
);

export default instance;
