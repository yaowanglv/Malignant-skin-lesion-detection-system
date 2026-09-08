import { createRouter, createWebHistory } from 'vue-router'


const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
      // {path: '/', redirect: '/manager/bjxx'},
      {path: '/', redirect: '/login'},
      {path: '/manager', component: () =>import('../views/Manager.vue'),
       children:[
         {path: '', redirect: '/manager/dataview'},
         {path: 'admin', meta:{name:'管理员信息', requiresAdmin: true},component: () =>import('../views/Admin.vue'),},
         {path: 'detect', meta:{name:'皮肤癌检测与分析'} ,component: () =>import('../views/Detect.vue'),},
         {path: 'video', meta:{name:'皮肤癌视频检测与分析'} ,component: () =>import('../views/Video.vue'),},
         {path: 'imagetovideo', meta:{name:'医学图像智能剪辑'} ,component: () =>import('../views/ImageToVideo.vue'),},
         {path: 'history', meta:{name:'检测历史'} ,component: () =>import('../views/History.vue'),},
         {path: 'dataview', meta:{name:'数据可视化'} ,component: () =>import('../views/Dataview.vue'),},
         {path: 'config', meta:{name:'系统配置', requiresAdmin: true} ,component: () =>import('../views/Config.vue'),},

          ]
        },

// 登录界面
    {path: '/login', component: () => import('../views/Login.vue'),},
      // 注册界面



    {path: '/notFound', name: '404', component: () => import('../views/404.vue'),},
      { path: '/notFound', component: () => import('../views/404.vue') },



  ],
})

const getLoginUser = () => {
  try {
    return JSON.parse(localStorage.getItem('code_user') || '{}')
  } catch {
    return {}
  }
}

const getToken = () => localStorage.getItem('token')

router.beforeEach((to) => {
  // 更新浏览器标签页标题
  if (to.meta?.name) {
    document.title = to.meta.name + ' - 皮肤癌智能检测与分析系统'
  } else {
    document.title = '皮肤癌智能检测与分析系统'
  }

  if (to.path === '/login') {
    if (getToken()) {
      return '/manager/dataview'
    }
    return true
  }

  if (!getToken()) {
    return '/login'
  }

  if (!to.meta?.requiresAdmin) {
    return true
  }

  const user = getLoginUser()
  if (String(user.role || '').toLowerCase() === 'admin') {
    return true
  }

  return '/manager/detect'
})

export default router
