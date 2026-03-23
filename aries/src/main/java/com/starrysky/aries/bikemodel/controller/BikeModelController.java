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
     */
    @GetMapping("/search")
    public ResponseResult<List<BikeModelSearchVO>> search(@RequestParam String keyword) {
        return ResponseResult.success(bikeModelService.searchModels(keyword));
    }

    /**
     * 获取选定型号的所有变体 (年份/尺码/几何)
     */
    @GetMapping("/tree")
    public ResponseResult<List<BikeModelVO>> getTree(@RequestParam String brand, @RequestParam String model) {
        return ResponseResult.success(bikeModelService.getModelTree(brand, model));
    }

    /**
     * 保存单车模型
     */
    @PostMapping("/save")
    public ResponseResult<Long> save(@RequestBody BikeModelDTO dto) {
        return ResponseResult.success(bikeModelService.saveModel(dto));
    }

    /**
     * 删除单车模型
     */
    @DeleteMapping("/{id}")
    public ResponseResult<Void> delete(@PathVariable Long id) {
        bikeModelService.deleteModel(id);
        return ResponseResult.success();
    }
}
