package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.HttpclientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 1.给用户颁发token
 * 2.验证其他业务功能接收token（用户所携带的token）的真伪
 *
 */
@Controller
public class GmallPassPortCotroller {

    @Reference
    UserService userService;

    @RequestMapping("vlogin")
    public String vlogin(String code, HttpServletRequest request) {
        System.out.println("code = " + code);

        String client_id = "3990817128";
        String client_secret = "2e624d6abf5e4929899c29b3464e381b";
        String redirect_uri = "http://passport.gmall.com:8085/vlogin";

        // 1：首页点击新浪微博图标去授权，返回此请求并会携带授权码code
        // 2：根据code获取access_token
        String getAccessToken = "https://api.weibo.com/oauth2/access_token";
        Map<String, String> map = new HashMap<>();
        map.put("client_id", client_id);
        map.put("client_secret", client_secret);
        map.put("grant_type", "authorization_code");
        map.put("code", code);
        map.put("redirect_uri", redirect_uri);
        String accessTokenJson = HttpclientUtil.doPost(getAccessToken, map);
        Map<String, String> access_token_map = JSON.parseObject(accessTokenJson, Map.class);
        System.out.println("accessTokenJson = " + accessTokenJson);

        // 3: 根据获取到信息获取用户信息
        String access_token = String.valueOf(access_token_map.get("access_token"));
        String uid = String.valueOf(access_token_map.get("uid"));
        String getUserInfo = "https://api.weibo.com/2/users/show.json?access_token=" + access_token + "&uid=" + uid;
        String user_json = HttpclientUtil.doGet(getUserInfo);
        Map<String, Object> user_map = JSON.parseObject(user_json, Map.class);
        System.out.println("user_json = " + user_json);

        // 4: 将用户信息保存到表中，来源为微博
        UmsMember umsMember = new UmsMember();
        umsMember.setAccessToken(access_token);
        umsMember.setSourceType(2);
        umsMember.setAccessCode(code);
        umsMember.setSourceUid(String.valueOf(user_map.get("idstr")));
        umsMember.setCity((String)user_map.get("location"));
        umsMember.setNickname((String)user_map.get("screen_name"));
        int g = 0;
        String gender = (String)user_map.get("gender");
        if(gender.equals("m")){
            g = 1;
        }
        umsMember.setGender(g);

        // 先检查用户是否已经保存到表中
        UmsMember checkUmeMember = new UmsMember();
        checkUmeMember.setSourceUid(umsMember.getSourceUid());
        checkUmeMember = userService.checkOauthUser(checkUmeMember);
        if (checkUmeMember == null) {
            umsMember = userService.addOauthUser(umsMember);
            System.out.println("umsMember = " + umsMember.toString());
        } else {
            umsMember = checkUmeMember;
        }

        //5：用jwt生成token
        String token = "";
        String memberId = umsMember.getId();
        String nickname = umsMember.getNickname();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("memberId", memberId);
        userMap.put("nickname", nickname);

        String ip = request.getHeader("x-forwarded-for");
        if (StringUtils.isBlank(ip)) {
            ip = request.getRemoteAddr();
        }
        if (StringUtils.isBlank(ip)) {
            ip = "127.0.0.1";
        }
        // 按照设计的算法对参数进行加密后，生成token
        token = JwtUtil.encode("2019gmall0105", userMap, ip);
        System.out.println("token = " + token);

        userService.addUserToken(token, ip);

        return "redirect:http://cart.gmall.com:8084/toTrade?token=" + token;
    }

    @RequestMapping("verity")
    @ResponseBody
    public String verity(String token, String currentIp, HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        if (StringUtils.isBlank(token)) {
            map.put("status", "fail");
        }

        String ip = request.getHeader("x-gorwarded-for");
        if (StringUtils.isBlank(ip)) {
            ip = request.getRemoteAddr();
        }
        System.out.println(ip);

        String successJson = "";
        Map<String, Object> decode = JwtUtil.decode(token, "2019gmall0105", currentIp);
        if (CollectionUtils.isEmpty(decode)) {
            map.put("status", "fail");
        }
        map.put("status", "success");
        map.put("memberId", String.valueOf(decode.get("memberId")));
        map.put("nickname", String.valueOf(decode.get("nickname")));

        return JSON.toJSONString(map);
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request) {
        UmsMember umsMember1 = userService.login(umsMember);

        String token = "";
        if (umsMember1 == null) {
            token = "fail";
        }
        //登录成功
        Map<String, Object> map = new HashMap<>();
        map.put("status", "success");
        map.put("memberId", umsMember1.getId());
        map.put("nickname", umsMember1.getNickname());

        // 通过nginx转发的客户端
        String ip = request.getHeader("x-forwarded-for");
        if (StringUtils.isBlank(ip)) {
            // 从request中获取ip
            ip = request.getRemoteAddr();
            System.out.println("request.getRemoteAddr() = " + ip);
        }
        if (StringUtils.isBlank(ip)) {
            ip = "127.0.0.1";
        }
        token = JwtUtil.encode("2019gmall0105", map, ip);
        // 把token存到redis中
        userService.addUserToken(token, umsMember1.getId());

        return token;
    }

    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap map) {
        map.put("ReturnUrl", ReturnUrl);
        return "index";
    }

}
