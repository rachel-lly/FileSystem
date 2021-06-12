package com.example.filesystem;

import com.example.filesystem.model.Index;
import com.example.filesystem.model.User;
import com.example.filesystem.util.JudgeUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@SpringBootApplication
@MapperScan(basePackages = {"com.example.filesystem.mapper"})
public class FileSystemApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(FileSystemApplication.class, args);
    }

    //存储用户当前所在的目录
    public static Map<User, Index> userPath = new HashMap<>();

    @Override
    public void run(String... args) throws Exception {
        while (true) {
            User user;
            //判断用户是否已经登录
            if (MessageHandle.getLoginUserList().isEmpty()) {
                user = MessageHandle.login();
                userPath.put(user, new Index(null, null, "/"));
            }
            else {
                user = MessageHandle.getLoginUserList().get(0);
            }
            Scanner scanner = new Scanner(System.in);
            while (true) {
                //输出当前路径
                if (JudgeUtil.isStringEmpty(userPath.get(user).getFileName())) {
                    System.out.print("[" + user.getName() + "@" + "localhost " + userPath.get(user).getPath() + "]$");
                }
                else {
                    if ("/".equals(userPath.get(user).getPath())) {
                        System.out.print("[" + user.getName() + "@" + "localhost " + userPath.get(user).getPath()
                                + userPath.get(user).getFileName() + "]$");
                    }
                    else {
                        System.out.print("[" + user.getName() + "@" + "localhost " + userPath.get(user).getPath() + "/"
                                + userPath.get(user).getFileName() + "]$");
                    }
                }

                String message = scanner.nextLine();
                if ("logout".equals(message)) {
                    System.out.println("Exit successfully");
                    MessageHandle.getLoginUserList().clear();
                    break;
                }
                MessageHandle.handleMessage(message, user);
            }
        }
    }
}
