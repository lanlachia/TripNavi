package com.tripnavi;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SavedPlaceRepository extends JpaRepository<SavedPlace,Long> {
 List<SavedPlace> findByUserIdOrderByCreatedAtDesc(Long userId);
}
