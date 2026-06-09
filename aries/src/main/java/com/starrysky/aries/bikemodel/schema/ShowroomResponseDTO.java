package com.starrysky.aries.bikemodel.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单车展厅响应DTO
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/11 14:10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomResponseDTO {
    /**
     * 单车列表
     */
    private List<BikeModelVO> bikes;
    
    /**
     * 品牌列表
     */
    private List<String> brands;
    
    /**
     * 分类列表
     */
    private List<CategoryVO> categories;
    
    /**
     * 总数
     */
    private Long total;
}
