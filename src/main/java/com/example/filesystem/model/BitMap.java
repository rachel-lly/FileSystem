package com.example.filesystem.model;

import lombok.Data;

/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 19:45
 */
@Data
public class BitMap {

    private Integer line;
    private Integer column;
    private Boolean[][] fBlocks;

    /**
    * @Description: 初始化位示图
    * @param：[line, column]
    */
    public BitMap(Integer line, Integer column) {
        this.line = line;
        this.column = column;
        this.fBlocks = new Boolean[line][column];
    }
    
}
