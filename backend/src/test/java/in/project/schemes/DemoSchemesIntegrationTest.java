package in.project.schemes;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class DemoSchemesIntegrationTest {
 @Autowired MockMvc mvc;@Autowired RuleEngine engine;@Autowired JdbcTemplate db;@Autowired Api api;@Autowired SessionTokens tokens;@Autowired DemoSchemes demos;
 @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager em;
 ApiIntegrationTest client;
 @BeforeEach void setup(){client=new ApiIntegrationTest();client.mvc=mvc;client.engine=engine;client.db=db;client.api=api;client.tokens=tokens;}
 @Test void student()throws Exception{verify(0);}
 @Test void women()throws Exception{verify(1);}
 @Test void farmer()throws Exception{verify(2);}
 @Test void senior()throws Exception{verify(3);}
 @Test void youth()throws Exception{verify(4);}
 @Test void disability()throws Exception{verify(5);}
 @Test void business()throws Exception{verify(6);}
 @Test void school()throws Exception{verify(7);}
 String sid(JsonNode item){return db.queryForObject("select id from schemes where name=?",String.class,DemoSchemes.PREFIX+item.path("name").asText());}
 Map<String,Object> good(JsonNode item){Map<String,Object> a=new LinkedHashMap<>();for(JsonNode c:item.path("conditions")){JsonNode v=c.path("value");a.put(c.path("question").asText(),engine.json.convertValue(v.isArray()?v.get(0):v,Object.class));}return a;}
 String save(ApiIntegrationTest.Citizen citizen,String sid,Map<String,String> ids,Map<String,Object> values,String rev,int status)throws Exception{
  var rows=new ArrayList<Map<String,Object>>();values.forEach((k,v)->{var row=new LinkedHashMap<String,Object>();row.put("questionId",ids.get(k));row.put("value",v);rows.add(row);});
  JsonNode r=client.request(citizen,"PUT","/api/v1/schemes/"+sid+"/answers",Map.of("expectedProfileRevision",rev,"answers",rows),status);return status==200?r.path("revision").asText():rev;
 }
 JsonNode evaluate(ApiIntegrationTest.Citizen c,String sid,String revision,int status)throws Exception{return client.request(c,"POST","/api/v1/eligibility/evaluate",Map.of("schemeId",sid,"expectedProfileRevision",revision),status);}
 void verify(int index)throws Exception{
  JsonNode spec=demos.specification(), item=spec.path("schemes").get(index);String sid=sid(item);var citizen=client.citizen();
  JsonNode catalog=client.request(citizen,"GET","/api/v1/schemes?size=100",null,200);
  assertTrue(catalog.path("items").findValuesAsText("id").contains(sid));assertTrue(catalog.path("items").findValuesAsText("name").stream().noneMatch(n->n.startsWith("Synthetic Demo:")));
  JsonNode detail=client.request(citizen,"GET","/api/v1/schemes/"+sid,null,200);assertTrue(detail.path("synthetic").asBoolean());assertFalse(detail.path("reviewerApproved").asBoolean());assertTrue(detail.path("benefits").asText().startsWith(item.path("benefit").asText()));assertTrue(detail.path("description").asText().contains(item.path("description").asText()));
  for(JsonNode doc:item.path("documents"))assertTrue(detail.path("guidance").asText().contains(doc.asText()));assertEquals(item.path("conditions").size(),detail.path("rule").path("children").size());
  JsonNode questions=client.request(citizen,"GET","/api/v1/schemes/"+sid+"/questions",null,200);assertEquals(item.path("questions").size(),questions.size());Map<String,String> ids=new HashMap<>();
  for(JsonNode k:item.path("questions")){String key=k.asText();JsonNode expected=spec.path("questions").path(key);JsonNode actual=null;for(JsonNode q:questions)if(q.path("label").equals(expected.path("label")))actual=q;assertNotNull(actual);assertEquals(expected.path("type"),actual.path("type"));assertEquals(expected.path("options"),actual.path("options"));ids.put(key,actual.path("id").asText());assertTrue(engine.parse(detail.path("sourceMapping").asText()).path(key).asText().contains("page "+item.path("page").asInt()));}
  Map<String,Object> values=good(item);String rev=save(citizen,sid,ids,values,"0",200);int histories=0;
  JsonNode positive=evaluate(citizen,sid,rev,200);histories++;assertEquals("ELIGIBLE",positive.path("verdict").asText());assertEquals(values.size(),positive.path("explanations").size());for(JsonNode e:positive.path("explanations"))assertEquals("TRUE",e.path("result").asText());
  assertEquals(values.size(),client.request(citizen,"GET","/api/v1/schemes/"+sid+"/questions",null,200).size());
  for(JsonNode condition:item.path("conditions")){
   String key=condition.path("question").asText(), op=condition.path("operator").asText();Object bad;
   if(op.equals("BETWEEN"))bad=new BigDecimal(condition.path("value").get(0).asText()).subtract(BigDecimal.ONE).toPlainString();
   else if(op.equals("LTE"))bad=new BigDecimal(condition.path("value").asText()).add(new BigDecimal("0.01")).toPlainString();
   else if(op.equals("GTE"))bad=new BigDecimal(condition.path("value").asText()).subtract(BigDecimal.ONE).toPlainString();
   else if(op.equals("IN"))bad="other";
   else if(condition.path("value").isBoolean())bad=false;
   else bad=key.equals("state")?"MH":"other";
   var changed=new LinkedHashMap<>(values);changed.put(key,bad);rev=save(citizen,sid,ids,changed,rev,200);JsonNode negative=evaluate(citizen,sid,rev,200);histories++;assertEquals("NOT_ELIGIBLE",negative.path("verdict").asText());assertTrue(negative.path("explanations").findValuesAsText("result").contains("FALSE"));
   changed.put(key,null);rev=save(citizen,sid,ids,changed,rev,200);assertEquals("INPUT_REQUIRED",evaluate(citizen,sid,rev,422).path("error").path("code").asText());
   if(op.equals("BETWEEN")||op.equals("IN"))for(JsonNode boundary:condition.path("value")){changed.put(key,engine.json.convertValue(boundary,Object.class));rev=save(citizen,sid,ids,changed,rev,200);assertEquals("ELIGIBLE",evaluate(citizen,sid,rev,200).path("verdict").asText());histories++;}
   if(op.equals("BETWEEN")){changed.put(key,new BigDecimal(condition.path("value").get(1).asText()).add(BigDecimal.ONE).toPlainString());rev=save(citizen,sid,ids,changed,rev,200);assertEquals("NOT_ELIGIBLE",evaluate(citizen,sid,rev,200).path("verdict").asText());histories++;}
   if(key.equals("marks")||key.equals("disability")){changed.put(key,"100");rev=save(citizen,sid,ids,changed,rev,200);assertEquals("ELIGIBLE",evaluate(citizen,sid,rev,200).path("verdict").asText());histories++;changed.put(key,"0");rev=save(citizen,sid,ids,changed,rev,200);assertEquals("NOT_ELIGIBLE",evaluate(citizen,sid,rev,200).path("verdict").asText());histories++;}
   if(key.equals("income")){BigDecimal exact=new BigDecimal(condition.path("value").asText()).add(new BigDecimal("0.00000000000000001"));changed.put(key,exact);rev=save(citizen,sid,ids,changed,rev,200);assertEquals("NOT_ELIGIBLE",evaluate(citizen,sid,rev,200).path("verdict").asText());histories++;for(JsonNode q:client.request(citizen,"GET","/api/v1/schemes/"+sid+"/questions",null,200))if(q.path("id").asText().equals(ids.get(key)))assertEquals(0,exact.compareTo(new BigDecimal(q.path("value").asText())));}
  }
  assertEquals(histories,db.queryForObject("select count(*) from eligibility_evaluations where user_id=? and scheme_id=?",Integer.class,citizen.id(),sid));
  String snapshot=db.queryForObject("select snapshot_json from eligibility_evaluations where id=?",String.class,positive.path("id").asText());assertTrue(snapshot.contains(DemoSchemes.PREFIX));assertEquals("ELIGIBLE",client.request(citizen,"GET","/api/v1/eligibility/history/"+positive.path("id").asText(),null,200).path("verdict").asText());
 }
 @Test void productionStillExcludesSyntheticFixtures(){
  Api production=new Api(engine,tokens,null,db);production.env=new org.springframework.mock.env.MockEnvironment();production.em=em;
  assertFalse(production.development());int count=db.queryForObject("select count(*) from schemes",Integer.class);new DemoSchemes(production,engine).seed();assertEquals(count,db.queryForObject("select count(*) from schemes",Integer.class));
  var catalog=(Map<?,?>)production.schemes("",null,null,0,100,"name");for(Object row:(List<?>)catalog.get("items"))assertFalse(Boolean.TRUE.equals(((Map<?,?>)row).get("synthetic")));
  for(JsonNode item:demos.specification().path("schemes"))assertThrows(ApiFailure.class,()->production.published(Long.parseLong(sid(item))));
 }
 @Test void restartIsIdempotentAndLegacyRecordsAreUntouched(){
  var before=db.queryForList("select * from schemes where name like 'Synthetic Demo:%'");var rules=db.queryForList("select * from scheme_rule_versions where scheme_id in (select id from schemes where name like 'Synthetic Demo:%')");var history=db.queryForList("select * from eligibility_evaluations");var audits=db.queryForList("select * from admin_audit_logs");int total=db.queryForObject("select count(*) from schemes",Integer.class),questions=db.queryForObject("select count(*) from questions",Integer.class);
  demos.seed();demos.seed();assertEquals(total,db.queryForObject("select count(*) from schemes",Integer.class));assertEquals(questions,db.queryForObject("select count(*) from questions",Integer.class));assertEquals(before,db.queryForList("select * from schemes where name like 'Synthetic Demo:%'"));assertEquals(rules,db.queryForList("select * from scheme_rule_versions where scheme_id in (select id from schemes where name like 'Synthetic Demo:%')"));assertEquals(history,db.queryForList("select * from eligibility_evaluations"));assertEquals(audits,db.queryForList("select * from admin_audit_logs"));
  Scheme unrelated=new Scheme();unrelated.name="Synthetic Browser Demo";unrelated.synthetic=true;assertFalse(DemoSchemes.legacyTemplate(unrelated));unrelated.name="Synthetic Demo: Learning Support";assertFalse(DemoSchemes.legacyTemplate(unrelated));
 }
 @Test void invalidIntegerAndPercentagesRejectedWithoutAnswerOrRevisionChanges()throws Exception{
  JsonNode item=demos.specification().path("schemes").get(5);String sid=sid(item);var citizen=client.citizen();Map<String,String> ids=new HashMap<>();for(JsonNode q:client.request(citizen,"GET","/api/v1/schemes/"+sid+"/questions",null,200))for(String key:List.of("age","disability","studying"))if(q.path("label").equals(demos.specification().path("questions").path(key).path("label")))ids.put(key,q.path("id").asText());
  var values=good(item);values.put("age","18.5");save(citizen,sid,ids,values,"0",400);values.put("age",new BigDecimal("1e1000000000"));save(citizen,sid,ids,values,"0",400);values.put("age","18");for(String percent:List.of("-0.01","100.01")){values.put("disability",percent);save(citizen,sid,ids,values,"0",400);}assertEquals(0,db.queryForObject("select count(*) from profile_answers where user_id=?",Integer.class,citizen.id()));assertEquals(0L,db.queryForObject("select revision from user_profiles where user_id=?",Long.class,citizen.id()));
 }
}
