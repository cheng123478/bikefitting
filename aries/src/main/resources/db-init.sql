CREATE INDEX idx_user_phone ON usr_user (phone);

CREATE INDEX idx_user_cleanup ON usr_user (is_deleted, delete_time);

-- ===============用于动态Fitting分析=================================
-- =================================================================
-- 1. 公路车 (ROAD_BIKE, Code: 1)
-- 标准：高效率、兼顾气动与长距离耐力
-- =================================================================
INSERT INTO fit_dynamic_standard (bike_type, view_type, metric_key, min_val, max_val, weight, sensitivity, low_sensitivity, high_sensitivity, part, low_advice, high_advice, low_advice_content, high_advice_content)
VALUES (1, 'SIDE', 'maxKnee', 140.0, 150.0, 0.35, 5.0, 4.0, 10.0, 'seat', 'advice.seat.low', 'advice.seat.high', '座垫偏低，建议增加座垫高度', '座垫偏高，建议降低座垫高度'),
       (1, 'SIDE', 'minHip', 45.0, 55.0, 0.20, 4.0, 4.0, 6.0, 'handlebar', 'advice.bar.low', 'advice.bar.high', '车把偏低导致髋角过小，建议抬高把立或更换把立', '车把偏高，建议降低把立高度'),
       (1, 'SIDE', 'avgTorso', 35.0, 45.0, 0.15, 2.5, 2.5, 3.5, 'stem', 'advice.stem.low', 'advice.stem.high', '躯干角过小，建议缩短把立或升高', '躯干角过大，建议加长把立或降低'),
       (1, 'SIDE', 'maxShoulder', 80.0, 90.0, 0.10, 2.0, 2.0, 3.0, 'stem', 'advice.reach.low', 'advice.reach.high', '前伸量不足，导致手臂过于直立，建议增加把立长度', '前伸量过大，导致手臂过度伸展，建议减少把立长度'),
       (1, 'SIDE', 'cadenceRpm', 80.0, 100.0, 0.05, 1.0, 1.0, 1.0, 'body', 'advice.cadence.low', 'advice.cadence.high', '踏频过低，建议降低齿比以保护膝盖', '踏频过高，建议增加齿比以提高踩踏效率'),
       (1, 'SIDE', 'kopsOffset', -10.0, 10.0, 0.10, 2.0, 2.0, 2.0, 'seat', 'advice.kops.low', 'advice.kops.high', '膝部重心过于靠后，建议将座垫前移', '膝部重心过于靠前，建议将座垫后移'),
       (1, 'SIDE', 'ankleAtBdc', 90.0, 110.0, 0.05, 1.0, 1.0, 2.0, 'cleat', 'advice.ankle.low', 'advice.ankle.high', '下死点脚跟下坠，建议检查座垫高度或锁片位置', '下死点存在垫脚尖动作，建议微降座垫或锁片后移'),
       (1, 'FRONT', 'maxWobble', 0.0, 20.0, 0.10, 4.0, 4.0, 6.0, 'cleat', NULL, 'advice.cleat.check', NULL, '膝盖晃动较大，建议检查锁片偏置及足弓支撑'),
       (1, 'FRONT', 'maxTilt', 0.0, 5.0, 0.10, 3.0, 3.0, 4.0, 'body', NULL, 'advice.core.check', NULL, '身体侧倾明显，建议加强核心力量或检查代偿'),
       (1, 'FRONT', 'maxElbowFlare', 0.0, 15.0, 0.05, 2.0, 2.0, 2.0, 'handlebar', NULL, 'advice.bar.wide', NULL, '手肘外张明显增加风阻并影响放松，检查把宽'),
       (1, 'BACK', 'maxPelvisSway', 0.0, 15.0, 0.10, 6.0, 6.0, 8.0, 'seat', NULL, 'advice.stability.low', NULL, '骨盆不稳，建议检查座垫高度或核心稳定性'),
       (1, 'BACK', 'maxSpineTilt', 0.0, 8.0, 0.10, 3.0, 3.0, 4.0, 'body', NULL, 'advice.spine.check', NULL, '脊柱侧移较大，可能有单侧代偿发力或车把偏斜');

