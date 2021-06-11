package com.example.filesystem.util;

import com.example.filesystem.UserServer;
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

    private static final List<User> loginUserList = new ArrayList<>();

    public static Map<SocketChannel, User> loginMap = new HashMap<>();

    private static UserLoginUtil userLoginUtil;

    @Autowired
    private UserServer userServer;

    @PostConstruct
    public void init() {
        userLoginUtil = this;
        userLoginUtil.userServer = this.userServer;
    }

    public static void addUser(User user) {
        if (!JudgeUtil.isNull(user)) {
            loginUserList.add(user);
        }
    }

    public static void removeUser(User user) {
        if (!JudgeUtil.isNull(user)) {
            loginUserList.remove(user);
        }
    }

    public static List<User> getLoginUserList() {
        return loginUserList;
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
            if (JudgeUtil.isStringEmpty(username) || JudgeUtil.isStringEmpty(password)) {
                System.out.println("请填写完整账号信息");
            }
            else {
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
            System.out.println("IO异常");
        }
        return user;
    }

    public static User login() {
        String username, password;
        User user;
        while (true) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("username:");
            username = scanner.nextLine();
            System.out.print("password:");
            password = scanner.nextLine();
            user = userLoginUtil.userServer.login(username, password);
            if (!JudgeUtil.isNull(user)) {
                break;
            }
        }
        return user;
    }
}
