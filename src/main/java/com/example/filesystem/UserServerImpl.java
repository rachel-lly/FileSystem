package com.example.filesystem;

import com.example.filesystem.mapper.UserMapper;
import com.example.filesystem.model.User;
import com.example.filesystem.util.JudgeUtil;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;


@Service
public class UserServerImpl implements UserServer{

    @Resource
    private UserMapper mapper;


    @Override
    public User login(String name, String password) {
        if(JudgeUtil.isStringEmpty(name)||JudgeUtil.isStringEmpty(password)){
            System.out.println("enter empty");
            return null;
        }

        User user = mapper.getUser(name,password);

        if(user!=null){
            System.out.println("Login successfully!");
            return user;
        }else{
            System.out.println("Couldn't find this account!");
            return null;
        }
    }
}