-- =================================================================
-- 2. 山地车 (MOUNTAIN_BIKE, Code: 2)
-- 标准：直立姿态、强调操控稳定性与重心平衡
-- =================================================================
INSERT INTO fit_dynamic_standard (bike_type, view_type, metric_key, min_val, max_val, weight, sensitivity, low_sensitivity, high_sensitivity, part, low_advice, high_advice, low_advice_content, high_advice_content)
VALUES (2, 'SIDE', 'maxKnee', 135.0, 145.0, 0.35, 5.0, 4.0, 9.0, 'seat', 'advice.seat.low', 'advice.seat.high', '座垫偏低，建议微升高度', '座垫偏高，建议微降高度'),
       (2, 'SIDE', 'minHip', 55.0, 70.0, 0.20, 3.0, 3.0, 5.0, 'handlebar', 'advice.bar.low', 'advice.bar.high', '车把过低，建议增加垫圈或更换高落差车把', '车把过高，建议减少碗组垫圈'),
       (2, 'SIDE', 'avgTorso', 45.0, 55.0, 0.15, 2.0, 2.0, 3.0, 'stem', 'advice.stem.low', 'advice.stem.high', '躯干角度过小，建议缩短把立或增加把立角度', '躯干角度过大，建议加长把立或降低角度'),
       (2, 'SIDE', 'cadenceRpm', 70.0, 90.0, 0.05, 1.0, 1.0, 1.0, 'body', 'advice.cadence.low', 'advice.cadence.high', '踏频偏低，建议提前降档应对复杂地形', '踏频过高，平路路段建议提升相应齿比'),
       (2, 'SIDE', 'kopsOffset', -15.0, 5.0, 0.10, 2.0, 2.0, 2.0, 'seat', 'advice.kops.low', 'advice.kops.high', '重心偏后影响爬坡，微调座垫位置', '重心偏前影响下坡，将座垫微后移'),
       (2, 'SIDE', 'ankleAtBdc', 90.0, 105.0, 0.05, 1.0, 1.0, 2.0, 'cleat', 'advice.ankle.low', 'advice.ankle.high', '下死点姿态偏低', '下死点有垫脚现象，建议调整座垫高度'),
       (2, 'FRONT', 'maxWobble', 0.0, 25.0, 0.15, 3.0, 3.0, 5.0, 'cleat', NULL, 'advice.cleat.check', NULL, '膝盖晃动较大，检查脚踏踩踏稳定性及锁鞋'),
       (2, 'FRONT', 'maxTilt', 0.0, 8.0, 0.10, 3.0, 3.0, 4.0, 'body', NULL, 'advice.core.check', NULL, '地形可能引入身体明显侧倾，保持核心平衡'),
       (2, 'FRONT', 'maxElbowFlare', 15.0, 40.0, 0.05, 2.0, 2.0, 2.0, 'handlebar', 'advice.bar.narrow', 'advice.bar.wide', '手肘夹紧不利越野，考虑更宽的把横', '手肘外扩过大，可能需要截短把横'),
       (2, 'BACK', 'maxPelvisSway', 0.0, 20.0, 0.15, 4.0, 4.0, 6.0, 'seat', NULL, 'advice.stability.low', NULL, '骨盆摇摆明显，检查核心稳定或略微降低座垫'),
       (2, 'BACK', 'maxSpineTilt', 0.0, 10.0, 0.10, 3.0, 3.0, 4.0, 'body', NULL, 'advice.spine.check', NULL, '脊柱过度侧移，建议检查坐姿及车座调平');

