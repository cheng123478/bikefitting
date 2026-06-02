package com.starrysky.aries.userbike.service;

import com.starrysky.aries.bikemodel.schema.BikeModelVO;
import com.starrysky.aries.entity.BikeModel;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.entity.UserBike;
import com.starrysky.aries.entity.UserBikeSetup;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.bikemodel.repository.BikeModelRepository;
import com.starrysky.aries.userbike.repository.UserBikeRepository;
import com.starrysky.aries.userbike.repository.UserBikeSetupRepository;
import com.starrysky.aries.userbike.schema.UserBikeDTO;
import com.starrysky.aries.userbike.schema.UserBikeSetupVO;
import com.starrysky.aries.userbike.schema.UserBikeVO;
import com.starrysky.aries.util.BikeStatus;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户单车档案业务服务
 * 负责战车资产管理及实车空间坐标系解析
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/10 22:33
 */
@Service
@Slf4j
public class UserBikeService {
    @Resource
    private UserBikeRepository userBikeRepository;
    @Resource
    private BikeModelRepository bikeModelRepository;
    @Resource
    private UserBikeSetupRepository userBikeSetupRepository;

    /**
     * 保存或校准单车信息
     *
     * @param dto  单车信息DTO
     * @param user 用户信息
     * @return 单车信息VO
     */
    @Transactional(rollbackFor = Exception.class)
    public UserBikeVO saveUserBikeInfo(UserBikeDTO dto, User user) {
        // 1. Handle BikeModel
        BikeModel bikeModel;
        if (dto.getBikeModelId() != null) {
            bikeModel = bikeModelRepository.findById(dto.getBikeModelId())
                    .orElseThrow(() -> new BizException(500, "指定的单车型号不存在"));
        } else {
            // Find exist or create new (based on brand, model, year, size)
            bikeModel = bikeModelRepository.findByBrandAndModelAndModelYearAndSize(
                    dto.getBrand(), dto.getModel(), dto.getModelYear(), dto.getSize()).orElseGet(BikeModel::new);

            if (bikeModel.getId() == null) {
                // It's a new BikeModel
                bikeModel.setBrand(dto.getBrand());
                bikeModel.setBrandCn(dto.getBrandCn());
                bikeModel.setModel(dto.getModel());
                bikeModel.setModelYear(dto.getModelYear());
                bikeModel.setSize(dto.getSize());
                bikeModel.setBikeType(dto.getBikeType());
                bikeModel.setStack(dto.getStack());
                bikeModel.setReach(dto.getReach());
                bikeModel.setSeatTubeAngle(dto.getSeatTubeAngle());
                bikeModel.setHeadTubeAngle(dto.getHeadTubeAngle());
                bikeModel.setBbDrop(dto.getBbDrop());
                bikeModel.setWheelBase(dto.getWheelBase());
                bikeModel.setStandOverHeight(dto.getStandOverHeight());
                bikeModel.setImageUrl(dto.getImageUrl());
                bikeModel.setSourceType(2); // 2: 手动输入
                bikeModel.setStatus(1); // 1: 待审核

                bikeModel = bikeModelRepository.save(bikeModel);
            }
        }

        // 2. Handle UserBike
        UserBike userBike;
        if (dto.getId() != null) {
            userBike = userBikeRepository.findById(dto.getId())
                    .orElseThrow(() -> new BizException(500, "单车记录不存在"));
            if (!user.getId().equals(userBike.getUser().getId())) {
                throw new BizException(500, "无权进行此操作");
            }
        } else {
            userBike = new UserBike();
            userBike.setUser(user);
        }

        userBike.setBikeModel(bikeModel);
        userBike.setRemark(dto.getRemark());
        userBike.setStatus(BikeStatus.ACTIVE.code);
        userBike = userBikeRepository.save(userBike);

        // 3. Handle UserBikeSetup
        // Always create a new setup to keep history
        UserBikeSetup setup = new UserBikeSetup();
        setup.setUserBike(userBike);
        setup.setCrankLength(dto.getCrankLength());
        setup.setStemLength(dto.getStemLength());
        setup.setHandlebarWidth(dto.getHandlebarWidth());
        setup.setStemAngle(dto.getStemAngle());
        setup.setSpacersHeight(dto.getSpacersHeight());
        setup.setHandlebarDrop(dto.getHandlebarDrop());
        setup.setSaddleWidth(dto.getSaddleWidth());
        setup.setSaddleHeight(dto.getSaddleHeight());
        setup.setSaddleSetback(dto.getSaddleSetback());
        setup.setSaddleTilt(dto.getSaddleTilt());
        setup.setPedalType(dto.getPedalType());
        setup.setCreateTime(LocalDateTime.now());

        validateBikeSetup(setup);

        setup = userBikeSetupRepository.save(setup);

        return convertToVO(userBike, setup);
    }

