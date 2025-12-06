# 前端后端API调用分析报告

## 1. 后端API结构概述

### 1.1 认证相关API（AuthController）
| API路径 | 方法 | 功能描述 | 请求体 | 响应体 |
|---------|------|----------|--------|--------|
| `/api/auth/register` | POST | 用户注册 | `RegisterRequest` | `Result<UserAccount>` |
| `/api/auth/login` | POST | 用户登录 | `LoginRequest` | `Result<Map<String, String>>` |
| `/api/auth/reset-password` | POST | 重置密码 | `ResetPasswordRequest` | `Result<Void>` |
| `/api/auth/send-code` | POST | 发送邮箱验证码 | `SendCodeRequest` | `Result<Void>` |
| `/api/auth/logout` | POST | 退出登录 | - | `Result<Void>` |

### 1.2 游戏房间相关API（GameRoomController）
| API路径 | 方法 | 功能描述 | 请求体 | 响应体 |
|---------|------|----------|--------|--------|
| `/api/game/rooms/random/join` | POST | 加入随机匹配 | - | `Result<MatchJoinResponse>` |
| `/api/game/rooms/custom` | POST | 创建自定义房间 | `CreateCustomRoomRequest` | `Result<GameRoomVO>` |
| `/api/game/rooms/custom/join` | POST | 通过房间码加入房间 | `JoinRoomByCodeRequest` | `Result<GameRoomVO>` |
| `/api/game/rooms/{roomId}/ready` | POST | 准备/取消准备 | `ReadyRequest` | `Result<GameRoomVO>` |
| `/api/game/rooms/{roomId}/start` | POST | 开始游戏 | - | `Result<GameRoomVO>` |
| `/api/game/rooms/my` | GET | 查询自己当前所在房间 | - | `Result<GameRoomVO>` |
| `/api/game/rooms/{roomId}` | GET | 查询房间详情 | - | `Result<GameRoomVO>` |

### 1.3 WebSocket游戏指令（GamePlayController）
| 消息路径 | 功能描述 | 消息体 |
|---------|----------|--------|
| `/room/{roomId}/snapshot` | 请求牌局快照 | - |
| `/room/{roomId}/heartbeat` | 心跳消息 | - |
| `/room/{roomId}/surrender` | 玩家投降 | - |
| `/room/{roomId}/bid` | 叫/抢地主 | `BidActionRequest` |
| `/room/{roomId}/play` | 出牌 | `PlayCardRequest` |
| `/room/{roomId}/pass` | 过牌/不出 | - |

## 2. 前端需要修改的内容

### 2.1 核心配置文件创建

#### 2.1.1 Axios配置文件
**文件路径**：`src/utils/request.js`
**功能**：配置Axios实例，设置基础URL、请求拦截器（添加token）和响应拦截器

```javascript
import axios from 'axios';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

// 创建axios实例
const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000
});

// 请求拦截器
request.interceptors.request.use(config => {
  // 从localStorage获取token
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, error => {
  return Promise.reject(error);
});

// 响应拦截器
request.interceptors.response.use(response => {
  const res = response.data;
  if (res.code !== 200) {
    ElMessage.error(res.message || '请求失败');
    return Promise.reject(new Error(res.message || '请求失败'));
  }
  return res;
}, error => {
  if (error.response?.status === 401) {
    const router = useRouter();
    router.push('/login');
    ElMessage.error('登录过期，请重新登录');
  } else {
    ElMessage.error(error.message || '网络异常');
  }
  return Promise.reject(error);
});

export default request;
```

#### 2.1.2 WebSocket配置文件
**文件路径**：`src/utils/websocket.js`
**功能**：配置WebSocket连接，处理实时游戏消息

```javascript
import { ElMessage } from 'element-plus';

class WebSocketService {
  constructor() {
    this.stompClient = null;
    this.token = localStorage.getItem('token');
  }

  connect(url, onConnected, onError, onMessage) {
    if (this.stompClient && this.stompClient.connected) {
      return;
    }

    const socket = new SockJS(url, null, {
      headers: {
        Authorization: `Bearer ${this.token}`
      }
    });

    this.stompClient = Stomp.over(socket);
    this.stompClient.connect({}, () => {
      ElMessage.success('WebSocket连接成功');
      onConnected && onConnected();
    }, (error) => {
      ElMessage.error('WebSocket连接失败');
      onError && onError(error);
    });
  }

  subscribe(destination, callback) {
    if (this.stompClient && this.stompClient.connected) {
      return this.stompClient.subscribe(destination, (message) => {
        const data = JSON.parse(message.body);
        callback(data);
      });
    }
    return null;
  }

  send(destination, headers, body) {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.send(destination, headers, JSON.stringify(body));
    }
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.disconnect();
      this.stompClient = null;
    }
  }
}

export default new WebSocketService();
```

### 2.2 页面组件修改

#### 2.2.1 登录页面（Login.vue）
**修改内容**：
- 引入axios请求工具
- 修改`handleLogin`方法，调用后端登录API
- 存储token到localStorage