-- =================================================================
-- 3. 全地形公路车 (GRAVEL_BIKE, Code: 3)
-- 标准：介于公路与山地之间，强调长途舒适度与颠簸路面稳定
-- =================================================================
INSERT INTO fit_dynamic_standard (bike_type, view_type, metric_key, min_val, max_val, weight, sensitivity, low_sensitivity, high_sensitivity, part, low_advice, high_advice, low_advice_content, high_advice_content)
VALUES (3, 'SIDE', 'maxKnee', 138.0, 148.0, 0.35, 5.0, 4.0, 10.0, 'seat', 'advice.seat.low', 'advice.seat.high', '座垫高度偏低，调高以优化踩踏效率', '座垫高度偏高，调低以保护膝盖关节'),
       (3, 'SIDE', 'minHip', 48.0, 60.0, 0.20, 3.5, 3.5, 5.5, 'handlebar', 'advice.bar.low', 'advice.bar.high', '落差过大长途易疲劳，抬高把组', '落差偏小，适当增大以增加操控感'),
       (3, 'SIDE', 'avgTorso', 40.0, 50.0, 0.15, 2.0, 2.0, 3.0, 'stem', 'advice.stem.low', 'advice.stem.high', '前伸量偏长，建议微调短把立', '前伸量偏短，建议微调长把立'),
       (3, 'SIDE', 'cadenceRpm', 75.0, 95.0, 0.05, 1.0, 1.0, 1.0, 'body', 'advice.cadence.low', 'advice.cadence.high', '建议提高踏频以延缓肌肉疲劳', '踏频稍高，建议切换至更大盘区'),
       (3, 'SIDE', 'kopsOffset', -10.0, 10.0, 0.10, 2.0, 2.0, 2.0, 'seat', 'advice.kops.low', 'advice.kops.high', '重心靠后爬坡吃力，座垫适当前移', '膝盖重心靠前可能降低舒适度，座垫微退'),
       (3, 'SIDE', 'ankleAtBdc', 90.0, 105.0, 0.05, 1.0, 1.0, 2.0, 'cleat', 'advice.ankle.low', 'advice.ankle.high', '脚跟过度下沉增加疲劳，尝试略升座垫', '过度垫脚引发抽筋，建议下调座垫'),
       (3, 'FRONT', 'maxWobble', 0.0, 22.0, 0.15, 4.0, 4.0, 6.0, 'cleat', NULL, 'advice.cleat.check', NULL, '建议检查足部锁定位置，减少膝内侧受力'),
       (3, 'FRONT', 'maxTilt', 0.0, 6.0, 0.10, 3.0, 3.0, 4.0, 'body', NULL, 'advice.core.check', NULL, '颠簸路面需稳健支撑，控制侧倾'),
       (3, 'FRONT', 'maxElbowFlare', 10.0, 35.0, 0.05, 2.0, 2.0, 2.0, 'handlebar', 'advice.bar.narrow', 'advice.bar.wide', '通过外延把增加手臂舒展度获取更好的操控平衡', '展度极大增加风阻，可换短把横'),
       (3, 'BACK', 'maxPelvisSway', 0.0, 18.0, 0.15, 5.0, 5.0, 7.0, 'seat', NULL, 'advice.stability.low', NULL, '颠簸路段骨盆稳定性待提升，优化座垫位置'),
       (3, 'BACK', 'maxSpineTilt', 0.0, 9.0, 0.10, 3.0, 3.0, 4.0, 'body', NULL, 'advice.spine.check', NULL, '长途骑单侧偏移引发下背痛，定期调整姿势');

