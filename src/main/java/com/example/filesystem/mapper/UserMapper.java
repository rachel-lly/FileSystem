package com.example.filesystem.mapper;

import com.example.filesystem.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("select id,name,password from user where name = #{name} and password = #{password}")
    User getUser(String name, String password);

}
