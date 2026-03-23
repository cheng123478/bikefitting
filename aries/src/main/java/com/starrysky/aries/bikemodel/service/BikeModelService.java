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
     */
    public List<BikeModelSearchVO> searchModels(String keyword) {
        List<Object[]> results = bikeModelRepository.findDistinctBrandAndModelByKeyword(keyword);
        return results.stream()
                .map(row -> new BikeModelSearchVO((String) row[0], (String) row[1], (String) row[2]))
                .collect(Collectors.toList());
    }

    /**
     * 获取选定型号的所有变体 (VO格式)
     */
    public List<BikeModelVO> getModelTree(String brand, String model) {
        List<BikeModel> models = bikeModelRepository.findByBrandAndModel(brand, model);
        return models.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 保存或更新单车模型
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
     */
    @Transactional
    public void deleteModel(Long id) {
        bikeModelRepository.deleteById(id);
    }

    private BikeModelVO convertToVO(BikeModel entity) {
        BikeModelVO vo = new BikeModelVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
