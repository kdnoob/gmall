package com.atguigu.gmall.interceptors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.HttpClientUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 该方法是否有loginRequired注解
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequired loginRequired = handlerMethod.getMethodAnnotation(LoginRequired.class);

        // 没有该注解执行放行
        if (loginRequired == null) {
            return true;
        }

        String token = "";
        // 查看cookies中是否有token
        // 没有 说明以前没有登录过，有的话 说明以前登录过
        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }

        // 在查看request中有木有
        // request中有的话，说明时刚刚登陆的，此时cookie有值的话就需要覆盖掉cookie中的值
        // 没有的话，踢回去登录
        String newToken = request.getParameter("token");
        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }

        // 默认为false，当token有值时，需要去验证token
        String success = "fail";
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isNotBlank(token)) {
            String ip = request.getHeader("x-forwarded-for");// 通过nginx转发的客户端ip
            if(StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr();// 从request中获取ip
                if(StringUtils.isBlank(ip)){
                    ip = "127.0.0.1";
                }
            }
            String successJson = HttpclientUtil.doGet("http://passport.gmall.com:8085/verity?token=" + token + "&currentIp=" + ip);

            Type type = new TypeReference<Map<String,String>>() {}.getType();
            map = JSON.parseObject(successJson, type);

            success = map.get("status");
        }

        boolean loginSuccess = loginRequired.loginSuccess();
        if (loginSuccess) {
            // 需要登录，没登录则被拦截
            if ("fail".equals(success)) {
                // 没登录，拦截，踢回i认证中心去登录
                StringBuffer requestURL = request.getRequestURL();
                response.sendRedirect("http://passport.gmall.com:8085/index?ReturnUrl=" + requestURL);
                return false;
            }
            // 登录了，记录信息
            request.setAttribute("memberId", map.get("memberId"));
            request.setAttribute("nickname", map.get("nickname"));

        } else {
            // 不一定要登录，但是登录了要记录信息
            if ("success".equals(success)) {
                request.setAttribute("memberId", map.get("memberId"));
                request.setAttribute("nickname", map.get("nickname"));
            }
        }

        // token不为空，并且验证通过，要在cookie中覆盖token
        if (StringUtils.isNotBlank(token) && "success".equals(success)) {
            CookieUtil.setCookie(request, response, "oldToken", token, 60 * 60 * 2, true);
        }

        System.out.println("进入了AuthInterceptor 拦截器");
        return true;
    }
}
