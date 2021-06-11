package com.example.filesystem.model;

import lombok.Data;

/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 19:44
 */
@Data
public class Disk {

    private Integer line;
    private Integer column;
    private String[][] content;//存放文件内容

    public Disk(Integer line, Integer column) {
        this.line = line;
        this.column = column;
        this.content = new String[line][column];
    }

}
