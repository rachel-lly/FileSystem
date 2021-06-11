package com.example.filesystem;

import com.example.filesystem.util.JudgeUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Service
public class UserLogin {

    public static UserLogin instance;

    @Resource
    private UserMapper mapper;

    public static UserLogin getInstance(){

        if(instance == null){
            instance = new UserLogin();
        }
        return instance;
    }

    public static Map<SocketChannel, User> loginMap = new HashMap<>();

    public static User userLogin(ByteBuffer byteBuffer, SocketChannel client){
        byteBuffer.clear();
        String name,password;
        User user = new User();
        while (true){
            Scanner scanner = new Scanner(System.in);
            System.out.print("name:");
            name = scanner.nextLine();
            System.out.print("password:");
            password = scanner.nextLine();
            if (JudgeUtil.isStringEmpty(name) || JudgeUtil.isStringEmpty(password)) {
                System.out.println("请填写完整账号信息");
            }
            else {
                break;
            }
        }
        user.setName(name);
        user.setPassword(password);
        user.setId(1);
        String message = "username:" + name + "," + "password:" + password;
        byteBuffer.put(message.getBytes());
        byteBuffer.flip();
        try {
            client.write(byteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO异常");
        }
        return user;
    }



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
