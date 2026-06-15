package com.starrysky.aries.peloton.service;

import com.starrysky.aries.entity.BikeModel;
import com.starrysky.aries.entity.PelotonTeam;
import com.starrysky.aries.entity.TeamGarage;
import com.starrysky.aries.entity.TeamMember;
import com.starrysky.aries.entity.User;
import com.starrysky.aries.entity.UserBike;
import com.starrysky.aries.exception.BizException;
import com.starrysky.aries.peloton.repository.PelotonTeamRepository;
import com.starrysky.aries.peloton.repository.TeamGarageRepository;
import com.starrysky.aries.peloton.repository.TeamMemberRepository;
import com.starrysky.aries.peloton.schema.PelotonPageResponse;
import com.starrysky.aries.peloton.schema.TeamGarageVO;
import com.starrysky.aries.user.repository.UserRepository;
import com.starrysky.aries.userbike.repository.UserBikeRepository;
import com.starrysky.aries.util.BikeType;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TeamGarageService
 * 车队车库管理服务（录入、移除、查询）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Slf4j
@Service
public class TeamGarageService {

    @Resource
    private PelotonTeamRepository pelotonTeamRepository;

    @Resource
    private TeamMemberRepository teamMemberRepository;

    @Resource
    private TeamGarageRepository teamGarageRepository;

    @Resource
    private UserBikeRepository userBikeRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private TeamMemberService teamMemberService;

    /** 车库卡片展示色值调色板（按 bikeId 取模循环使用） */
    private static final String[] BIKE_COLORS = {
            "#e74c3c", "#2c3e50", "#27ae60", "#8e44ad",
            "#2980b9", "#d35400", "#16a085", "#34495e",
            "#f39c12", "#1abc9c"
    };

    /**
     * 获取车队车库列表
     * <p>
     * 聚合车主头像、单车品牌/型号/尺码等信息，供前端直接渲染车库卡片。
     *
     * @param teamId 车队ID
     * @return 车库列表（含 total）
     */
    public PelotonPageResponse<TeamGarageVO> getTeamGarage(Long teamId) {
        pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        List<TeamGarage> records = teamGarageRepository
                .findByTeamIdAndStatusOrderByCreatedAtDesc(teamId, TeamGarage.STATUS_ACTIVE);

        if (records.isEmpty()) {
            return PelotonPageResponse.<TeamGarageVO>builder()
                    .records(Collections.emptyList())
                    .total(0L)
                    .page(1)
                    .size(0)
                    .pages(0)
                    .build();
        }

        List<Long> bikeIds = records.stream().map(TeamGarage::getBikeId).collect(Collectors.toList());
        Map<Long, UserBike> bikeMap = userBikeRepository.findAllById(bikeIds).stream()
                .collect(Collectors.toMap(UserBike::getId, b -> b));

        Set<Long> ownerIds = records.stream().map(TeamGarage::getUserId).collect(Collectors.toSet());
        Map<Long, User> ownerMap = ownerIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(ownerIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        List<TeamGarageVO> voList = records.stream()
                .map(tg -> buildGarageVO(tg, bikeMap.get(tg.getBikeId()), ownerMap.get(tg.getUserId())))
                .collect(Collectors.toList());

        int count = voList.size();
        return PelotonPageResponse.<TeamGarageVO>builder()
                .records(voList)
                .total((long) count)
                .page(1)
                .size(count)
                .pages(1)
                .build();
    }

    /**
     * 将个人车库中的单车同步至车队车库
     * <p>
     * 当前用户必须已是车队成员。同一辆单车不能重复录入。
     *
     * @param teamId 车队ID
     * @param bikeId 个人车库单车ID
     * @param user   当前登录用户
     * @return 新增的车库记录
     */
    @Transactional(rollbackFor = Exception.class)
    public TeamGarageVO addBikeToGarage(Long teamId, Long bikeId, User user) {
        pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, user.getId(), TeamMember.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(403, "非车队成员无法录入车库"));

        UserBike userBike = userBikeRepository.findById(bikeId)
                .orElseThrow(() -> new BizException(404, "单车不存在"));
        if (!Objects.equals(userBike.getUser().getId(), user.getId())) {
            throw new BizException(403, "只能录入自己的单车");
        }

        // #2 fix: 仅查询一次，复用结果
        var existingRecord = teamGarageRepository.findByTeamIdAndBikeId(teamId, bikeId);

        existingRecord
                .filter(tg -> tg.getStatus() == TeamGarage.STATUS_ACTIVE)
                .ifPresent(tg -> {
                    throw new BizException(400, "该单车已在车队车库中");
                });

        // 若之前被移除过，则重新激活
        if (existingRecord.isPresent()) {
            TeamGarage existing = existingRecord.get();
            existing.setStatus(TeamGarage.STATUS_ACTIVE);
            teamGarageRepository.save(existing);
            return buildGarageVO(existing, userBike, user);
        }

        TeamGarage tg = TeamGarage.builder()
                .teamId(teamId)
                .userId(user.getId())
                .bikeId(bikeId)
                .status(TeamGarage.STATUS_ACTIVE)
                .build();
        tg = teamGarageRepository.save(tg);
        log.info("用户 {} 在车队 {} 车库中录入单车 {}", user.getId(), teamId, bikeId);
        return buildGarageVO(tg, userBike, user);
    }

