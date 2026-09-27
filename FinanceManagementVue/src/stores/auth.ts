import { defineStore } from "pinia";
import { ref, computed } from "vue";
//所有用户相关类型
import type {
  UserInfo,
  LoginRequest,
  RegisterRequest,
  ChangePasswordRequest,
  UpdateProfileRequest,
  UpdateAvatarRequest,
} from "@/types";
//后端接口（登录、注册、获取信息...）
import { authApi } from "@/api/modules/auth";

export const useAuthStore = defineStore("auth", () => {
  const token = ref<string>(localStorage.getItem("token") || "");
  const userInfo = ref<UserInfo | null>(null);

  // 页面刷新后自动恢复用户信息
  const saved = localStorage.getItem("userInfo");
  if (saved) {
    try {
      userInfo.value = JSON.parse(saved);
    } catch {
      /* ignore */
    }
  }

  const isLoggedIn = computed(() => !!token.value); //是否登录
  const isAdmin = computed(() => userInfo.value?.role === "ADMIN"); //是否管理员

  //登录成功 → 保存状态
  function setAuth(t: string, info: UserInfo) {
    token.value = t;
    userInfo.value = info;
    localStorage.setItem("token", t);
    localStorage.setItem("userInfo", JSON.stringify(info));
  }

  //退出登录 / 清空状态
  function clearAuth() {
    token.value = "";
    userInfo.value = null;
    localStorage.removeItem("token");
    localStorage.removeItem("userInfo");
  }

  //登录接口使用
  async function login(data: LoginRequest) {
    const result = await authApi.login(data);
    setAuth(result.token, result.userInfo);
  }

  //注册接口使用
  async function register(data: RegisterRequest) {
    await authApi.register(data);
  }

  //获取用户信息（刷新页面用）使用
  async function fetchProfile() {
    const info = await authApi.getProfile();
    userInfo.value = info;
    localStorage.setItem("userInfo", JSON.stringify(info));
  }

  //修改资料接口使用
  async function updateProfile(data: UpdateProfileRequest) {
    const info = await authApi.updateProfile(data);
    userInfo.value = info;
    localStorage.setItem("userInfo", JSON.stringify(info));
  }

  // 修改头像
  async function updateAvatar(file: File) {
    // 直接传入 File 对象
    const info = await authApi.updateAvatar(file);

    userInfo.value = info;
    localStorage.setItem("userInfo", JSON.stringify(info));
  }

  //修改密码接口使用
  async function changePassword(data: ChangePasswordRequest) {
    await authApi.changePassword(data);
    clearAuth();
  }

  //退出
  function logout() {
    clearAuth();
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    login,
    register,
    fetchProfile,
    updateProfile,
    updateAvatar,
    changePassword,
    logout,
    clearAuth,
  };
});
