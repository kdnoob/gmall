package com.atguigu.gmall.user.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Override
    public List<UmsMember> getAllUser() {
//        return userMapper.selectAllUser();
        return userMapper.selectAll();
    }

    @Override
    public UmsMember login(UmsMember umsMember) {

        // 先从缓存中读取
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            if (jedis != null) {
                String userInfo = jedis.get("user:" + umsMember.getUsername() + umsMember.getPassword() + ":info");
                if (StringUtils.isNoneEmpty(userInfo)) {
                    // 缓存有数据
                    return JSON.parseObject(userInfo, UmsMember.class);
                }
            }
        } finally {
            jedis.close();
        }
        // 没有获取到jedis
        // 缓存中没有
        // 密码错误
        // 从db中读数据
        UmsMember umsMember1 = loginFromDB(umsMember);
        jedis.setex("user:" + umsMember.getUsername() + umsMember.getPassword() + ":info", 60 * 24, JSON.toJSONString(umsMember1));
        return umsMember1;
    }

    @Override
    public void addUserToken(String token, String id) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            if (jedis != null) {
                jedis.setex("user:" + id + ":token", 60 * 60 * 2, token);
            }
        } finally {
            jedis.close();
        }
    }

    @Override
    public UmsMember addOauthUser(UmsMember umsMember) {
        userMapper.insertSelective(umsMember);
        return umsMember;
    }

    @Override
    public UmsMember checkOauthUser(UmsMember umsMember) {
        return userMapper.selectOne(umsMember);
    }

    @Override
    public UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setId(receiveAddressId);
        return umsMemberReceiveAddressMapper.selectOne(umsMemberReceiveAddress);
    }

    private UmsMember loginFromDB(UmsMember umsMember) {
        List<UmsMember> umsMembers = userMapper.select(umsMember);
        if (CollectionUtils.isEmpty(umsMembers)) {
            return null;
        }
        return umsMembers.get(0);
    }
}
