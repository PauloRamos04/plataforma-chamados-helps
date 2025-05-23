package com.helps.controller;

import com.helps.domain.service.ActivityLogService;
import com.helps.dto.ActivityLogDto;
import com.helps.dto.ActivityStatsDto;
import com.helps.dto.ApiResponse;
import com.helps.dto.UserSessionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/activity")
@PreAuthorize("hasAuthority('ADMIN')")
public class ActivityLogController {

    @Autowired
    private ActivityLogService activityLogService;

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<ActivityLogDto>>> getActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ActivityLogDto> logs = activityLogService.getActivityLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/logs/date-range")
    public ResponseEntity<ApiResponse<Page<ActivityLogDto>>> getActivityLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ActivityLogDto> logs = activityLogService.getActivityLogsByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Page<UserSessionDto>>> getUserSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "loginTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserSessionDto> sessions = activityLogService.getUserSessions(pageable);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/sessions/active")
    public ResponseEntity<ApiResponse<List<UserSessionDto>>> getActiveSessions() {
        List<UserSessionDto> activeSessions = activityLogService.getActiveSessions();
        return ResponseEntity.ok(ApiResponse.success(activeSessions));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ActivityStatsDto>> getActivityStats() {
        ActivityStatsDto stats = activityLogService.getActivityStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}