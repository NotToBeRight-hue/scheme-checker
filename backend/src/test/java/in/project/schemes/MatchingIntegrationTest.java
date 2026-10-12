package in.project.schemes;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest @AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class MatchingIntegrationTest {
 @Autowired MockMvc mvc;@Autowired RuleEngine engine;@Autowired JdbcTemplate db;@Autowired Api api;@Autowired SessionTokens tokens;@Autowired DemoSchemes demos;
 @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager em;
 ApiIntegrationTest client;ApiIntegrationTest.Citizen citizen;Map<String,String> ids;String rev;
 @BeforeEach void setup()throws Exception{
  client=new ApiIntegrationTest();client.mvc=mvc;client.engine=engine;client.db=db;client.api=api;client.tokens=tokens;citizen=client.citizen();ids=new LinkedHashMap<>();
  JsonNode schema=client.request(citizen,"GET","/api/v1/matching/questions",null,200);assertEquals(14,schema.path("questions").size());rev=schema.path("revision").asText();
  demos.specification().path("questions").fields().forEachRemaining(entry->{for(JsonNode q:schema.path("questions"))if(q.path("label").equals(entry.getValue().path("label")))ids.put(entry.getKey(),q.path("id").asText());});assertEquals(14,ids.size());
 }
 Map<String,Object> good(){var v=new LinkedHashMap<String,Object>();v.put("age","21");v.put("state","TN");v.put("income","200000");v.put("college",true);v.put("gender","female");v.put("higher",true);v.put("occupation","farmer");v.put("unemployed",true);v.put("training",true);v.put("disability","40");v.put("studying",true);v.put("first",true);v.put("class","10");v.put("marks","75");return v;}
 Object payload(Map<String,Object> values,String revision){var rows=new ArrayList<Map<String,Object>>();values.forEach((key,value)->{var r=new LinkedHashMap<String,Object>();r.put("questionId",ids.getOrDefault(key,key));r.put("value",value);rows.add(r);});return Map.of("expectedProfileRevision",revision,"answers",rows);}
 void save(Map<String,Object> values)throws Exception{rev=client.request(citizen,"PUT","/api/v1/matching/answers",payload(values,rev),200).path("revision").asText();}
 JsonNode preview()throws Exception{return client.request(citizen,"GET","/api/v1/matching/preview?expectedProfileRevision="+rev,null,200);}
 JsonNode result(JsonNode preview,String name){for(JsonNode row:preview.path("results"))if(row.path("schemeName").asText().equals(DemoSchemes.PREFIX+name))return row;throw new AssertionError(name);}
 int history(){return db.queryForObject("select count(*) from eligibility_evaluations where user_id=?",Integer.class,citizen.id());}
 @Test void allEightReuseSharedAnswersAndOnlyIndividualChecksCreateHistory()throws Exception{
  var beforeSchemes=db.queryForList("select * from schemes order by id");var beforeRules=db.queryForList("select * from scheme_rule_versions order by id");var beforeHistory=db.queryForList("select * from eligibility_evaluations order by id");save(good());assertEquals("1",rev);assertEquals(14,db.queryForObject("select count(*) from profile_answers where user_id=?",Integer.class,citizen.id()));
  JsonNode matched=preview();assertEquals(8,matched.path("results").size());assertEquals(7,matched.path("results").findValuesAsText("classification").stream().filter("ELIGIBLE"::equals).count());assertEquals("NOT_ELIGIBLE",result(matched,"Senior Citizen Welfare Support").path("classification").asText());
  Set<String> expected=new HashSet<>();for(JsonNode item:demos.specification().path("schemes"))expected.add(DemoSchemes.PREFIX+item.path("name").asText());assertEquals(expected,new HashSet<>(matched.path("results").findValuesAsText("schemeName")));
  for(JsonNode row:matched.path("results")){String sid=row.path("schemeId").asText();assertEquals(db.queryForObject("select published_version_id from schemes where id=?",String.class,sid),row.path("ruleVersionId").asText());JsonNode qs=client.request(citizen,"GET","/api/v1/schemes/"+sid+"/questions",null,200);for(JsonNode q:qs)assertFalse(q.path("value").isNull());for(JsonNode e:row.path("explanations"))assertTrue(e.path("questionLabel").asText().startsWith("Demo:"));}
  preview();assertEquals(0,history());assertEquals(beforeHistory,db.queryForList("select * from eligibility_evaluations order by id"));assertEquals(beforeSchemes,db.queryForList("select * from schemes order by id"));assertEquals(beforeRules,db.queryForList("select * from scheme_rule_versions order by id"));
  JsonNode student=result(matched,"Student Education Support");JsonNode evaluation=client.request(citizen,"POST","/api/v1/eligibility/evaluate",Map.of("schemeId",student.path("schemeId").asText(),"expectedProfileRevision",rev),200);assertEquals("ELIGIBLE",evaluation.path("verdict").asText());assertEquals(1,history());String snapshot=db.queryForObject("select snapshot_json from eligibility_evaluations where id=?",String.class,evaluation.path("id").asText());preview();assertEquals(1,history());assertEquals(snapshot,db.queryForObject("select snapshot_json from eligibility_evaluations where id=?",String.class,evaluation.path("id").asText()));
 }
 @Test void unknownDoesNotSubstituteProfileAndFalseDominatesUnknown()throws Exception{
  rev=client.profile(citizen,rev).path("revision").asText();JsonNode empty=preview();assertTrue(empty.path("results").findValuesAsText("classification").stream().allMatch("NEEDS_INFORMATION"::equals));assertTrue(result(empty,"Student Education Support").path("missingQuestions").findValuesAsText("label").contains("Demo: Age (integer years)"));
  save(Map.of("college",false));JsonNode partial=preview();assertEquals("NOT_ELIGIBLE",result(partial,"Student Education Support").path("classification").asText());assertEquals("NEEDS_INFORMATION",result(partial,"Women Higher Education Grant").path("classification").asText());assertTrue(result(partial,"Student Education Support").path("explanations").findValuesAsText("result").contains("UNKNOWN"));assertEquals(0,history());
  save(Map.of());assertTrue(preview().path("results").findValuesAsText("classification").stream().allMatch("NEEDS_INFORMATION"::equals));assertEquals(0,history());
 }
 @Test void exactNumericBoundaryIsSavedReadAndMatchedWithoutRounding()throws Exception{
  var values=good();values.put("income",new BigDecimal("300000.00000000000000001"));save(values);assertEquals("NOT_ELIGIBLE",result(preview(),"Student Education Support").path("classification").asText());
  JsonNode schema=client.request(citizen,"GET","/api/v1/matching/questions",null,200);for(JsonNode q:schema.path("questions"))if(q.path("id").asText().equals(ids.get("income")))assertEquals("300000.00000000000000001",q.path("value").asText());
  values.put("income","300000");values.put("age","25");save(values);assertEquals("ELIGIBLE",result(preview(),"Student Education Support").path("classification").asText());assertEquals(0,history());
 }
 @Test void atomicValidationRevisionConflictAndUnrelatedQuestionProtection()throws Exception{
  save(good());var original=db.queryForList("select * from profile_answers where user_id=? order by id",citizen.id());var bad=good();bad.put("age","21.5");client.request(citizen,"PUT","/api/v1/matching/answers",payload(bad,rev),400);bad=good();bad.put("marks","100.01");client.request(citizen,"PUT","/api/v1/matching/answers",payload(bad,rev),400);
  bad=good();bad.put("state","INVALID");client.request(citizen,"PUT","/api/v1/matching/answers",payload(bad,rev),400);client.request(citizen,"PUT","/api/v1/matching/answers",payload(Map.of("1",true),rev),400);
  client.request(citizen,"PUT","/api/v1/matching/answers",payload(good(),"0"),409);client.request(citizen,"GET","/api/v1/matching/preview?expectedProfileRevision=0",null,409);assertEquals(original,db.queryForList("select * from profile_answers where user_id=? order by id",citizen.id()));assertEquals(rev,client.request(citizen,"GET","/api/v1/profile",null,200).path("revision").asText());assertEquals(0,history());
 }
 @Test void authenticationCsrfRbacAndCitizenIsolationRemainEnforced()throws Exception{
  for(String path:List.of("/matching/questions","/matching/preview?expectedProfileRevision=0"))assertEquals(401,mvc.perform(get("/api/v1"+path)).andReturn().getResponse().getStatus());
  assertEquals(403,mvc.perform(put("/api/v1/matching/answers").cookie(citizen.cookie()).contentType("application/json").content("{}")).andReturn().getResponse().getStatus());client.request(citizen,"GET","/api/v1/admin/schemes",null,403);
  assertEquals(401,mvc.perform(put("/api/v1/matching/answers").with(client.realCsrf()).contentType("application/json").content("{}")).andReturn().getResponse().getStatus());
  save(good());var other=client.citizen();JsonNode q=client.request(other,"GET","/api/v1/matching/questions",null,200);for(JsonNode row:q.path("questions"))assertTrue(row.path("value").isNull());assertEquals(0,history());
 }
 @Test void productionEndpointsReturnNoSyntheticDataOrWrites(){
  Api production=new Api(engine,tokens,null,db);production.em=em;production.env=new org.springframework.mock.env.MockEnvironment();var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(citizen.id(),null,List.of());
  int answers=db.queryForObject("select count(*) from profile_answers",Integer.class),histories=db.queryForObject("select count(*) from eligibility_evaluations",Integer.class);
  assertThrows(ApiFailure.class,()->production.matchingQuestionnaire(auth));assertThrows(ApiFailure.class,()->production.previewMatches(auth,"0"));assertThrows(ApiFailure.class,()->production.saveMatchingAnswers(auth,"{}"));assertEquals(answers,db.queryForObject("select count(*) from profile_answers",Integer.class));assertEquals(histories,db.queryForObject("select count(*) from eligibility_evaluations",Integer.class));
 }
 @Test @org.springframework.transaction.annotation.Transactional void previewUsesNewlyPublishedVersionAndExistingOrSemantics()throws Exception{
  String sid=db.queryForObject("select id from schemes where name=?",String.class,DemoSchemes.PREFIX+"Student Education Support");
  var leaves=List.of(Map.of("type","CONDITION","nodeId","age","field","question:"+ids.get("age"),"operator","GT","value","100"),Map.of("type","CONDITION","nodeId","college","field","question:"+ids.get("college"),"operator","EQ","value",true));
  var rule=Map.of("schemaVersion",1,"type","GROUP","operator","OR","children",leaves);
  JsonNode draft=client.admin("POST","/api/v1/admin/schemes/"+sid+"/rules",Map.of("rule",rule),200);client.admin("POST","/api/v1/admin/schemes/"+sid+"/publish",Map.of("draftVersionId",draft.path("id").asText(),"expectedDraftRevision",draft.path("revision").asText()),200);
  save(Map.of("college",true));JsonNode row=result(preview(),"Student Education Support");assertEquals(draft.path("id").asText(),row.path("ruleVersionId").asText());assertEquals("ELIGIBLE",row.path("classification").asText());assertTrue(row.path("explanations").findValuesAsText("result").contains("UNKNOWN"));
  save(Map.of("college",false));assertEquals("NEEDS_INFORMATION",result(preview(),"Student Education Support").path("classification").asText());save(Map.of("college",false,"age","21"));assertEquals("NOT_ELIGIBLE",result(preview(),"Student Education Support").path("classification").asText());assertEquals(0,history());
 }
}
