package com.example.filesystem;

import com.example.filesystem.model.FCB;
import com.example.filesystem.model.User;
import com.example.filesystem.service.CommandHandle;
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
    public static Map<User, FCB> userPath = new HashMap<>();

    @Override
    public void run(String... args){
        while (true) {
            User user;
            //判断用户是否已经登录
            if (CommandHandle.getLoginUserList().isEmpty()) {
                user = CommandHandle.login();
                userPath.put(user, new FCB(null, null, "\\"));

                System.out.println("\n\t-------------- FileSystem 的常用命令-------------");
                System.out.println("\tcd      改变当前目录（.. 返回上一级）");
                System.out.println("\tcreate  创建文件");
                System.out.println("\tdelete  删除文件");
                System.out.println("\topen    打开文件");
                System.out.println("\tclose   关闭文件");
                System.out.println("\twrite   写入文件");
                System.out.println("\tread    读取文件");
                System.out.println("\tmkdir   创建文件夹");
                System.out.println("\tlink    连接文件（多用户的共享文件夹：share）");
                System.out.println("\tbitmap  查看当前文件系统的位示图");
                System.out.println("\tlogout  退出文件系统");
                System.out.println("\t------------------------------------------------\n");
                System.out.println("\t注意：命令后加空格 eg：cd "+user.getName()+"[用户专用文件夹]\n");

            }
            else {
                user = CommandHandle.getLoginUserList().get(0);
            }
            Scanner scanner = new Scanner(System.in);
            while (true) {
                //输出当前路径
                if (Util.isStringEmpty(userPath.get(user).getFileName())) {
                    System.out.print("C:"+"\\"+"FileSystem" + ">");
                }
                else {
                    if ("\\".equals(userPath.get(user).getPath())) {
                        System.out.print("C:"+"\\"+"FileSystem" + userPath.get(user).getPath() + userPath.get(user).getFileName() + ">");
                    }
                    else {
                        System.out.print("C:"+"\\"+"FileSystem" + userPath.get(user).getPath() + "\\" + userPath.get(user).getFileName() + ">");
                    }
                }

                String command = scanner.nextLine();
                if ("logout".equals(command)) {
                    System.out.println("Exit successfully!");
                    CommandHandle.getLoginUserList().clear();
                    break;
                }
                CommandHandle.handleCommand(command, user);
            }
        }
    }
}
