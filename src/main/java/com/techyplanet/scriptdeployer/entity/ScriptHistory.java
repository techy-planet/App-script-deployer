package com.techyplanet.scriptdeployer.entity;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity(name = "Schema_History")
public class ScriptHistory {

	@EmbeddedId
	private FileId fileId;
	private String type;
	private Long sequence;
	private Long version;
	private String checksum;
	private String pattern;
	private Date createDate;
	private String deploymentReqNo;

	public ScriptHistory() {
		super();
	}

	public ScriptHistory(String path, String type, Long sequence, Long version, String checksum, String pattern,
			Date createDate, Date updateDate, String deploymentReqNo) {
		super();
		this.fileId = new FileId(path, updateDate);
		this.type = type;
		this.sequence = sequence;
		this.version = version;
		this.checksum = checksum;
		this.pattern = pattern;
		this.createDate = createDate;
		this.deploymentReqNo = deploymentReqNo;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Long getSequence() {
		return sequence;
	}

	public void setSequence(Long sequence) {
		this.sequence = sequence;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getDeploymentReqNo() {
		return deploymentReqNo;
	}

	public FileId getFileId() {
		return fileId;
	}

	@Override
	public String toString() {
		return "ScriptHistory [path=" + getFileId().getPath() + ", type=" + type + ", sequence=" + sequence
				+ ", version=" + version + ", checksum=" + checksum + ", pattern=" + pattern + ", createDate="
				+ createDate + ", updateDate=" + getFileId().getUpdateDate() + "]";
	}

	@Embeddable
	public static class FileId implements Serializable {

		private static final long serialVersionUID = -7121364495848282028L;
		private String path;
		private Date updateDate;

		public FileId() {
			super();
		}

		public FileId(String path, Date updateDate) {
			super();
			this.path = path;
			this.updateDate = updateDate;
		}

		public String getPath() {
			return path;
		}

		public Date getUpdateDate() {
			return updateDate;
		}

	}
}
