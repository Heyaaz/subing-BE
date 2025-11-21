package com.project.subing.dto.preference;

import com.project.subing.service.PreferenceService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswersRequest {
    private List<PreferenceService.AnswerDto> answers;
}
