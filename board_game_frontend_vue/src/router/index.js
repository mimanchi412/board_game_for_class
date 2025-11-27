import { createRouter, createWebHistory } from 'vue-router';

// 定义路由组件
// 注意：这里使用动态导入，实现路由懒加载
const Home = () => import('../views/Home.vue');
const Login = () => import('../views/Login.vue');
const Register = () => import('../views/Register.vue');
const GameRoom = () => import('../views/GameRoom.vue');

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
        title: '登录'
      }
    },
    {
      path: '/register',
      name: 'register',
      component: Register,
      meta: {
        title: '注册'
      }
    },
    {
      path: '/game-room/:roomId',
      name: 'gameRoom',
      component: GameRoom,
      meta: {
        title: '游戏房间'
      },
      // 动态路由参数，roomId用于标识不同的游戏房间
      props: true
    },
    // 404页面配置
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ]
});

// 路由守卫，用于设置页面标题
router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - 桌游平台`;
  }
  next();
});

export default router;
