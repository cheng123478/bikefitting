package com.starrysky.aries.user.repository;

import com.starrysky.aries.entity.UserLoginRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLoginRecordRepository extends JpaRepository<UserLoginRecord, Integer> {
}

