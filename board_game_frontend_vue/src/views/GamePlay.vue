<template>
  <div class="game-play-container">
    <div class="game-header">
      <h2>3人斗地主</h2>
      <div class="game-info">
        <span class="room-code">房间号: {{ gameInfo.roomCode }}</span>
        <span class="game-status">{{ gameStatusText }}</span>
      </div>
    </div>

    <div class="game-main">
      <!-- 地主信息 -->
      <div class="landlord-info" v-if="gameInfo.landlordId">
        <span>地主: {{ getPlayerName(gameInfo.landlordId) }}</span>
      </div>

      <!-- 顶部玩家区域 -->
      <div class="player-area top-player" v-if="otherPlayers[0]">
        <div class="player-info">
          <div class="player-name">{{ getPlayerName(otherPlayers[0]?.userId) }}</div>
          <div class="player-status">
            <span v-if="otherPlayers[0]?.userId === gameInfo.landlordId" class="landlord-tag">地主</span>
            <span class="card-count">剩余: {{ otherPlayers[0]?.handCards?.length || 0 }}</span>
          </div>
        </div>
        <div class="player-cards">
          <div class="card back" v-for="n in otherPlayers[0]?.handCards?.length" :key="`top-${n}`"></div>
        </div>
        <div class="last-played" v-if="lastPlayedCards && lastPlayedPlayerId === otherPlayers[0]?.userId">
          <div class="last-played-label">刚刚出:</div>
          <div class="last-played-cards">
            <div class="card" v-for="card in lastPlayedCards" :key="`last-top-${card}`" :class="cardClass(card)">
              {{ cardName(card) }}
            </div>
          </div>
        </div>
      </div>

      <!-- 中间游戏区域 -->
      <div class="game-center">
        <!-- 地主牌 -->
        <div class="landlord-cards" v-if="gameInfo.landlordCards && gameInfo.landlordCards.length > 0">
          <div class="landlord-cards-label">地主牌:</div>
          <div class="landlord-cards-list">
            <div class="card" v-for="card in gameInfo.landlordCards" :key="`landlord-${card}`" :class="cardClass(card)">
              {{ cardName(card) }}
            </div>
          </div>
        </div>

        <!-- 游戏状态提示 -->
        <div class="game-message">
          {{ gameMessage }}
        </div>

        <!-- 当前玩家操作提示 -->
        <div class="current-turn" v-if="gameInfo.currentTurnId">
          {{ gameInfo.currentTurnId === userStore.userInfo?.id ? '轮到你出牌' : `轮到${getPlayerName(gameInfo.currentTurnId)}出牌` }}
          <div class="countdown">
            剩余时间: {{ remainingTime }}秒
          </div>
        </div>
      </div>

      <!-- 左侧玩家区域 -->
      <div class="player-area left-player" v-if="otherPlayers[1]">
        <div class="player-info">
          <div class="player-name">{{ getPlayerName(otherPlayers[1]?.userId) }}</div>
          <div class="player-status">
            <span v-if="otherPlayers[1]?.userId === gameInfo.landlordId" class="landlord-tag">地主</span>
            <span class="card-count">剩余: {{ otherPlayers[1]?.handCards?.length || 0 }}</span>
          </div>
        </div>
        <div class="player-cards">
          <div class="card back vertical" v-for="n in otherPlayers[1]?.handCards?.length" :key="`left-${n}`"></div>
        </div>
        <div class="last-played" v-if="lastPlayedCards && lastPlayedPlayerId === otherPlayers[1]?.userId">
          <div class="last-played-label">刚刚出:</div>
          <div class="last-played-cards">
            <div class="card vertical" v-for="card in lastPlayedCards" :key="`last-left-${card}`" :class="cardClass(card)">
              {{ cardName(card) }}
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧玩家区域（通常不会显示，仅作备用） -->
      <div class="player-area right-player" v-if="otherPlayers[2]">
        <div class="player-info">
          <div class="player-name">{{ getPlayerName(otherPlayers[2]?.userId) }}</div>
          <div class="player-status">
            <span v-if="otherPlayers[2]?.userId === gameInfo.landlordId" class="landlord-tag">地主</span>
            <span class="card-count">剩余: {{ otherPlayers[2]?.handCards?.length || 0 }}</span>
          </div>
        </div>
        <div class="player-cards">
          <div class="card back vertical" v-for="n in otherPlayers[2]?.handCards?.length" :key="`right-${n}`"></div>
        </div>
        <div class="last-played" v-if="lastPlayedCards && lastPlayedPlayerId === otherPlayers[2]?.userId">
          <div class="last-played-label">刚刚出:</div>
          <div class="last-played-cards">
            <div class="card vertical" v-for="card in lastPlayedCards" :key="`last-right-${card}`" :class="cardClass(card)">
              {{ cardName(card) }}
            </div>
          </div>
        </div>
      </div>

      <!-- 底部玩家区域（当前用户） -->
      <div class="player-area bottom-player">
        <div class="player-info">
          <div class="player-name">{{ getPlayerName(userStore.userInfo?.id) }} (我)</div>
          <div class="player-status">
            <span v-if="userStore.userInfo?.id === gameInfo.landlordId" class="landlord-tag">地主</span>
            <span class="card-count">剩余: {{ myHandCards?.length || 0 }}</span>
          </div>
        </div>
        <div class="my-cards">
          <div 
            class="card" 
            v-for="card in myHandCards" 
            :key="card" 
            :class="[cardClass(card), { selected: selectedCards.includes(card) }]"
            @click="toggleCardSelection(card)"
          >
            {{ cardName(card) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 操作按钮区域 -->
    <div class="action-buttons">
      <!-- 叫地主阶段按钮 -->
      <template v-if="gameInfo.status === 'BIDDING' && gameInfo.currentTurnId === userStore.userInfo?.id">
        <button 
          class="btn btn-primary"
          @click="bidLandlord(1)"
        >
          叫地主
        </button>
        <button 
          class="btn btn-secondary"
          @click="bidLandlord(0)"
        >
          不叫
        </button>
      </template>
      
      <!-- 游戏进行中按钮 -->
      <template v-else-if="gameInfo.status === 'PLAYING'">
        <button 
          class="btn btn-primary"
          @click="playCards"
          :disabled="gameInfo.currentTurnId !== userStore.userInfo?.id || selectedCards.length === 0"
        >
          出牌
        </button>
        <button 
          class="btn btn-secondary"
          @click="passCards"
          :disabled="gameInfo.currentTurnId !== userStore.userInfo?.id"
        >
          不出
        </button>
        <button 
          class="btn btn-danger"
          @click="surrender"
          :disabled="gameInfo.currentTurnId !== userStore.userInfo?.id"
        >
          投降
        </button>
      </template>
      
      <!-- 游戏等待中 -->
      <template v-else>
        <button 
          class="btn btn-primary"
          disabled
        >
          {{ gameStatusText }}
        </button>
      </template>
    </div>

    <!-- 游戏结束提示 -->
    <div class="game-over" v-if="gameInfo.status === 'ENDED'">
      <div class="game-over-content">
        <h3>{{ gameResult }}</h3>
        <div class="game-over-buttons">
          <button class="btn btn-primary" @click="playAgain">再来一局</button>
          <button class="btn btn-secondary" @click="returnToRoom">返回房间</button>
          <button class="btn btn-danger" @click="exitRoom">退出房间</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useUserStore } from '../stores/user';
import webSocketService from '../utils/websocket';
import axios from 'axios';
import { ElMessage } from 'element-plus';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

// 游戏信息
const gameInfo = ref({
  roomId: route.params.roomId,
  roomCode: '',
  mode: '',
  status: '',
  ownerId: '',
  memberIds: [],
  landlordId: null,
  landlordCards: [],
  currentTurnId: null,
  gamePhase: '',
  lastUpdateTimestamp: 0 // 用于跟踪数据的新鲜度
});

// 玩家信息数组（包含叫地主状态）
const players = ref([]);

// 统一映射后端数据到前端格式的函数
const mapBackendDataToFrontend = (data) => {
  if (!data) return null;
  
  // 映射游戏状态
  const mapStatus = (phase, status) => {
    if (phase) {
      // 格式1：使用phase字段
      return phase === 'ENDED'  || phase === 'SETTLEMENT'? 'ENDED' : 
             phase === 'PLAYING' || phase === 'PLAY' ? 'PLAYING' : 
             phase === 'BID' ? 'BIDDING' : 'WAITING';
    } else if (status) {
      // 格式2：直接使用status字段
      return status;
    }
    return 'WAITING'; // 默认状态
  };
  
  // 映射玩家数据
  const mapPlayers = (playersData) => {
    if (!playersData) return [];
    
    // 处理不同的玩家数据格式
    let playersList = [];
    if (typeof playersData === 'object' && !Array.isArray(playersData)) {
      // 如果是Map结构
      playersList = Object.values(playersData);
    } else if (Array.isArray(playersData)) {
      // 如果已经是List结构
      playersList = playersData;
    }
    
    // 确保最多3个玩家，并添加缺失的字段
    return playersList.slice(0, 3).map(player => ({
      ...player,
      userId: player.userId || player.id, // 处理不同的用户ID字段名
      isLandlord: player.isLandlord || false,
      bidStatus: player.bidStatus || null,
      handCards: player.handCards || []
    }));
  };
  
  // 映射游戏信息
  const gameInfo = {
    ...data,
    status: mapStatus(data.phase, data.status),
    currentTurnId: data.currentTurnUserId || data.currentTurnId || null,
    landlordCards: data.landlordCards || [],
    memberIds: data.memberIds || data.players ? Object.keys(data.players) : []
  };
  
  // 映射玩家信息
  const players = mapPlayers(data.players);
  
  return { gameInfo, players };
};

// 生成随机手牌
const generateRandomCards = () => {
  // 生成一副完整的牌
  const suits = ['S', 'H', 'D', 'C'];
  const values = ['3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K', 'A', '2'];
  let deck = [];
  
  // 生成普通牌
  for (let suit of suits) {
    for (let value of values) {
      deck.push(suit + value);
    }
  }
  
  // 添加大小王
  deck.push('BJ'); // 小王
  deck.push('RJ'); // 大王
  
  // 洗牌算法：Fisher-Yates
  for (let i = deck.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [deck[i], deck[j]] = [deck[j], deck[i]];
  }
  
  return deck;
};

// 模拟数据，用于测试
const loadMockData = () => {
  console.log('加载模拟数据...');
  
  // 生成随机牌
  const deck = generateRandomCards();
  
  // 模拟玩家信息 (3个玩家)
  const myUserId = userStore.userInfo?.id || 3;
  const otherUserIds = [1, 2];
  
  // 随机选择地主
  const landlordIndex = Math.floor(Math.random() * 3);
  const landlordId = [otherUserIds[0], otherUserIds[1], myUserId][landlordIndex];
  
  // 分配牌：地主20张，农民17张
  const landlordCards = deck.slice(0, 3); // 地主牌
  const playerCards = [
    deck.slice(3, 20),  // 玩家1: 17张
    deck.slice(20, 37), // 玩家2: 17张
    deck.slice(37)      // 我: 20张（如果是地主）或17张（如果是农民）
  ];
  
  // 调整我的牌数（如果是农民）
  if (myUserId !== landlordId) {
    playerCards[2] = deck.slice(37, 54); // 17张
  }
  
  // 模拟后端数据格式
  const mockBackendData = {
    roomCode: '123456',
    mode: 'CLASSIC',
    phase: 'PLAYING',
    ownerId: otherUserIds[0],
    memberIds: [otherUserIds[0], otherUserIds[1], myUserId],
    landlordId: landlordId,
    landlordCards: landlordCards,
    currentTurnUserId: myUserId,
    players: {
      [otherUserIds[0]]: {
        userId: otherUserIds[0],
        username: '玩家1',
        handCards: sortCards(playerCards[0]),
        isLandlord: otherUserIds[0] === landlordId
      },
      [otherUserIds[1]]: {
        userId: otherUserIds[1],
        username: '玩家2',
        handCards: sortCards(playerCards[1]),
        isLandlord: otherUserIds[1] === landlordId
      },
      [myUserId]: {
        userId: myUserId,
        username: '我',
        handCards: sortCards(playerCards[2]),
        isLandlord: myUserId === landlordId
      }
    }
  };
  
  // 使用统一映射函数处理模拟数据
  const mappedData = mapBackendDataToFrontend(mockBackendData);
  
  if (mappedData) {
    // 更新游戏信息
    gameInfo.value = {
      ...gameInfo.value,
      ...mappedData.gameInfo
    };
    
    // 更新玩家信息
    players.value = mappedData.players;
    
    // 模拟用户信息映射表
    userMap.value = {
      [String(otherUserIds[0])]: '玩家1',
      [String(otherUserIds[1])]: '玩家2',
      [String(myUserId)]: '我'
    };
    
    // 模拟我的手牌
    const myPlayer = mappedData.players.find(p => p.userId === myUserId);
    if (myPlayer) {
      myHandCards.value = myPlayer.handCards;
    }
  }
  
  console.log('模拟数据加载完成:', {
    gameInfo: gameInfo.value,
    players: players.value,
    myHandCards: myHandCards.value,
    userMap: userMap.value
  });
};

// 用户信息映射表
const userMap = ref({});

// 我的手牌
const myHandCards = ref([]);

// 选中的牌
const selectedCards = ref([]);

// 最后出的牌
const lastPlayedCards = ref([]);
const lastPlayedPlayerId = ref(null);

// 游戏消息
const gameMessage = ref('游戏开始');

// 倒计时相关
const remainingTime = ref(0);
const timerInterval = ref(null);

// 更新倒计时
const updateCountdown = async () => {
  if (!gameInfo.value.turnDeadlineEpochMillis) {
    remainingTime.value = 0;
    return;
  }
  
  const now = Date.now();
  const deadline = gameInfo.value.turnDeadlineEpochMillis;
  const timeLeft = Math.max(0, Math.floor((deadline - now) / 1000));
  remainingTime.value = timeLeft;
  
  // 如果倒计时结束且是当前用户回合，自动执行不出牌
  if (timeLeft === 0 && String(gameInfo.value.currentTurnId) === String(userStore.userInfo?.id)) {
    gameMessage.value = '时间到！自动不出牌...';
    // 自动执行不出牌操作
    try {
      await passCards();
    } catch (error) {
      console.error('自动不出牌失败:', error);
    }
  }
};

// 启动倒计时
const startCountdown = () => {
  // 清除之前的定时器
  if (timerInterval.value) {
    clearInterval(timerInterval.value);
  }
  
  // 更新一次倒计时
  updateCountdown();
  
  // 设置每秒更新一次
  timerInterval.value = setInterval(updateCountdown, 1000);
};

// 停止倒计时
const stopCountdown = () => {
  if (timerInterval.value) {
    clearInterval(timerInterval.value);
    timerInterval.value = null;
  }
};

// 计算属性：其他玩家（不包括当前用户）
const otherPlayers = computed(() => {
  const currentUserId = userStore.userInfo?.id;
  if (!currentUserId) return [];
  // 过滤出其他玩家
  const others = players.value.filter(p => String(p.userId) !== String(currentUserId));
  // 确保最多只有2个其他玩家
  return others.slice(0, 2);
});

// 计算属性：游戏状态文本
const gameStatusText = computed(() => {
  const statusMap = {
    'WAITING': '等待中',
    'PLAYING': '游戏中',
    'ENDED': '已结束'
  };
  return statusMap[gameInfo.value.status] || gameInfo.value.status;
});

// 计算属性：游戏结果
const gameResult = computed(() => {
  if (gameInfo.value.status !== 'ENDED') return '';
  
  // 这里需要根据游戏结果判断胜负
  // 假设后端会返回winnerId或类似字段
  if (String(gameInfo.value.winnerId) === String(userStore.userInfo?.id)) {
    return '恭喜你，获胜了！';
  } else {
    return '很遗憾，游戏失败！';
  }
});

// 获取游戏信息功能已通过WebSocket实现

// 卡片选择
const toggleCardSelection = (card) => {
  if (gameInfo.value.currentTurnId !== userStore.userInfo?.id) return;
  
  const index = selectedCards.value.indexOf(card);
  if (index > -1) {
    selectedCards.value.splice(index, 1);
  } else {
    selectedCards.value.push(card);
  }
};

// 获取牌面值
const getCardValue = (card) => {
  // 处理大小王
  if (card === 'BJ' || card === 'RJ') {
    return card;
  }
  return card.length === 3 ? card.substring(1, 3) : card.substring(1);
};

// 获取牌值对应的数字（用于比较）
const getCardNumber = (value) => {
  const numberMap = {
    '3': 3, '4': 4, '5': 5, '6': 6, '7': 7, '8': 8, '9': 9, '10': 10,
    'J': 11, 'Q': 12, 'K': 13, 'A': 14, '2': 15, 'BJ': 16, 'RJ': 17
  };
  return numberMap[value] || 0;
};

// 分析牌型
const analyzeCardPattern = (cards) => {
  if (!cards || cards.length === 0) return { type: 'INVALID', description: '无效牌型' };
  
  // 统计每个牌值的数量
  const cardCounts = {};
  for (const card of cards) {
    const value = getCardValue(card);
    cardCounts[value] = (cardCounts[value] || 0) + 1;
  }
  
  const values = Object.keys(cardCounts);
  const counts = Object.values(cardCounts);
  
  // 单张
  if (cards.length === 1) {
    return { type: 'SINGLE', description: '单张' };
  }
  
  // 对子
  if (cards.length === 2 && counts.every(count => count === 2)) {
    return { type: 'PAIR', description: '对子' };
  }
  
  // 三张
  if (cards.length === 3 && counts.every(count => count === 3)) {
    return { type: 'TRIPLE', description: '三张' };
  }
  
  // 三带一
  if (cards.length === 4) {
    const tripletValues = values.filter(value => cardCounts[value] === 3);
    const singleValues = values.filter(value => cardCounts[value] === 1);
    if (tripletValues.length === 1 && singleValues.length === 1) {
      return { type: 'TRIPS_WITH_SINGLE', description: '三带一' };
    }
  }
  
  // 三带二
  if (cards.length === 5) {
    const tripletValues = values.filter(value => cardCounts[value] === 3);
    const pairValues = values.filter(value => cardCounts[value] === 2);
    if (tripletValues.length === 1 && pairValues.length === 1) {
      return { type: 'TRIPS_WITH_PAIR', description: '三带二' };
    }
  }
  
  // 炸弹（四张相同）
  if (cards.length === 4 && counts.every(count => count === 4)) {
    return { type: 'BOMB', description: '炸弹' };
  }
  
  // 王炸
  if (cards.length === 2) {
    const isJokerBomb = values.includes('BJ') && values.includes('RJ');
    if (isJokerBomb) {
      return { type: 'ROYAL_BOMB', description: '王炸' };
    }
  }
  
  // 顺子（至少5张连续单牌）
  if (cards.length >= 5 && counts.every(count => count === 1)) {
    // 将牌值转换为数字并排序
    const numbers = values.map(v => getCardNumber(v)).sort((a, b) => a - b);
    
    // 检查是否连续
    for (let i = 1; i < numbers.length; i++) {
      if (numbers[i] - numbers[i - 1] !== 1) {
        return { type: 'INVALID', description: '无效顺子' };
      }
    }
    
    return { type: 'SEQUENCE', description: `顺子 (${cards.length}张)` };
  }
  
  // 连对（拖拉机，至少3对连续的对子）
  if (cards.length >= 6 && cards.length % 2 === 0 && counts.every(count => count === 2)) {
    // 将牌值转换为数字并排序
    const numbers = values.map(v => getCardNumber(v)).sort((a, b) => a - b);
    
    // 检查是否连续
    for (let i = 1; i < numbers.length; i++) {
      if (numbers[i] - numbers[i - 1] !== 1) {
        return { type: 'INVALID', description: '无效连对' };
      }
    }
    
    const pairCount = cards.length / 2;
    return { type: 'STRAIGHT_PAIRS', description: `连对 (${pairCount}对)` };
  }
  
  // 飞机牌型（连续的三张，至少两组）
  // 检查是否有连续的三张牌组
  const tripletValues = values.filter(value => cardCounts[value] === 3);
  if (tripletValues.length >= 2) {
    // 将三张牌组的值转换为数字并排序
    const tripletNumbers = tripletValues.map(v => getCardNumber(v)).sort((a, b) => a - b);
    
    // 检查三张牌组是否连续
    let isConsecutive = true;
    for (let i = 1; i < tripletNumbers.length; i++) {
      if (tripletNumbers[i] - tripletNumbers[i - 1] !== 1) {
        isConsecutive = false;
        break;
      }
    }
    
    if (isConsecutive) {
      const tripletCount = tripletValues.length;
      const totalCards = cards.length;
      
      // 纯飞机（只有连续的三张）
      if (totalCards === tripletCount * 3) {
        return { type: 'AIRPLANE', description: `飞机 (${tripletCount}组)` };
      }
      
      // 飞机带单牌（每组三张带一张单牌）
      if (totalCards === tripletCount * 4) {
        const singleValues = values.filter(value => cardCounts[value] === 1);
        if (singleValues.length === tripletCount) {
          return { type: 'AIRPLANE_WITH_SINGLE', description: `飞机带单牌 (${tripletCount}组)` };
        }
      }
      
      // 飞机带对子（每组三张带一对子）
      if (totalCards === tripletCount * 5) {
        const pairValues = values.filter(value => cardCounts[value] === 2);
        if (pairValues.length === tripletCount) {
          return { type: 'AIRPLANE_WITH_PAIRS', description: `飞机带对子 (${tripletCount}组)` };
        }
      }
    }
  }
  
  return { type: 'INVALID', description: '无效牌型' };
};

// 验证牌型是否合法
const validateCards = (cards) => {
  if (!cards || cards.length === 0) {
    gameMessage.value = '请选择要出的牌';
    return false;
  }
  
  // 1. 检查是否所有牌都在玩家的手牌中
  for (const card of cards) {
    if (!myHandCards.value.includes(card)) {
      gameMessage.value = '不能出不属于你的牌';
      return false;
    }
  }
  
  // 2. 检查牌型是否合法
  const pattern = analyzeCardPattern(cards);
  if (pattern.type === 'INVALID') {
    gameMessage.value = pattern.description;
    return false;
  }
  
  // 3. 检查是否能压过上一手牌
  if (lastPlayedCards.value.length > 0) {
    // 获取上一手牌型
    const lastPattern = analyzeCardPattern(lastPlayedCards.value);
    
    // 简单的压牌规则：王炸最大，炸弹次之，其他牌型需要同类型且数值更大
    if (pattern.type === 'ROYAL_BOMB') {
      // 王炸最大，可以压任何牌
    } else if (lastPattern.type === 'ROYAL_BOMB') {
      // 无法压王炸
      gameMessage.value = '无法压过王炸';
      return false;
    } else if (pattern.type === 'BOMB' && lastPattern.type !== 'BOMB') {
      // 炸弹可以压非炸弹
    } else if (pattern.type === 'BOMB' && lastPattern.type === 'BOMB') {
      // 炸弹之间比大小
      const currentValue = Math.max(...cards.map(card => getCardNumber(getCardValue(card))));
      const lastValue = Math.max(...lastPlayedCards.value.map(card => getCardNumber(getCardValue(card))));
      if (currentValue <= lastValue) {
        gameMessage.value = '炸弹大小不够';
        return false;
      }
    } else {
      // 同类型牌比较
      if (pattern.type !== lastPattern.type || cards.length !== lastPlayedCards.value.length) {
        gameMessage.value = '牌型不匹配，无法压过';
        return false;
      }
      
      // 比较牌值大小
      const currentValue = Math.max(...cards.map(card => getCardNumber(getCardValue(card))));
      const lastValue = Math.max(...lastPlayedCards.value.map(card => getCardNumber(getCardValue(card))));
      if (currentValue <= lastValue) {
        gameMessage.value = '牌值不够大，无法压过';
        return false;
      }
    }
  }
  
  return true;
};

// 出牌
const playCards = async () => {
  if (selectedCards.value.length === 0) {
    gameMessage.value = '请选择要出的牌';
    return;
  }
  
  // 前端验证牌型
  if (!validateCards(selectedCards.value)) {
    return;
  }
  
  try {
    // 使用WebSocket发送出牌消息
    await webSocketService.send(
      `/app/room/${gameInfo.value.roomId}/play`,
      {},
      { cards: selectedCards.value }
    );
    
    // 清空选择
    selectedCards.value = [];
  } catch (error) {
    console.error('出牌失败:', error);
    gameMessage.value = '出牌失败，请稍后重试';
  }
};

// 不出牌
const passCards = async () => {
  try {
    // 使用WebSocket发送不出牌消息
    await webSocketService.send(
      `/app/room/${gameInfo.value.roomId}/pass`,
      {},
      {}
    );
  } catch (error) {
    console.error('不出牌失败:', error);
    gameMessage.value = '不出牌失败，请稍后重试';
  }
};





// 叫地主
const bidLandlord = async (bidAmount) => {
  try {
    // 转换参数：>0表示叫地主，=0表示不叫
    const callLandlord = bidAmount > 0;
    // 使用WebSocket发送叫地主消息
    await webSocketService.send(
      `/app/room/${gameInfo.value.roomId}/bid`,
      {},
      {
        callLandlord: callLandlord
      }
    );
    // 可以添加一些用户反馈，比如显示正在处理
    gameMessage.value = callLandlord ? `正在叫地主(${bidAmount}分)...` : '正在不叫...';
  } catch (error) {
    console.error('叫地主失败:', error);
    gameMessage.value = '叫地主失败，请稍后重试';
  }
};

// 返回房间
const returnToRoom = () => {
  router.push(`/room/${gameInfo.value.roomId}`);
};

// 再来一局
const playAgain = () => {
  // 返回房间，让用户重新准备开始游戏
  returnToRoom();
};

// 退出房间
const exitRoom = async () => {
  try {
    await axios.post(`/api/game/rooms/${gameInfo.value.roomId}/leave`);
    router.push('/');
    ElMessage.success('已退出房间');
  } catch (error) {
    console.error('退出房间失败:', error);
    ElMessage.error('退出房间失败：' + error.response?.data?.message || '网络错误');
    // 即使调用失败也返回首页
    router.push('/');
  }
};

// 卡片排序
const sortCards = (cards) => {
  // 卡片排序逻辑：按牌型和大小排序
  // 假设卡片格式为 "花色+数字"，例如 "SA" 表示黑桃A
  const cardOrder = {
      '3': 3, '4': 4, '5': 5, '6': 6, '7': 7, '8': 8, '9': 9, '10': 10,
      'J': 11, 'Q': 12, 'K': 13, 'A': 14, '2': 15, 'BJ': 16, 'RJ': 17
    };
  
  return [...cards].sort((a, b) => {
    const aValue = a.substring(1);
    const bValue = b.substring(1);
    const aOrder = cardOrder[aValue] || 0;
    const bOrder = cardOrder[bValue] || 0;
    
    if (aOrder !== bOrder) {
      return aOrder - bOrder;
    }
    
    // 同大小按花色排序
    const suitOrder = { 'S': 1, 'H': 2, 'D': 3, 'C': 4 };
    const aSuit = a.charAt(0);
    const bSuit = b.charAt(0);
    return suitOrder[aSuit] - suitOrder[bSuit];
  });
};

// 卡片样式类
const cardClass = (card) => {
  if (!card) return '';
  
  // 处理大小王
  if (card === 'BJ') {
    return 'joker bj';
  } else if (card === 'RJ') {
    return 'joker rj';
  }
  
  const suit = card.charAt(0);
  const suitMap = {
    'S': 'spade',
    'H': 'heart',
    'D': 'diamond',
    'C': 'club'
  };
  
  return suitMap[suit] || '';
};

// 卡片显示名称
const cardName = (card) => {
  if (!card) return '';
  
  // 处理大小王
  if (card === 'BJ') return '🃏';
  if (card === 'RJ') return '🃏';
  
  const suit = card.charAt(0);
  const value = card.substring(1);
  
  const suitSymbols = {
    'S': '♠',
    'H': '♥',
    'D': '♦',
    'C': '♣'
  };
  
  return `${suitSymbols[suit]}${value}`;
};

// 获取玩家名称
const getPlayerName = (userId) => {
  if (!userId) return '未知玩家';
  
  // 将用户ID转换为字符串键，确保与userMap的键类型一致
  const userIdStr = String(userId);
  
  // 首先从用户信息映射表中查找
  if (userMap.value[userIdStr]) {
    return userMap.value[userIdStr];
  }
  
  // 然后从玩家信息中查找名称
  const player = players.value.find(p => p.userId === userId);
  if (player) {
    if (player.userInfo && player.userInfo.username) {
      return player.userInfo.username;
    } else if (player.username) {
      return player.username;
    } else if (player.nickname) {
      return player.nickname;
    }
  }
  
  return `玩家${userId}`;
};

// 更新游戏消息
const updateGameMessage = () => {
  if (gameInfo.value.status === 'ENDED') {
    gameMessage.value = gameResult.value;
    return;
  }
  
  if (gameInfo.value.gamePhase === 'BIDDING') {
    gameMessage.value = '正在叫地主...';
    return;
  }
  
  if (gameInfo.value.currentTurnId) {
    if (gameInfo.value.currentTurnId === userStore.userInfo?.id) {
      gameMessage.value = '轮到你出牌';
    } else {
      gameMessage.value = `轮到${getPlayerName(gameInfo.value.currentTurnId)}出牌`;
    }
  } else {
    gameMessage.value = '游戏进行中...';
  }
};

// 监听游戏状态变化
watch(
  () => gameInfo.value,
  (newVal) => {
    updateGameMessage();
    updateCountdown();
    
    // 根据游戏状态启动或停止倒计时
    if (newVal.status === 'ENDED') {
      stopCountdown();
    } else if (newVal.status === 'BIDDING' || newVal.status === 'PLAYING') {
      // 如果有截止时间，启动倒计时
      if (newVal.turnDeadlineEpochMillis) {
        startCountdown();
      }
    }
  },
  { deep: true }
);

// 游戏信息刷新定时器（备用，仅在WebSocket连接失败时使用）
let refreshInterval = null;

// 启动自动刷新机制
const startAutoRefresh = () => {
  // 清除之前的定时器
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
  
  // 每30秒自动刷新一次游戏信息
  refreshInterval = setInterval(async () => {
    try {
      // 检查WebSocket连接状态
      if (!webSocketService.isConnected && gameInfo.value.roomId) {
        console.log('WebSocket连接已断开，尝试获取最新游戏信息...');
        await webSocketService.send(`/app/room/${gameInfo.value.roomId}/snapshot`);
      }
    } catch (error) {
      console.error('自动刷新失败:', error);
    }
  }, 30000);
};

// WebSocket服务实例（直接使用导入的单例）
let subscriptions = [];

// 组件挂载时获取游戏信息
onMounted(async () => {
  if (!userStore.isAuthenticated) {
    router.push('/login');
    return;
  }
  
  try {
    // 先获取当前登录用户的信息
    await userStore.getUserInfo();
    
    // 建立WebSocket连接
    await webSocketService.connect();
    
    // 心跳机制现在由WebSocketService内部管理
    
    // 订阅游戏快照主题（个人专属队列）
    const snapshotSubscription = webSocketService.subscribe(
      `/user/queue/room/${gameInfo.value.roomId}/snapshot`,
      (message) => {
        console.log('收到快照消息:', message);
        // 检查消息格式，兼容不同的后端返回格式
        if (message.type === 'SNAPSHOT' && message.payload) {
          // 格式3：{type: 'SNAPSHOT', payload: {...游戏数据...}, timestamp: ...}
          handleGameSnapshot(message.payload);
        } else if (message.eventType === 'SNAPSHOT' && message.data) {
          // 格式1：{eventType: 'SNAPSHOT', data: {...游戏数据...}}
          handleGameSnapshot(message.data);
        } else if (message && typeof message === 'object') {
          // 格式2：直接返回游戏数据对象
          handleGameSnapshot(message);
        } else {
          console.error('快照消息格式未知:', message);
        }
      }
    );
    subscriptions.push(snapshotSubscription);
    
    // 订阅游戏事件主题（主题路径与后端保持一致：使用复数rooms）
    const eventSubscription = webSocketService.subscribe(
      `/topic/rooms/${gameInfo.value.roomId}`,
      handleGameEvent
    );
    subscriptions.push(eventSubscription);
    
    // 订阅错误主题
    const errorSubscription = webSocketService.subscribe(
      `/user/queue/errors`,
      (message) => {
        if (message.eventType === 'ERROR' && message.data) {
          gameMessage.value = message.data.message || '操作失败';
        }
      }
    );
    subscriptions.push(errorSubscription);
    
      // 无论游戏状态如何，都先尝试获取游戏快照
    try {
      console.log('尝试获取游戏快照...');
      await webSocketService.send(`/app/room/${gameInfo.value.roomId}/snapshot`);
      
      // 启动自动刷新机制
      startAutoRefresh();
      
      // 优化的快照获取重试机制
      let retryCount = 0;
      const maxRetries = 8; // 增加重试次数
      let baseDelay = 1000; // 初始延迟1秒
      const maxDelay = 16000; // 最大延迟16秒
      const retryTimeout = 30000; // 总超时时间30秒
      let retryTimer;
      
      // 指数退避重试函数
      const retrySnapshot = async () => {
        if (gameInfo.value.status !== '') {
          console.log(`游戏快照已成功获取，状态: ${gameInfo.value.status}`);
          return;
        }
        
        retryCount++;
        if (retryCount > maxRetries) {
          console.log(`已达到最大重试次数 (${maxRetries})，停止重试`);
          return;
        }
        
        // 指数退避算法：baseDelay * 2^(retryCount-1)，但不超过maxDelay
        const delay = Math.min(baseDelay * Math.pow(2, retryCount - 1), maxDelay);
        console.log(`第 ${retryCount}/${maxRetries} 次尝试重新获取游戏快照，延迟 ${delay}ms...`);
        
        retryTimer = setTimeout(async () => {
          try {
            // 检查WebSocket连接状态
            if (webSocketService.isConnected) {
              await webSocketService.send(`/app/room/${gameInfo.value.roomId}/snapshot`);
              retrySnapshot(); // 继续重试直到成功或达到最大次数
            } else {
              console.log('WebSocket连接已断开，尝试重新连接...');
              await webSocketService.connect();
              await webSocketService.send(`/app/room/${gameInfo.value.roomId}/snapshot`);
              retrySnapshot(); // 继续重试直到成功或达到最大次数
            }
          } catch (error) {
            console.error(`第 ${retryCount} 次获取快照失败:`, error);
            retrySnapshot(); // 即使出错也继续重试
          }
        }, delay);
      };
      
      // 启动重试机制
      retrySnapshot();
      
      // 总超时控制
      setTimeout(() => {
        if (retryTimer) {
          clearTimeout(retryTimer);
          console.log('快照获取总超时，强制停止重试机制');
          
          if (gameInfo.value.status === '') {
            console.error('获取游戏快照失败，服务器无响应');
            // 可以添加错误提示给用户
            gameMessage.value = '获取游戏数据失败，请检查网络连接或刷新页面';
          } else {
            console.log(`超时前已成功获取游戏快照，状态: ${gameInfo.value.status}`);
          }
        }
      }, retryTimeout);
      
    } catch (error) {
      console.error('发送快照请求失败:', error);
      // 立即启动重试机制
      retrySnapshot();
    }
  } catch (error) {
    console.error('获取游戏数据失败:', error);
    // 处理错误，尝试重新建立连接和获取数据
    setTimeout(async () => {
      try {
        // 重新检查认证状态
        if (!userStore.isAuthenticated) {
          router.push('/login');
          return;
        }
        
        // 重新建立WebSocket连接
        await webSocketService.connect();
        
        // 重新订阅主题
        // 订阅游戏快照主题
        const snapshotSubscription = webSocketService.subscribe(
          `/user/queue/room/${gameInfo.value.roomId}/snapshot`,
          (message) => {
            console.log('收到快照消息:', message);
            // 检查消息格式，兼容不同的后端返回格式
            if (message.eventType === 'SNAPSHOT' && message.data) {
              // 格式1：{eventType: 'SNAPSHOT', data: {...游戏数据...}}
              handleGameSnapshot(message.data);
            } else if (message && typeof message === 'object') {
              // 格式2：直接返回游戏数据对象
              handleGameSnapshot(message);
            } else {
              console.error('快照消息格式未知:', message);
            }
          }
        );
        subscriptions.push(snapshotSubscription);
        
        // 订阅游戏事件主题
        const eventSubscription = webSocketService.subscribe(
          `/topic/room/${gameInfo.value.roomId}/events`,
          handleGameEvent
        );
        subscriptions.push(eventSubscription);
        
        // 请求游戏快照
        await webSocketService.send(`/app/room/${gameInfo.value.roomId}/snapshot`);
        
        console.log('重新获取游戏数据成功');
      } catch (retryError) {
        console.error('重新获取游戏数据失败:', retryError);
      }
    }, 2000);
  }
});

// 组件卸载时清除定时器和WebSocket连接
onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
  stopCountdown();
  
  // 取消所有WebSocket订阅
  subscriptions.forEach(sub => {
    if (sub) sub.unsubscribe();
  });
  
  // 断开WebSocket连接
  webSocketService.disconnect();
});

