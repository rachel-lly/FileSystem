package com.example.filesystem;

import com.example.filesystem.util.JudgeUtil;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MessageHandle {

    private static UserLogin userLogin = UserLogin.getInstance();
    private static final List<User> loginUserList = new ArrayList<>();


    public static void addUser(User user) {
        if (!JudgeUtil.isNull(user)) {
            loginUserList.add(user);
        }
    }


    public static void clientMessage(String receiveMessage, SocketChannel client) {

        if (JudgeUtil.isStringEmpty(receiveMessage)) {
            System.out.println("服务端发送空消息");
            return;
        }
        if (receiveMessage.equals("loginSuccess")) {
            System.out.println("登录成功！请按回车键进入根目录！");
        }
        if (receiveMessage.equals("loginFail")) {
            System.out.println("登录失败！请按回车键重新登陆！");
            UserLogin.loginMap.remove(client);
        }

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

            user = userLogin.login(username, password);
            if (!JudgeUtil.isNull(user)) {
                // TODO: 2021/6/11 文件处理
                //登录成功后自动初始化文件
               // messageUtil.fileService.initDirectory(username);
                addUser(user);
                break;
            }
        }
        return user;
    }

}
