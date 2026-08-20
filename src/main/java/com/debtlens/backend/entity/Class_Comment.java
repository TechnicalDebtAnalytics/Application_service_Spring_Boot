package com.debtlens.backend.entity;

import jakarta.persistence.*;

/**
 * Represents one raw source-code comment extracted from an analyzed class.
 *
 * An extracted comment is not automatically technical debt. Future V14
 * SATD_Detection persistence will reference comment_id to classify the comment
 * and store the resulting category and confidence separately.
 */
@Entity
@Table(name = "class_comments")
public class Class_Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    // Identifies the analyzed class/file from which this comment was extracted.
    // One Class_Metrics row may own multiple extracted comments.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private Class_Metrics classMetrics;

    // TEXT preserves complete source-code comments without a short VARCHAR limit.
    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Class_Metrics getClassMetrics() {
        return classMetrics;
    }

    public void setClassMetrics(Class_Metrics classMetrics) {
        this.classMetrics = classMetrics;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
