package com.handyman.repository;
import com.handyman.model.Category;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface CategoryRepository extends JpaRepository<Category,Long> {
 List<Category> findAllByOrderBySortOrderAscIdAsc();
 Optional<Category> findBySlug(String slug);
 boolean existsBySlug(String slug);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Category c where c.id = :id")
 Optional<Category> lockById(@Param("id") Long id);
}
