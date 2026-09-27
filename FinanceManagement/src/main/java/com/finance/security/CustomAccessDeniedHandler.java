package com.finance.security;

import cn.hutool.json.JSONUtil;
import com.finance.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 权限不足处理
 * 当用户访问了没有权限的接口时（403 错误），
 * 返回统一的 JSON 格式错误信息，而非默认的空白页面
 */
//AccessDeniedHandler是Spring Security 提供的权限不足处理接口
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * 当一个已经登录的用户访问了他没权限的接口时（如普通用户访问 /api/v1/admin/**），
     * SecurityFilterChain 直接拦截，不会进入 Controller，而是调用这个 Handler。
     * @param request
     * @param response
     * @param accessDeniedException
     * @throws IOException
     */

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        //设置返回格式是 JSON
        response.setContentType("application/json;charset=UTF-8");
        //设置状态码 403
        response.setStatus(403);
        //返回统一格式的错误信息
        response.getWriter().write(JSONUtil.toJsonStr(Result.error(403, "无权限")));
    }
}
