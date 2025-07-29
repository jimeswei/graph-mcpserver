package com.example.graph.mcp.repository;

import com.example.graph.mcp.entity.GraphCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GraphCacheRepository extends JpaRepository<GraphCache, Long> {

    List<GraphCache> findByThreadId(String threadId);

    @Query("SELECT gc FROM GraphCache gc WHERE gc.threadId = :threadId ORDER BY gc.createdTime DESC")
    List<GraphCache> findByThreadIdOrderByCreatedTimeDesc(@Param("threadId") String threadId);

    Optional<GraphCache> findTopByThreadIdOrderByCreatedTimeDesc(String threadId);
}