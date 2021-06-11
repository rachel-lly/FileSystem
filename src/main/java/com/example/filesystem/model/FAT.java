package com.example.filesystem.model;

import lombok.Data;

/**
 * @Author: rachel-lly
 * @Date: 2021-06-11 19:57
 */
@Data
public class FAT {

    private Integer line;
    private Integer column;
    private FatBlock[] fatBlocks;

    public FAT(Integer line, Integer column) {
        this.line = line;
        this.column = column;
        this.fatBlocks = new FatBlock[line*column];
    }
}



