package com._bit.Unison.groups.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.*;

@Document(collection = "study_groups")
public class StudyGroup {
	
	@Id
	private String groupId;
	private String courseId;
	private String title;
	private String description;
	private String location;
	private boolean isVirtual;
	private LocalDateTime startTime;
	private LocalDateTime createdAt;
	private int durationMinutes;
	private int maxCapacity;
	private String createdByUserId;

	public StudyGroup() {
	}

	public StudyGroup(String courseId, String title, String description, String location, boolean isVirtual, LocalDateTime startTime, int maxCapacity, String createdByUserId){
		this(courseId, title, description, location, isVirtual, startTime, 60, maxCapacity, createdByUserId);
	}

	public StudyGroup(
			String courseId,
			String title,
			String description,
			String location,
			boolean isVirtual,
			LocalDateTime startTime,
			int durationMinutes,
			int maxCapacity,
			String createdByUserId
	){
		this.courseId = courseId;
		this.title = title;
		this.description = description;
		this.location = location;
		this.isVirtual = isVirtual;
		this.startTime = startTime;
		this.createdAt = LocalDateTime.now();
		this.durationMinutes = durationMinutes;
		this.maxCapacity = maxCapacity;
		this.createdByUserId = createdByUserId;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getCourseId() {
		return courseId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public boolean isVirtual() {
		return isVirtual;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public int getDurationMinutes() {
		return durationMinutes;
	}

	public int getMaxCapacity() {
		return maxCapacity;
	}

	public String getCreatedByUserId() {
		return createdByUserId;
	}
}