// 处理游戏快照
const handleGameSnapshot = (data) => {
  console.log('收到游戏快照:', data);
  console.log('快照数据类型:', typeof data);
  
  if (data) {
    // 生成当前时间戳作为快照的版本号
    const snapshotTimestamp = Date.now();
    
    // 检查快照是否比当前状态更新
    if (snapshotTimestamp < gameInfo.value.lastUpdateTimestamp) {
      console.warn('接收到的快照数据过时，跳过更新');
      return;
    }
    
    // 使用统一映射函数处理数据
    const mappedData = mapBackendDataToFrontend(data);
    
    if (mappedData) {
      // 更新游戏信息
      gameInfo.value = {
        ...gameInfo.value,
        ...mappedData.gameInfo,
        lastUpdateTimestamp: snapshotTimestamp // 更新时间戳
      };
      
      console.log('更新后的gameInfo:', gameInfo.value);
      console.log('更新后的gameInfo.status:', gameInfo.value.status);
      
      // 更新玩家信息
      if (mappedData.players.length > 0) {
        players.value = mappedData.players;
        
        // 构建用户信息映射表
        players.value.forEach(player => {
          if (player.userId) {
            // 将用户ID转换为字符串键
            const userIdStr = String(player.userId);
            let username = `玩家${player.userId}`;
            if (player.userInfo && player.userInfo.username) {
              username = player.userInfo.username;
            } else if (player.username) {
              username = player.username;
            } else if (player.nickname) {
              username = player.nickname;
            }
            userMap.value[userIdStr] = username;
          }
        });
      }
      
      // 更新我的手牌
      const me = players.value.find(p => String(p.userId) === String(userStore.userInfo?.id));
      if (me && me.handCards) {
        myHandCards.value = sortCards(me.handCards);
      } else {
        myHandCards.value = [];
      }
    }
    
    // 更新最后出牌信息
    if (data.lastPlay) {
      lastPlayedCards.value = data.lastPlay.cards || [];
      lastPlayedPlayerId.value = data.lastPlay.userId || null;
    } else {
      lastPlayedCards.value = [];
      lastPlayedPlayerId.value = null;
    }
    
    // 确保当前只有一个出牌者
    if (data.currentTurnUserId) {
      gameInfo.value.currentTurnId = data.currentTurnUserId;
    }
    
    // 更新游戏消息
    updateGameMessage();
    
    console.log('更新后的游戏信息:', gameInfo.value);
  }
};

