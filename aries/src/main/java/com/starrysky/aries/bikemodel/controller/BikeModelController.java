package com.starrysky.aries.bikemodel.controller;

import com.starrysky.aries.bikemodel.schema.BikeModelDTO;
import com.starrysky.aries.bikemodel.schema.BikeModelSearchVO;
import com.starrysky.aries.bikemodel.schema.BikeModelVO;
import com.starrysky.aries.bikemodel.schema.ShowroomResponseDTO;
import com.starrysky.aries.bikemodel.service.BikeModelService;
import com.starrysky.aries.util.ResponseResult;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 单车模型控制器
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/11 14:10
 */
@RestController
@RequestMapping("/bike-model")
public class BikeModelController {

    @Resource
    private BikeModelService bikeModelService;

    /**
     * 模糊搜索品牌和型号名
     *
     * @param keyword 搜索关键字
     * @return 搜索结果
     */
    @GetMapping("/search")
    public ResponseResult<List<BikeModelSearchVO>> search(@RequestParam(value = "keyword") String keyword) {
        return ResponseResult.success(bikeModelService.searchModels(keyword));
    }

    /**
     * 获取单车模型树
     *
     * @param brand 品牌
     * @param model 型号
     * @return 单车模型树
     */
    @GetMapping("/tree")
    public ResponseResult<List<BikeModelVO>> getTree(@RequestParam String brand, @RequestParam String model) {
        return ResponseResult.success(bikeModelService.getModelTree(brand, model));
    }

    /**
     * 保存单车模型
     *
     * @param dto 模型数据
     * @return 模型ID
     */
    @PostMapping("/save")
    public ResponseResult<Long> save(@RequestBody BikeModelDTO dto) {
        return ResponseResult.success(bikeModelService.saveModel(dto));
    }

    /**
     * 删除单车模型
     *
     * @param id 模型ID
     * @return 无返回值
     */
    @DeleteMapping("/{id}")
    public ResponseResult<Void> delete(@PathVariable Long id) {
        bikeModelService.deleteModel(id);
        return ResponseResult.success();
    }

    /**
     * 获取单车展厅列表
     *
     * @param keyword 搜索关键词
     * @param bikeTypes 单车类型筛选（支持多个，逗号分隔）
     * @param brands 品牌筛选（支持多个，逗号分隔）
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param sortBy 排序方式
     * @param page 页码
     * @param size 每页数量
     * @return 展厅单车分页数据
     */
    @GetMapping("/showroom")
    public ResponseResult<ShowroomResponseDTO> getShowroomBikes(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "bikeTypes", required = false) String bikeTypes,
            @RequestParam(value = "brands", required = false) String brands,
            @RequestParam(value = "minPrice", required = false) Integer minPrice,
            @RequestParam(value = "maxPrice", required = false) Integer maxPrice,
            @RequestParam(value = "sortBy", required = false, defaultValue = "default") String sortBy,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        return ResponseResult.success(bikeModelService.getShowroomBikes(keyword, bikeTypes, brands, minPrice, maxPrice, sortBy, pageable));
    }

    /**
     * 获取单车详情
     *
     * @param id 单车ID
     * @return 单车详情
     */
    @GetMapping("/{id}")
    public ResponseResult<BikeModelVO> getBikeDetail(@PathVariable Long id) {
        return ResponseResult.success(bikeModelService.getBikeDetail(id));
    }

    /**
     * 根据车型名称获取所有尺寸列表
     *
     * @param modelName 车型名称
     * @param brand 品牌（可选，用于更精确匹配）
     * @return 该车型的所有尺寸列表
     */
    @GetMapping("/model/{modelName}")
    public ResponseResult<List<BikeModelVO>> getModelSizes(
            @PathVariable String modelName,
            @RequestParam(value = "brand", required = false) String brand) {
        return ResponseResult.success(bikeModelService.getModelSizes(modelName, brand));
    }
}
