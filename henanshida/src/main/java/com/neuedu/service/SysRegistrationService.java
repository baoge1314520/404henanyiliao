package com.neuedu.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.neuedu.entity.SysPatient;
import com.neuedu.entity.SysRegistration;
import com.neuedu.vo.RegistrationVO;

/**
 * 挂号记录表 服务层。
 *
 * @author wangyu
 * @since 2026-11-09
 */
public interface SysRegistrationService extends IService<SysRegistration> {

    /**
     * 分页查询挂号记录（含患者、科室、医生、职称信息）。patientId 非空时只查该患者（患者端"我的挂号"）。
     */
    Page<RegistrationVO> list(Integer pageNo, Integer pageSize, String patientName, String regDate, String status, Long patientId);

    /**
     * 同上，但按患者ID集合过滤（患者端"本人与家人"的挂号记录）。patientIds 为 null 时不过滤。
     */
    Page<RegistrationVO> list(Integer pageNo, Integer pageSize, String patientName, String regDate, String status, java.util.List<Long> patientIds);

    /**
     * 医生端历史记录：本人名下全部日期的挂号分页（含未来预约），
     * 支持患者姓名、状态、就诊日期范围过滤，就诊日期倒序。
     */
    Page<RegistrationVO> listForDoctor(Integer pageNo, Integer pageSize, Long doctorId, String patientName,
                                       String status, java.time.LocalDate dateFrom, java.time.LocalDate dateTo);

    /**
     * 挂号：患者不存在时与挂号记录一起保存，已存在则直接复用患者档案。
     */
    boolean add(SysRegistration registration, SysPatient patient);

    /**
     * 根据身份证号查询患者信息，用于前端自动回填。
     */
    SysPatient getPatientByIdCard(String idCard);

    /**
     * 修改挂号状态。
     */
    boolean updateStatus(Long id, String status);

    /**
     * 叫号队列：待就诊记录，未叫号在前、按就诊日期与挂号先后排序。
     */
    java.util.List<RegistrationVO> queue();

    /**
     * 叫号：记录叫号时间。
     */
    boolean call(Long id);

    /**
     * 分诊：调整挂号的科室/医生/职称，并按职称快照挂号费。
     */
    boolean triage(Long id, Long deptId, Long doctorId, Long titleId);
}