**API调用**：
- POST `/api/auth/login`

#### 2.2.2 注册页面（Register.vue）
**修改内容**：
- 引入axios请求工具
- 添加发送验证码功能
- 修改注册逻辑，调用后端注册API

**API调用**：
- POST `/api/auth/send-code`
- POST `/api/auth/register`

#### 2.2.3 首页（Home.vue）
**修改内容**：
- 添加创建房间功能
- 添加加入随机匹配功能
- 添加通过房间码加入房间功能

**API调用**：
- POST `/api/game/rooms/custom`
- POST `/api/game/rooms/random/join`
- POST `/api/game/rooms/custom/join`

#### 2.2.4 游戏房间页面（GameRoom.vue）
**修改内容**：
- 引入WebSocket服务
- 添加准备/取消准备功能
- 添加开始游戏功能
- 处理WebSocket消息

**API调用**：
- POST `/api/game/rooms/{roomId}/ready`
- POST `/api/game/rooms/{roomId}/start`
- WebSocket消息处理

### 2.3 状态管理（Pinia）

#### 2.3.1 用户状态管理
**文件路径**：`src/stores/user.js`
**功能**：管理用户登录状态、token等

```javascript
import { defineStore } from 'pinia';
import request from '../utils/request';

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo')) || null
  }),
  actions: {
    async login(loginForm) {
      const res = await request.post('/api/auth/login', loginForm);
      this.token = res.data.token;
      localStorage.setItem('token', this.token);
      return res;
    },
    async register(registerForm) {
      const res = await request.post('/api/auth/register', registerForm);
      return res;
    },
    logout() {
      this.token = '';
      this.userInfo = null;
      localStorage.removeItem('token');
      localStorage.removeItem('userInfo');
    }
  }
});
```

#### 2.3.2 游戏状态管理
**文件路径**：`src/stores/game.js`
**功能**：管理游戏房间状态、玩家信息等

```javascript
import { defineStore } from 'pinia';
import request from '../utils/request';

export const useGameStore = defineStore('game', {
  state: () => ({
    currentRoom: null,
    gameSnapshot: null,
    players: []
  }),
  actions: {
    async createCustomRoom(roomConfig) {
      const res = await request.post('/api/game/rooms/custom', roomConfig);
      this.currentRoom = res.data;
      return res;
    },
    async joinByCode(roomCode) {
      const res = await request.post('/api/game/rooms/custom/join', { code: roomCode });
      this.currentRoom = res.data;
      return res;
    },
    async toggleReady(roomId, isReady) {
      const res = await request.post(`/api/game/rooms/${roomId}/ready`, { isReady });
      this.currentRoom = res.data;
      return res;
    },
    async startGame(roomId) {
      const res = await request.post(`/api/game/rooms/${roomId}/start`);
      this.currentRoom = res.data;
      return res;
    }
  }
});
```

## 3. 前端项目依赖添加

### 3.1 安装必要依赖
```bash
# 已安装的依赖
# vue@3.5.22, element-plus@2.11.8, pinia@3.0.4, socket.io-client@4.8.1, vue-router@4.6.3

# 需要安装的依赖
npm install axios stompjs sockjs-client
```

### 3.2 引入WebSocket相关库
在`index.html`中引入Stomp.js和SockJS
```html
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/dist/stomp.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js"></script>
```

## 4. 具体页面修改示例

### 4.1 登录页面（Login.vue）修改

```javascript
<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';

const router = useRouter();
const userStore = useUserStore();
const loginFormRef = ref(null);
const loading = ref(false);

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
const handleLogin = async () => {
  loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        await userStore.login(loginForm);
        console.log('登录成功，用户名:', loginForm.username);
        // 登录成功后跳转到首页
        router.push('/');
      } catch (error) {
        console.error('登录失败:', error);
      } finally {
        loading.value = false;
      }
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

// 导航到注册页面
const navigateToRegister = () => {
  router.push('/register');
};
</script>
```

## 5. 项目启动顺序

1. 启动PostgreSQL数据库
2. 启动Redis服务
3. 启动RabbitMQ服务
4. 启动后端服务：`./mvnw.cmd spring-boot:run -DskipTests`
5. 启动前端服务：`npm run dev`

## 6. 预期效果

- 前端可以通过API调用后端服务
- 用户可以正常注册、登录
- 可以创建和加入游戏房间
- 可以进行实时游戏互动
- WebSocket连接正常，实时消息处理流畅

## 7. 注意事项

1. 确保后端服务运行在8080端口
2. 确保数据库配置正确，用户密码与配置文件一致
3. 确保Redis和RabbitMQ服务正常运行
4. WebSocket连接需要携带token进行认证
5. 前端需要处理token过期情况

## 8. 后续优化建议

1. 添加API文档自动生成（如Swagger）
2. 添加前端请求封装，统一处理错误
3. 添加WebSocket重连机制
4. 添加游戏状态持久化
5. 添加用户在线状态管理
6. 添加游戏历史记录查询功能