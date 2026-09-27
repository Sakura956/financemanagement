import request from "@/api/request";
//导入 TS 类型，约束传参和返回值
import type {
  LoginRequest,
  LoginResult,
  RegisterRequest,
  ChangePasswordRequest,
  UpdateProfileRequest,
  UpdateAvatarRequest,
  UserInfo,
} from "@/types";

//导出一个对象 authApi，里面包含所有用户相关接口方法
export const authApi = {
  /**
   * 登录接口
   * @param data
   * @returns
   */
  login(data: LoginRequest): Promise<LoginResult> {
    return request.post("/auth/login", data);
  },

  /**
   * 注册接口
   * @param data
   * @returns
   */
  register(data: RegisterRequest): Promise<null> {
    return request.post("/auth/register", data);
  },

  /**
   * 修改密码接口
   * @param data
   * @returns
   */
  changePassword(data: ChangePasswordRequest): Promise<null> {
    return request.put("/auth/change-password", data);
  },

  /**
   * 个人信息接口
   * @param data
   * @returns
   */
  getProfile(): Promise<UserInfo> {
    return request.get("/auth/me");
  },

  /**
   * 修改个人信息接口
   * @param data
   * @returns
   */
  updateProfile(data: UpdateProfileRequest): Promise<UserInfo> {
    return request.put("/auth/profile", data);
  },

  /**
   * 修改头像，文件形式
   * @param data
   * @returns
   */
  updateAvatar(file: File): Promise<UserInfo> {
    //必须创建 FormData
    const formData = new FormData();
    //后端要求字段名：file
    formData.append("file", file);

    //请求方法 POST + 自动处理 multipart/form-data
    return request.post("/auth/avatar", formData, {
      headers: {
       'Content-Type': 'multipart/form-data'
      },
    });
  },
};
