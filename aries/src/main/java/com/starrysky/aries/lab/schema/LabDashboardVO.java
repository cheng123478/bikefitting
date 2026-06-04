package com.starrysky.aries.lab.schema;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 实验室数据展示
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/18 20:03
 */
@Setter
@Getter
public class LabDashboardVO {
    // --- 1. 身体图谱 (Body Profile) ---
    /** 是否已录入身体数据 */
    private boolean hasBodyProfile;
    /** 身体灵活性/韧性得分 (基于最新身体图谱计算) */
    private Integer flexibilityScore;
    /** 身体稳定性得分 (基于最新身体图谱计算) */
    private Integer stabilityScore;

    // --- 2. 我的车库 (Garage) ---
    /** 活跃车辆总数 */
    private Integer bikeCount;
    /** 车辆列表 (包含每辆车的调教状态) */
    private List<BikeSummaryVO> bikes;

    /**
     * 内部类：车辆摘要信息
     */
    @Setter
    @Getter
    public static class BikeSummaryVO {
        private Long id;
        /** 品牌与型号 (如: Specialized Tarmac SL7) */
        private String brand;
        /** 品牌中文名称 (如: 捷安特,崔克) */
        private String brandCn;
        /** 车型名称 (如: Tarmac SL7) */
        private String model;
        /** 车型年份 */
        private String modelYear;
        /** 车型分类 (1:公路, 2:山地, 3:Gravel, 4:TT, 5:城市) */
        private Integer bikeType;
        /** 车辆备注 */
        private String remark;
        /** 标记此车是否已完成【静态】几何诊断 */
        private boolean hasStaticReport;
        /** 标记此车是否已完成【动态】视觉分析 (新增) */
        private boolean hasDynamicReport;
    }
}