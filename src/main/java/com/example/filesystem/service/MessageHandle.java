package com.example.filesystem.service;

import com.example.filesystem.model.User;
import com.example.filesystem.util.UserLoginUtil;
import com.example.filesystem.util.Util;
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
    
    @Autowired
    private Service service;
    
    private static MessageHandle messageHandle;

    @PostConstruct
    public void init() {
        messageHandle = this;
        messageHandle.service = this.service;
    }

    public static void addUser(User user) {
        if (!Util.isNull(user)) {
            loginUserList.add(user);
        }
    }

    public static List<User> getLoginUserList() {
        return loginUserList;
    }


    public static void clientMessage(String receiveMessage, SocketChannel client) {

        if (Util.isStringEmpty(receiveMessage)) {
            System.out.println("Send empty message!");
            return;
        }
        if (receiveMessage.equals("loginSuccess")) {
            System.out.println("Login successfully！enter ENTER to in root directory！");
        }
        if (receiveMessage.equals("loginFail")) {
            System.out.println("Login fail！enter ENTER to login again！");
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

            user = messageHandle.service.login(username, password);
            if (!Util.isNull(user)) {
                messageHandle.service.initDirectory(username);
                addUser(user);
                break;
            }
        }
        return user;
    }

    public static void handleMessage(String message, User user) {
        if (Util.isStringEmpty(message)) {
            return;
        }
        if ("dir".equals(message)) {
            messageHandle.service.getDirectory(user);
        }
        else if (message.matches("cd .+")) {
            messageHandle.service.changeDirectory(message, user);
        }
        else if (message.matches("create .+")) {
            messageHandle.service.createFile(message, user);
        }
        else if (message.matches("delete .+")) {
            messageHandle.service.deleteFile(message, user);
        }
        else if (message.matches("open .+")) {
            messageHandle.service.openFile(message, user);
        }
        else if (message.matches("close .+")) {
            messageHandle.service.closeFile(message, user);
        }
        else if (message.matches("read .+")) {
            messageHandle.service.readFile(message, user);
        }
        else if (message.matches("write .+")) {
            messageHandle.service.writeFile(message, user);
        }
        else if (message.matches("mkdir .+")) {
            messageHandle.service.createDirectory(message, user);
        }
        else if (message.matches("link .+")) {
            messageHandle.service.linkFile(message, user);
        }
        else if (message.matches("bitmap")) {
            messageHandle.service.showBitMap();
        }
        else {
            System.out.println("Invalid command " + message);
        }
    }

}
