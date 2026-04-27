package com.starrysky.aries.bikemodel.service;

import com.starrysky.aries.bikemodel.repository.BikeModelRepository;
import com.starrysky.aries.bikemodel.schema.BikeModelDTO;
import com.starrysky.aries.bikemodel.schema.BikeModelSearchVO;
import com.starrysky.aries.bikemodel.schema.BikeModelVO;
import com.starrysky.aries.entity.BikeModel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 单车模型服务
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/11 14:13
 */
@Service
@RequiredArgsConstructor
public class BikeModelService {

    private final BikeModelRepository bikeModelRepository;

    /**
     * 模糊搜索品牌和型号名
     *
     * @param keyword 搜索关键字
     * @return 搜索结果
     */
    public List<BikeModelSearchVO> searchModels(String keyword) {
        // 如果关键词包含空格，尝试分词搜索（品牌 + 型号）
        if (keyword != null && keyword.contains(" ")) {
            String[] parts = keyword.trim().split("\\s+");
            if (parts.length >= 2) {
                String brandKeyword = parts[0];
                String modelKeyword = parts[1];
                // 如果还有更多词，合并到型号搜索中
                for (int i = 2; i < parts.length; i++) {
                    modelKeyword += " " + parts[i];
                }
                
                List<Object[]> results = bikeModelRepository.findDistinctBrandAndModelBySeparatedKeywords(
                        brandKeyword, modelKeyword);
                if (!results.isEmpty()) {
                    return results.stream()
                            .map(row -> new BikeModelSearchVO((String) row[0], (String) row[1], (String) row[2]))
                            .collect(Collectors.toList());
                }
            }
        }
        
        // 否则使用原有的单关键词搜索
        List<Object[]> results = bikeModelRepository.findDistinctBrandAndModelByKeyword(keyword);
        return results.stream()
                .map(row -> new BikeModelSearchVO((String) row[0], (String) row[1], (String) row[2]))
                .collect(Collectors.toList());
    }

    /**
     * 获取单车模型树
     *
     * @param brand 品牌
     * @param model 型号
     * @return 单车模型树
     */
    public List<BikeModelVO> getModelTree(String brand, String model) {
        List<BikeModel> models = bikeModelRepository.findByBrandAndModel(brand, model);
        return models.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 保存单车模型
     *
     * @param dto 模型数据
     * @return 模型ID
     */
    @Transactional
    public Long saveModel(BikeModelDTO dto) {
        BikeModel bikeModel;
        if (dto.getId() != null) {
            bikeModel = bikeModelRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("BikeModel not found"));
        } else {
            bikeModel = new BikeModel();
        }
        BeanUtils.copyProperties(dto, bikeModel);
        // 特殊字段映射 (VO/DTO 字段名不一致的情况)
        if (dto.getStack() != null) bikeModel.setStack(dto.getStack());
        if (dto.getReach() != null) bikeModel.setReach(dto.getReach());
        if (dto.getStandOverHeight() != null) bikeModel.setStandOverHeight(dto.getStandOverHeight());

        return bikeModelRepository.save(bikeModel).getId();
    }

    /**
     * 删除单车模型
     *
     * @param id 模型ID
     */
    @Transactional
    public void deleteModel(Long id) {
        bikeModelRepository.deleteById(id);
    }

    /**
     * 单车模型实体转换为VO
     *
     * @param entity 单车模型实体
     * @return 单车模型VO
     */
    private BikeModelVO convertToVO(BikeModel entity) {
        BikeModelVO vo = new BikeModelVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
