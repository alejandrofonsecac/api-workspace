package com.workspace.api.retention.service;

import com.workspace.api.retention.repository.RetentionRepository;
import com.workspace.api.retention.repository.RetentionRepository.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RetentionBatchService {
    private final RetentionRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
    public int deleteBatch(Resource resource, Instant cutoff, int batchSize) {
        if (batchSize < 1) throw new IllegalArgumentException("batchSize must be positive");
        return repository.deleteBatch(resource, cutoff, batchSize);
    }
}
