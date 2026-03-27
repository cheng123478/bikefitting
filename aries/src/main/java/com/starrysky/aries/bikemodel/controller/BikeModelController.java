package com.starrysky.aries.bikemodel.controller;

import com.starrysky.aries.bikemodel.schema.BikeModelDTO;
import com.starrysky.aries.bikemodel.schema.BikeModelSearchVO;
import com.starrysky.aries.bikemodel.schema.BikeModelVO;
import com.starrysky.aries.bikemodel.service.BikeModelService;
import com.starrysky.aries.util.ResponseResult;
import jakarta.annotation.Resource;
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
    public ResponseResult<List<BikeModelSearchVO>> search(@RequestParam String keyword) {
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
}
