package com.example.filesystem.util;

import com.example.filesystem.service.Service;
import com.example.filesystem.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 23:04
 */
@Component
public class UserLoginUtil {



    public static Map<SocketChannel, User> loginMap = new HashMap<>();

    private static UserLoginUtil userLoginUtil;

    @Autowired
    private Service service;

    @PostConstruct
    public void init() {
        userLoginUtil = this;
        userLoginUtil.service = this.service;
    }



    public static User userLogin(ByteBuffer byteBuffer, SocketChannel client) {
        byteBuffer.clear();
        String username, password;
        User user = new User();
        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("username:");
            username = scanner.nextLine();
            System.out.print("password:");
            password = scanner.nextLine();
            if (Util.isStringEmpty(username)) {
                System.out.println("username is empty");
            }else if( Util.isStringEmpty(password)){
                System.out.println("password is empty");
            }else {
                break;
            }
        }
        user.setPassword(password);
        user.setName(username);
        String message = "username:" + username + "," + "password:" + password;
        byteBuffer.put(message.getBytes());
        byteBuffer.flip();
        try {
            client.write(byteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO exception");
        }
        return user;
    }


}
