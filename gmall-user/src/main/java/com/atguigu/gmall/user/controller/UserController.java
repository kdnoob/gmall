package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.user.bean.UmsMember;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    @Autowired
    UserService userService;

    @RequestMapping("/index")
    public String index() {
        return "hello world!";
    }

    @RequestMapping("/getAllUms")
    public List<UmsMember> getAllUser() {
//        List<UmsMember> umsMemberList = userService.getAllUser();
        List<UmsMember> umsMemberList = userService.getAllUser();
        return umsMemberList;
    }
}
