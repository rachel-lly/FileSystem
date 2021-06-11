package com.example.filesystem;

import com.example.filesystem.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE name = #{name} and password = #{password}")
    User getUser(String name, String password);

}
