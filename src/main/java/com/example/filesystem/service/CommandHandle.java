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
public class CommandHandle {

    private static final List<User> loginUserList = new ArrayList<>();
    
    @Autowired
    private Service service;
    
    private static CommandHandle commandHandle;

    @PostConstruct
    public void init() {
        commandHandle = this;
        commandHandle.service = this.service;
    }


    public static List<User> getLoginUserList() {
        return loginUserList;
    }


    public static void clientMessage(String receiveMessage, SocketChannel client) {

        if (Util.isStringEmpty(receiveMessage)) {
            System.out.println("Empty message!");
            return;
        }
        if (receiveMessage.equals("loginSuccess")) {
            System.out.println("Login successfully!");
        }
        if (receiveMessage.equals("loginFail")) {
            System.out.println("Login fail!");
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

            user = commandHandle.service.login(username, password);
            if (!Util.isNull(user)) {
                commandHandle.service.initUserDirectory(username);
                loginUserList.add(user);
                break;
            }
        }
        return user;
    }

    public static void handleCommand(String command, User user) {
        if (Util.isStringEmpty(command)) {
            return;
        }
        if ("dir".equals(command)) {
            commandHandle.service.getFileList(user);
        }
        else if (command.matches("cd .+")) {
            commandHandle.service.changeDirectory(command, user);
        }
        else if (command.matches("create .+")) {
            commandHandle.service.createFile(command, user);
        }
        else if (command.matches("delete .+")) {
            commandHandle.service.deleteFile(command, user);
        }
        else if (command.matches("open .+")) {
            commandHandle.service.openFile(command, user);
        }
        else if (command.matches("close .+")) {
            commandHandle.service.closeFile(command, user);
        }
        else if (command.matches("read .+")) {
            commandHandle.service.readFile(command, user);
        }
        else if (command.matches("write .+")) {
            commandHandle.service.writeFile(command, user);
        }
        else if (command.matches("mkdir .+")) {
            commandHandle.service.createDirectory(command, user);
        }
        else if (command.matches("link .+")) {
            commandHandle.service.linkFile(command, user);
        }
        else if (command.matches("bitmap")) {
            commandHandle.service.showBitMap();
        }
        else {
            System.out.println("Invalid command " + command);
        }
    }

}
