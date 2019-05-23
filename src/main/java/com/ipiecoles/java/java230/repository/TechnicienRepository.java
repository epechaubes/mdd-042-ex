package com.ipiecoles.java.java230.repository;

import com.ipiecoles.java.java230.model.Technicien;
import org.springframework.data.domain.Slice;

import java.awt.print.Pageable;
import java.util.List;

public interface TechnicienRepository extends BaseEmployeRepository<Technicien> {

    List<Technicien> findByGradeBetween(Integer gradeLower, Integer gradeUpper);

    Slice<Technicien> findTop5ByGrade(Integer grade);

}
