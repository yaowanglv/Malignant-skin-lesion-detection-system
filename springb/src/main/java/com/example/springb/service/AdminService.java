package com.example.springb.service;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.example.springb.entity.Admin;
import com.example.springb.exception.CustomerException;
import com.example.springb.mapper.AdminMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminService {

    @Resource
    AdminMapper adminMapper;


    public Admin createAdmin(Admin admin) {
        // 手动设置创建时间

//        admin.setTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        adminMapper.insert(admin); // 插入数据库
        return admin;
    }

    public void add(Admin admin) {
        // 根据新的账号查询数据库  是否存在同样账号的数据
        admin.setTime(DateUtil.now());// tBody 接收前端传来的json参数
        Admin dbAdmin = adminMapper.selectByUsername(admin.getUsername());
        if (dbAdmin != null) {
            throw new CustomerException("9527","账号重复");//抛出自定义异常
        }
        // 默认密码设置为123456
        if (StrUtil.isBlank(admin.getPassword())) {
            admin.setPassword("123456");
        }
        if (StrUtil.isBlank(admin.getRole())) {
            admin.setRole("user");
        }
        //插入数据库
        adminMapper.insert(admin);
    }


    public String admin(String name) {
            if ("admin".equals(name)) {
                return "admin";
            } else {
                throw new CustomerException("1234","账号错误");
            }
    }

    public void update(Admin admin) {
        adminMapper.updateById(admin);//根据主键去修改，主键唯一
    }

    public void changePassword(Integer id, String oldPassword, String newPassword) {
        if (id == null) {
            throw new CustomerException("400", "用户不存在");
        }
        if (StrUtil.isBlank(oldPassword)) {
            throw new CustomerException("400", "请输入原密码");
        }
        if (StrUtil.isBlank(newPassword)) {
            throw new CustomerException("400", "请输入新密码");
        }

        Admin dbAdmin = adminMapper.selectById(id);
        if (dbAdmin == null) {
            throw new CustomerException("400", "用户不存在");
        }
        if (!oldPassword.equals(dbAdmin.getPassword())) {
            throw new CustomerException("400", "原密码错误");
        }

        Admin updateAdmin = new Admin();
        updateAdmin.setId(id);
        updateAdmin.setPassword(newPassword);
        adminMapper.updatePasswordById(updateAdmin);
    }

    public List<Admin> selectAll(Admin admin){
            return adminMapper.selectAll(admin);
        }

        //单个删除
    public void deleteById(Integer id) {
        adminMapper.deleteById(id);
    }

    //批量删除
    public void deleteBatch(List<Admin> list) {
        for (Admin admin : list) {
            this.deleteById(admin.getId());
        }
    }


    public PageInfo<Admin> selectPage(Integer pageNum, Integer pageSize,Admin admin) {
       //开启分页查询
        PageHelper.startPage(pageNum, pageSize);
        List<Admin> list = adminMapper.selectAll(admin);
        return PageInfo.of(list);
    }


//    public Admin login(Account account) {
//        // 验证账号是否存在
//        Admin dbAdmin = adminMapper.selectByUsername(account.getUsername());
//        if (dbAdmin == null) {
//            throw new CustomerException("账号不存在");
//        }
//        // 验证密码是否正确
//        if (!dbAdmin.getPassword().equals(account.getPassword())) {
//            throw new CustomerException("账号或密码错误");
//        }
//        return dbAdmin;
//    }

    public Admin login(Admin admin) {
        if (admin == null || StrUtil.isBlank(admin.getUsername())) {
            throw new CustomerException("400", "用户名不能为空");
        }
        if (StrUtil.isBlank(admin.getPassword())) {
            throw new CustomerException("400", "密码不能为空");
        }

        // 验证账号是否存在
        Admin dbAdmin = adminMapper.selectByUsername(admin.getUsername().trim());
        if (dbAdmin == null) {
            throw new CustomerException("401", "用户名或密码错误");
        }
        // 验证密码是否正确
        if (!dbAdmin.getPassword().equals(admin.getPassword())) {
            throw new CustomerException("401", "用户名或密码错误");
        }
        if (StrUtil.isBlank(dbAdmin.getRole())) {
            dbAdmin.setRole("user");
        }
        return dbAdmin;
    }




}