    /**
     * 从车队车库中移除单车
     * <p>
     * 车主本人或队长/管理员可操作。
     *
     * @param teamId 车队ID
     * @param bikeId 个人车库单车ID
     * @param user   当前登录用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeBikeFromGarage(Long teamId, Long bikeId, User user) {
        PelotonTeam team = pelotonTeamRepository.findById(teamId)
                .orElseThrow(() -> new BizException(404, "车队不存在"));

        TeamGarage tg = teamGarageRepository.findByTeamIdAndBikeId(teamId, bikeId)
                .filter(r -> r.getStatus() == TeamGarage.STATUS_ACTIVE)
                .orElseThrow(() -> new BizException(404, "该单车不在车队车库中"));

        boolean isOwner = Objects.equals(tg.getUserId(), user.getId());
        boolean isCreator = Objects.equals(team.getCreatorId(), user.getId());
        if (!isOwner && !isCreator) {
            TeamMember operatorMember = teamMemberRepository
                    .findByTeamIdAndUserIdAndStatus(teamId, user.getId(), TeamMember.STATUS_ACTIVE)
                    .orElseThrow(() -> new BizException(403, "非车队成员无权操作"));
            if (operatorMember.getRole() != TeamMember.ROLE_ADMIN) {
                throw new BizException(403, "仅车主、队长或管理员可移除单车");
            }
        } else if (isCreator) {
            // 防御性校验：确保队长本人仍是车队活跃成员
            teamMemberRepository.findByTeamIdAndUserIdAndStatus(teamId, user.getId(), TeamMember.STATUS_ACTIVE)
                    .orElseThrow(() -> new BizException(403, "您已不是该车队成员"));
        }

        tg.setStatus(TeamGarage.STATUS_REMOVED);
        teamGarageRepository.save(tg);
        log.info("用户 {} 在车队 {} 车库中移除了单车 {}", user.getId(), teamId, bikeId);
    }

    // ============================ 内部方法 ============================

    /**
     * 车队车库记录 -> VO（聚合车主 + 单车信息）
     */
    private TeamGarageVO buildGarageVO(TeamGarage tg, UserBike userBike, User owner) {
        BikeModel bm = (userBike != null) ? userBike.getBikeModel() : null;

        TeamGarageVO.TeamGarageVOBuilder builder = TeamGarageVO.builder()
                .id(tg.getId())
                .bikeId(tg.getBikeId())
                .ownerId(tg.getUserId())
                .addedAt(tg.getCreatedAt())
                .color(BIKE_COLORS[(int) (tg.getBikeId() % BIKE_COLORS.length)]);

        if (owner != null) {
            builder.ownerName(owner.getName());
            builder.ownerAvatar(owner.getAvatar());
        }

        if (bm != null) {
            builder.brand(bm.getBrand());
            builder.brandCn(bm.getBrandCn());
            builder.model(bm.getModel());
            builder.frameSize(bm.getSize());
            builder.modelYear(bm.getModelYear());
            builder.imageUrl(bm.getImageUrl());
            // #9 fix: 复用 BikeType 枚举
            builder.type(convertBikeType(bm.getBikeType()));
        }

        return builder.build();
    }

    /** BikeType.code -> displayName 映射（避免每次线性遍历枚举值） */
    private static final Map<Integer, String> BIKE_TYPE_MAP = new HashMap<>();
    static {
        for (BikeType bt : BikeType.values()) {
            BIKE_TYPE_MAP.put(bt.code, bt.displayName);
        }
    }

    /**
     * 单车类型整数 -> 中文文本（O(1)常量查找）
     */
    private String convertBikeType(Integer bikeType) {
        if (bikeType == null) return BikeType.ROAD_BIKE.displayName;
        return BIKE_TYPE_MAP.getOrDefault(bikeType, BikeType.ROAD_BIKE.displayName);
    }
}
