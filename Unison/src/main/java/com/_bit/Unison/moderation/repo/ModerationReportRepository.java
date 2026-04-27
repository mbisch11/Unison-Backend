package com._bit.Unison.moderation.repo;

import com._bit.Unison.moderation.model.ModerationReport;
import com._bit.Unison.moderation.model.ReportStatus;
import com._bit.Unison.moderation.model.ReportTargetType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface ModerationReportRepository extends MongoRepository<ModerationReport, String> {

    List<ModerationReport> findAllByOrderByCreatedAtDesc();

    List<ModerationReport> findByReporterUserIdOrderByCreatedAtDesc(String reporterUserId);

    List<ModerationReport> findByReportedUserIdOrderByCreatedAtDesc(String reportedUserId);

    List<ModerationReport> findByStatusInOrderByCreatedAtDesc(Collection<ReportStatus> statuses);

    boolean existsByReporterUserIdAndTargetTypeAndTargetIdAndStatusIn(
            String reporterUserId,
            ReportTargetType targetType,
            String targetId,
            Collection<ReportStatus> statuses
    );
}
