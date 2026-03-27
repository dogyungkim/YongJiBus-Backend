package com.yongjibus.vacation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.yongjibus.global.error.exception.VacationException;
import com.yongjibus.vacation.domain.VacationPeriod;
import com.yongjibus.vacation.domain.VacationForm;
import com.yongjibus.vacation.service.VacationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/vacation")
@RequiredArgsConstructor
public class VacationController {

    private final VacationService vacationService;

    @GetMapping
    public String showVacationForm(Model model) {
        // 현재 설정된 방학 정보 조회
        VacationPeriod currentVacation = vacationService.getCurrentVacation();
        model.addAttribute("currentVacation", currentVacation);
        model.addAttribute("vacationForm", new VacationForm());
        return "vacation";
    }

    @PostMapping("/vacation-period")
    public String saveVacation(@Valid @ModelAttribute("vacationForm") VacationForm form, 
                             BindingResult result, 
                             Model model) {
        if (result.hasErrors()) {
            return "vacation";
        }

        try {
            VacationPeriod vacationPeriod = VacationPeriod.builder()
                .startDate(form.getStartDate())
                .endDate(form.getEndDate())
                .vacationDescription(form.getVacationDescription())
                .build();
            vacationService.saveVacationPeriod(vacationPeriod);
            model.addAttribute("message", "방학 기간이 성공적으로 저장되었습니다.");
            model.addAttribute("messageType", "success");
            model.addAttribute("currentVacation", vacationService.getCurrentVacation());
        } catch (VacationException e) {
            model.addAttribute("message", e.getMessage());
            model.addAttribute("messageType", "error");
            model.addAttribute("currentVacation", vacationService.getCurrentVacation());
        } catch (Exception e) {
            model.addAttribute("message", "저장 중 오류가 발생했습니다.");
            model.addAttribute("messageType", "error");
        }

        return "vacation";
    }
}