// 处理游戏事件（修复事件格式解析：后端发送的是type/payload/timestamp结构）
const handleGameEvent = (data) => {
  console.log('收到游戏事件:', data);
  
  // 检查事件格式是否正确（后端发送的GameEventMessage格式：type/payload/timestamp）
  if (!data.type || !data.payload) {
    console.error('无效的游戏事件格式:', data);
    return;
  }
  
  // 生成当前时间戳作为事件的版本号
  const eventTimestamp = Date.now();
  
  // 检查事件是否比当前状态更新
  if (eventTimestamp < gameInfo.value.lastUpdateTimestamp) {
    console.warn('接收到的事件数据过时，跳过更新');
    return;
  }
  
  // 从payload中获取具体事件数据
  const eventData = data.payload;
  
  // 根据事件类型处理不同的游戏事件
  switch (data.type) {
    case 'ROOM_STARTED':
      // 游戏房间开始
      gameMessage.value = '游戏开始';
      // 触发重新获取游戏快照
      webSocketService.send(`/app/room/${gameInfo.value.roomId}/snapshot`).catch(err => {
        console.error('请求游戏快照失败:', err);
      });
      break;
    case 'TURN_START':
      // 处理回合开始事件
      gameInfo.value.currentTurnId = eventData.userId;
      gameMessage.value = `${getPlayerName(eventData.userId)}的回合`;
      // 触发重新获取游戏快照，确保所有状态更新
      webSocketService.send(`/app/room/${gameInfo.value.roomId}/snapshot`).catch(err => {
        console.error('请求游戏快照失败:', err);
      });
      break;
    case 'BID_RESULT':
      // 处理叫地主结果
      if (eventData.callLandlord) {
        gameMessage.value = `${getPlayerName(eventData.userId)}叫地主`;
      } else {
        gameMessage.value = `${getPlayerName(eventData.userId)}不叫地主`;
      }
      // 更新玩家叫地主状态
      const playerIndex = players.value.findIndex(p => p.userId === eventData.userId);
      if (playerIndex !== -1) {
        players.value[playerIndex].bidStatus = eventData.callLandlord;
      }
      // 更新叫地主状态
      if (eventData.landlordId) {
        gameInfo.value.landlordId = eventData.landlordId;
        gameInfo.value.status = 'PLAYING'; // 切换到出牌阶段
        gameMessage.value = `${getPlayerName(eventData.landlordId)}成为地主！`;
        // 更新地主玩家的isLandlord状态
        const landlordIndex = players.value.findIndex(p => p.userId === eventData.landlordId);
        if (landlordIndex !== -1) {
          players.value[landlordIndex].isLandlord = true;
        }
      }
      // 触发重新获取游戏快照，确保所有状态更新
      webSocketService.send(`/app/room/${gameInfo.value.roomId}/snapshot`).catch(err => {
        console.error('请求游戏快照失败:', err);
      });
      break;
    case 'PLAY_CARD':
      // 处理出牌事件
      gameMessage.value = `${getPlayerName(eventData.userId)}出了牌`;
      lastPlayedCards.value = eventData.cards || [];
      lastPlayedPlayerId.value = eventData.userId || null;
      // 更新当前回合信息
      if (eventData.nextTurnId) {
        gameInfo.value.currentTurnId = eventData.nextTurnId;
      }
      // 更新玩家手牌（如果是当前用户出牌）
      if (String(eventData.userId) === String(userStore.userInfo?.id)) {
        // 减少当前用户的手牌数量
        const me = players.value.find(p => String(p.userId) === String(userStore.userInfo?.id));
        if (me && me.handCards) {
          // 过滤掉已经出的牌
          me.handCards = me.handCards.filter(card => !eventData.cards?.includes(card));
          // 重新排序并更新手牌
          myHandCards.value = sortCards(me.handCards);
        }
      }
      // 触发重新获取游戏快照，确保所有状态更新
      webSocketService.send(`/app/room/${gameInfo.value.roomId}/snapshot`).catch(err => {
        console.error('请求游戏快照失败:', err);
      });
      break;
    case 'PASS':
      // 处理不出牌事件
      gameMessage.value = `${getPlayerName(eventData.userId)}不出牌`;
      // 更新当前回合信息
      if (eventData.nextTurnId) {
        gameInfo.value.currentTurnId = eventData.nextTurnId;
      }
      // 触发重新获取游戏快照，确保所有状态更新
      webSocketService.send(`/app/room/${gameInfo.value.roomId}/snapshot`).catch(err => {
        console.error('请求游戏快照失败:', err);
      });
      break;
    case 'SURRENDER':
      // 处理投降事件
      gameMessage.value = `${getPlayerName(eventData.userId)}投降了`;
      // 触发重新获取游戏快照，确保所有状态更新
      webSocketService.send(`/app/room/${gameInfo.value.roomId}/snapshot`).catch(err => {
        console.error('请求游戏快照失败:', err);
      });
      break;
    case 'GAME_RESULT':
      // 处理游戏结果事件
      gameMessage.value = `游戏结束！${eventData.winnerSide === 'LANDLORD' ? '地主' : '农民'}获胜！`;
      gameInfo.value.status = 'ENDED';
      gameInfo.value.winnerId = eventData.winnerId;
      // 停止倒计时
      stopCountdown();
      // 触发重新获取游戏快照，确保所有状态更新
      webSocketService.send(`/app/room/${gameInfo.value.roomId}/snapshot`).catch(err => {
        console.error('请求游戏快照失败:', err);
      });
      break;
    case 'TURN_CHANGED':
      // 处理回合切换事件
      if (gameInfo.value.status === 'BIDDING') {
        gameMessage.value = `${getPlayerName(eventData.userId)}请叫地主`;
      } else {
        gameMessage.value = `${getPlayerName(eventData.userId)}请出牌`;
      }
      gameInfo.value.currentTurnId = eventData.userId;
      gameInfo.value.turnDeadlineEpochMillis = eventData.turnDeadline;
      // 启动倒计时
      startCountdown();
      break;
    case 'PLAY_TIMEOUT':
      // 处理出牌超时事件
      gameMessage.value = `${getPlayerName(eventData.userId)}出牌超时，自动不出`;
      // 更新当前回合信息
      if (eventData.nextTurnId) {
        gameInfo.value.currentTurnId = eventData.nextTurnId;
      }
      // 触发重新获取游戏快照，确保所有状态更新
      webSocketService.send(`/app/room/${gameInfo.value.roomId}/snapshot`).catch(err => {
        console.error('请求游戏快照失败:', err);
      });
      break;
    default:
      console.log('未处理的游戏事件类型:', data.type);
      break;
  }
  
  // 更新游戏状态的时间戳
  gameInfo.value.lastUpdateTimestamp = eventTimestamp;
};



