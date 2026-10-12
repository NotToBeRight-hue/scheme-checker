package in.project.schemes;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

/** Development-only PDF fixtures; existing API validation/publication is reused. */
@Component
class DemoSchemes {
 static final String MARKER="eight-demo-pdf-v1";
 static final String PREFIX="FICTIONAL / DEMO ONLY: ";
 static final Set<String> LEGACY=Set.of("Learning Support","Training Access","Senior Wellbeing","Rural Enterprise","Student Equipment","Employment Readiness","Household Support","Skills Transition");
 @PersistenceContext EntityManager em;
 final Api api; final RuleEngine engine;
 DemoSchemes(Api api,RuleEngine engine){this.api=api;this.engine=engine;}
 JsonNode specification(){return specification(engine);}
 static JsonNode specification(RuleEngine engine){try(var in=DemoSchemes.class.getResourceAsStream("/demo/eight-schemes.json")){if(in==null)throw new IllegalStateException("Demo specification missing");return engine.json.readTree(in);}catch(Exception e){throw new IllegalStateException("Cannot read demo specification",e);}}
 static boolean legacyTemplate(Scheme s){return Boolean.TRUE.equals(s.synthetic)&&LEGACY.stream().anyMatch(n->s.name.equals("Synthetic Demo: "+n))&&"Fictional development scheme used to demonstrate preliminary eligibility screening. It does not represent a real government programme.".equals(s.description)&&"Demonstration benefit only. No money, service or entitlement is offered.".equals(s.benefits)&&"Use the profile and eligibility questions to test the application. No application can be submitted for this fictional scheme.".equals(s.guidance)&&s.sourceUrl==null;}
 @Transactional void seed(){
  if(!api.development())return;
  var admins=api.list(User.class,"where role='ADMIN' and status='ACTIVE' order by id",Map.of());
  if(admins.isEmpty())throw new IllegalStateException("Provision a local administrator before installing demo fixtures");
  em.refresh(admins.get(0),LockModeType.PESSIMISTIC_WRITE);
  var actor=new UsernamePasswordAuthenticationToken(admins.get(0).id.toString(),null,List.of());
  JsonNode spec=specification();
  for(JsonNode item:spec.path("schemes")){
   String name=PREFIX+item.path("name").asText();
   var found=api.list(Scheme.class,"where name=:n",Map.of("n",name));
   if(!found.isEmpty()){
    if(found.size()!=1||!Boolean.TRUE.equals(found.get(0).synthetic)||found.get(0).publishedVersionId==null||!engine.parse(found.get(0).sourceMapping).path("demoSpecification").asText().equals(MARKER))throw new IllegalStateException("Conflicting/incomplete demo fixture: "+name);
    continue;
   }
   String category=item.path("category").asText();
   var categories=api.list(Category.class,"where name=:n",Map.of("n",category));
   if(categories.isEmpty())api.category(Map.of("name",category),actor);
   String categoryId=api.list(Category.class,"where name=:n order by id",Map.of("n",category)).get(0).id.toString();
   List<String> questionIds=new ArrayList<>(); Map<String,String> ids=new HashMap<>();
   for(JsonNode key:item.path("questions")){
    String k=key.asText();JsonNode definition=spec.path("questions").path(k);
    var qs=api.list(Question.class,"where label=:l",Map.of("l",definition.path("label").asText()));
    if(qs.isEmpty()){api.createQuestion(engine.json.convertValue(definition,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){}),actor);qs=api.list(Question.class,"where label=:l",Map.of("l",definition.path("label").asText()));}
    if(qs.size()!=1||!Boolean.TRUE.equals(qs.get(0).active)||!qs.get(0).type.equals(definition.path("type").asText())||!engine.parse(qs.get(0).optionsJson).equals(definition.path("options")))throw new IllegalStateException("Conflicting demo question: "+k);
    ids.put(k,qs.get(0).id.toString());questionIds.add(qs.get(0).id.toString());
   }
   List<Map<String,Object>> leaves=new ArrayList<>();Map<String,Object> mapping=new LinkedHashMap<>();mapping.put("demoSpecification",MARKER);
   for(JsonNode c:item.path("conditions")){
    String key=c.path("question").asText();
    leaves.add(Map.of("type","CONDITION","nodeId",key,"field","question:"+ids.get(key),"operator",c.path("operator").asText(),"value",engine.json.convertValue(c.path("value"),Object.class)));
    mapping.put(key,"Eight_Demo_Schemes_Full_Specification.pdf, page "+item.path("page").asInt()+", eligibility criterion "+key+"; FICTIONAL / DEMO ONLY");
   }
   var metadata=new LinkedHashMap<String,Object>();metadata.put("name",name);metadata.put("categoryId",categoryId);metadata.put("description",item.path("description").asText()+" FICTIONAL / DEMO ONLY. Not a government programme or available financial assistance.");metadata.put("benefits",item.path("benefit").asText()+" — invented demonstration amount only. No payout or entitlement.");metadata.put("guidance","FICTIONAL / DEMO ONLY. Required documents (display-only): "+String.join("; ",engine.json.convertValue(item.path("documents"),new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){}))+". Do not submit documents, Aadhaar numbers or bank details. No application or official verification occurs.");metadata.put("synthetic",true);metadata.put("reviewerApproved",false);metadata.put("sourceMapping",mapping);metadata.put("exceptions","Every listed condition is required (AND). Missing decisive information yields INPUT_REQUIRED. Fictional demonstration only.");metadata.put("questionIds",questionIds);
   String sid=((Map<?,?>)api.createScheme(metadata,actor).getBody()).get("id").toString();
   var draft=(Map<?,?>)api.createRuleDraft(Long.parseLong(sid),Map.of("rule",Map.of("schemaVersion",1,"type","GROUP","operator","AND","children",leaves)),actor);
   api.publish(Long.parseLong(sid),Map.of("draftVersionId",draft.get("id"),"expectedDraftRevision",draft.get("revision")),actor);
  }
 }
 static JsonNode validateAnswer(Scheme scheme,Question question,JsonNode value,RuleEngine engine){
  if(!Boolean.TRUE.equals(scheme.synthetic)||scheme.sourceMapping==null||!engine.parse(scheme.sourceMapping).path("demoSpecification").asText().equals(MARKER)||value.isNull())return value;
  JsonNode spec=specification(engine);
  for(JsonNode d:spec.path("questions"))if(question.label.equals(d.path("label").asText())&&d.path("type").asText().equals("NUMBER")){
   BigDecimal n=new BigDecimal(value.asText());
   if(d.path("integer").asBoolean()&&n.stripTrailingZeros().scale()>0)throw new IllegalArgumentException(question.label+" must be whole integer years");
   if(d.has("min")&&n.compareTo(new BigDecimal(d.path("min").asText()))<0||d.has("max")&&n.compareTo(new BigDecimal(d.path("max").asText()))>0)throw new IllegalArgumentException(question.label+" is outside the permitted input range");
  }
  if(question.type.equals("NUMBER")&&value.isNumber()){
   try{return engine.exactDecimal(value);}catch(IllegalArgumentException e){throw new IllegalArgumentException("Numeric answer exceeds the supported exact decimal length");}
  }
  return value;
 }
}