-- =================================================================
-- 4. 铁三/计时车 (TT_BIKE, Code: 4)
-- 标准：极致气动姿态、身体大幅前倾、极低髋角
-- =================================================================
INSERT INTO fit_dynamic_standard (bike_type, view_type, metric_key, min_val, max_val, weight, sensitivity, low_sensitivity, high_sensitivity, part, low_advice, high_advice, low_advice_content, high_advice_content)
VALUES (4, 'SIDE', 'maxKnee', 142.0, 152.0, 0.30, 5.0, 4.0, 11.0, 'seat', 'advice.seat.low', 'advice.seat.high', 'TT姿态座垫偏低，建议调高以保证腿部伸展', '座垫过高，建议调低以优化骨盆稳定性'),
       (4, 'SIDE', 'minHip', 35.0, 45.0, 0.25, 5.0, 5.0, 8.0, 'handlebar', 'advice.aerobar.low', 'advice.aerobar.high', '髋角过窄可能导致呼吸不畅，抬高休息把', '休息把偏高，建议降低体现气动表现'),
       (4, 'SIDE', 'avgTorso', 20.0, 32.0, 0.20, 3.0, 3.0, 5.0, 'handlebar', 'advice.aero.low', 'advice.aero.high', '姿态过于挺立，建议降低前端以减少风阻', '姿态过于激进，适当抬高前端以保证功率'),
       (4, 'SIDE', 'maxShoulder', 90.0, 105.0, 0.15, 2.5, 2.5, 4.0, 'stem', 'advice.reach.low', 'advice.reach.high', '上肢过于局促，建议前移休息把位置', '上肢伸展过度，建议后移休息把位置'),
       (4, 'SIDE', 'cadenceRpm', 85.0, 105.0, 0.05, 1.0, 1.0, 1.0, 'body', 'advice.cadence.low', 'advice.cadence.high', '稳定高频输出来降低肌肉负荷，建议提高踏频', '平路踏频过高易早衰，建议略降踏频或增加齿比'),
       (4, 'SIDE', 'kopsOffset', 0.0, 20.0, 0.10, 2.0, 2.0, 2.0, 'seat', 'advice.kops.low', 'advice.kops.high', 'TT重心靠前，座垫可大幅度推前', '太极端靠前易引发不稳，适当回退'),
       (4, 'SIDE', 'ankleAtBdc', 95.0, 115.0, 0.05, 1.0, 1.0, 2.0, 'cleat', 'advice.ankle.low', 'advice.ankle.high', '勿为座垫高度放低脚跟，注意踩踏流畅', '脚尖过度下探易抽筋，建议调低座垫'),
       (4, 'FRONT', 'maxWobble', 0.0, 15.0, 0.15, 4.0, 4.0, 6.0, 'cleat', NULL, 'advice.cleat.check', NULL, '破风骑行要求发力一致性高，建议微调锁片'),
       (4, 'FRONT', 'maxTilt', 0.0, 4.0, 0.10, 3.0, 3.0, 4.0, 'body', NULL, 'advice.core.check', NULL, '气流要求上半身绝对静止，注意核心肌群激活'),
       (4, 'FRONT', 'maxElbowFlare', 0.0, 10.0, 0.05, 2.0, 2.0, 2.0, 'handlebar', NULL, 'advice.bar.wide', NULL, '若外张破坏风阻面，需聚拢肘垫'),
       (4, 'BACK', 'maxPelvisSway', 0.0, 10.0, 0.10, 8.0, 8.0, 12.0, 'seat', NULL, 'advice.stability.low', NULL, '核心不稳骨盆晃动，优化座垫支撑'),
       (4, 'BACK', 'maxSpineTilt', 0.0, 6.0, 0.10, 3.0, 3.0, 4.0, 'body', NULL, 'advice.spine.check', NULL, '脊柱居中以获得完整导流效果，排查不对称发力');

-- =================================================================
-- 5. 城市通勤车 (CITY_BIKE, Code: 5)
-- 标准：极度直立舒适、便于观察路况、膝盖伸展度适中
-- =================================================================
INSERT INTO fit_dynamic_standard (bike_type, view_type, metric_key, min_val, max_val, weight, sensitivity, low_sensitivity, high_sensitivity, part, low_advice, high_advice, low_advice_content, high_advice_content)
VALUES (5, 'SIDE', 'maxKnee', 130.0, 142.0, 0.35, 4.0, 3.0, 7.0, 'seat', 'advice.seat.low', 'advice.seat.high', '座垫过低增加膝盖负担，建议调高', '座垫过高踩踏不自然，建议调低'),
       (5, 'SIDE', 'minHip', 65.0, 85.0, 0.25, 2.0, 2.0, 3.0, 'handlebar', 'advice.bar.low', 'advice.bar.high', '车把偏低，调整高度以减轻背部压力', '车把偏高，建议适当下调'),
       (5, 'SIDE', 'avgTorso', 60.0, 80.0, 0.20, 1.5, 1.5, 2.5, 'stem', 'advice.reach.low', 'advice.reach.high', '姿态不够舒展，建议调整把立', '姿态过于前倾，建议换短把立'),
       (5, 'SIDE', 'cadenceRpm', 60.0, 80.0, 0.05, 1.0, 1.0, 1.0, 'body', 'advice.cadence.low', 'advice.cadence.high', '城市随性骑行，避免长期重踩', '踩踏非常轻快，注意不要空转'),
       (5, 'SIDE', 'kopsOffset', -20.0, 0.0, 0.10, 2.0, 2.0, 2.0, 'seat', 'advice.kops.low', 'advice.kops.high', '城市车可适当后靠重心提升舒适度', '向下垂直更适合放松坐姿'),
       (5, 'SIDE', 'ankleAtBdc', 80.0, 100.0, 0.05, 1.0, 1.0, 2.0, 'pedal', 'advice.ankle.low', 'advice.ankle.high', '平底鞋踩踏注意贴合自然', '脚掌贴合面可以放松接触'),
       (5, 'FRONT', 'maxWobble', 0.0, 35.0, 0.10, 2.0, 2.0, 4.0, 'cleat', NULL, 'advice.technique.check', NULL, '建议通过平衡练习增强踩踏稳定性'),
       (5, 'FRONT', 'maxTilt', 0.0, 10.0, 0.10, 3.0, 3.0, 4.0, 'body', NULL, 'advice.core.check', NULL, '稍微放松身体，避免过度摇摆即可'),
       (5, 'FRONT', 'maxElbowFlare', 20.0, 50.0, 0.05, 2.0, 2.0, 2.0, 'handlebar', 'advice.bar.narrow', 'advice.bar.wide', '可抬起双臂更为舒展的骑行', '手肘自然外扩，过大可换窄把横'),
       (5, 'BACK', 'maxPelvisSway', 0.0, 25.0, 0.10, 3.0, 3.0, 5.0, 'seat', NULL, 'advice.stability.low', NULL, '建议检查座垫舒适度或调整高度'),
       (5, 'BACK', 'maxSpineTilt', 0.0, 12.0, 0.10, 3.0, 3.0, 4.0, 'body', NULL, 'advice.spine.check', NULL, '通勤坐姿自由，注意背部不要单侧紧绷');


