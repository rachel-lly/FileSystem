package com.example.filesystem.model;

import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 20:24
 */

@Data
@NoArgsConstructor
public class User {

    private int id;
    private String name;
    private String password;

}
