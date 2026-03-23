CREATE INDEX idx_user_phone ON usr_user (phone);

CREATE INDEX idx_user_cleanup ON usr_user (is_deleted, delete_time);

-- ===============用于动态Fitting分析=================================
-- =================================================================
-- 1. 公路车 (ROAD_BIKE, Code: 1)
-- 标准：高效率、兼顾气动与长距离耐力
-- =================================================================
INSERT INTO fit_dynamic_standard (bike_type, view_type, metric_key, min_val, max_val, weight, sensitivity, low_sensitivity, high_sensitivity, part, low_advice, high_advice)
VALUES (1, 'SIDE', 'maxKnee', 140.0, 150.0, 0.35, 5.0, 4.0, 10.0, 'seat', 'advice.seat.low', 'advice.seat.high'),
       (1, 'SIDE', 'minHip', 45.0, 55.0, 0.20, 4.0, 4.0, 6.0, 'handlebar', 'advice.bar.low', 'advice.bar.high'),
       (1, 'SIDE', 'avgTorso', 35.0, 45.0, 0.15, 2.5, 2.5, 3.5, 'stem', 'advice.stem.low', 'advice.stem.high'),
       (1, 'SIDE', 'maxShoulder', 80.0, 90.0, 0.10, 2.0, 2.0, 3.0, 'stem', 'advice.reach.low', 'advice.reach.high'),
       (1, 'FRONT', 'maxWobble', 0.0, 20.0, 0.10, 4.0, 4.0, 6.0, 'cleat', NULL, 'advice.cleat.check'),
       (1, 'BACK', 'maxPelvisSway', 0.0, 15.0, 0.10, 6.0, 6.0, 8.0, 'seat', NULL, 'advice.stability.low');

-- =================================================================
-- 2. 山地车 (MOUNTAIN_BIKE, Code: 2)
-- 标准：直立姿态、强调操控稳定性与重心平衡
-- =================================================================
INSERT INTO fit_dynamic_standard (bike_type, view_type, metric_key, min_val, max_val, weight, sensitivity, low_sensitivity, high_sensitivity, part, low_advice, high_advice)
VALUES (2, 'SIDE', 'maxKnee', 135.0, 145.0, 0.35, 5.0, 4.0, 9.0, 'seat', 'advice.seat.low', 'advice.seat.high'),
       (2, 'SIDE', 'minHip', 55.0, 70.0, 0.20, 3.0, 3.0, 5.0, 'handlebar', 'advice.bar.low', 'advice.bar.high'),
       (2, 'SIDE', 'avgTorso', 45.0, 55.0, 0.15, 2.0, 2.0, 3.0, 'stem', 'advice.stem.low', 'advice.stem.high'),
       (2, 'FRONT', 'maxWobble', 0.0, 25.0, 0.15, 3.0, 3.0, 5.0, 'cleat', NULL, 'advice.cleat.check'),
       (2, 'BACK', 'maxPelvisSway', 0.0, 20.0, 0.15, 4.0, 4.0, 6.0, 'seat', NULL, 'advice.stability.low');

-- =================================================================
-- 3. 全地形公路车 (GRAVEL_BIKE, Code: 3)
-- 标准：介于公路与山地之间，强调长途舒适度与颠簸路面稳定
-- =================================================================
INSERT INTO fit_dynamic_standard (bike_type, view_type, metric_key, min_val, max_val, weight, sensitivity, low_sensitivity, high_sensitivity, part, low_advice, high_advice)
VALUES (3, 'SIDE', 'maxKnee', 138.0, 148.0, 0.35, 5.0, 4.0, 10.0, 'seat', 'advice.seat.low', 'advice.seat.high'),
       (3, 'SIDE', 'minHip', 48.0, 60.0, 0.20, 3.5, 3.5, 5.5, 'handlebar', 'advice.bar.low', 'advice.bar.high'),
       (3, 'SIDE', 'avgTorso', 40.0, 50.0, 0.15, 2.0, 2.0, 3.0, 'stem', 'advice.stem.low', 'advice.stem.high'),
       (3, 'FRONT', 'maxWobble', 0.0, 22.0, 0.15, 4.0, 4.0, 6.0, 'cleat', NULL, 'advice.cleat.check'),
       (3, 'BACK', 'maxPelvisSway', 0.0, 18.0, 0.15, 5.0, 5.0, 7.0, 'seat', NULL, 'advice.stability.low');

