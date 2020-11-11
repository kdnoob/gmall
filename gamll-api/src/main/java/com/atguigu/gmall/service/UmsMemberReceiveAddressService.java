package com.atguigu.gmall.service;


import com.atguigu.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UmsMemberReceiveAddressService {

    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);

    UmsMemberReceiveAddress getReceiveAddressByReceiveAddressId(String receiveAddressId);
}
