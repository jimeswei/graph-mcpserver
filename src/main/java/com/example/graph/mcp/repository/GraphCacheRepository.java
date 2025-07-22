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

    List<GraphCache> findBySessionId(String sessionId);

    List<GraphCache> findByThreadId(String threadId);

    List<GraphCache> findBySessionIdAndThreadId(String sessionId, String threadId);

    @Query("SELECT gc FROM GraphCache gc WHERE gc.sessionId = :sessionId ORDER BY gc.createdAt DESC")
    List<GraphCache> findBySessionIdOrderByCreatedAtDesc(@Param("sessionId") String sessionId);

    @Query("SELECT gc FROM GraphCache gc WHERE gc.threadId = :threadId ORDER BY gc.createdAt DESC")
    List<GraphCache> findByThreadIdOrderByCreatedAtDesc(@Param("threadId") String threadId);

    Optional<GraphCache> findTopBySessionIdAndThreadIdOrderByCreatedAtDesc(String sessionId, String threadId);
}