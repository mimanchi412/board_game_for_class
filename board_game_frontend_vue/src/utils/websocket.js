import { useUserStore } from '../stores/user';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  constructor() {
    this.stompClient = null;
    this.isConnected = false;
    this.callbacks = {};
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectInterval = 3000;
    this.userStore = useUserStore();
    this.heartbeatInterval = null;
    this.heartbeatFrequency = 15000; // 心跳频率改为15秒
  }

  // 初始化WebSocket连接
  connect(url = 'http://localhost:8080/ws') {
    return new Promise((resolve, reject) => {
      try {
        // 从用户store获取token
        const token = this.userStore.token;
        if (!token) {
          reject(new Error('未登录，无法建立WebSocket连接'));
          return;
        }

        // 创建SockJS实例，添加token参数
        const sockJSUrl = `${url}?token=${token}`;
        const socket = new SockJS(sockJSUrl);

        // 创建STOMP客户端
        this.stompClient = new Client({
          webSocketFactory: () => socket,
          reconnectDelay: this.reconnectInterval,
          debug: (str) => {
            // 仅在开发环境下启用调试日志
            if (process.env.NODE_ENV === 'development') {
              console.log(str);
            }
          },
          onConnect: () => {
            console.log('WebSocket连接成功');
            this.isConnected = true;
            this.reconnectAttempts = 0;
            this.startHeartbeat();
            resolve();
          },
          onWebSocketError: (error) => {
            console.error('WebSocket连接错误:', error);
            this.isConnected = false;
            reject(error);
          },
          onWebSocketClose: () => {
            console.log('WebSocket连接已关闭');
            this.isConnected = false;
            this.stopHeartbeat();
            this.attemptReconnect();
          },
          onStompError: (frame) => {
            console.error('STOMP错误:', frame);
            this.isConnected = false;
          }
        });

        // 启动STOMP客户端
        this.stompClient.activate();

      } catch (error) {
        console.error('WebSocket初始化失败:', error);
        reject(error);
      }
    });
  }

  // 断开WebSocket连接
  disconnect() {
    if (this.stompClient && this.isConnected) {
      this.stompClient.deactivate();
      console.log('WebSocket连接已断开');
    }
    this.isConnected = false;
  }

  // 发送消息
  send(destination, headers = {}, body = {}) {
    return new Promise((resolve, reject) => {
      if (!this.stompClient || !this.isConnected) {
        console.error('WebSocket未连接，尝试重新连接后发送消息');
        this.connect().then(() => {
          this.stompClient.publish({
            destination,
            headers,
            body: JSON.stringify(body)
          });
          resolve();
        }).catch(error => {
          reject(error);
        });
        return;
      }

      try {
        this.stompClient.publish({
          destination,
          headers,
          body: JSON.stringify(body)
        });
        resolve();
      } catch (error) {
        console.error('发送WebSocket消息失败:', error);
        reject(error);
      }
    });
  }

  // 订阅主题
  subscribe(destination, callback) {
    if (!this.stompClient || !this.isConnected) {
      console.error('WebSocket未连接，无法订阅主题');
      return null;
    }

    try {
      const subscription = this.stompClient.subscribe(destination, (message) => {
        try {
          console.log(`[WebSocket] 收到消息 - 目的地: ${destination}, 完整消息:`, message);
          const data = JSON.parse(message.body);
          console.log(`[WebSocket] 解析后的数据:`, data);
          callback(data);
        } catch (error) {
          console.error(`[WebSocket] 解析消息失败 (目的地: ${destination}):`, error);
          console.error(`[WebSocket] 原始消息:`, message.body);
          callback(message.body);
        }
      });

      // 保存订阅信息
      if (!this.callbacks[destination]) {
        this.callbacks[destination] = [];
      }
      this.callbacks[destination].push(subscription);

      return subscription;
    } catch (error) {
      console.error('订阅WebSocket主题失败:', error);
      return null;
    }
  }

  // 取消订阅
  unsubscribe(destination) {
    if (this.callbacks[destination]) {
      this.callbacks[destination].forEach(subscription => {
        subscription.unsubscribe();
      });
      delete this.callbacks[destination];
    }
  }

  // 取消所有订阅
  unsubscribeAll() {
    Object.keys(this.callbacks).forEach(destination => {
      this.unsubscribe(destination);
    });
  }

  // 检查连接状态
  getConnectionStatus() {
    return this.isConnected;
  }
  
  // 检查连接状态（别名方法）
  isConnected() {
    return this.getConnectionStatus();
  }

  // 尝试重新连接
  attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('已达到最大重连次数，停止尝试');
      return;
    }

    this.reconnectAttempts++;
    console.log(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);

    setTimeout(async () => {
      try {
        await this.connect();
        console.log('重新连接成功');
      } catch (error) {
        console.error('重新连接失败:', error);
        this.attemptReconnect();
      }
    }, this.reconnectInterval * Math.pow(1.5, this.reconnectAttempts - 1)); // 指数退避
  }

  // 启动心跳机制
  startHeartbeat() {
    this.stopHeartbeat(); // 先停止现有的心跳

    this.heartbeatInterval = setInterval(() => {
      if (this.isConnected) {
        console.log('发送WebSocket心跳...');
        // 发送一个空的心跳消息到服务器
        this.send('/app/heartbeat').catch(error => {
          console.error('发送心跳失败:', error);
        });
      }
    }, this.heartbeatFrequency);
  }

  // 停止心跳机制
  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
      console.log('心跳机制已停止');
    }
  }
}

// 导出单例实例
export default new WebSocketService();
