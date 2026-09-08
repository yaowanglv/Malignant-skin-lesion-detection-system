package com.example.springb.controller;

import com.example.springb.common.Result;
import com.example.springb.entity.Admin;
import com.example.springb.service.AdminService;
import com.example.springb.util.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class WebController {

    @Resource
    AdminService adminService;

    @Resource
    JwtUtil jwtUtil;



    @GetMapping("/")
    public Result miao(){
        return Result.success("2077");
    }


//    @PostMapping("/login")
//    public Result login(@RequestBody Admin admin) {
//        adminService.login(admin);
//        if ("ADMIN".equals(account.getRole())) {
//            dbAccount = adminService.login(account);
//        } else if ("USER".equals(account.getRole())) {
//            dbAccount = userService.login(account);
//        } else {
//            throw new CustomerException("非法请求");
//        }
//        return Result.success(dbAccount);
//    }

    @PostMapping("/login")
    public Result login(@RequestBody Admin admin) {
        Admin dbadmin = adminService.login(admin);
        String token = jwtUtil.generateToken(dbadmin.getId(), dbadmin.getUsername(), dbadmin.getRole());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", dbadmin.getId());
        userInfo.put("username", dbadmin.getUsername());
        userInfo.put("name", dbadmin.getName());
        userInfo.put("role", dbadmin.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", userInfo);

        return Result.success(data);
    }





}
