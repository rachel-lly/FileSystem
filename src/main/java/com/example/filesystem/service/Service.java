package com.example.filesystem.service;

import com.example.filesystem.model.User;


/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 21:44
 */

public interface Service {

    void initDirectory(String directoryName);
    void getDirectory(User user);
    void changeDirectory(String message, User user);
    void createDirectory(String message, User user);


    void createFile(String message, User user);
    void openFile(String message, User user);
    void closeFile(String message, User user);
    void readFile(String message, User user);
    void writeFile(String message, User user);
    void deleteFile(String message, User user);
    void linkFile(String message, User user);

    void showBitMap();


    User login(String name, String password);

}