    /**
     * 获取战车详情
     *
     * @param id 单车ID
     * @return 单车详情
     */
    public UserBikeVO getBikeDetail(Long id) {
        UserBike userBike = userBikeRepository.findById(id).orElse(null);
        if (userBike == null)
            return null;

        UserBikeSetup setup = userBikeSetupRepository.findTopByUserBikeIdOrderByCreateTimeDesc(id)
                .orElse(new UserBikeSetup());
        return convertToVO(userBike, setup);
    }

    /**
     * 获取车库列表
     *
     * @param userId 用户ID
     * @return 车库列表
     */
    public List<UserBikeVO> getMyGarage(Long userId) {
        return userBikeRepository.findByUserIdAndStatusOrderByCreateTimeDesc(userId, BikeStatus.ACTIVE.code).stream()
                .map(bike -> {
                    UserBikeSetup setup = userBikeSetupRepository.findTopByUserBikeIdOrderByCreateTimeDesc(bike.getId())
                            .orElse(new UserBikeSetup());
                    return convertToVO(bike, setup);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取退役单车列表
     *
     * @param userId 用户ID
     * @return 退役单车列表
     */
    public List<UserBikeVO> getRetiredBikeList(Long userId) {
        return userBikeRepository.findByUserIdAndStatusOrderByCreateTimeDesc(userId, BikeStatus.RETIRED.code).stream()
                .map(bike -> {
                    UserBikeSetup setup = userBikeSetupRepository.findTopByUserBikeIdOrderByCreateTimeDesc(bike.getId())
                            .orElse(new UserBikeSetup());
                    return convertToVO(bike, setup);
                })
                .collect(Collectors.toList());
    }

    /**
     * 退役单车
     *
     * @param user 当前登录用户
     * @param id   单车ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void retireBike(User user, Long id) {
        UserBike bike = userBikeRepository.findById(id)
                .orElseThrow(() -> new BizException(500, "该单车不存在"));
        if (!user.getId().equals(bike.getUser().getId())) {
            throw new BizException(500, "无权进行此操作");
        }
        bike.setStatus(BikeStatus.RETIRED.code);
        userBikeRepository.save(bike);
    }

    /**
     * 恢复战车至现役状态
     *
     * @param id 单车ID
     */
    @Transactional
    public void restoreBike(User user, Long id) {
        UserBike bike = userBikeRepository.findById(id).orElseThrow();
        bike.setStatus(BikeStatus.ACTIVE.code);
        userBikeRepository.save(bike);
    }

    /**
     * 删除单车（软删除）
     *
     * @param user 当前登录用户
     * @param id   单车ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBike(User user, Long id) {
        UserBike bike = userBikeRepository.findById(id)
                .orElseThrow(() -> new BizException(500, "该单车不存在"));
        if (!user.getId().equals(bike.getUser().getId())) {
            throw new BizException(500, "无权进行此操作");
        }
        bike.setStatus(BikeStatus.DELETED.code);
        userBikeRepository.save(bike);
    }

    /**
     * 【Fittor 核心转换逻辑】
     * 负责处理空数据并计算实车空间坐标
     *
     * @param entity 单车实体
     * @param setup  单车设置实体
     * @return 单车VO
     */
    public UserBikeVO convertToVO(UserBike entity, UserBikeSetup setup) {
        UserBikeVO vo = new UserBikeVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUser().getId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setRemark(entity.getRemark());
        vo.setStatus(entity.getStatus());

        // 容错处理：捕获 BikeModel 被删除的异常
        try {
            BikeModelVO bikeModelVO = new BikeModelVO();
            BeanUtils.copyProperties(entity.getBikeModel(), bikeModelVO);
            vo.setBikeModelVO(bikeModelVO);
            
            BikeModel model = entity.getBikeModel();
            if (model != null) {
                vo.setBrand(model.getBrand());
                vo.setBrandCn(model.getBrandCn());
                vo.setModel(model.getModel());
                vo.setModelYear(model.getModelYear());
                vo.setImageUrl(model.getImageUrl());
                vo.setBikeType(model.getBikeType());
                vo.setFrameSize(model.getSize()); // 车架尺码

                // --- 1. 计算 STR (车架激进程度) ---
                if (isPositive(model.getStack()) && isPositive(model.getReach())) {
                    double str = (double) model.getStack() / model.getReach();
                    vo.setFrameStr(Math.round(str * 100.0) / 100.0);
                }

                // --- 2. 计算 Effective Reach+ (实车水平总前伸) ---
                if (isPositive(model.getReach()) && setup != null && isPositive(setup.getStemLength())) {
                    double angleRad = Math.toRadians(setup.getStemAngle() != null ? Math.abs(setup.getStemAngle()) : 7.0);
                    double reachPlus = model.getReach() + (setup.getStemLength() * Math.cos(angleRad));
                    vo.setEffectiveReachPlus(Math.round(reachPlus * 10.0) / 10.0);
                }

                // --- 3. 计算 Effective Stack+ (实车垂直总高度) ---
                if (isPositive(model.getStack()) && setup != null) {
                    double angleRad = Math.toRadians(setup.getStemAngle() != null ? Math.abs(setup.getStemAngle()) : 7.0);
                    double spacers = setup.getSpacersHeight() != null ? setup.getSpacersHeight() : 0;
                    double stackPlus = model.getStack() + spacers
                            + (setup.getStemLength() != null ? setup.getStemLength() * Math.sin(angleRad) : 0);
                    vo.setEffectiveStackPlus(Math.round(stackPlus * 10.0) / 10.0);
                }
            }
        } catch (Exception e) {
            // BikeModel 已被删除，设置默认值并标记该UserBike为已删除
            log.warn("发现孤立的 UserBike 记录 | UserBike ID: {} | 关联的 BikeModel 不存在，自动软删除", entity.getId());
            
            // 软删除这个孤立的 UserBike
            entity.setStatus(BikeStatus.DELETED.code);
            userBikeRepository.save(entity);
            
            vo.setBrand("Unknown");
            vo.setBrandCn("未知品牌");
            vo.setModel("未知型号");
            vo.setStatus(BikeStatus.DELETED.code);
        }
        
        UserBikeSetupVO userBikeSetupVO = new UserBikeSetupVO();
        BeanUtils.copyProperties(setup, userBikeSetupVO);
        vo.setCurrentSetupVO(userBikeSetupVO);

        // 曲柄长度（从 setup 中获取）
        if (setup != null && setup.getCrankLength() != null) {
            vo.setCrankLength(setup.getCrankLength());
        }

        return vo;
    }

    /**
     * 实车数据常模校验
     *
     * @param setup 单车设置实体
     */
    private void validateBikeSetup(UserBikeSetup setup) {
        validateRequiredNumber("把立长度", setup.getStemLength());
        validateRequiredNumber("把立角度", setup.getStemAngle());
        validateRequiredNumber("垫圈高度", setup.getSpacersHeight());
        validateRange("把立长度", setup.getStemLength(), 40, 160, "mm");
        validateRange("把立角度", setup.getStemAngle(), -30, 30, "°");
        validateRange("垫圈高度", setup.getSpacersHeight(), 0, 80, "mm");

        // 如果填了座高，基本判定不能小于 400mm (成人车)
        if (isPositive(setup.getSaddleHeight()) && setup.getSaddleHeight() < 400) {
            throw new IllegalArgumentException("单车设定异常：座垫高度数值过小，请核实是否以毫米(mm)为单位。");
        }
    }

    private void validateRequiredNumber(String label, Number value) {
        if (value == null) {
            throw new BizException(500, String.format("[%s] 为必填项，用于换算静态 fitting 的车头落地方案。", label));
        }
    }

    private void validateRange(String label, Number value, double min, double max, String unit) {
        if (value != null && (value.doubleValue() < min || value.doubleValue() > max)) {
            throw new BizException(500, String.format("[%s] 超出正常范围，请确认单位为 %s，正常范围 %.0f-%.0f%s。", label, unit, min, max, unit));
        }
    }

    /**
     * 获取用户的在役车辆数量
     *
     * @param userId 用户ID
     * @return 在役车辆数量
     */
    public long getActiveBikeCount(Long userId) {
        return userBikeRepository.countByUserIdAndStatus(userId, BikeStatus.ACTIVE.code);
    }

    private boolean isPositive(Number n) {
        return n != null && n.doubleValue() > 0;
    }
}
