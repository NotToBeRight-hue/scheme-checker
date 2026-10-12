package in.project.schemes;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import jakarta.servlet.http.Cookie;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class ApiIntegrationTest {
 static org.testcontainers.mysql.MySQLContainer container;
 @org.springframework.test.context.DynamicPropertySource
 static void database(org.springframework.test.context.DynamicPropertyRegistry props){if(Boolean.parseBoolean(System.getenv("USE_TESTCONTAINERS"))){container=new org.testcontainers.mysql.MySQLContainer("mysql:8.4.11").withDatabaseName("schemes").withCommand("--skip-log-bin");container.start();props.add("spring.datasource.url",container::getJdbcUrl);props.add("spring.datasource.username",container::getUsername);props.add("spring.datasource.password",container::getPassword);props.add("spring.profiles.active",()->"dev");Runtime.getRuntime().addShutdownHook(new Thread(()->container.stop()));}}
 @Autowired MockMvc mvc;@Autowired RuleEngine engine;@Autowired JdbcTemplate db;@Autowired Api api;@Autowired SessionTokens tokens;
 org.springframework.test.web.servlet.request.RequestPostProcessor realCsrf(){return q->{String token=UUID.randomUUID().toString();List<Cookie> cookies=new ArrayList<>();if(q.getCookies()!=null)cookies.addAll(Arrays.asList(q.getCookies()));cookies.add(new Cookie("XSRF-TOKEN",token));q.setCookies(cookies.toArray(Cookie[]::new));q.addHeader("X-XSRF-TOKEN",token);return q;};}
 record Citizen(String id,Cookie cookie,String email){}
 Citizen citizen() throws Exception {String email=UUID.randomUUID()+"@example.test";var r=mvc.perform(post("/api/v1/auth/register").with(realCsrf()).contentType(MediaType.APPLICATION_JSON).content(engine.json.writeValueAsString(Map.of("fullName","Test Citizen","email",email,"password","a".repeat(128))))).andReturn();assertEquals(201,r.getResponse().getStatus(),r.getResponse().getContentAsString());String session=r.getResponse().getHeaders("Set-Cookie").stream().filter(x->x.startsWith("SESSION=")).findFirst().orElseThrow().split(";",2)[0].substring(8);return new Citizen(engine.parse(r.getResponse().getContentAsString()).path("id").asText(),new Cookie("SESSION",session),email);}
 JsonNode request(Citizen c,String method,String path,Object body,int status)throws Exception{MockHttpServletRequestBuilder b=switch(method){case "POST"->post(path);case "PUT"->put(path);default->get(path);};b.cookie(c.cookie());if(!method.equals("GET"))b.with(realCsrf()).contentType(MediaType.APPLICATION_JSON).content(engine.json.writeValueAsString(body));var r=mvc.perform(b).andReturn();assertEquals(status,r.getResponse().getStatus(),r.getResponse().getContentAsString());return status==204?null:engine.parse(r.getResponse().getContentAsString());}
 JsonNode admin(String method,String path,Object body,int status)throws Exception{String aid=db.queryForObject("select id from users where role='ADMIN' limit 1",String.class);MockHttpServletRequestBuilder b=switch(method){case "POST"->post(path);case "PUT"->put(path);default->get(path);};b.with(user(aid).roles("ADMIN"));if(!method.equals("GET"))b.with(realCsrf()).contentType(MediaType.APPLICATION_JSON).content(engine.json.writeValueAsString(body));var r=mvc.perform(b).andReturn();assertEquals(status,r.getResponse().getStatus(),r.getResponse().getContentAsString());return engine.parse(r.getResponse().getContentAsString());}
 JsonNode profile(Citizen c,String rev)throws Exception{return request(c,"PUT","/api/v1/profile",Map.of("expectedRevision",rev,"dateOfBirth","2000-01-01","stateCode","MH","annualIncome","100.00","educationLevel","GRADUATE","occupation","STUDENT"),200);}
 String scheme(){return db.queryForObject("select min(id) from schemes where published_version_id is not null",String.class);}
 JsonNode evaluate(Citizen c,String rev,int status)throws Exception{return request(c,"POST","/api/v1/eligibility/evaluate",Map.of("schemeId",scheme(),"expectedProfileRevision",rev),status);}
 @Test void migrationsExactlyElevenDomainTables(){assertEquals(12,db.queryForObject("select count(*) from information_schema.tables where table_schema=database()",Integer.class));assertEquals(1,db.queryForObject("select count(*) from flyway_schema_history where success=1",Integer.class));assertTrue(db.queryForObject("select version()",String.class).startsWith("8.4."));}
 @Test void registrationLoginLogoutAndNoJwtJson()throws Exception{Citizen c=citizen();JsonNode me=request(c,"GET","/api/v1/auth/me",null,200);assertFalse(me.has("passwordHash"));assertFalse(me.has("token"));var login=mvc.perform(post("/api/v1/auth/login").with(realCsrf()).contentType(MediaType.APPLICATION_JSON).content(engine.json.writeValueAsString(Map.of("email",c.email(),"password","a".repeat(128))))).andReturn();assertEquals(200,login.getResponse().getStatus());String cookie=login.getResponse().getHeaders("Set-Cookie").stream().filter(x->x.startsWith("SESSION=")).findFirst().orElseThrow();assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("Path=/api"));assertTrue(cookie.contains("SameSite=Lax"));var out=mvc.perform(post("/api/v1/auth/logout").cookie(c.cookie()).with(realCsrf())).andReturn();assertEquals(204,out.getResponse().getStatus());assertTrue(out.getResponse().getHeader("Set-Cookie").contains("Max-Age=0"));}
 @Test void csrfBootstrapAndActualHeader()throws Exception{var boot=mvc.perform(get("/api/v1/auth/csrf")).andReturn();assertEquals(200,boot.getResponse().getStatus());String t=engine.parse(boot.getResponse().getContentAsString()).path("csrfToken").asText();assertFalse(t.isBlank());var r=mvc.perform(post("/api/v1/auth/register").cookie(new Cookie("XSRF-TOKEN",t)).header("X-XSRF-TOKEN",t).contentType(MediaType.APPLICATION_JSON).content(engine.json.writeValueAsString(Map.of("fullName","CSRF test","email",UUID.randomUUID()+"@test.dev","password","password123")))).andReturn();assertEquals(201,r.getResponse().getStatus());assertTrue(r.getResponse().getHeaders("Set-Cookie").stream().anyMatch(x->x.startsWith("XSRF-TOKEN=")&&!x.startsWith("XSRF-TOKEN="+t+";")));}
 @Test void csrfRequiredForAllAuthMutations()throws Exception{Citizen c=citizen();for(String path:List.of("login","register","logout")){var r=mvc.perform(post("/api/v1/auth/"+path).cookie(c.cookie()).contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();assertEquals(403,r.getResponse().getStatus());}request(c,"GET","/api/v1/auth/me",null,200);}
 @Test void csrfRequiredForProfileMutation()throws Exception{Citizen c=citizen();assertEquals(403,mvc.perform(put("/api/v1/profile").cookie(c.cookie()).contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse().getStatus());}
 @Test void backendRbacAndAnonymousProtection()throws Exception{Citizen c=citizen();request(c,"GET","/api/v1/admin/audit-logs",null,403);assertEquals(401,mvc.perform(get("/api/v1/profile")).andReturn().getResponse().getStatus());}
 @Test void invalidJwtRejected()throws Exception{assertEquals(401,mvc.perform(get("/api/v1/profile").cookie(new Cookie("SESSION","invalid.token.here"))).andReturn().getResponse().getStatus());}
 @Test void expiredJwtRejected()throws Exception{Citizen c=citizen();String[] p=c.cookie().getValue().split("\\.");var claims=engine.json.convertValue(engine.json.readTree(Base64.getUrlDecoder().decode(p[1])),Map.class);claims.put("iat",1);claims.put("exp",901);String unsigned=p[0]+"."+tokens.b64(engine.json.writeValueAsBytes(claims));String expired=unsigned+"."+tokens.b64(tokens.sign(unsigned));assertEquals(401,mvc.perform(get("/api/v1/profile").cookie(new Cookie("SESSION",expired))).andReturn().getResponse().getStatus());}
 @Test void staleProfileUpdateRejected()throws Exception{Citizen c=citizen();profile(c,"0");request(c,"PUT","/api/v1/profile",Map.of("expectedRevision","0"),409);assertEquals("1",request(c,"GET","/api/v1/profile",null,200).path("revision").asText());}
 @Test void criticalUnknown422CreatesNoHistory()throws Exception{Citizen c=citizen();evaluate(c,"0",422);assertEquals(0,db.queryForObject("select count(*) from eligibility_evaluations where user_id=?",Integer.class,c.id()));}
 @Test void staleEvaluation409CreatesNoHistory()throws Exception{Citizen c=citizen();profile(c,"0");evaluate(c,"0",409);assertEquals(0,db.queryForObject("select count(*) from eligibility_evaluations where user_id=?",Integer.class,c.id()));}
 @Test void historyOwnershipSnapshotAndStaleness()throws Exception{Citizen c=citizen(),other=citizen();profile(c,"0");JsonNode e=evaluate(c,"1",200);assertEquals(2,e.path("explanations").size());String eid=e.path("evaluationId").asText();request(other,"GET","/api/v1/eligibility/history/"+eid,null,404);String snapshot=db.queryForObject("select snapshot_json from eligibility_evaluations where id=?",String.class,eid);profile(c,"1");JsonNode stale=request(c,"GET","/api/v1/eligibility/history/"+eid,null,200);assertTrue(stale.path("stale").asBoolean());assertEquals(snapshot,db.queryForObject("select snapshot_json from eligibility_evaluations where id=?",String.class,eid));assertThrows(Exception.class,()->db.update("update eligibility_evaluations set verdict='NOT_ELIGIBLE' where id=?",eid));}
 @Test void sharedAnswersPreservedAndConflictRejected()throws Exception{Citizen c=citizen();String qid=db.queryForObject("select min(id) from questions",String.class);request(c,"PUT","/api/v1/schemes/"+scheme()+"/answers",Map.of("expectedProfileRevision","0","answers",List.of(Map.of("questionId",qid,"value",true))),200);request(c,"PUT","/api/v1/schemes/"+scheme()+"/answers",Map.of("expectedProfileRevision","0","answers",List.of()),409);request(c,"PUT","/api/v1/schemes/"+scheme()+"/answers",Map.of("expectedProfileRevision","1","answers",List.of()),200);assertEquals(1,db.queryForObject("select count(*) from profile_answers where user_id=?",Integer.class,c.id()));}
 @Test void duplicateUnrelatedAndInvalidAnswersRejected()throws Exception{Citizen c=citizen();String qid=db.queryForObject("select min(id) from questions",String.class);var answer=Map.of("questionId",qid,"value",true);request(c,"PUT","/api/v1/schemes/"+scheme()+"/answers",Map.of("expectedProfileRevision","0","answers",List.of(answer,answer)),400);request(c,"PUT","/api/v1/schemes/"+scheme()+"/answers",Map.of("expectedProfileRevision","0","answers",List.of(Map.of("questionId","999999","value",true))),400);request(c,"PUT","/api/v1/schemes/"+scheme()+"/answers",Map.of("expectedProfileRevision","0","answers",List.of(Map.of("questionId",qid,"value","yes"))),400);assertEquals("0",request(c,"GET","/api/v1/profile",null,200).path("revision").asText());}
 @Test void suggestionsIsolatedAndAcceptanceDoesNotPublish()throws Exception{Citizen c=citizen(),other=citizen();JsonNode sug=request(c,"POST","/api/v1/suggestions",Map.of("type","SCHEME","schemeName","Suggested sample","description","Source needs checking"),201);assertEquals(0,request(other,"GET","/api/v1/suggestions/mine",null,200).path("total").asInt());long before=db.queryForObject("select count(*) from schemes",Long.class);admin("POST","/api/v1/admin/suggestions/"+sug.path("id").asText()+"/review",Map.of("status","ACCEPTED","reviewNotes","Accepted for drafting only","expectedRevision","0"),200);assertEquals(before,db.queryForObject("select count(*) from schemes",Long.class));}
 @Test void publicationAtomicImmutableAndDraftRevisionConflict()throws Exception{String sid=scheme();var rule=engine.parse("{\"schemaVersion\":1,\"type\":\"GROUP\",\"operator\":\"AND\",\"children\":[{\"type\":\"CONDITION\",\"nodeId\":\"age\",\"field\":\"completedAgeYears\",\"operator\":\"GTE\",\"value\":18},{\"type\":\"CONDITION\",\"nodeId\":\"income\",\"field\":\"annualIncome\",\"operator\":\"LTE\",\"value\":\"100000.00\"}]}");Object raw=engine.json.convertValue(rule,Object.class);JsonNode draft=admin("POST","/api/v1/admin/schemes/"+sid+"/rules",Map.of("rule",raw),200);String rid=draft.path("id").asText();String previous=db.queryForObject("select published_version_id from schemes where id=?",String.class,sid);admin("POST","/api/v1/admin/schemes/"+sid+"/publish",Map.of("draftVersionId",rid,"expectedDraftRevision","9"),409);assertEquals(previous,db.queryForObject("select published_version_id from schemes where id=?",String.class,sid));admin("POST","/api/v1/admin/schemes/"+sid+"/publish",Map.of("draftVersionId",rid,"expectedDraftRevision","0"),200);assertEquals(rid,db.queryForObject("select published_version_id from schemes where id=?",String.class,sid));admin("PUT","/api/v1/admin/schemes/"+sid+"/rules/"+rid,Map.of("expectedDraftRevision","0","rule",raw),409);assertThrows(Exception.class,()->db.update("update scheme_rule_versions set rule_json='{}' where id=?",rid));}
 @Test void concurrentProfileUpdateExactlyOneSucceeds()throws Exception{Citizen c=citizen();ExecutorService pool=Executors.newFixedThreadPool(2);try{Callable<Integer> task=()->mvc.perform(put("/api/v1/profile").cookie(c.cookie()).with(realCsrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedRevision\":\"0\",\"occupation\":\"TEST\"}")).andReturn().getResponse().getStatus();var results=pool.invokeAll(List.of(task,task));var statuses=new ArrayList<Integer>();for(var f:results)statuses.add(f.get());Collections.sort(statuses);assertEquals(List.of(200,409),statuses);}finally{pool.shutdownNow();}}
 @Test void accountRateLimit()throws Exception{String email=UUID.randomUUID()+"@test.dev";for(int i=0;i<11;i++){var r=mvc.perform(post("/api/v1/auth/login").with(q->{q.setRemoteAddr("192.0.2."+new Random().nextInt(250));return q;}).with(realCsrf()).contentType(MediaType.APPLICATION_JSON).content(engine.json.writeValueAsString(Map.of("email",email,"password","wrongpassword")))).andReturn();assertEquals(i==10?429:401,r.getResponse().getStatus());}}
 @Test void ipRateLimit()throws Exception{for(int i=0;i<31;i++){var r=mvc.perform(post("/api/v1/auth/login").with(q->{q.setRemoteAddr("198.51.100.200");return q;}).with(realCsrf()).contentType(MediaType.APPLICATION_JSON).content(engine.json.writeValueAsString(Map.of("email",UUID.randomUUID()+"@test.dev","password","wrongpassword")))).andReturn();assertEquals(i==30?429:401,r.getResponse().getStatus());}}
 @Test void demoSeedsExplicitlyFictional(){assertTrue(db.queryForObject("select count(*) from schemes where synthetic=1 and name like 'Synthetic Demo:%'",Integer.class)>=8);assertEquals(0,db.queryForObject("select count(*) from schemes where synthetic=0 and published_version_id is not null",Integer.class));}

 Map<String,Object> metadata(String category,boolean synthetic,List<String> qids){return new HashMap<>(Map.of("name","Synthetic Test Draft "+UUID.randomUUID(),"categoryId",category,"description","Fictional test data","benefits","No real benefit","guidance","No real application","synthetic",synthetic,"questionIds",qids));}
 @Test void categoryQuestionMetadataAndDraftWorkflows()throws Exception{
  String cat=admin("POST","/api/v1/admin/categories",Map.of("name","Test "+UUID.randomUUID()),200).path("id").asText();admin("PUT","/api/v1/admin/categories/"+cat,Map.of("name","Updated "+UUID.randomUUID()),200);
  var q=admin("POST","/api/v1/admin/questions",Map.of("label","Test boolean","type","BOOLEAN"),200);String qid=q.path("id").asText();admin("PUT","/api/v1/admin/questions/"+qid,Map.of("label","Updated boolean","type","BOOLEAN","expectedRevision","0"),200);admin("PUT","/api/v1/admin/questions/"+qid,Map.of("label","Stale","type","BOOLEAN","expectedRevision","0"),409);
  var meta=metadata(cat,true,List.of(qid));String sid=admin("POST","/api/v1/admin/schemes",meta,201).path("id").asText();meta.put("expectedRevision","0");meta.put("name","Synthetic Updated Draft");admin("PUT","/api/v1/admin/schemes/"+sid,meta,200);admin("PUT","/api/v1/admin/schemes/"+sid,meta,409);assertEquals(1,admin("GET","/api/v1/admin/schemes/"+sid+"/questions",null,200).size());
  var rule=Map.of("schemaVersion",1,"type","GROUP","operator","AND","children",List.of(Map.of("type","CONDITION","nodeId","q","field","question:"+qid,"operator","EQ","value",true)));
  String rid=admin("POST","/api/v1/admin/schemes/"+sid+"/rules",Map.of("rule",rule),200).path("id").asText();admin("PUT","/api/v1/admin/schemes/"+sid+"/rules/"+rid,Map.of("rule",rule,"expectedDraftRevision","0"),200);admin("PUT","/api/v1/admin/schemes/"+sid+"/rules/"+rid,Map.of("rule",rule,"expectedDraftRevision","0"),409);admin("POST","/api/v1/admin/schemes/"+sid+"/publish",Map.of("draftVersionId",rid,"expectedDraftRevision","1"),200);
  admin("PUT","/api/v1/admin/questions/"+qid,Map.of("label","Cannot change","type","BOOLEAN","expectedRevision","1"),409);meta.put("expectedRevision","2");admin("PUT","/api/v1/admin/schemes/"+sid,meta,409);
  assertTrue(admin("GET","/api/v1/admin/overview",null,200).has("schemes"));assertTrue(admin("GET","/api/v1/admin/schemes",null,200).isArray());assertTrue(admin("GET","/api/v1/admin/questions",null,200).isArray());assertEquals(1,admin("GET","/api/v1/admin/schemes/"+sid+"/rules",null,200).size());assertTrue(admin("GET","/api/v1/admin/audit-logs",null,200).has("items"));
 }
 @Test void genuineUnverifiedSchemeCannotPublish()throws Exception{String cat=db.queryForObject("select min(id) from scheme_categories",String.class);String sid=admin("POST","/api/v1/admin/schemes",metadata(cat,false,List.of()),201).path("id").asText();Object rule=engine.json.convertValue(engine.parse("{\"schemaVersion\":1,\"type\":\"GROUP\",\"operator\":\"AND\",\"children\":[{\"type\":\"CONDITION\",\"nodeId\":\"age\",\"field\":\"completedAgeYears\",\"operator\":\"GTE\",\"value\":18}]}"),Object.class);String rid=admin("POST","/api/v1/admin/schemes/"+sid+"/rules",Map.of("rule",rule),200).path("id").asText();admin("POST","/api/v1/admin/schemes/"+sid+"/publish",Map.of("draftVersionId",rid,"expectedDraftRevision","0"),422);assertNull(db.queryForObject("select published_version_id from schemes where id=?",String.class,sid));}
 @Test void typedAnswerStorageAndQuestionValidation()throws Exception{Citizen c=citizen();String cat=db.queryForObject("select min(id) from scheme_categories",String.class);var multi=admin("POST","/api/v1/admin/questions",Map.of("label","Select activities","type","MULTI_SELECT","options",List.of("A","B")),200);var select=admin("POST","/api/v1/admin/questions",Map.of("label","Select one","type","SELECT","options",List.of("A","B")),200);String mid=multi.path("id").asText(),qid=select.path("id").asText();String sid=admin("POST","/api/v1/admin/schemes",metadata(cat,true,List.of(mid,qid)),201).path("id").asText();Object rule=Map.of("schemaVersion",1,"type","GROUP","operator","AND","children",List.of(Map.of("type","CONDITION","nodeId","choice","field","question:"+qid,"operator","IN","value",List.of("A","B"))));String rid=admin("POST","/api/v1/admin/schemes/"+sid+"/rules",Map.of("rule",rule),200).path("id").asText();admin("POST","/api/v1/admin/schemes/"+sid+"/publish",Map.of("draftVersionId",rid,"expectedDraftRevision","0"),200);request(c,"PUT","/api/v1/schemes/"+sid+"/answers",Map.of("expectedProfileRevision","0","answers",List.of(Map.of("questionId",mid,"value",List.of("A","B")),Map.of("questionId",qid,"value","A"))),200);assertEquals(2,request(c,"GET","/api/v1/schemes/"+sid+"/questions",null,200).size());request(c,"PUT","/api/v1/schemes/"+sid+"/answers",Map.of("expectedProfileRevision","1","answers",List.of(Map.of("questionId",qid,"value","C"))),400);request(c,"PUT","/api/v1/schemes/"+sid+"/answers",Map.of("expectedProfileRevision","1","answers",List.of()),200);assertEquals(0,db.queryForObject("select count(*) from profile_answers where user_id=?",Integer.class,c.id()));admin("POST","/api/v1/admin/questions",Map.of("label","Bad options","type","SELECT","options",List.of()),400);admin("POST","/api/v1/admin/questions",Map.of("label","Bad type","type","UPLOAD"),400);}
 @Test void acceptedSuggestionConversionCreatesOnlyDraft()throws Exception{Citizen c=citizen();String sug=request(c,"POST","/api/v1/suggestions",Map.of("type","SCHEME","schemeName","Synthetic proposed","description","Proposed demo only"),201).path("id").asText();String cat=db.queryForObject("select min(id) from scheme_categories",String.class);admin("POST","/api/v1/admin/suggestions/"+sug+"/convert",metadata(cat,true,List.of()),409);admin("POST","/api/v1/admin/suggestions/"+sug+"/review",Map.of("status","ACCEPTED","reviewNotes","Reviewed for drafting","expectedRevision","0"),200);admin("POST","/api/v1/admin/suggestions/"+sug+"/review",Map.of("status","REJECTED","reviewNotes","Stale review","expectedRevision","0"),409);JsonNode converted=admin("POST","/api/v1/admin/suggestions/"+sug+"/convert",metadata(cat,true,List.of()),200);assertTrue(converted.path("publishedVersionId").isNull());admin("GET","/api/v1/admin/suggestions/"+sug,null,200);admin("GET","/api/v1/admin/suggestions",null,200);request(c,"POST","/api/v1/suggestions",Map.of("type","CRITERION","description","Missing scheme"),400);request(c,"POST","/api/v1/suggestions",Map.of("type","SCHEME","schemeName","Unsafe source","description","test","sourceUrl","http://example.com"),400);}
 @Test void profileCatalogValidationAndDashboard()throws Exception{Citizen c=citizen();request(c,"PUT","/api/v1/profile",Map.of("expectedRevision","0","annualIncome","-1"),400);request(c,"PUT","/api/v1/profile",Map.of("expectedRevision","0","stateCode","XX"),400);request(c,"PUT","/api/v1/profile",Map.of("expectedRevision","0","dateOfBirth","2099-01-01"),400);request(c,"PUT","/api/v1/profile",Map.of("expectedRevision","0","annualIncome","1.001"),400);request(c,"GET","/api/v1/schemes?sort=malicious",null,400);request(c,"GET","/api/v1/schemes?size=101",null,400);request(c,"GET","/api/v1/schemes?search=Learning&stateCode=MH",null,200);request(c,"GET","/api/v1/schemes/"+scheme(),null,200);assertEquals(0,request(c,"GET","/api/v1/dashboard",null,200).path("profileCompleteness").asInt());request(c,"GET","/api/v1/categories",null,200);}
 @Test void publicSignupCannotChooseAdminAndDbConstraintsHold()throws Exception{String email=UUID.randomUUID()+"@test.dev";var r=mvc.perform(post("/api/v1/auth/register").with(realCsrf()).contentType(MediaType.APPLICATION_JSON).content(engine.json.writeValueAsString(Map.of("fullName","Role test","email",email,"password","password123","role","ADMIN")))).andReturn();assertEquals(201,r.getResponse().getStatus());assertEquals("CITIZEN",engine.parse(r.getResponse().getContentAsString()).path("role").asText());assertThrows(Exception.class,()->db.update("insert into profile_answers(user_id,question_id,value_json) values(999999999,999999999,'true')"));assertThrows(Exception.class,()->db.update("insert into users(full_name,email,password_hash,role,status) values('a',?,'x','ADMIN','ACTIVE')",email));}

 @Test void concurrentEvaluationAndProfileUpdateRemainConsistent()throws Exception{Citizen c=citizen();profile(c,"0");ExecutorService pool=Executors.newFixedThreadPool(2);try{Callable<MvcResult> check=()->mvc.perform(post("/api/v1/eligibility/evaluate").cookie(c.cookie()).with(realCsrf()).contentType(MediaType.APPLICATION_JSON).content(engine.json.writeValueAsString(Map.of("schemeId",scheme(),"expectedProfileRevision","1")))).andReturn();Callable<MvcResult> update=()->mvc.perform(put("/api/v1/profile").cookie(c.cookie()).with(realCsrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedRevision\":\"1\",\"annualIncome\":\"999999.00\"}")).andReturn();var results=pool.invokeAll(List.of(check,update));var checked=results.get(0).get().getResponse();assertEquals(200,results.get(1).get().getResponse().getStatus());assertTrue(checked.getStatus()==200||checked.getStatus()==409);int count=db.queryForObject("select count(*) from eligibility_evaluations where user_id=?",Integer.class,c.id());assertEquals(checked.getStatus()==200?1:0,count);if(count==1){var e=engine.parse(checked.getContentAsString());assertEquals("1",e.path("profileRevision").asText());assertEquals("100.00",e.path("snapshot").path("facts").path("annualIncome").asText());}}finally{pool.shutdownNow();}}
 @Test void concurrentPublicationExactlyOneSucceeds()throws Exception{String cat=db.queryForObject("select min(id) from scheme_categories",String.class);String sid=admin("POST","/api/v1/admin/schemes",metadata(cat,true,List.of()),201).path("id").asText();Object rule=Map.of("schemaVersion",1,"type","GROUP","operator","AND","children",List.of(Map.of("type","CONDITION","nodeId","age","field","completedAgeYears","operator","GTE","value",18)));String rid=admin("POST","/api/v1/admin/schemes/"+sid+"/rules",Map.of("rule",rule),200).path("id").asText();String aid=db.queryForObject("select id from users where role='ADMIN' limit 1",String.class);ExecutorService pool=Executors.newFixedThreadPool(2);try{Callable<Integer> task=()->mvc.perform(post("/api/v1/admin/schemes/"+sid+"/publish").with(user(aid).roles("ADMIN")).with(realCsrf()).contentType(MediaType.APPLICATION_JSON).content(engine.json.writeValueAsString(Map.of("draftVersionId",rid,"expectedDraftRevision","0")))).andReturn().getResponse().getStatus();var results=pool.invokeAll(List.of(task,task));var statuses=new ArrayList<Integer>();for(var f:results)statuses.add(f.get());Collections.sort(statuses);assertEquals(List.of(200,409),statuses);assertEquals(1,db.queryForObject("select count(*) from admin_audit_logs where action='RULE_PUBLISHED' and target_id=?",Integer.class,rid));}finally{pool.shutdownNow();}}
 @Test void currentUserStatusRecheckedForEveryJwtRequest()throws Exception{Citizen c=citizen();db.update("update users set status='DISABLED' where id=?",c.id());try{request(c,"GET","/api/v1/auth/me",null,401);}finally{db.update("update users set status='ACTIVE' where id=?",c.id());}}

 record PublicationFixture(String schemeId, String ruleId, String activeQuestionId,
                           String questionId, String questionLabel, Map<String,Object> metadata) {}

 PublicationFixture publicationFixture(boolean questionActive) throws Exception {
  String category = db.queryForObject("select min(id) from scheme_categories", String.class);
  String activeId = admin("POST", "/api/v1/admin/questions", Map.of(
      "label", "Active linked question " + UUID.randomUUID(), "type", "BOOLEAN", "active", true), 200)
      .path("id").asText();
  String label = "Unused linked question " + UUID.randomUUID();
  String questionId = admin("POST", "/api/v1/admin/questions", Map.of(
      "label", label, "type", "TEXT", "active", questionActive), 200).path("id").asText();
  Map<String,Object> metadata = metadata(category, true, List.of(activeId, questionId));
  String schemeId = admin("POST", "/api/v1/admin/schemes", metadata, 201).path("id").asText();
  // The rule intentionally references neither linked question.
  Object rule = Map.of("schemaVersion", 1, "type", "GROUP", "operator", "AND", "children", List.of(
      Map.of("type", "CONDITION", "nodeId", "age", "field", "completedAgeYears", "operator", "GTE", "value", 18)));
  String ruleId = admin("POST", "/api/v1/admin/schemes/" + schemeId + "/rules",
      Map.of("rule", rule), 200).path("id").asText();
  return new PublicationFixture(schemeId, ruleId, activeId, questionId, label, metadata);
 }

 Map<String,Object> publicationBody(PublicationFixture fixture) {
  return Map.of("draftVersionId", fixture.ruleId(), "expectedDraftRevision", "0");
 }

 void assertRejectedPublicationHasNoPartialState(PublicationFixture fixture) throws Exception {
  JsonNode response = admin("POST", "/api/v1/admin/schemes/" + fixture.schemeId() + "/publish",
      publicationBody(fixture), 422);
  assertEquals("VALIDATION_ERROR", response.path("error").path("code").asText());
  assertEquals(fixture.questionId(), response.path("error").path("details").path("questionId").asText());
  assertTrue(response.path("error").path("message").asText().contains(fixture.questionLabel()));
  assertFalse(response.path("requestId").asText().isBlank());
  assertNull(db.queryForObject("select published_version_id from schemes where id=?", String.class, fixture.schemeId()));
  assertEquals("0", db.queryForObject("select revision from schemes where id=?", String.class, fixture.schemeId()));
  assertEquals("DRAFT", db.queryForObject("select status from scheme_rule_versions where id=?", String.class, fixture.ruleId()));
  assertEquals("0", db.queryForObject("select revision from scheme_rule_versions where id=?", String.class, fixture.ruleId()));
  assertEquals(1, db.queryForObject("select count(*) from scheme_rule_versions where scheme_id=?", Integer.class, fixture.schemeId()));
  assertEquals(0, db.queryForObject("select count(*) from scheme_rule_versions where scheme_id=? and status='PUBLISHED'", Integer.class, fixture.schemeId()));
  assertEquals(0, db.queryForObject("select count(*) from admin_audit_logs where action='RULE_PUBLISHED' and target_id=?", Integer.class, fixture.ruleId()));
  assertEquals(2, db.queryForObject("select count(*) from scheme_questions where scheme_id=?", Integer.class, fixture.schemeId()));
  assertFalse(db.queryForObject("select active from questions where id=?", Boolean.class, fixture.questionId()));
 }

 void assertSuccessfulPublication(PublicationFixture fixture, int linkedQuestionCount) throws Exception {
  JsonNode response = admin("POST", "/api/v1/admin/schemes/" + fixture.schemeId() + "/publish",
      publicationBody(fixture), 200);
  assertEquals(fixture.ruleId(), response.path("publishedVersionId").asText());
  assertEquals(fixture.ruleId(), db.queryForObject("select published_version_id from schemes where id=?", String.class, fixture.schemeId()));
  assertEquals("PUBLISHED", db.queryForObject("select status from scheme_rule_versions where id=?", String.class, fixture.ruleId()));
  assertEquals("1", db.queryForObject("select revision from schemes where id=?", String.class, fixture.schemeId()));
  assertEquals("0", db.queryForObject("select revision from scheme_rule_versions where id=?", String.class, fixture.ruleId()));
  assertEquals(1, db.queryForObject("select count(*) from scheme_rule_versions where scheme_id=?", Integer.class, fixture.schemeId()));
  assertEquals(1, db.queryForObject("select count(*) from admin_audit_logs where action='RULE_PUBLISHED' and target_id=?", Integer.class, fixture.ruleId()));
  Citizen citizen = citizen();
  JsonNode questions = request(citizen, "GET", "/api/v1/schemes/" + fixture.schemeId() + "/questions", null, 200);
  assertEquals(linkedQuestionCount, questions.size());
  List<Map<String,Object>> answers = new ArrayList<>();
  for (JsonNode question : questions) {
   assertTrue(question.path("active").asBoolean());
   Object answer = "BOOLEAN".equals(question.path("type").asText()) ? true : "Test answer";
   answers.add(Map.of("questionId", question.path("id").asText(), "value", answer));
  }
  request(citizen, "PUT", "/api/v1/schemes/" + fixture.schemeId() + "/answers",
      Map.of("expectedProfileRevision", "0", "answers", answers), 200);
 }

 @Test void publicationRejectsInactiveLinkedQuestionWithoutRuleReference() throws Exception {
  PublicationFixture fixture = publicationFixture(false);
  assertRejectedPublicationHasNoPartialState(fixture);
 }

 @Test void publicationSucceedsAfterRemovingInactiveLink() throws Exception {
  PublicationFixture fixture = publicationFixture(false);
  assertRejectedPublicationHasNoPartialState(fixture);
  Map<String,Object> updatedMetadata = new HashMap<>(fixture.metadata());
  updatedMetadata.put("questionIds", List.of(fixture.activeQuestionId()));
  updatedMetadata.put("expectedRevision", "0");
  admin("PUT", "/api/v1/admin/schemes/" + fixture.schemeId(), updatedMetadata, 200);
  // Metadata edits advance the scheme revision once; publication advances it once more.
  JsonNode published = admin("POST", "/api/v1/admin/schemes/" + fixture.schemeId() + "/publish",
      publicationBody(fixture), 200);
  assertEquals(fixture.ruleId(), published.path("publishedVersionId").asText());
  assertEquals("2", published.path("revision").asText());
  assertEquals("PUBLISHED", db.queryForObject("select status from scheme_rule_versions where id=?", String.class, fixture.ruleId()));
  assertEquals(1, db.queryForObject("select count(*) from scheme_rule_versions where scheme_id=?", Integer.class, fixture.schemeId()));
  assertEquals(1, db.queryForObject("select count(*) from admin_audit_logs where action='RULE_PUBLISHED' and target_id=?", Integer.class, fixture.ruleId()));
  assertEquals(0, db.queryForObject("select count(*) from scheme_questions where scheme_id=? and question_id=?", Integer.class, fixture.schemeId(), fixture.questionId()));
  assertFalse(db.queryForObject("select active from questions where id=?", Boolean.class, fixture.questionId()));
  Citizen citizen = citizen();
  JsonNode questions = request(citizen, "GET", "/api/v1/schemes/" + fixture.schemeId() + "/questions", null, 200);
  assertEquals(1, questions.size());
  assertEquals(fixture.activeQuestionId(), questions.get(0).path("id").asText());
  request(citizen, "PUT", "/api/v1/schemes/" + fixture.schemeId() + "/answers", Map.of(
      "expectedProfileRevision", "0", "answers", List.of(Map.of("questionId", fixture.activeQuestionId(), "value", true))), 200);
 }

 @Test void publicationSucceedsAfterReactivatingLinkedQuestion() throws Exception {
  PublicationFixture fixture = publicationFixture(false);
  assertRejectedPublicationHasNoPartialState(fixture);
  JsonNode question = admin("PUT", "/api/v1/admin/questions/" + fixture.questionId(), Map.of(
      "label", fixture.questionLabel(), "type", "TEXT", "active", true, "expectedRevision", "0"), 200);
  assertTrue(question.path("active").asBoolean());
  assertEquals("1", question.path("revision").asText());
  assertSuccessfulPublication(fixture, 2);
  admin("PUT", "/api/v1/admin/questions/" + fixture.questionId(), Map.of(
      "label", fixture.questionLabel(), "type", "TEXT", "active", false, "expectedRevision", "1"), 409);
 }

 @Test void publicationSucceedsWithActiveLinkedQuestions() throws Exception {
  assertSuccessfulPublication(publicationFixture(true), 2);
 }

 JsonNode rawAdmin(String method,String path,String json,int status) throws Exception {
  String administrator=db.queryForObject("select id from users where role='ADMIN' limit 1",String.class);
  var builder=method.equals("PUT")?put(path):post(path);
  var response=mvc.perform(builder.with(user(administrator).roles("ADMIN")).with(realCsrf())
      .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn().getResponse();
  assertEquals(status,response.getStatus(),response.getContentAsString());
  return engine.parse(response.getContentAsString());
 }
 String precisionScheme(List<String> questionIds) throws Exception {
  String category=db.queryForObject("select min(id) from scheme_categories",String.class);
  return admin("POST","/api/v1/admin/schemes",metadata(category,true,questionIds),201).path("id").asText();
 }
 String precisionRule(String field,String operator,String valueToken){
  return "{\"schemaVersion\":1,\"type\":\"GROUP\",\"operator\":\"AND\",\"children\":[{\"type\":\"CONDITION\",\"nodeId\":\"exact\",\"field\":\""+field+"\",\"operator\":\""+operator+"\",\"value\":"+valueToken+"}]}";
 }
 JsonNode storedPrecisionValue(String ruleId){
  return engine.parse(db.queryForObject("select rule_json from scheme_rule_versions where id=?",String.class,ruleId)).path("children").get(0).path("value");
 }
 @Test void precisionRuleRoundTripThroughCreateUpdatePublicationAndEvaluation() throws Exception {
  String schemeId=precisionScheme(List.of());
  JsonNode draft=rawAdmin("POST","/api/v1/admin/schemes/"+schemeId+"/rules",
      "{\"rule\":"+precisionRule("annualIncome","EQ","1000000000000000.11")+"}",200);
  String ruleId=draft.path("id").asText();
  assertEquals("1000000000000000.11",draft.path("rule").path("children").get(0).path("value").textValue());
  assertEquals("1000000000000000.11",storedPrecisionValue(ruleId).textValue());
  assertEquals("1000000000000000.11",admin("GET","/api/v1/admin/schemes/"+schemeId+"/rules",null,200)
      .get(0).path("rule").path("children").get(0).path("value").textValue());
  rawAdmin("PUT","/api/v1/admin/schemes/"+schemeId+"/rules/"+ruleId,
      "{\"expectedDraftRevision\":\"0\",\"rule\":"+precisionRule("annualIncome","EQ","1000000000000000.12")+"}",200);
  assertEquals("1000000000000000.12",storedPrecisionValue(ruleId).textValue());
  admin("POST","/api/v1/admin/schemes/"+schemeId+"/publish",Map.of("draftVersionId",ruleId,"expectedDraftRevision","1"),200);
  Citizen citizen=citizen();
  request(citizen,"PUT","/api/v1/profile",Map.of("expectedRevision","0","annualIncome","1000000000000000.12"),200);
  JsonNode eligible=request(citizen,"POST","/api/v1/eligibility/evaluate",Map.of("schemeId",schemeId,"expectedProfileRevision","1"),200);
  assertEquals("ELIGIBLE",eligible.path("verdict").asText());
  assertEquals("1000000000000000.12",eligible.path("explanations").get(0).path("expectedValue").textValue());
  assertEquals("1000000000000000.12",eligible.path("explanations").get(0).path("actualValue").textValue());
  assertEquals("1000000000000000.12",request(citizen,"GET","/api/v1/schemes/"+schemeId,null,200)
      .path("rule").path("children").get(0).path("value").textValue());
  request(citizen,"PUT","/api/v1/profile",Map.of("expectedRevision","1","annualIncome","1000000000000000.11"),200);
  JsonNode ineligible=request(citizen,"POST","/api/v1/eligibility/evaluate",Map.of("schemeId",schemeId,"expectedProfileRevision","2"),200);
  assertEquals("NOT_ELIGIBLE",ineligible.path("verdict").asText());
  JsonNode snapshot=request(citizen,"GET","/api/v1/eligibility/history/"+eligible.path("evaluationId").asText(),null,200);
  assertEquals("1000000000000000.12",snapshot.path("snapshot").path("rule").path("children").get(0).path("value").textValue());
  assertEquals("ELIGIBLE",snapshot.path("verdict").asText());
 }
 @Test void precisionNumericRangesAndListsRetainExactBounds() throws Exception {
  String schemeId=precisionScheme(List.of());
  for(String operator:List.of("IN","NOT_IN","BETWEEN")){
   JsonNode draft=rawAdmin("POST","/api/v1/admin/schemes/"+schemeId+"/rules",
       "{\"rule\":"+precisionRule("annualIncome",operator,"[1000000000000000.11,1000000000000000.12]")+"}",200);
   JsonNode values=storedPrecisionValue(draft.path("id").asText());
   assertEquals("1000000000000000.11",values.get(0).textValue());assertEquals("1000000000000000.12",values.get(1).textValue());
   var result=engine.evaluate(engine.json.valueToTree(engine.json.convertValue(draft.path("rule"),Object.class)),
       Map.of("annualIncome","1000000000000000.11"),Map.of());
   assertEquals(operator.equals("NOT_IN")?RuleEngine.Truth.FALSE:RuleEngine.Truth.TRUE,result.truth());
   assertEquals(List.of("1000000000000000.11","1000000000000000.12"),result.explanations().get(0).get("expectedValue"));
  }
 }
 @Test void precisionSafeNumbersAndDecimalStringsRemainCompatible() throws Exception {
  String schemeId=precisionScheme(List.of());
  for(String token:List.of("18","50000.25","1.2345e5","9007199254740993","\"1000000000000000.1100\"")){
   JsonNode draft=rawAdmin("POST","/api/v1/admin/schemes/"+schemeId+"/rules",
       "{\"rule\":"+precisionRule("annualIncome","EQ",token)+"}",200);
   String expected=token.startsWith("\"")?engine.parse(token).textValue():new java.math.BigDecimal(token).toPlainString();
   assertEquals(expected,storedPrecisionValue(draft.path("id").asText()).textValue());
   assertEquals(RuleEngine.Truth.TRUE,engine.evaluate(draft.path("rule"),Map.of("annualIncome",expected),Map.of()).truth());
  }
 }
 @Test void precisionInvalidRepresentationsAreRejectedWithoutSavingDrafts() throws Exception {
  String schemeId=precisionScheme(List.of());
  for(String token:List.of("NaN","Infinity","-Infinity","1e10000","\"NaN\"","true")){
   JsonNode error=rawAdmin("POST","/api/v1/admin/schemes/"+schemeId+"/rules",
       "{\"rule\":"+precisionRule("annualIncome","EQ",token)+"}",400);
   assertEquals("VALIDATION_ERROR",error.path("error").path("code").asText());
  }
  rawAdmin("POST","/api/v1/admin/schemes/"+schemeId+"/rules",
      "{\"rule\":"+precisionRule("stateCode","EQ","18")+"}",400);
  assertEquals(0,db.queryForObject("select count(*) from scheme_rule_versions where scheme_id=?",Integer.class,schemeId));
 }
 @Test void precisionCriterionSuggestionConversionCreatesAnExactDraft() throws Exception {
  String schemeId=precisionScheme(List.of());
  String original=rawAdmin("POST","/api/v1/admin/schemes/"+schemeId+"/rules",
      "{\"rule\":"+precisionRule("annualIncome","LTE","100000")+"}",200).path("id").asText();
  admin("POST","/api/v1/admin/schemes/"+schemeId+"/publish",Map.of("draftVersionId",original,"expectedDraftRevision","0"),200);
  Citizen citizen=citizen();
  String suggestion=request(citizen,"POST","/api/v1/suggestions",Map.of("type","CRITERION","schemeId",schemeId,"description","Synthetic precision test only"),201).path("id").asText();
  admin("POST","/api/v1/admin/suggestions/"+suggestion+"/review",Map.of("status","ACCEPTED","reviewNotes","Test draft only","expectedRevision","0"),200);
  JsonNode draft=rawAdmin("POST","/api/v1/admin/suggestions/"+suggestion+"/convert",
      "{\"rule\":"+precisionRule("annualIncome","EQ","1000000000000000.11")+"}",200);
  assertEquals("DRAFT",draft.path("status").asText());
  assertEquals("1000000000000000.11",storedPrecisionValue(draft.path("id").asText()).textValue());
  assertEquals(original,db.queryForObject("select published_version_id from schemes where id=?",String.class,schemeId));
 }
 @Test void precisionNumericQuestionRulesRemainExactAndUsable() throws Exception {
  String question=admin("POST","/api/v1/admin/questions",Map.of("label","Synthetic numeric precision question","type","NUMBER"),200).path("id").asText();
  String schemeId=precisionScheme(List.of(question));
  String ruleId=rawAdmin("POST","/api/v1/admin/schemes/"+schemeId+"/rules",
      "{\"rule\":"+precisionRule("question:"+question,"EQ","1000000000000000.11")+"}",200).path("id").asText();
  assertEquals("1000000000000000.11",storedPrecisionValue(ruleId).textValue());
  admin("POST","/api/v1/admin/schemes/"+schemeId+"/publish",Map.of("draftVersionId",ruleId,"expectedDraftRevision","0"),200);
  Citizen citizen=citizen();
  request(citizen,"PUT","/api/v1/schemes/"+schemeId+"/answers",Map.of("expectedProfileRevision","0","answers",List.of(Map.of("questionId",question,"value","1000000000000000.11"))),200);
  JsonNode evaluation=request(citizen,"POST","/api/v1/eligibility/evaluate",Map.of("schemeId",schemeId,"expectedProfileRevision","1"),200);
  assertEquals("ELIGIBLE",evaluation.path("verdict").asText());
  assertEquals("1000000000000000.11",evaluation.path("explanations").get(0).path("expectedValue").textValue());
 }
}
