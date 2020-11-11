package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsOrderMapper omsOrderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;

    @Reference
    CartService cartService;

    @Override
    public String getTradeCode(String memberId) {
        String tradeCode = UUID.randomUUID().toString();
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            if (jedis != null) {
                jedis.setex("user:" + memberId + ":tradeCode", 60*15, tradeCode);
            }
        } finally {
            jedis.close();
        }
        return tradeCode;
    }

    @Override
    public String checkTradeCode(String memberId, String tradeCode) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            if (jedis != null) {
                String tradeKey = "user:" + memberId + ":tradeCode";
                String tradeCodeFromCache = jedis.get(tradeKey);
//                if (StringUtils.isNotBlank(tradeCodeFromCache) && Objects.equals(tradeCodeFromCache, tradeCode)) {
//                    jedis.del(tradeKey);
//                }
                // 使用lua脚本在发现key的同时将key删除，防止并发订单攻击
                //对比防重删令牌
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                Long eval = (Long) jedis.eval(script, Collections.singletonList(tradeKey), Collections.singletonList(tradeCode));

                if (eval!=null&&eval!=0) {
                    // jedis.del(tradeKey);
                    return "success";
                } else {
                    return "fail";
                }
            }
        } finally {
            jedis.close();
        }
        return "fail";
    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {
        // 保存订单表
        omsOrderMapper.insertSelective(omsOrder);
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(omsOrder.getId());
            omsOrderItemMapper.insertSelective(omsOrderItem);
            // 删除购物车数据
//            cartService.delCartList(omsOrderItem.getProductSkuId());
        }
    }

}
