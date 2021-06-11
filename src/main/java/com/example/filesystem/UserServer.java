package com.example.filesystem;

import com.example.filesystem.model.User;
import org.springframework.stereotype.Service;

/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 23:00
 */
@Service
public interface UserServer {

    User login(String name, String password);

}
