package com.example.filesystem.model;

import lombok.Data;

/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 20:21
 */
@Data
public class FCB {

    private IndexFile IndexFile;//对应文件
    private String fileName;//文件名
    private String path;

    public FCB(IndexFile IndexFile, String fileName, String path) {
        this.IndexFile = IndexFile;
        this.fileName = fileName;
        this.path = path;
    }
}