// 心跳机制现在由WebSocketService内部管理


</script>

<style scoped>
.game-play-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  background-color: #2c6e49;
  color: white;
  overflow: hidden;
}

.game-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background-color: rgba(0, 0, 0, 0.2);
}

.game-header h2 {
  margin: 0;
  font-size: 1.5rem;
}

.game-info {
  display: flex;
  gap: 20px;
}

.room-code, .game-status {
  padding: 5px 10px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 5px;
}

.game-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 20px;
  gap: 20px;
  overflow: hidden;
  position: relative;
}

.player-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.top-player, .bottom-player {
  flex: 1;
  max-height: 25%;
}

.left-player, .right-player {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  max-height: 50%;
}

.left-player {
  left: 20px;
}

.right-player {
  right: 20px;
}

.player-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.player-name {
  font-size: 1rem;
  font-weight: bold;
}

.player-status {
  display: flex;
  gap: 10px;
  align-items: center;
}

.landlord-tag {
  background-color: #ffc107;
  color: #000;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.8rem;
  font-weight: bold;
}

.card-count {
  background-color: rgba(0, 0, 0, 0.3);
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.8rem;
}

.player-cards {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  justify-content: center;
}

.card {
  width: 50px;
  height: 70px;
  border-radius: 5px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.card.back {
  background-color: #8B0000;
  border: 2px solid #650000;
}

.card.back.vertical {
  transform: rotate(90deg);
}

.card.spade, .card.club {
  background-color: #fff;
  color: #000;
  border: 2px solid #ccc;
  background-image: linear-gradient(135deg, #f5f5f5 0%, #ffffff 100%);
}

.card.heart, .card.diamond {
  background-color: #fff;
  color: #ff0000;
  border: 2px solid #ccc;
  background-image: linear-gradient(135deg, #fff5f5 0%, #ffffff 100%);
}

.card.selected {
  transform: translateY(-15px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.4);
  border: 3px solid #ffc107;
}

/* 大小王样式 */
.card.joker {
  font-size: 1.5rem;
  border: 2px solid #ccc;
}

/* 小王（BJ）样式 - 黑色背景 */
.card.joker.bj {
  background-color: #333;
  color: white;
  background-image: linear-gradient(135deg, #222 0%, #444 100%);
}

/* 大王（RJ）样式 - 红色背景 */
.card.joker.rj {
  background-color: #dc3545;
  color: white;
  background-image: linear-gradient(135deg, #c82333 0%, #dc3545 100%);
}

.my-cards {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  justify-content: center;
  max-width: 100%;
  overflow-x: auto;
  padding: 10px 0;
}

.game-center {
  flex: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  position: relative;
}

.landlord-cards {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.landlord-cards-label {
  font-weight: bold;
  font-size: 1.2rem;
}

.landlord-cards-list {
  display: flex;
  gap: 5px;
}

.game-message {
  font-size: 1.5rem;
  font-weight: bold;
  text-align: center;
}

.current-turn {
  background-color: #ffc107;
  color: #000;
  padding: 10px 20px;
  border-radius: 20px;
  font-weight: bold;
  font-size: 1.2rem;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 20px;
  background-color: rgba(0, 0, 0, 0.2);
  z-index: 100;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 5px;
  font-size: 1rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 100px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.btn-primary {
  background-color: #007bff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #0056b3;
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0, 123, 255, 0.4);
}

.btn-primary:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 5px rgba(0, 123, 255, 0.4);
}

.btn-primary:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
  opacity: 0.6;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background-color: #545b62;
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(108, 117, 125, 0.4);
}

.btn-secondary:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 5px rgba(108, 117, 125, 0.4);
}

.btn-secondary:disabled {
  background-color: #adb5bd;
  cursor: not-allowed;
  opacity: 0.6;
}

.last-played {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
}

.last-played-label {
  font-size: 0.8rem;
  color: #ccc;
}

.last-played-cards {
  display: flex;
  gap: 5px;
}

.game-over {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.game-over-content {
  background-color: white;
  color: #000;
  padding: 30px;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.game-over-content h3 {
  margin: 0;
  font-size: 1.5rem;
  color: #dc3545;
}

.return-to-room {
  margin-top: 20px;
}
</style>