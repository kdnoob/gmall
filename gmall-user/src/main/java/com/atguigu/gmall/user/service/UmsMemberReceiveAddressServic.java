package com.atguigu.gmall.user.service;

import com.atguigu.gmall.user.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UmsMemberReceiveAddressServic {

    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) ;
}