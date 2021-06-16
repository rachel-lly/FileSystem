package com.example.filesystem;

import com.example.filesystem.model.IndexFile;
import com.example.filesystem.model.User;
import com.example.filesystem.service.MessageHandle;
import com.example.filesystem.util.Util;
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
    public static Map<User, IndexFile> userPath = new HashMap<>();

    @Override
    public void run(String... args){
        while (true) {
            User user;
            //判断用户是否已经登录
            if (MessageHandle.getLoginUserList().isEmpty()) {
                user = MessageHandle.login();
                userPath.put(user, new IndexFile(null, null, "\\"));
            }
            else {
                user = MessageHandle.getLoginUserList().get(0);
            }
            Scanner scanner = new Scanner(System.in);
            while (true) {
                //输出当前路径
                if (Util.isStringEmpty(userPath.get(user).getFileName())) {
                    System.out.print("C:"+"\\"+"FileSystem" + userPath.get(user).getPath() + ">");
                }
                else {
                    if ("\\".equals(userPath.get(user).getPath())) {
                        System.out.print("C:"+"\\"+"FileSystem" + userPath.get(user).getPath() + userPath.get(user).getFileName() + ">");
                    }
                    else {
                        System.out.print("C:"+"\\"+"FileSystem" + userPath.get(user).getPath() + "\\" + userPath.get(user).getFileName() + ">");
                    }
                }

                String message = scanner.nextLine();
                if ("logout".equals(message)) {
                    System.out.println("Exit successfully!");
                    MessageHandle.getLoginUserList().clear();
                    break;
                }
                MessageHandle.handleMessage(message, user);
            }
        }
    }
}
