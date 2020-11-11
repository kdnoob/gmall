package com.atguigu.gmall.util;

import com.alibaba.fastjson.JSON;
import com.sun.xml.internal.ws.resources.UtilMessages;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TestJwt {

    public static void main(String[] args) {
//        Map<String,Object> map = new HashMap<>();
//        map.put("memberId","1");
//        map.put("nickname","zhangsan");
//        String ip = "127.0.0.1";
//        String time = new SimpleDateFormat("yyyyMMdd HHmm").format(new Date());
//        String encode = JwtUtil.encode("2019gmall0105", map, ip + time);
//        System.err.println(encode);


        Map<String, Object> map = new HashMap<>();
        map.put("statue", "success");
        map.put("memeberId", "1");
        map.put("nickname", "zkd");
        String ip = "127.0.0.1";
        LocalDateTime localDateTime = LocalDateTime.now();
        System.out.println(localDateTime);
        String encode = JwtUtil.encode("2019gmall0105", map, ip + localDateTime);
        System.out.println(encode);

        // String tokenUserInfo = StringUtils.substringBetween(encode, ".");
//        decode();


    }

    private static void decode() {
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] tokenBytes = base64UrlCodec.decode("eyJuaWNrbmFtZSI6InpoYW5nc2FuIiwibWVtYmVySWQiOiIxIn0");
        String tokenJson = null;
        try {
            tokenJson = new String(tokenBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map map1 = JSON.parseObject(tokenJson, Map.class);
        System.out.println("64="+map1);
    }
}
