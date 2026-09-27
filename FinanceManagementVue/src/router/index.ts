import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
} from "vue-router";
import { useAuthStore } from "@/stores/auth"; // 用于判断登录、角色

//路由列表 routes
const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/auth/Login.vue"),
    meta: { public: true }, //标记：公开页面，所有人可访问
  },
  {
    path: "/register",
    name: "Register",
    component: () => import("@/views/auth/Register.vue"),
    meta: { public: true },
  },
  {
    path: "/",
    component: () => import("@/components/layout/AppLayout.vue"),
    redirect: "/dashboard", // 默认跳首页
    // 所有子页面
    children: [
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("@/views/dashboard/Index.vue"),
        meta: { title: "首页" },
      },
      {
        path: "bills",
        name: "Bills",
        component: () => import("@/views/bill/BillList.vue"),
        meta: { title: "收支账单" },
      },
      {
        path: "finance-plans",
        name: "FinancePlans",
        component: () => import("@/views/financePlan/Index.vue"),
        meta: { title: "理财计划" },
      },
      {
        path: "statistics",
        name: "Statistics",
        component: () => import("@/views/statistics/Index.vue"),
        meta: { title: "统计分析" },
      },
      {
        path: "memos",
        name: "Memos",
        component: () => import("@/views/memo/Index.vue"),
        meta: { title: "备忘录" },
      },
      {
        path: "ai",
        name: "AIChat",
        component: () => import("@/views/ai/Chat.vue"),
        meta: { title: "AI 助手" },
      },
      {
        path: "profile",
        name: "Profile",
        component: () => import("@/views/profile/Index.vue"),
        meta: { title: "个人设置" },
      },
      {
        path: "admin/users",
        name: "AdminUsers",
        component: () => import("@/views/admin/UserList.vue"),
        meta: { title: "用户管理", role: "ADMIN" },//只有管理员能访问
      },
      {
        path: "admin/categories",
        name: "AdminCategories",
        component: () => import("@/views/admin/CategoryList.vue"),
        meta: { title: "分类管理", role: "ADMIN" },
      },
      {
        path: "admin/dashboard",
        name: "AdminDashboard",
        component: () => import("@/views/admin/Dashboard.vue"),
        meta: { title: "管理后台", role: "ADMIN" },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

//全局路由守卫
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore();

  // 已登录的用户，不能再去登录页，直接送回首页
  if (to.meta.public) {
    if (
      authStore.isLoggedIn &&
      (to.name === "Login" || to.name === "Register")
    ) {
      return next("/dashboard");
    }
    return next();
  }

  //未登录强制跳转登录
  if (!authStore.isLoggedIn) {
    return next("/login");
  }

  // 管理员权限校验
  if (to.meta.role === "ADMIN" && !authStore.isAdmin) {
    return next("/dashboard");
  }

  next();
});

export default router;