-- =================================================================
-- 4. 铁三/计时车 (TT_BIKE, Code: 4)
-- 标准：极致气动姿态、身体大幅前倾、极低髋角
-- =================================================================
INSERT INTO fit_dynamic_standard (bike_type, view_type, metric_key, min_val, max_val, weight, sensitivity, low_sensitivity, high_sensitivity, part, low_advice, high_advice)
VALUES (4, 'SIDE', 'maxKnee', 142.0, 152.0, 0.30, 5.0, 4.0, 11.0, 'seat', 'advice.seat.low', 'advice.seat.high'),
       (4, 'SIDE', 'minHip', 35.0, 45.0, 0.25, 5.0, 5.0, 8.0, 'handlebar', 'advice.aerobar.low', 'advice.aerobar.high'),
       (4, 'SIDE', 'avgTorso', 20.0, 32.0, 0.20, 3.0, 3.0, 5.0, 'handlebar', 'advice.aero.low', 'advice.aero.high'),
       (4, 'SIDE', 'maxShoulder', 90.0, 105.0, 0.15, 2.5, 2.5, 4.0, 'stem', 'advice.reach.low', 'advice.reach.high'),
       (4, 'BACK', 'maxPelvisSway', 0.0, 10.0, 0.10, 8.0, 8.0, 12.0, 'seat', NULL, 'advice.stability.low');

-- =================================================================
-- 5. 城市通勤车 (CITY_BIKE, Code: 5)
-- 标准：极度直立舒适、便于观察路况、膝盖伸展度适中
-- =================================================================
INSERT INTO fit_dynamic_standard (bike_type, view_type, metric_key, min_val, max_val, weight, sensitivity, low_sensitivity, high_sensitivity, part, low_advice, high_advice)
VALUES (5, 'SIDE', 'maxKnee', 130.0, 142.0, 0.35, 4.0, 3.0, 7.0, 'seat', 'advice.seat.low', 'advice.seat.high'),
       (5, 'SIDE', 'minHip', 65.0, 85.0, 0.25, 2.0, 2.0, 3.0, 'handlebar', 'advice.bar.low', 'advice.bar.high'),
       (5, 'SIDE', 'avgTorso', 60.0, 80.0, 0.20, 1.5, 1.5, 2.5, 'stem', 'advice.reach.low', 'advice.reach.high'),
       (5, 'FRONT', 'maxWobble', 0.0, 35.0, 0.10, 2.0, 2.0, 4.0, 'cleat', NULL, 'advice.technique.check'),
       (5, 'BACK', 'maxPelvisSway', 0.0, 25.0, 0.10, 3.0, 3.0, 5.0, 'seat', NULL, 'advice.stability.low');


-- 测试数据
INSERT INTO `usr_user` (`account_non_expired`, `account_non_locked`, `birthday`, `credentials_non_expired`, `enabled`, `gender`, `is_deleted`, `login_failure_count`, `delete_time`, `id`, `lock_time`, `registration_time`, `old_phone`, `phone`, `introduction`, `name`, `email`, `location`, `password`, `avatar`) VALUES (b'1', b'1', NULL, b'1', b'1', NULL, b'0', 0, NULL, 2028760927918231552, NULL, '2026-03-03 17:14:32.428347', NULL, '15932673121', NULL, '骑士3121', NULL, NULL, NULL, NULL);
INSERT INTO `fit_user_body` (`arm_raise_angle`, `breast_height`, `calf_length`, `foot_length`, `height`, `ischial_distance`, `long_arm_length`, `short_arm_length`, `shoulder_width`, `span_height`, `squat_depth`, `squat_stability_score`, `thigh_length`, `thoracic_extension_angle`, `thoracic_pain_level`, `thoracic_rotation_angle`, `create_time`, `id`, `user_id`) VALUES (160, 1450, 430, NULL, 1800, 120, 330, 300, 450, 820, -5, 2, 450, 20, 4, 40, '2026-03-14 21:45:12.637003', 1, 2028760927918231552);
INSERT INTO `bike_model` (`bb_drop`, `bike_type`, `head_tube_angle`, `reach`, `seat_tube_angle`, `source_type`, `stack`, `stand_over_height`, `status`, `wheel_base`, `id`, `brand`, `brand_cn`, `image_url`, `model`, `model_year`, `size`, `source_url`) VALUES (66, 1, 74, 395, 74, 2, 557, 782, 1, NULL, 1, 'Giant', '捷安特', '/resource/2028760927918231552/bike/1f421434b63348d897b059ed468b19df.png', 'S100', '2025', 'M', NULL);
INSERT INTO `fit_user_bike` (`status`, `bike_model_id`, `create_time`, `id`, `user_id`, `remark`) VALUES (1, 1, '2026-03-14 21:49:34.450712', 1, 2028760927918231552, '日常通勤');
INSERT INTO `fit_user_bike_setup` (`crank_length`, `handlebar_drop`, `handlebar_width`, `pedal_type`, `saddle_height`, `saddle_setback`, `saddle_tilt`, `saddle_width`, `spacers_height`, `stem_angle`, `stem_length`, `create_time`, `fit_user_bike_id`, `id`) VALUES (NULL, NULL, NULL, 1, NULL, 20, 0, NULL, NULL, NULL, NULL, '2026-03-14 21:49:34.454723', 1, 1);
