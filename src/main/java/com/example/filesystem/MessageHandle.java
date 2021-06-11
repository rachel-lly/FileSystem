package com.example.filesystem;

import com.example.filesystem.model.User;
import com.example.filesystem.util.JudgeUtil;
import com.example.filesystem.util.UserLoginUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
@Component
public class MessageHandle {

    private static final List<User> loginUserList = new ArrayList<>();


    private FileServer fileService;

    private UserServer userServer;

    private static MessageHandle messageHandle;

    @PostConstruct
    public void init() {
        messageHandle = this;
        messageHandle.fileService = this.fileService;
        messageHandle.userServer = this.userServer;
    }

    public static void addUser(User user) {
        if (!JudgeUtil.isNull(user)) {
            loginUserList.add(user);
        }
    }

    public static List<User> getLoginUserList() {
        return loginUserList;
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
            UserLoginUtil.loginMap.remove(client);
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

            user = messageHandle.userServer.login(username, password);
            if (!JudgeUtil.isNull(user)) {
                // TODO: 2021/6/11 文件处理
                //登录成功后自动初始化文件
                messageHandle.fileService.initDirectory(username);
                addUser(user);
                break;
            }
        }
        return user;
    }

    public static void handleMessage(String message, User user) {
        if (JudgeUtil.isStringEmpty(message)) {
            //消息为空直接返回
            return;
        }
        if ("dir".equals(message)) {
            //查看当前路径下的所有文件目录
            messageHandle.fileService.getDirectory(user);
        }
        else if (message.matches("cd .+")) {
            //说明是想更换目录
            messageHandle.fileService.changeDirectory(message, user);
        }
        else if (message.matches("create .+")) {
            messageHandle.fileService.createFile(message, user);
        }
        else if (message.matches("open .+")) {
            messageHandle.fileService.openFile(message, user);
        }
        else if (message.matches("read .+")) {
            messageHandle.fileService.readFile(message, user);
        }
        else if (message.matches("write .+")) {
            messageHandle.fileService.writeFile(message, user);
        }
        else if (message.matches("close .+")) {
            messageHandle.fileService.closeFile(message, user);
        }
        else if (message.matches("delete .+")) {
            messageHandle.fileService.deleteFile(message, user);
        }
        else if (message.matches("mkdir .+")) {
            messageHandle.fileService.createDirectory(message, user);
        }
        else if (message.matches("link .+")) {
            messageHandle.fileService.linkFile(message, user);
        }
        else {
            System.out.println("Unknown command " + message);
        }
    }

}
