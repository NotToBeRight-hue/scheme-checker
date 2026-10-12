package in.project.schemes;

import jakarta.persistence.*;

import jakarta.servlet.http.*;

import org.springframework.web.bind.annotation.*;

import org.springframework.http.*;

import org.springframework.security.core.Authentication;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.csrf.*;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

import java.time.*;

import java.math.BigDecimal;

import java.util.concurrent.ConcurrentHashMap;



@RestController @RequestMapping("/api/v1") @Transactional

public class Api {

 @PersistenceContext EntityManager em;

 @org.springframework.beans.factory.annotation.Autowired SecurityEvents securityEvents;
 final RuleEngine engine;final SessionTokens tokens;final PasswordEncoder passwords;final JdbcTemplate jdbc;

 Api(RuleEngine e,SessionTokens t,PasswordEncoder p,JdbcTemplate j){engine=e;tokens=t;passwords=p;jdbc=j;}

 static String text(Map<String,Object>b,String k){Object v=b.get(k);return v==null?null:v.toString().trim();}

 static String required(Map<String,Object>b,String k,int max){String v=text(b,k);if(v==null||v.isBlank()||v.length()>max)throw new IllegalArgumentException(k+" is required (maximum "+max+")");return v;}

 static long id(Object x){try{if(x==null||!x.toString().matches("[1-9][0-9]*"))throw new Exception();return Long.parseLong(x.toString());}catch(Exception e){throw new IllegalArgumentException("Invalid decimal ID");}}

 long uid(Authentication a){return id(a.getName());}

 <T>T find(Class<T> c,long id){if(id<=0)throw new IllegalArgumentException("Invalid decimal ID");T v=em.find(c,id);if(v==null)throw new ApiFailure(404,"NOT_FOUND","Resource not found");return v;}

 <T>List<T> list(Class<T> c,String where,Map<String,Object> args){var q=em.createQuery("from "+c.getSimpleName()+" "+where,c);args.forEach(q::setParameter);return q.getResultList();}

 Object value(String s){return s==null?null:engine.json.convertValue(engine.parse(s),Object.class);}

