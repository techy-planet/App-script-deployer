package com.techyplanet.scriptdeployer.repository;

import org.springframework.data.repository.CrudRepository;

import com.techyplanet.scriptdeployer.entity.ScriptHistory;

public interface ScriptHistoryRepository extends CrudRepository<ScriptHistory, Long> {

	ScriptHistory findFirstBySequenceAndPatternOrderByVersionDesc(Long sequence, String pattern);

	ScriptHistory findFirstByFileIdPathOrderByFileIdUpdateDateDesc(String path);
}