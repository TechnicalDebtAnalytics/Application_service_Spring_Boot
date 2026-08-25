package com.debtlens.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefactoringActionDTO {
    private String type;
    private String priority;
    private String title;
    private String description;
    private String suggestedRefactoring;
}
