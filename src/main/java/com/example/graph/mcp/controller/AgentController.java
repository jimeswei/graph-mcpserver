package com.example.graph.mcp.controller;

import com.alibaba.fastjson2.JSONObject;
import com.example.graph.mcp.agent.GraphQueryAgent;
import com.example.graph.mcp.agent.GraphQueryAgent.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 图谱Agent控制器 - 提供智能查询接口
 */
@Slf4j
@RestController
@RequestMapping("/agent")
public class AgentController {

    @Autowired
    private GraphQueryAgent queryAgent;

    /**
     * 智能查询接口
     * 支持自然语言查询，如：
     * - "周杰伦和刘德华的共同好友"
     * - "张三与李四怎么认识"
     * - "成龙和周星驰合作的电影"
     */
    /*@PostMapping("/query")
    public JSONObject query(@RequestBody JSONObject request) {
        String userQuery = request.getString("query");
        log.info("收到智能查询请求: {}", userQuery);

        // 使用Agent处理查询
        QueryResult result = queryAgent.processQuery(userQuery);

        // 构建响应
        JSONObject response = new JSONObject();
        response.put("success", result.isSuccess());

        if (result.isSuccess()) {
            response.put("intent", result.getIntent().getDescription());
            response.put("entities", result.getEntities());
            response.put("data", JSONObject.parseObject(result.getData()));
        } else {
            response.put("error", result.getMessage());
        }

        return response;
    }*/

    /**
     * 获取支持的查询示例
     */
    @GetMapping("/examples")
    public JSONObject getExamples() {
        JSONObject examples = new JSONObject();

        examples.put("mutual_friends", new String[] {
                "周杰伦和刘德华的共同好友",
                "张三与李四的共同好友"
        });

        examples.put("relationship_chain", new String[] {
                "周杰伦和刘德华的关系",
                "张三与李四怎么认识"
        });

        examples.put("collaboration", new String[] {
                "周杰伦和刘德华合作的电影",
                "成龙和周星驰共同参演"
        });

        return examples;
    }
}