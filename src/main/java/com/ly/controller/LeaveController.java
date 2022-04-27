package com.ly.controller;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
@GetMapping("hello")
public String hello() {

        return "HelloWorld";
        }*/
@Controller
@RequestMapping(value = "leave")
public class LeaveController {

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private ProcessEngine processEngine;

    /**
     * @author xiaofu
     * @description 启动流程
     * @date 2020/8/26 17:36
     */
    @RequestMapping(value = "startLeaveProcess")
    @ResponseBody
    public String startLeaveProcess(String staffId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("leaveTask", staffId);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("lyModelKey", map);
        StringBuilder sb = new StringBuilder();
        sb.append("创建请假流程 processId：" + processInstance.getId());
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(staffId).orderByTaskCreateTime().desc().list();
        for (Task task : tasks) {
            sb.append("任务taskId:" + task.getId());
        }
        return sb.toString();
    }

    /**
     * @param taskId
     * @author xinzhifu
     * @description 批准
     * @date 2020/8/27 14:30
     */
    @RequestMapping(value = "applyTask")
    @ResponseBody
    public String applyTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("流程不存在");
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("checkResult", "通过");
        taskService.complete(taskId, map);
        return "申请审核通过~";
    }

    /**
     * @param taskId
     * @author xinzhifu
     * @description 驳回
     * @date 2020/8/27 14:30
     */
    @ResponseBody
    @RequestMapping(value = "rejectTask")
    public String rejectTask(String taskId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("checkResult", "驳回");
        taskService.complete(taskId, map);
        return "申请审核驳回~";
    }


    /**
     * @author xiaofu
     * @description 生成流程图
     * @date 2020/8/27 14:29
     */
    @RequestMapping(value = "createProcessDiagramPic")
    public void createProcessDiagramPic(HttpServletResponse httpServletResponse, String processId) throws Exception {

        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();

        //流程走完的不显示图
        if (pi == null) {
            return;
        }
        Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        //使用流程实例ID，查询正在执行的执行对象表，返回流程实例对象
        String InstanceId = task.getProcessInstanceId();
        List<Execution> executions = runtimeService
                .createExecutionQuery()
                .processInstanceId(InstanceId)
                .list();

        //得到正在执行的Activity的Id
        List<String> activityIds = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for (Execution exe : executions) {
            List<String> ids = runtimeService.getActiveActivityIds(exe.getId());
            activityIds.addAll(ids);
        }

        //获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png",
                activityIds, flows, "宋体","宋体","宋体",null,2.0,true);
        /*FileOutputStream out1=new FileOutputStream(new File("F:\\java_test\\git\\java_test\\sb_flowable\\a.png"));
        byte[] buf1=new byte[1024];
        int length=0;
        try {
            while ((length = in.read(buf1)) != -1) {
                out1.write(buf1, 0, length);
            }
        }finally {
            if (in != null) {
                in.close();
            }
            if (out1 != null) {
                out1.close();
            }
        }
        out1.close();
        in=new FileInputStream(new File("F:\\java_test\\git\\java_test\\sb_flowable\\a.png"));*/

        OutputStream out = null;
        byte[] buf = new byte[1024];
        int legth = 0;
        try {

            httpServletResponse.reset();
            httpServletResponse.setContentType("image/png");
            out = httpServletResponse.getOutputStream();
            while ((legth = in.read(buf)) != -1) {
                out.write(buf, 0, legth);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }
}
