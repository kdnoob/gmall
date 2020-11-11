package com.atguigu.gmall.order.contorller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    CartService cartService;

    @Reference
    OrderService orderService;

    @Reference
    UserService userService;

    @Reference
    SkuService skuService;

    @Reference
    UmsMemberReceiveAddressService umsMemberReceiveAddressService;

    @RequestMapping("submitOrder")
    @LoginRequired(loginSuccess = true)
    public ModelAndView submitOrder(String receiveAddressId, BigDecimal totalAmount, String tradeCode, HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap modelMap) {

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        // 检查交易码
        String success = orderService.checkTradeCode(memberId, tradeCode);

        if ("fail".equals(success)) {
            ModelAndView mv = new ModelAndView("tradeFail");
            return mv;
        }

        // 获取信息保存订单表及订单详情表
        // 检查价格及库存

        List<OmsCartItem> omsCartItems = cartService.getCartList(memberId);
        List<OmsOrderItem> omsOrderItems = new ArrayList<>();
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setAutoConfirmDay(7);
        omsOrder.setCreateTime(new Date());
        omsOrder.setDiscountAmount(null);
        //omsOrder.setFreightAmount(); 运费，支付后，在生成物流信息时
        omsOrder.setMemberId(memberId);
        omsOrder.setMemberUsername(nickname);
        omsOrder.setNote("快点发货");

        String outTradeNo = "gmall";
        outTradeNo = outTradeNo + System.currentTimeMillis();// 将毫秒时间戳拼接到外部订单号
        SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDDHHmmss");
        outTradeNo = outTradeNo + sdf.format(new Date());// 将时间字符串拼接到外部订单号
        omsOrder.setOrderSn(outTradeNo);//外部订单号

        omsOrder.setPayAmount(totalAmount);
        omsOrder.setOrderType(1);
        UmsMemberReceiveAddress umsMemberReceiveAddress = userService.getReceiveAddressById(receiveAddressId);
        omsOrder.setReceiverCity(umsMemberReceiveAddress.getCity());
        omsOrder.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
        omsOrder.setReceiverName(umsMemberReceiveAddress.getName());
        omsOrder.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
        omsOrder.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
        omsOrder.setReceiverProvince(umsMemberReceiveAddress.getProvince());
        omsOrder.setReceiverRegion(umsMemberReceiveAddress.getRegion());

        // 当前日期加一天，一天后配送
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE,1);
        Date time = c.getTime();
        omsOrder.setReceiveTime(time);

        omsOrder.setSourceType(0);
        omsOrder.setStatus(0);
        omsOrder.setOrderType(0);
        omsOrder.setTotalAmount(totalAmount);

        for (OmsCartItem omsCartItem : omsCartItems) {
            // 获得订单详情列表
            OmsOrderItem omsOrderItem = new OmsOrderItem();
            // 检价
            boolean b = skuService.checkPrice(omsCartItem.getProductSkuId(),omsCartItem.getPrice());
            if (b == false) {
                ModelAndView mv = new ModelAndView("tradeFail");
                return mv;
            }
            // 验库存,远程调用库存系统
            omsOrderItem.setProductPic(omsCartItem.getProductPic());
            omsOrderItem.setProductName(omsCartItem.getProductName());

            omsOrderItem.setOrderSn(outTradeNo);// 外部订单号，用来和其他系统进行交互，防止重复
            omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());
            omsOrderItem.setProductPrice(omsCartItem.getPrice());
            omsOrderItem.setRealAmount(omsCartItem.getTotalPrice());
            omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
            omsOrderItem.setProductSkuCode("111111111111");
            omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
            omsOrderItem.setProductId(omsCartItem.getProductId());
            omsOrderItem.setProductSn("仓库对应的商品编号");// 在仓库中的skuId

            omsOrderItems.add(omsOrderItem);
        }
        omsOrder.setOmsOrderItems(omsOrderItems);

        // 保存到数据库中
        // 删除购物车的对应商品
        orderService.saveOrder(omsOrder);

        // 重定向到支付系统
        ModelAndView mv = new ModelAndView("redirect:http://payment.gmall.com:8087/index");
        mv.addObject("outTradeNo",outTradeNo);
        mv.addObject("totalAmount","0.01");
        return mv;
    }

    @RequestMapping("toTrade")
    @LoginRequired(loginSuccess = true)
    public String toTrade(HttpServletRequest request, ModelMap modelMap) {

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        // 获取地址
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressService.getReceiveAddressByMemberId(memberId);

        // 获取购物车信息
        List<OmsCartItem> omsCartItems = cartService.getCartList(memberId);

        List<OmsOrderItem> omsOrderItems = new ArrayList<>();

        for (OmsCartItem omsCartItem : omsCartItems) {
            if ("1".equals(omsCartItem.getIsChecked())) {
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItems.add(omsOrderItem);
            }
        }

        modelMap.put("userAddressList", umsMemberReceiveAddresses);
        modelMap.put("omsOrderItems",omsOrderItems);

        // 生成交易码
        String tradeCode = orderService.getTradeCode(memberId);
        modelMap.put("tradeCode",tradeCode);

        return "trade";
    }
}
