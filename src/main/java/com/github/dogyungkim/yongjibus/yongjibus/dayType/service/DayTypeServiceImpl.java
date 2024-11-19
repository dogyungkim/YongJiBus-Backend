package com.github.dogyungkim.yongjibus.yongjibus.dayType.service;

import com.github.dogyungkim.yongjibus.yongjibus.dayType.repository.DayTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DayTypeServiceImpl implements DayTypeService {

    private final DayTypeRepository dayTypeRepository;

    @Override
    public Boolean getDayType(LocalDate date) {

        return dayTypeRepository.findByDate(date);
    }
}
