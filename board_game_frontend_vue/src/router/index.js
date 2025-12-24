import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '../stores/user';

// 定义路由组件
// 注意：这里使用动态导入，实现路由懒加载
const Home = () => import('../views/Home.vue');
const Login = () => import('../views/Login.vue');
const Register = () => import('../views/Register.vue');
const ResetPassword = () => import('../views/ResetPassword.vue');
const GameRoom = () => import('../views/GameRoom.vue');
const GamePlay = () => import('../views/GamePlay.vue');
const Profile = () => import('../views/Profile.vue');

// 创建路由实例
const router = createRouter({
  // 使用HTML5历史模式，去掉URL中的#
  history: createWebHistory(import.meta.env.BASE_URL),
  // 路由配置数组
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home,
      meta: {
        title: '桌游大厅'
      }
    },
    {
      path: '/login',
      name: 'login',
      component: Login,
      meta: {
        title: '登录',
        requiresAuth: false // 不需要登录
      }
    },
    {
      path: '/register',
      name: 'register',
      component: Register,
      meta: {
        title: '注册',
        requiresAuth: false // 不需要登录
      }
    },
    {
      path: '/reset-password',
      name: 'resetPassword',
      component: ResetPassword,
      meta: {
        title: '重置密码',
        requiresAuth: false
      }
    },
    {
      path: '/game-room/:roomId',
      name: 'gameRoom',
      component: GameRoom,
      meta: {
        title: '游戏房间',
        requiresAuth: true // 需要登录
      },
      // 动态路由参数，roomId用于标识不同的游戏房间
      props: true
    },
    {
      path: '/game-play/:roomId',
      name: 'gamePlay',
      component: GamePlay,
      meta: {
        title: '斗地主游戏',
        requiresAuth: true // 需要登录
      },
      // 动态路由参数，roomId用于标识不同的游戏房间
      props: true
    },
    {
      path: '/profile',
      name: 'profile',
      component: Profile,
      meta: {
        title: '个人资料',
        requiresAuth: true
      }
    },
    // 404页面配置
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ]
});

// 路由守卫，用于设置页面标题和检查登录状态
router.beforeEach((to, from, next) => {
  const userStore = useUserStore();
  
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 斗地主`;
  }
  
  // 已登录用户不允许访问登录或注册页面
  if (userStore.isAuthenticated && (to.path === '/login' || to.path === '/register')) {
    next('/'); // 重定向到首页
    return;
  }
  
  // 检查是否需要登录
  if (to.meta.requiresAuth) {
    if (userStore.isAuthenticated) {
      next(); // 已登录，继续访问
    } else {
      next('/login'); // 未登录，跳转到登录页面
    }
  } else {
    // 不需要登录的页面，直接访问
    next();
  }
});

// 路由后置守卫，用于处理用户离开房间页面时的自动退出
router.afterEach((to, from) => {
  // 当用户从游戏房间或游戏页面离开时，尝试自动退出房间
  if ((from.path.startsWith('/game-room/') || from.path.startsWith('/game-play/')) && 
      !(to.path.startsWith('/game-room/') || to.path.startsWith('/game-play/'))) {
    const roomId = from.params.roomId;
    if (roomId) {
      // 尝试自动退出房间
      try {
        import('../utils/axios').then(({ default: axios }) => {
          axios.post(`/game/room/leave/${roomId}`).catch(() => {
            // 忽略退出失败的情况
          });
        });
      } catch (error) {
        // 忽略任何错误
      }
    }
  }
});

export default router;
