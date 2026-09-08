package com.example.springb.mapper;

import com.example.springb.entity.Admin;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface AdminMapper {
    List<Admin> selectAll(Admin admin);

    void insert(Admin admin);

    @Select("select  * from  `admin` where username=#{username}")
    Admin selectByUsername(String username);

    @Select("select * from `admin` where id=#{id}")
    Admin selectById(Integer id);

    void updateById(Admin admin);

    @Update("update `admin` set password=#{password} where id=#{id}")
    void updatePasswordById(Admin admin);

    @Delete("delete from  `admin` where id=#{id}")
    void deleteById(Integer id);
}
