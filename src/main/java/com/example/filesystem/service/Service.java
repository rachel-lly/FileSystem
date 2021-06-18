package com.example.filesystem.service;

import com.example.filesystem.model.User;


/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 21:44
 */

public interface Service {

    void initDirectory(String directoryName);
    void getDirectory(User user);
    void changeDirectory(String command, User user);
    void createDirectory(String command, User user);


    void createFile(String command, User user);
    void openFile(String command, User user);
    void closeFile(String command, User user);
    void readFile(String command, User user);
    void writeFile(String command, User user);
    void deleteFile(String command, User user);
    void linkFile(String command, User user);

    void showBitMap();


    User login(String name, String password);

}
