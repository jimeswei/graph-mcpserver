package com.example.graph.mcp.service;

import com.example.graph.mcp.entity.GraphCache;
import com.example.graph.mcp.repository.GraphCacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class GraphCacheService {

    private GraphCacheRepository graphCacheRepository;

    public GraphCacheService() {
        // 空构造函数，数据库不可用时使用
    }

    @Autowired(required = false)
    public void setGraphCacheRepository(GraphCacheRepository graphCacheRepository) {
        this.graphCacheRepository = graphCacheRepository;
    }

    public GraphCache saveCacheRecord(String threadId, String content, String operationType) {
        if (threadId == null || threadId.trim().isEmpty()) {
            throw new IllegalArgumentException("threadId cannot be null or empty");
        }
        
        if (graphCacheRepository == null) {
            log.warn("Database not available, skipping cache for {} operation. threadId: {}", 
                    operationType, threadId);
            return null;
        }
        
        try {
            GraphCache cache = new GraphCache();
            cache.setThreadId(threadId);
            cache.setContent(content);
            
            GraphCache saved = graphCacheRepository.save(cache);
            log.info("Saved {} cache record with ID: {}, threadId: {}", 
                    operationType, saved.getId(), threadId);
            
            return saved;
        } catch (Exception e) {
            log.error("Failed to save cache record for {} operation: {}", operationType, e.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<GraphCache> getCacheByThreadId(String threadId) {
        return graphCacheRepository.findByThreadIdOrderByCreatedTimeDesc(threadId);
    }

    @Transactional(readOnly = true)
    public Optional<GraphCache> getLatestCache(String threadId) {
        return graphCacheRepository.findTopByThreadIdOrderByCreatedTimeDesc(threadId);
    }

    @Transactional(readOnly = true)
    public List<GraphCache> getAllCacheRecords() {
        return graphCacheRepository.findAll();
    }

    public void deleteCacheRecord(Long id) {
        graphCacheRepository.deleteById(id);
        log.info("Deleted cache record with ID: {}", id);
    }


    public void deleteCacheByThread(String threadId) {
        List<GraphCache> caches = graphCacheRepository.findByThreadId(threadId);
        graphCacheRepository.deleteAll(caches);
        log.info("Deleted {} cache records for threadId: {}", caches.size(), threadId);
    }
}