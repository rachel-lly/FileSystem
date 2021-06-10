package com.example.filesystem;

import com.example.filesystem.util.JudgeUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class UserCheck {

    @Resource
    private UserMapper mapper;

    public User login(String name,String password){
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