 Map<String,Object> exactRuleRequest(String request){
  JsonNode body=engine.parse(request);
  if(body==null||!body.isObject())throw new IllegalArgumentException("Request body must be a JSON object");
  return engine.json.convertValue(body,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
 }

 String json(Object o){try{return engine.json.writeValueAsString(o);}catch(Exception e){throw new IllegalArgumentException("Invalid JSON");}}

 Map<String,Object> dto(Object o){var m=new LinkedHashMap<String,Object>();try{for(var f:o.getClass().getFields()){Object v=f.get(o);String k=f.getName();if(k.equals("passwordHash"))continue;if(v instanceof Long||v instanceof BigDecimal)v=v.toString();if(k.endsWith("Json")){k=k.substring(0,k.length()-4);v=value((String)v);}m.put(k,v);}}catch(Exception e){throw new IllegalStateException(e);}return m;}

 Map<String,Object> page(List<?> all,int page,int size){if(page<0||size<1||size>100)throw new IllegalArgumentException("Invalid pagination");int start=(int)Math.min((long)page*size,all.size());return Map.of("items",all.subList(start,Math.min(start+size,all.size())),"page",page,"size",size,"total",all.size());}

 void revision(Long actual,Object expected){if(expected==null||!actual.toString().equals(expected.toString()))throw new ApiFailure(409,"STALE_REVISION","Data changed. Reload before submitting.");}

 Profile profile(long user){var p=list(Profile.class,"where userId=:u",Map.of("u",user));if(p.isEmpty())throw new ApiFailure(404,"NOT_FOUND","Profile not found");return p.get(0);}

 Profile lockProfile(long user){Profile p=profile(user);em.refresh(p,LockModeType.PESSIMISTIC_WRITE);return p;}

 Scheme published(long id){Scheme s=find(Scheme.class,id);if(s.publishedVersionId==null || Boolean.TRUE.equals(s.synthetic)&&!development())throw new ApiFailure(404,"NOT_FOUND","Published scheme not found");return s;}

 void audit(long actor,String action,Object target){Audit a=new Audit();a.actorId=actor;a.action=action;a.targetId=target.toString();a.createdAt=Instant.now().toString();em.persist(a);}

 void rotate(HttpServletRequest q,HttpServletResponse r){var repo=CookieCsrfTokenRepository.withHttpOnlyFalse();repo.setCookiePath("/");repo.saveToken(repo.generateToken(q),q,r);}

 @GetMapping("/auth/csrf") Object csrf(CsrfToken t,Authentication a){return Map.of("csrfToken",t.getToken(),"authenticated",a!=null&&a.isAuthenticated());}

 @GetMapping("/auth/me") Object me(Authentication a){return dto(find(User.class,uid(a)));}

 String email(Map<String,Object>b){String e=required(b,"email",200).toLowerCase(Locale.ROOT);if(!e.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new IllegalArgumentException("Invalid email");return e;}

 String password(Map<String,Object>b){Object raw=b.get("password");String p=raw==null?"":raw.toString();if(p.length()<8||p.length()>128)throw new IllegalArgumentException("Password must be 8 to 128 characters ");return p;}

 @PostMapping("/auth/register") ResponseEntity<?> register(@RequestBody Map<String,Object>b,HttpServletRequest q,HttpServletResponse r){String e=email(b);String p=password(b);if(!list(User.class,"where email=:e",Map.of("e",e)).isEmpty())throw new ApiFailure(409,"CONFLICT","Email already registered");User u=new User();u.email=e;u.fullName=required(b,"fullName",100);u.passwordHash=passwords.encode(p);u.role="CITIZEN";u.status="ACTIVE";em.persist(u);Profile pr=new Profile();pr.userId=u.id;pr.revision=0L;em.persist(pr);audit(u.id,"CITIZEN_REGISTERED",u.id);tokens.cookie(q,r,tokens.issue(u));rotate(q,r);return ResponseEntity.status(201).body(dto(u));}

 record Window(long start,int count){}

 final ConcurrentHashMap<String,Window> limits=new ConcurrentHashMap<>();

 void limit(String key,int max){long now=System.currentTimeMillis();if(limits.size()>10000)limits.entrySet().removeIf(e->now-e.getValue().start()>900000);Window w=limits.compute(key,(k,v)->v==null||now-v.start()>900000?new Window(now,1):new Window(v.start(),v.count()+1));if(w.count()>max){securityEvents.rejected("LOGIN_RATE_LIMITED");throw new ApiFailure(429,"RATE_LIMITED","Too many attempts. Try again in 15 minutes.");}}

 @PostMapping("/auth/login") Object login(@RequestBody Map<String,Object>b,HttpServletRequest q,HttpServletResponse r){limit("ip:"+q.getRemoteAddr(),30);String e=email(b);limit("account:"+e,10);String p=password(b);var users=list(User.class,"where email=:e",Map.of("e",e));String hash=users.isEmpty()?"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy":users.get(0).passwordHash;boolean valid=passwords.matches(p,hash);if(!valid||users.isEmpty()||!"ACTIVE".equals(users.get(0).status)){securityEvents.rejected("LOGIN_REJECTED");throw new ApiFailure(401,"INVALID_CREDENTIALS","Email or password is incorrect");}User u=users.get(0);audit(u.id,"LOGIN_SUCCEEDED",u.id);tokens.cookie(q,r,tokens.issue(u));rotate(q,r);return dto(u);}

 @PostMapping("/auth/logout") ResponseEntity<?> logout(HttpServletRequest q,HttpServletResponse r,Authentication a){audit(uid(a),"SESSION_LOGOUT",uid(a));tokens.cookie(q,r,"");rotate(q,r);return ResponseEntity.noContent().build();}

 @GetMapping("/profile") Object getProfile(Authentication a){Profile p=profile(uid(a));return Map.of("profile",dto(p),"revision",p.revision.toString());}

 static final Set<String> STATES=Set.of("AN","AP","AR","AS","BR","CH","CG","DN","DL","GA","GJ","HR","HP","JK","JH","KA","KL","LA","LD","MP","MH","MN","ML","MZ","NL","OD","PY","PB","RJ","SK","TN","TS","TR","UP","UK","WB");

 @PutMapping("/profile") Object putProfile(Authentication a,@RequestBody Map<String,Object>b){Profile p=lockProfile(uid(a));revision(p.revision,b.get("expectedRevision"));String dob=text(b,"dateOfBirth");if(dob!=null&&!dob.isBlank()){if(!dob.matches("\\d{4}-\\d{2}-\\d{2}"))throw new IllegalArgumentException("DOB must be YYYY-MM-DD");LocalDate d=LocalDate.parse(dob);if(d.isAfter(today())||d.isBefore(today().minusYears(130)))throw new IllegalArgumentException("Invalid date of birth");}else dob=null;p.dateOfBirth=dob;p.stateCode=optional(b,"stateCode",2);if(p.stateCode!=null&&!STATES.contains(p.stateCode))throw new IllegalArgumentException("Invalid state code");String income=text(b,"annualIncome");p.annualIncome=income==null||income.isBlank()?null:new BigDecimal(income);if(p.annualIncome!=null&&(p.annualIncome.signum()<0||p.annualIncome.scale()>2||p.annualIncome.precision()>18))throw new IllegalArgumentException("Invalid annual income");p.educationLevel=optional(b,"educationLevel",100);p.occupation=optional(b,"occupation",100);p.revision++;return Map.of("profile",dto(p),"revision",p.revision.toString());}

 String optional(Map<String,Object>b,String k,int max){String v=text(b,k);if(v==null||v.isBlank())return null;if(v.length()>max)throw new IllegalArgumentException(k+" too long");return v;}

 boolean development(){return Arrays.asList(env.getActiveProfiles()).contains("dev")||Arrays.asList(env.getActiveProfiles()).contains("test");}

 static LocalDate today(){return LocalDate.now(ZoneId.of("Asia/Kolkata"));}

 @GetMapping("/categories") Object categories(){return list(Category.class,"order by name",Map.of()).stream().map(this::dto).toList();}

 @GetMapping("/schemes") Object schemes(@RequestParam(defaultValue="")String search,@RequestParam(required=false)String stateCode,@RequestParam(required=false)String categoryId,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(defaultValue="name")String sort){if(!Set.of("name","id").contains(sort)||search.length()>200)throw new IllegalArgumentException("Invalid sort/search");var all=list(Scheme.class,"where publishedVersionId is not null order by "+sort,Map.of()).stream().filter(s->(!Boolean.TRUE.equals(s.synthetic)||development())&&!DemoSchemes.legacyTemplate(s)&&s.name.toLowerCase().contains(search.toLowerCase())&&(stateCode==null||s.stateCode==null||stateCode.equals(s.stateCode))&&(categoryId==null||s.categoryId==id(categoryId))).map(this::dto).toList();return page(all,page,size);}

 @GetMapping("/schemes/{id}") Object scheme(@PathVariable long id){Scheme s=published(id);Map<String,Object> d=dto(s);d.put("rule",value(find(Rule.class,s.publishedVersionId).ruleJson));d.put("disclaimer","Preliminary screening only. Not government affiliation, entitlement, legal advice or an application submission.");return d;}

 List<Question> questions(long scheme){return em.createQuery("select q from Question q, Link l where l.schemeId=:s and l.questionId=q.id order by q.id",Question.class).setParameter("s",scheme).getResultList();}

 @GetMapping("/schemes/{id}/questions") Object questions(@PathVariable long id,Authentication a){published(id);var answers=list(Answer.class,"where userId=:u",Map.of("u",uid(a)));return questions(id).stream().map(q->{var d=dto(q);d.put("value",answers.stream().filter(x->x.questionId.equals(q.id)).findFirst().map(x->value(x.valueJson)).orElse(null));return d;}).toList();}

 @PutMapping("/schemes/{schemeId}/answers") Object answers(@PathVariable long schemeId,@RequestBody String request,Authentication a){Map<String,Object>b=exactRuleRequest(request);Scheme scheme=published(schemeId);Profile p=lockProfile(uid(a));revision(p.revision,b.get("expectedProfileRevision"));return saveAnswers(p,questions(schemeId),scheme,b,false);}

 Object saveAnswers(Profile p,List<Question> qs,Scheme scheme,Map<String,Object>b,boolean replaceAll){
Map<Long,Question> linked=new HashMap<>();qs.forEach(q->linked.put(q.id,q));JsonNode rows=engine.json.valueToTree(b.get("answers"));if(!rows.isArray()||rows.size()>100)throw new IllegalArgumentException("answers must be an array");Set<Long> seen=new HashSet<>();for(JsonNode row:rows){long qid=id(row.path("questionId").asText());Question q=linked.get(qid);if(q==null||!Boolean.TRUE.equals(q.active)||!seen.add(qid))throw new IllegalArgumentException("Unrelated, inactive or duplicate question");JsonNode v=row.get("value");if(v==null)throw new IllegalArgumentException("Answer value required");if(!v.isNull()){if(q.type.equals("MULTI_SELECT")){if(!v.isArray())throw new IllegalArgumentException("Array required");Set<String> unique=new HashSet<>();for(JsonNode x:v){engine.checkType(x,"TEXT");if(!unique.add(x.asText())||!engine.parse(q.optionsJson).toString().contains(json(x.asText())))throw new IllegalArgumentException("Invalid option");}}else{engine.checkType(v,q.type.equals("SELECT")?"TEXT":q.type);if(q.type.equals("SELECT")&&!engine.parse(q.optionsJson).toString().contains(json(v.asText())))throw new IllegalArgumentException("Invalid option");}v=DemoSchemes.validateAnswer(scheme,q,v,engine);if(v.toString().length()>4000)throw new IllegalArgumentException("Answer too long");}var existing=list(Answer.class,"where userId=:u and questionId=:q",Map.of("u",p.userId,"q",qid));if(v.isNull()){existing.forEach(em::remove);}else{Answer ans=existing.isEmpty()?new Answer():existing.get(0);ans.userId=p.userId;ans.questionId=qid;ans.valueJson=v.toString();if(ans.id==null)em.persist(ans);}}

 for(Answer ans:list(Answer.class,"where userId=:u",Map.of("u",p.userId))){if(linked.containsKey(ans.questionId)&&!seen.contains(ans.questionId)){long count=em.createQuery("select count(l) from Link l where questionId=:q",Long.class).setParameter("q",ans.questionId).getSingleResult();if(replaceAll||count==1)em.remove(ans);}}

 p.revision++;return Map.of("revision",p.revision.toString());}

 Map<String,String> types(long scheme){var map=new HashMap<String,String>();for(Question q:questions(scheme))if(Boolean.TRUE.equals(q.active))map.put("question:"+q.id,q.type.equals("SELECT")?"TEXT":q.type);return map;}

 Map<String,Object> facts(Profile p){Map<String,Object> facts=new HashMap<>();facts.put("completedAgeYears",p.dateOfBirth==null?null:RuleEngine.age(LocalDate.parse(p.dateOfBirth),today()));facts.put("annualIncome",p.annualIncome==null?null:p.annualIncome.toPlainString());facts.put("stateCode",p.stateCode);facts.put("educationLevel",p.educationLevel);facts.put("occupation",p.occupation);for(Answer ans:list(Answer.class,"where userId=:u",Map.of("u",p.userId)))facts.put("question:"+ans.questionId,value(ans.valueJson));return facts;}

 List<Scheme> matchingSchemes(){
  if(!development())throw new ApiFailure(404,"NOT_FOUND","Demo matching is unavailable");
  List<Scheme> selected=new ArrayList<>();
  for(JsonNode item:DemoSchemes.specification(engine).path("schemes")){
   var candidates=list(Scheme.class,"where name=:n order by id",Map.of("n",DemoSchemes.PREFIX+item.path("name").asText()));
   if(candidates.size()!=1)throw new ApiFailure(409,"MATCHING_UNAVAILABLE","The eight demo schemes must be uniquely published");
   Scheme scheme=candidates.get(0);em.refresh(scheme,LockModeType.PESSIMISTIC_READ);
   if(!Boolean.TRUE.equals(scheme.synthetic)||scheme.publishedVersionId==null||scheme.sourceMapping==null||!DemoSchemes.MARKER.equals(engine.parse(scheme.sourceMapping).path("demoSpecification").asText()))throw new ApiFailure(409,"MATCHING_UNAVAILABLE","A PDF demo scheme is unavailable");
   if(!"PUBLISHED".equals(find(Rule.class,scheme.publishedVersionId).status))throw new ApiFailure(409,"MATCHING_UNAVAILABLE","Published demo rule required");
   selected.add(scheme);
  }
  return selected;
 }
 List<Question> matchingQuestions(List<Scheme> schemes){
  var union=new TreeMap<Long,Question>();for(Scheme scheme:schemes)for(Question q:questions(scheme.id)){
   if(!Boolean.TRUE.equals(q.active))throw new ApiFailure(409,"MATCHING_UNAVAILABLE","Linked demo question is inactive");union.put(q.id,q);
  }
  if(union.size()!=14)throw new ApiFailure(409,"MATCHING_UNAVAILABLE","The fourteen shared demo questions are required");
  return new ArrayList<>(union.values());
 }
 Profile matchingProfile(Authentication a,boolean write){Profile p=profile(uid(a));em.refresh(p,write?LockModeType.PESSIMISTIC_WRITE:LockModeType.PESSIMISTIC_READ);return p;}
 @GetMapping("/matching/questions") Object matchingQuestionnaire(Authentication a){
  if(!development())throw new ApiFailure(404,"NOT_FOUND","Demo matching is unavailable");
  Profile p=matchingProfile(a,false);var schemes=matchingSchemes();var stored=list(Answer.class,"where userId=:u",Map.of("u",p.userId));
  var rows=matchingQuestions(schemes).stream().map(q->{var row=dto(q);row.put("value",stored.stream().filter(x->x.questionId.equals(q.id)).findFirst().map(x->value(x.valueJson)).orElse(null));return row;}).toList();
  return Map.of("revision",p.revision.toString(),"questions",rows,"disclaimer","FICTIONAL / DEMO ONLY. No government benefit, application or entitlement.");
 }
 @PutMapping("/matching/answers") Object saveMatchingAnswers(Authentication a,@RequestBody String request){
  if(!development())throw new ApiFailure(404,"NOT_FOUND","Demo matching is unavailable");
  Map<String,Object>b=exactRuleRequest(request);Profile p=matchingProfile(a,true);revision(p.revision,b.get("expectedProfileRevision"));var schemes=matchingSchemes();return saveAnswers(p,matchingQuestions(schemes),schemes.get(0),b,true);
 }
 @GetMapping("/matching/preview") Object previewMatches(Authentication a,@RequestParam String expectedProfileRevision){
  if(!development())throw new ApiFailure(404,"NOT_FOUND","Demo matching is unavailable");
  Profile p=matchingProfile(a,false);revision(p.revision,expectedProfileRevision);var schemes=matchingSchemes();var qs=matchingQuestions(schemes);var labels=new HashMap<String,String>();for(Question q:qs)labels.put("question:"+q.id,q.label);
  Map<String,Object> facts=facts(p);List<Map<String,Object>> rows=new ArrayList<>();
  for(Scheme scheme:schemes){Rule rule=find(Rule.class,scheme.publishedVersionId);var result=engine.evaluate(engine.parse(rule.ruleJson),facts,types(scheme.id));
   var explanations=result.explanations().stream().map(e->{var row=new LinkedHashMap<String,Object>(e);row.put("questionLabel",labels.getOrDefault(e.get("fieldKey").toString(),e.get("fieldKey").toString()));return row;}).toList();
   var missing=result.missing().stream().map(f->Map.of("fieldKey",f,"label",labels.getOrDefault(f,f))).toList();
   rows.add(Map.of("schemeId",scheme.id.toString(),"schemeName",scheme.name,"benefits",scheme.benefits,"synthetic",true,"ruleVersionId",rule.id.toString(),"classification",switch(result.truth()){case TRUE->"ELIGIBLE";case FALSE->"NOT_ELIGIBLE";case UNKNOWN->"NEEDS_INFORMATION";},"explanations",explanations,"missingQuestions",missing));
  }
  return Map.of("profileRevision",p.revision.toString(),"results",rows,"disclaimer","FICTIONAL / DEMO ONLY. Preview does not save eligibility history or confer entitlement.");
 }

 @PostMapping("/eligibility/evaluate") Object evaluate(@RequestBody Map<String,Object>b,Authentication a){Profile p=lockProfile(uid(a));revision(p.revision,b.get("expectedProfileRevision"));Scheme s=published(id(b.get("schemeId")));em.refresh(s,LockModeType.PESSIMISTIC_READ);Rule rule=find(Rule.class,s.publishedVersionId);Map<String,Object> facts=facts(p);var result=engine.evaluate(engine.parse(rule.ruleJson),facts,types(s.id));if(result.truth()==RuleEngine.Truth.UNKNOWN)throw new ApiFailure(422,"INPUT_REQUIRED","Complete the missing profile fields or questions",Map.of("missing",result.missing()));Evaluation e=new Evaluation();e.userId=p.userId;e.schemeId=s.id;e.ruleVersionId=rule.id;e.profileRevision=p.revision;e.evaluationDate=today().toString();e.verdict=result.truth()==RuleEngine.Truth.TRUE?"ELIGIBLE":"NOT_ELIGIBLE";e.evaluatedAt=Instant.now().toString();e.dateSensitive=rule.ruleJson.contains("completedAgeYears");var snapshot=new LinkedHashMap<String,Object>();snapshot.put("facts",facts);snapshot.put("rule",value(rule.ruleJson));snapshot.put("explanations",result.explanations());snapshot.put("schemeName",s.name);snapshot.put("synthetic",s.synthetic);e.snapshotJson=json(snapshot);em.persist(e);return evaluation(e);}

 Object evaluation(Evaluation e){var d=dto(e);d.put("evaluationId",e.id.toString());Profile p=profile(e.userId);Scheme s=find(Scheme.class,e.schemeId);List<String> reasons=new ArrayList<>();if(!p.revision.equals(e.profileRevision))reasons.add("PROFILE_CHANGED");if(!Objects.equals(s.publishedVersionId,e.ruleVersionId))reasons.add("RULE_CHANGED");if(Boolean.TRUE.equals(e.dateSensitive)&&p.dateOfBirth!=null&&engine.parse(e.snapshotJson).path("facts").path("completedAgeYears").asInt(-1)!=RuleEngine.age(LocalDate.parse(p.dateOfBirth),today()))reasons.add("AGE_CHANGED");d.put("stale",!reasons.isEmpty());d.put("staleReasons",reasons);d.put("explanations",value(e.snapshotJson) instanceof Map<?,?> snap ? snap.get("explanations") : List.of());return d;}

 @GetMapping("/eligibility/history") Object history(Authentication a,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return page(list(Evaluation.class,"where userId=:u order by id desc",Map.of("u",uid(a))).stream().map(this::evaluation).toList(),page,size);}

 @GetMapping("/eligibility/history/{id}") Object historyDetail(@PathVariable long id,Authentication a){Evaluation e=find(Evaluation.class,id);if(e.userId!=uid(a))throw new ApiFailure(404,"NOT_FOUND","Evaluation not found");return evaluation(e);}

 @GetMapping("/dashboard") Object dashboard(Authentication a){long u=uid(a);Profile p=profile(u);int complete=0;for(Object x:Arrays.asList(p.dateOfBirth,p.stateCode,p.annualIncome,p.educationLevel,p.occupation))if(x!=null)complete++;return Map.of("profileCompleteness",complete*20,"evaluationCount",jdbc.queryForObject("select count(*) from eligibility_evaluations where user_id=?",Long.class,u),"pendingSuggestions",jdbc.queryForObject("select count(*) from scheme_suggestions where user_id=? and status in ('PENDING','UNDER_REVIEW','NEEDS_INFO')",Long.class,u),"recentChecks",list(Evaluation.class,"where userId=:u order by id desc",Map.of("u",u)).stream().limit(5).map(this::evaluation).toList());}

 void url(String s){if(s==null)return;try{var u=java.net.URI.create(s);if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||s.length()>255)throw new Exception();}catch(Exception e){throw new IllegalArgumentException("Source must be a valid HTTPS URL");}}

 @PostMapping("/suggestions") ResponseEntity<?> suggest(@RequestBody Map<String,Object>b,Authentication a){Suggestion s=new Suggestion();s.userId=uid(a);s.type=required(b,"type",20);if(!Set.of("SCHEME","CRITERION").contains(s.type))throw new IllegalArgumentException("Invalid suggestion type");s.schemeName=s.type.equals("SCHEME")?required(b,"schemeName",200):optional(b,"schemeName",200);s.schemeId=b.get("schemeId")==null?null:id(b.get("schemeId"));if(s.type.equals("CRITERION")&&s.schemeId==null)throw new IllegalArgumentException("schemeId required");if(s.schemeId!=null)published(s.schemeId);s.description=required(b,"description",4000);s.proposedCriterion=optional(b,"proposedCriterion",4000);s.sourceUrl=optional(b,"sourceUrl",255);url(s.sourceUrl);s.status="PENDING";s.revision=0L;s.createdAt=Instant.now().toString();em.persist(s);return ResponseEntity.status(201).body(dto(s));}

 @GetMapping("/suggestions/mine") Object mine(Authentication a,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return page(list(Suggestion.class,"where userId=:u order by id desc",Map.of("u",uid(a))).stream().map(this::dto).toList(),page,size);}

 @GetMapping("/admin/schemes") Object adminSchemes(){return list(Scheme.class,"order by id desc",Map.of()).stream().map(this::dto).toList();}

 @GetMapping("/admin/schemes/{id}/questions") Object adminLinked(@PathVariable long id){find(Scheme.class,id);return questions(id).stream().map(this::dto).toList();}

 @GetMapping("/admin/questions") Object adminQuestions(){return list(Question.class,"order by id",Map.of()).stream().map(this::dto).toList();}

 @GetMapping("/admin/overview") Object overview(){return Map.of("schemes",em.createQuery("select count(s) from Scheme s",Long.class).getSingleResult(),"pendingSuggestions",em.createQuery("select count(s) from Suggestion s where status='PENDING'",Long.class).getSingleResult());}

 @PostMapping("/admin/categories") Object category(@RequestBody Map<String,Object>b,Authentication a){Category c=new Category();c.name=required(b,"name",100);em.persist(c);audit(uid(a),"CATEGORY_CREATED",c.id);return dto(c);}

 @PutMapping("/admin/categories/{id}") Object categoryUpdate(@PathVariable long id,@RequestBody Map<String,Object>b,Authentication a){Category c=find(Category.class,id);c.name=required(b,"name",100);audit(uid(a),"CATEGORY_UPDATED",id);return dto(c);}

 @PostMapping("/admin/schemes") ResponseEntity<?> createScheme(@RequestBody Map<String,Object>b,Authentication a){Scheme s=new Scheme();s.revision=0L;metadata(s,b);em.persist(s);linkQuestions(s,b);audit(uid(a),"SCHEME_CREATED",s.id);return ResponseEntity.status(201).body(dto(s));}

 @PutMapping("/admin/schemes/{id}") Object editScheme(@PathVariable long id,@RequestBody Map<String,Object>b,Authentication a){Scheme s=find(Scheme.class,id);em.refresh(s,LockModeType.PESSIMISTIC_WRITE);revision(s.revision,b.get("expectedRevision"));if(s.publishedVersionId!=null)throw new ApiFailure(409,"IMMUTABLE_METADATA","Published metadata is immutable. Create a new draft scheme.");metadata(s,b);linkQuestions(s,b);s.revision++;audit(uid(a),"SCHEME_UPDATED",id);return dto(s);}

 void metadata(Scheme s,Map<String,Object>b){s.name=required(b,"name",200);s.categoryId=id(b.get("categoryId"));find(Category.class,s.categoryId);s.stateCode=optional(b,"stateCode",2);if(s.stateCode!=null&&!STATES.contains(s.stateCode))throw new IllegalArgumentException("Invalid state");s.description=required(b,"description",10000);s.benefits=required(b,"benefits",10000);s.guidance=required(b,"guidance",10000);s.synthetic=Boolean.TRUE.equals(b.get("synthetic"));s.sourceUrl=optional(b,"sourceUrl",255);url(s.sourceUrl);s.verifiedDate=optional(b,"verifiedDate",10);if(s.verifiedDate!=null&&LocalDate.parse(s.verifiedDate).isAfter(today()))throw new IllegalArgumentException("Verification date in future");s.reviewerApproved=Boolean.TRUE.equals(b.get("reviewerApproved"));s.sourceMapping=json(b.getOrDefault("sourceMapping",Map.of()));s.exceptions=optional(b,"exceptions",10000);}

 void linkQuestions(Scheme s,Map<String,Object>b){Object raw=b.get("questionIds");if(raw==null)return;if(!(raw instanceof List<?> ids)||ids.size()>100)throw new IllegalArgumentException("questionIds must be array");list(Link.class,"where schemeId=:s",Map.of("s",s.id)).forEach(em::remove);em.flush();Set<Long> seen=new HashSet<>();for(Object x:ids){long id=id(x);if(!seen.add(id))throw new IllegalArgumentException("Duplicate question");find(Question.class,id);Link l=new Link();l.schemeId=s.id;l.questionId=id;em.persist(l);}}

 @PostMapping("/admin/questions") Object createQuestion(@RequestBody Map<String,Object>b,Authentication a){Question q=new Question();q.revision=0L;questionFields(q,b);em.persist(q);audit(uid(a),"QUESTION_CREATED",q.id);return dto(q);}

 @PutMapping("/admin/questions/{id}") Object editQuestion(@PathVariable long id,@RequestBody Map<String,Object>b,Authentication a){Question q=find(Question.class,id);em.refresh(q,LockModeType.PESSIMISTIC_WRITE);revision(q.revision,b.get("expectedRevision"));Long refs=em.createQuery("select count(l) from Link l, Scheme s where l.questionId=:q and l.schemeId=s.id and s.publishedVersionId is not null",Long.class).setParameter("q",id).getSingleResult();if(refs>0)throw new ApiFailure(409,"IMMUTABLE_QUESTION","Question used by a published scheme cannot change");questionFields(q,b);q.revision++;audit(uid(a),"QUESTION_UPDATED",id);return dto(q);}

 void questionFields(Question q,Map<String,Object>b){q.label=required(b,"label",255);q.type=required(b,"type",20);if(!Set.of("TEXT","NUMBER","DATE","BOOLEAN","SELECT","MULTI_SELECT").contains(q.type))throw new IllegalArgumentException("Invalid question type");q.active=!Boolean.FALSE.equals(b.get("active"));q.optionsJson=json(b.getOrDefault("options",List.of()));JsonNode opts=engine.parse(q.optionsJson);if(!opts.isArray())throw new IllegalArgumentException("options must be array");Set<String> distinct=new HashSet<>();for(JsonNode o:opts){if(!o.isTextual()||o.asText().isBlank()||o.asText().length()>100||!distinct.add(o.asText()))throw new IllegalArgumentException("Invalid options");}if(Set.of("SELECT","MULTI_SELECT").contains(q.type)&&opts.isEmpty())throw new IllegalArgumentException("Options required");}

 @GetMapping("/admin/schemes/{id}/rules") Object rules(@PathVariable long id){find(Scheme.class,id);return list(Rule.class,"where schemeId=:s order by id desc",Map.of("s",id)).stream().map(this::dto).toList();}

 @PostMapping("/admin/schemes/{id}/rules") Object createRule(@PathVariable long id,@RequestBody String request,Authentication a){return createRuleDraft(id,exactRuleRequest(request),a);}

 Object createRuleDraft(long id,Map<String,Object>b,Authentication a){find(Scheme.class,id);Rule r=new Rule();r.schemeId=id;r.status="DRAFT";r.revision=0L;r.ruleJson=engine.canonicalRule(b.get("rule"),types(id));r.createdAt=Instant.now().toString();em.persist(r);audit(uid(a),"RULE_DRAFT_CREATED",r.id);return dto(r);}

 @PutMapping("/admin/schemes/{id}/rules/{versionId}") Object editRule(@PathVariable long id,@PathVariable long versionId,@RequestBody String request,Authentication a){Map<String,Object>b=exactRuleRequest(request);Rule r=find(Rule.class,versionId);em.refresh(r,LockModeType.PESSIMISTIC_WRITE);if(r.schemeId!=id||!r.status.equals("DRAFT"))throw new ApiFailure(409,"IMMUTABLE_RULE","Only draft rules can be edited");revision(r.revision,b.get("expectedDraftRevision"));String rule=engine.canonicalRule(b.get("rule"),types(id));r.ruleJson=rule;r.revision++;audit(uid(a),"RULE_DRAFT_UPDATED",versionId);return dto(r);}

 @PostMapping("/admin/schemes/{id}/publish") Object publish(@PathVariable long id,@RequestBody Map<String,Object>b,Authentication a){Scheme s=find(Scheme.class,id);em.refresh(s,LockModeType.PESSIMISTIC_WRITE);Rule r=find(Rule.class,id(b.get("draftVersionId")));em.refresh(r,LockModeType.PESSIMISTIC_WRITE);if(r.schemeId!=id||!"DRAFT".equals(r.status))throw new ApiFailure(409,"IMMUTABLE_RULE","Select an unpublished draft");revision(r.revision,b.get("expectedDraftRevision"));for(Question q:questions(id)){
  em.refresh(q,LockModeType.PESSIMISTIC_READ);
  if(!Boolean.TRUE.equals(q.active))throw new ApiFailure(422,"VALIDATION_ERROR",
      "Linked question " + q.id + " (" + q.label + ") is inactive. Remove its draft link or reactivate the question before publishing.",
      Map.of("questionId",q.id.toString()));
 }engine.validate(engine.parse(r.ruleJson),types(id));if(Boolean.TRUE.equals(s.synthetic)){if(!Arrays.asList(env.getActiveProfiles()).contains("dev")&&!Arrays.asList(env.getActiveProfiles()).contains("test"))throw new ApiFailure(422,"SYNTHETIC_DISABLED","Synthetic publication is restricted to development/test");}else{if(s.sourceUrl==null||s.verifiedDate==null||!Boolean.TRUE.equals(s.reviewerApproved)||s.exceptions==null)throw new ApiFailure(422,"SOURCE_REQUIRED","Official source, verification date, exceptions and reviewer approval required");String host=java.net.URI.create(s.sourceUrl).getHost();if(!(host.endsWith(".gov.in")||host.endsWith(".nic.in")))throw new ApiFailure(422,"SOURCE_REQUIRED","Official government domain required");JsonNode mapping=engine.parse(s.sourceMapping);for(var leaf:engine.evaluate(engine.parse(r.ruleJson),Map.of(),types(id)).explanations())if(!mapping.path(leaf.get("nodeId").toString()).isTextual()||mapping.path(leaf.get("nodeId").toString()).asText().isBlank())throw new ApiFailure(422,"SOURCE_REQUIRED","Map each criterion to its source section");}r.status="PUBLISHED";s.publishedVersionId=r.id;s.revision++;audit(uid(a),"RULE_PUBLISHED",r.id);return dto(s);}

 @org.springframework.beans.factory.annotation.Autowired org.springframework.core.env.Environment env;

 @GetMapping("/admin/audit-logs") Object audits(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return page(list(Audit.class,"order by id desc",Map.of()).stream().map(this::dto).toList(),page,size);}

 @GetMapping("/admin/suggestions") Object suggestions(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return page(list(Suggestion.class,"order by id desc",Map.of()).stream().map(this::dto).toList(),page,size);}

 @GetMapping("/admin/suggestions/{id}") Object suggestion(@PathVariable long id){return dto(find(Suggestion.class,id));}

 @PostMapping("/admin/suggestions/{id}/review") Object review(@PathVariable long id,@RequestBody Map<String,Object>b,Authentication a){Suggestion s=find(Suggestion.class,id);em.refresh(s,LockModeType.PESSIMISTIC_WRITE);revision(s.revision,b.get("expectedRevision"));String status=required(b,"status",30);if(!Set.of("UNDER_REVIEW","NEEDS_INFO","ACCEPTED","REJECTED").contains(status))throw new IllegalArgumentException("Invalid review status");s.status=status;s.reviewNotes=required(b,"reviewNotes",4000);s.revision++;audit(uid(a),"SUGGESTION_"+status,id);return dto(s);}

 @PostMapping("/admin/suggestions/{id}/convert") Object convert(@PathVariable long id,@RequestBody String request,Authentication a){Map<String,Object>b=exactRuleRequest(request);Suggestion suggestion=find(Suggestion.class,id);if(!"ACCEPTED".equals(suggestion.status))throw new ApiFailure(409,"NOT_ACCEPTED","Accept suggestion before creating a draft");if(suggestion.type.equals("CRITERION")){Map<String,Object> data=new HashMap<>();data.put("rule",b.get("rule"));return createRuleDraft(suggestion.schemeId,data,a);}return createScheme(b,a).getBody();}

}

