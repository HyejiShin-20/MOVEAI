package com.moveai.common.controller;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/health")
public class HealthController {
 private final JdbcTemplate jdbc;
 public HealthController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @GetMapping public Map<String,Object> health(){return Map.of("status","UP","service","MOVE-AI backend");}
 @GetMapping("/db") public Map<String,Object> db(){
  Integer one=jdbc.queryForObject("SELECT 1",Integer.class);
  String db=jdbc.queryForObject("SELECT DATABASE()",String.class);
  return Map.of("status",one!=null&&one==1?"UP":"DOWN","database",db==null?"":db,"host","127.0.0.1","port",3306);
 }
}
