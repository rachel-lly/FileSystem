package com.example.filesystem.model;

import lombok.Data;

@Data
public class FatBlock{
    private Integer blockId;
    private Integer nextBlockId;
}