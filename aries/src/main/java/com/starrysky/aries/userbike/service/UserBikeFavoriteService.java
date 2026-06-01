package com.starrysky.aries.userbike.service;

import com.starrysky.aries.entity.BikeModel;
import com.starrysky.aries.entity.BikeModelStats;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.entity.UserBikeFavorite;
import com.starrysky.aries.bikemodel.repository.BikeModelRepository;
import com.starrysky.aries.bikemodel.repository.BikeModelStatsRepository;
import com.starrysky.aries.userbike.repository.UserBikeFavoriteRepository;
import com.starrysky.aries.userbike.schema.FavoriteVO;
import com.starrysky.aries.userbike.schema.PageResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 用户收藏Service
 *
 * @author adi
 * @version 1.0
 * @since 2026/05/25
 */
@Service
@Slf4j
public class UserBikeFavoriteService {
    
    @Resource(name = "userBikeUserBikeFavoriteRepository")
    private UserBikeFavoriteRepository favoriteRepository;
    
    @Resource
    private BikeModelRepository bikeModelRepository;
    
    @Resource
    private BikeModelStatsRepository bikeModelStatsRepository;
    
    /**
     * 收藏车型
     *
     * @param bikeModelId 车型ID
     * @param user 当前用户
     * @return 收藏VO
     */
    @Transactional
    public FavoriteVO favoriteBikeModel(Long bikeModelId, User user) {
        try {
            // 检查车型是否存在
            BikeModel bikeModel = bikeModelRepository.findById(bikeModelId)
                    .orElseThrow(() -> new RuntimeException("车型不存在"));
            
            // 检查是否已收藏
            Optional<UserBikeFavorite> existing = favoriteRepository
                    .findByUserIdAndBikeModelId(user.getId(), bikeModelId);
            
            UserBikeFavorite favorite;
            if (existing.isPresent()) {
                // 已收藏，直接返回
                favorite = existing.get();
                log.info("用户 {} 已收藏车型 {}", user.getId(), bikeModelId);
            } else {
                // 新增收藏
                favorite = new UserBikeFavorite();
                favorite.setUser(user);
                favorite.setBikeModel(bikeModel);
                favorite = favoriteRepository.save(favorite);
                
                // 更新车型统计表的收藏量
                incrementFavoriteCount(bikeModel);
                
                log.info("用户 {} 收藏车型 {}", user.getId(), bikeModelId);
            }
            
            return convertToVO(favorite);
        } catch (Exception e) {
            log.error("收藏车型失败, 用户: {}, 车型ID: {}", user.getId(), bikeModelId, e);
            throw e;
        }
    }
    
    /**
     * 取消收藏
     *
     * @param favoriteId 收藏记录ID
     * @param user 当前用户
     */
    @Transactional
    public void unfavorite(Long favoriteId, User user) {
        favoriteRepository.deleteByUserIdAndId(user.getId(), favoriteId);
        log.info("用户 {} 取消收藏记录 {}", user.getId(), favoriteId);
    }
    
    /**
     * 取消收藏(通过车型ID)
     *
     * @param bikeModelId 车型ID
     * @param user 当前用户
     */
    @Transactional
    public void unfavoriteByBikeModelId(Long bikeModelId, User user) {
        Optional<UserBikeFavorite> existing = favoriteRepository
                .findByUserIdAndBikeModelId(user.getId(), bikeModelId);
        
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            
            // 获取车型信息以更新统计
            BikeModel bikeModel = bikeModelRepository.findById(bikeModelId).orElse(null);
            if (bikeModel != null) {
                // 更新车型统计表的收藏量
                decrementFavoriteCount(bikeModel);
            }
            
            log.info("用户 {} 取消收藏车型 {}", user.getId(), bikeModelId);
        }
    }
    
    /**
     * 获取用户收藏列表(分页)
     *
     * @param user 当前用户
     * @param page 页码
     * @param size 每页数量
     * @return 分页收藏列表
     */
    public PageResponse<FavoriteVO> getFavoriteList(User user, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        
        Page<UserBikeFavorite> favoritePage = favoriteRepository.findByUserId(user.getId(), pageable);
        
        List<FavoriteVO> voList = favoritePage.getContent().stream()
                .map(this::convertToVO)
                .toList();
        
        return PageResponse.<FavoriteVO>builder()
                .records(voList)
                .total(favoritePage.getTotalElements())
                .page(page)
                .size(size)
                .pages(favoritePage.getTotalPages())
                .build();
    }
    
    /**
     * 检查是否已收藏
     *
     * @param bikeModelId 车型ID
     * @param user 当前用户
     * @return 是否已收藏
     */
    public boolean isFavorited(Long bikeModelId, User user) {
        return favoriteRepository.findByUserIdAndBikeModelId(user.getId(), bikeModelId).isPresent();
    }
    
    /**
     * 统计用户收藏数量
     *
     * @param user 当前用户
     * @return 收藏数量
     */
    public Long countFavorites(User user) {
        return favoriteRepository.countByUserId(user.getId());
    }
    
    /**
     * 转换为VO
     */
    private FavoriteVO convertToVO(UserBikeFavorite favorite) {
        BikeModel bikeModel = favorite.getBikeModel();
        
        return FavoriteVO.builder()
                .id(favorite.getId())
                .bikeModelId(bikeModel.getId())
                .bikeModelName(bikeModel.getModel())
                .bikeModelBrand(bikeModel.getBrand())
                .bikeModelBrandCn(bikeModel.getBrandCn())
                .bikeModelImg(bikeModel.getImageUrl())
                .bikeModelDesc(bikeModel.getDescription())
                .bikeModelType(bikeModel.getBikeType())
                .bikeModelPrice(bikeModel.getPrice())
                .createTime(favorite.getCreateTime())
                .build();
    }
    
    /**
     * 增加车型收藏量
     * 策略：使用同一型号中最小id的尺寸作为代表
     *
     * @param bikeModel 车型
     */
    private void incrementFavoriteCount(BikeModel bikeModel) {
        try {
            // 找到代表尺寸的id（同一型号中最小id）
            Long representativeId = bikeModelRepository.findRepresentativeByModel(bikeModel.getModel())
                    .map(BikeModel::getId)
                    .orElse(bikeModel.getId());
            
            int updated = bikeModelStatsRepository.incrementFavoriteCount(representativeId);
            
            if (updated == 0) {
                // 记录不存在，创建新记录
                BikeModelStats stats = new BikeModelStats();
                stats.setBikeModelId(representativeId);
                stats.setViewCount(0L);
                stats.setFavoriteCount(1L);
                stats.setRecommendWeight(0.0);
                stats.setCreateTime(java.time.LocalDateTime.now());
                bikeModelStatsRepository.save(stats);
            }
        } catch (Exception e) {
            log.error("增加车型收藏量失败: model={}", bikeModel.getModel(), e);
        }
    }
    
    /**
     * 减少车型收藏量
     *
     * @param bikeModel 车型
     */
    private void decrementFavoriteCount(BikeModel bikeModel) {
        try {
            // 找到代表尺寸的id（同一型号中最小id）
            Long representativeId = bikeModelRepository.findRepresentativeByModel(bikeModel.getModel())
                    .map(BikeModel::getId)
                    .orElse(bikeModel.getId());
            
            bikeModelStatsRepository.decrementFavoriteCount(representativeId);
        } catch (Exception e) {
            log.error("减少车型收藏量失败: model={}", bikeModel.getModel(), e);
        }
    }
}
