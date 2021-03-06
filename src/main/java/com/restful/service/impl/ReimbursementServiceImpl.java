package com.restful.service.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.restful.entity.*;
import com.restful.exception.UnAuthenticationException;
import com.restful.mapper.ReimbursementMapper;
import com.restful.service.ReimbursementService;
import com.restful.service.SysUserService;
import com.restful.service.WorkFlowService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author  * 
 *   ┏ ┓   ┏ ┓
 *  ┏┛ ┻━━━┛ ┻┓
 *  ┃         ┃
 *  ┃    ━    ┃
 *  ┃  ┳┛  ┗┳ ┃
 *  ┃         ┃
 *  ┃    ┻    ┃
 *  ┃         ┃
 *  ┗━━┓    ┏━┛
 *     ┃    ┃神兽保佑
 *     ┃    ┃代码无BUG！
 *     ┃    ┗━━━━━━━┓
 *     ┃            ┣┓
 *     ┃            ┏┛
 *     ┗┓┓┏━━━━━━┳┓┏┛
 *      ┃┫┫      ┃┫┫
 *      ┗┻┛      ┗┻┛
 * @since 2018-08-18
 */
@Service
public class ReimbursementServiceImpl extends ServiceImpl<ReimbursementMapper, Reimbursement> implements ReimbursementService {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private WorkFlowService workFlowService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private TaskService taskService;

    /**
     * describe: 插入报销申请
     * creat_user: baily
     * creat_date: 2018/8/19
     **/
    @Override
    public boolean insert(Reimbursement entity) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        SysUser currentUser = userService.current(request);
        if (ObjectUtils.isEmpty(currentUser)) {
            throw new UnAuthenticationException();
        }
        entity.setCreator(currentUser.getId());
        return super.insert(entity);
    }

    @Override
    public Page<Reimbursement> selectPage(Page<Reimbursement> page, Wrapper<Reimbursement> wrapper) {
        Page<Reimbursement> reimbursementPage = super.selectPage(page, wrapper);
        Iterator<Reimbursement> i = reimbursementPage.getRecords().iterator();
        while (i.hasNext()){
            Reimbursement reimbursement = i.next();
            EntityWrapper<WorkFlowInstance> workFlowInstanceEntityWrapper = new EntityWrapper<>();
            workFlowInstanceEntityWrapper.eq("business_id", reimbursement.getId());
        }
        return reimbursementPage;
    }

    @Override
    public boolean apply(Reimbursement reimbursement, Integer flowId) {
        WorkFlow workFlow = workFlowService.selectById(flowId);
        reimbursement.setStatus(1); // TODO magic number 需要转用枚举类型
        // 更新实体类
        this.updateById(reimbursement);
        Map<String, Object> map = new HashMap<>();
        map.put("optor", "admin");
        // 启动流程
        return workFlowService.startProcess(reimbursement, workFlow, map);
    }

    @Override
    public Page<Reimbursement> recordFlowStatus(Page<Reimbursement> page) {
        List<Reimbursement> list = new ArrayList<>();
        List<Reimbursement> records = page.getRecords();
        records.forEach(reimbursement -> {
            String busniessKey = reimbursement.getClass().getSimpleName() + reimbursement.getId();
            ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(busniessKey).singleResult();
            if(!ObjectUtils.isEmpty(pi)){
                List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).active().list();
                UserTask userTask = new UserTask();
                TaskEntity taskEntity = (TaskEntity) tasks.get(0);
                userTask.setId(taskEntity.getId());
                userTask.setName(taskEntity.getName());
                userTask.setDescription(taskEntity.getDescription());
                userTask.setIsEnd(false);
                reimbursement.setTask(userTask);
            }else{
                // 如果流程为空，在历史记录中去找， 历史记录中存在说明这个流程已经结束
                List<HistoricProcessInstance> hpi = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(busniessKey).list();
                if(!ObjectUtils.isEmpty(hpi)){
                    UserTask userTask = new UserTask();
                    userTask.setId(hpi.get(0).getId());
                    userTask.setName("流程结束");
                    userTask.setIsEnd(true);
                    List<Task> tasks = taskService.createTaskQuery().processInstanceId(hpi.get(0).getId()).active().list();
                    reimbursement.setTask(userTask);
                }
            }
            list.add(reimbursement);
        });
        page.setRecords(list);
        return page;
    }

    @Autowired
    private SysUserService userService;
}
