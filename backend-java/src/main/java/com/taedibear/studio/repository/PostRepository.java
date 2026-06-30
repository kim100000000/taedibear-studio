package com.taedibear.studio.repository;

import com.taedibear.studio.domain.Post;
import com.taedibear.studio.domain.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
	Page<Post> findAllByUserId(Long userId, Pageable pageable);

	Page<Post> findAllByUserIdAndStatus(Long userId, PostStatus status, Pageable pageable);

	Optional<Post> findByIdAndUserId(Long id, Long userId);
}
