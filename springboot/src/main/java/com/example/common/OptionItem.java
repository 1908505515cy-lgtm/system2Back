package com.example.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下拉选项项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionItem {

    /** 显示文本 */
    private String label;

    /** 选项值 */
    private Object value;
}