-- 测试数据
INSERT INTO `usr_user` (`account_non_expired`, `account_non_locked`, `birthday`, `credentials_non_expired`, `enabled`, `gender`, `is_deleted`, `login_failure_count`, `delete_time`, `id`, `lock_time`, `registration_time`, `old_phone`, `phone`, `introduction`, `name`, `email`, `location`, `password`, `avatar`) VALUES (b'1', b'1', NULL, b'1', b'1', NULL, b'0', 0, NULL, 2028760927918231552, NULL, '2026-03-03 17:14:32.428347', NULL, '15932673121', NULL, '骑士3121', NULL, NULL, NULL, NULL);
INSERT INTO `fit_user_body` (`arm_raise_angle`, `breast_height`, `calf_length`, `foot_length`, `height`, `ischial_distance`, `long_arm_length`, `short_arm_length`, `shoulder_width`, `span_height`, `squat_depth`, `squat_stability_score`, `thigh_length`, `thoracic_extension_angle`, `thoracic_pain_level`, `thoracic_rotation_angle`, `create_time`, `id`, `user_id`) VALUES (160, 1450, 430, NULL, 1800, 120, 330, 300, 450, 820, -5, 2, 450, 20, 4, 40, '2026-03-14 21:45:12.637003', 1, 2028760927918231552);
INSERT INTO `bike_model` (`bb_drop`, `bike_type`, `head_tube_angle`, `reach`, `seat_tube_angle`, `source_type`, `stack`, `stand_over_height`, `status`, `wheel_base`, `id`, `brand`, `brand_cn`, `image_url`, `model`, `model_year`, `size`, `source_url`) VALUES (66, 1, 74, 395, 74, 2, 557, 782, 1, NULL, 1, 'Giant', '捷安特', '/resource/2028760927918231552/bike/1f421434b63348d897b059ed468b19df.png', 'S100', '2025', 'M', NULL);
INSERT INTO `fit_user_bike` (`status`, `bike_model_id`, `create_time`, `id`, `user_id`, `remark`) VALUES (1, 1, '2026-03-14 21:49:34.450712', 1, 2028760927918231552, '日常通勤');
INSERT INTO `fit_user_bike_setup` (`crank_length`, `handlebar_drop`, `handlebar_width`, `pedal_type`, `saddle_height`, `saddle_setback`, `saddle_tilt`, `saddle_width`, `spacers_height`, `stem_angle`, `stem_length`, `create_time`, `fit_user_bike_id`, `id`) VALUES (NULL, NULL, NULL, 1, NULL, 20, 0, NULL, NULL, NULL, NULL, '2026-03-14 21:49:34.454723', 1, 1);

-- =================================================================
-- 迁移脚本: 添加平板支撑评分字段 (plank_score)
-- 执行时间: 2026-04-09
-- 说明: 核心稳定性模块增加平板支撑评分，计算公式更新为 coreScore = squatScore * 0.6 + plankScore * 0.4
-- =================================================================
-- ALTER TABLE fit_user_body ADD COLUMN plank_score INT COMMENT '平板支撑评分(1-5)' AFTER squat_stability_score;
