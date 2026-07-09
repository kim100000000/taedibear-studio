package com.taedibear.studio.repository;

import com.taedibear.studio.domain.SavedCaption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface SavedCaptionRepository extends JpaRepository<SavedCaption, Long> {

	List<SavedCaption> findAllByUserIdOrderByCreatedAtDesc(Long userId);

	Optional<SavedCaption> findByIdAndUserId(Long id, Long userId);

	long countByUserId(Long userId);

	// 회원 탈퇴 시 정리
	@Modifying
	void deleteAllByUserId(Long userId);
}
