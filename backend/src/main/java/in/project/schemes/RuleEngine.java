package in.project.schemes;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class RuleEngine {
 public enum Truth { TRUE,FALSE,UNKNOWN }
 public record Result(Truth truth,List<Map<String,Object>> explanations,List<String> missing) {}
 public final ObjectMapper json=new ObjectMapper()
     .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
     .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
 static final Set<String> OPS=Set.of("EQ","NE","GT","GTE","LT","LTE","IN","NOT_IN","BETWEEN");
 static final Set<String> NUM=Set.of("completedAgeYears","annualIncome");
 public JsonNode parse(String rule) {try{return json.readTree(rule);}catch(Exception e){throw new IllegalArgumentException("Invalid rule JSON");}}
 public String canonicalRule(Object rule,Map<String,String> questions){
  JsonNode tree=json.valueToTree(rule);
  validate(tree,questions);
  canonicalNumbers(tree,questions);
  try{return json.writeValueAsString(tree);}catch(Exception e){throw new IllegalArgumentException("Invalid rule JSON");}
 }
 void canonicalNumbers(JsonNode node,Map<String,String> questions){
  if(node.path("type").asText().equals("GROUP")){
   for(JsonNode child:node.path("children"))canonicalNumbers(child,questions);
   return;
  }
  String field=node.path("field").asText();
  if(!NUM.contains(field)&&!"NUMBER".equals(questions.get(field)))return;
  JsonNode value=node.get("value");
  if(value.isArray()){
   ArrayNode values=(ArrayNode)value;
   for(int i=0;i<values.size();i++)if(values.get(i).isNumber())values.set(i,exactDecimal(values.get(i)));
  }else if(value.isNumber())((ObjectNode)node).set("value",exactDecimal(value));
 }
 TextNode exactDecimal(JsonNode number){
  final BigDecimal decimal;
  try{decimal=new BigDecimal(number.asText());}catch(NumberFormatException e){throw new IllegalArgumentException("Numeric rule values must be finite decimals");}
  long scale=decimal.scale();
  long length=(decimal.signum()<0?1:0)+Math.max(1L,decimal.precision()-scale)+(scale>0?1+scale:0);
  if(length>json.getFactory().streamReadConstraints().getMaxNumberLength())
   throw new IllegalArgumentException("Numeric rule value exceeds the supported JSON number length. Use a plain decimal string.");
  return TextNode.valueOf(decimal.toPlainString());
 }
 public void validate(JsonNode root,Map<String,String> questions){if(root.path("schemaVersion").asInt()!=1||!root.path("type").asText().equals("GROUP"))throw new IllegalArgumentException("Root must be schemaVersion 1 GROUP");validateNode(root,0,new HashSet<>(),questions);}
 void validateNode(JsonNode n,int depth,Set<String> ids,Map<String,String> qs){
  if(n.path("type").asText().equals("GROUP")){if(++depth>3||!Set.of("AND","OR").contains(n.path("operator").asText())||!n.path("children").isArray()||n.path("children").isEmpty())throw new IllegalArgumentException("Invalid group or depth");for(JsonNode c:n.path("children"))validateNode(c,depth,ids,qs);return;}
  String id=n.path("nodeId").asText(),f=n.path("field").asText(),op=n.path("operator").asText();
  if(!n.path("type").asText().equals("CONDITION")||id.isBlank()||id.length()>100||!ids.add(id)||!OPS.contains(op))throw new IllegalArgumentException("Invalid leaf, duplicate nodeId or operator");
  String t=NUM.contains(f)?"NUMBER":Set.of("stateCode","educationLevel","occupation").contains(f)?"TEXT":qs.get(f);
  if(t==null||t.equals("MULTI_SELECT"))throw new IllegalArgumentException("Unknown, inactive, unlinked or multi-select field");
  if(!Set.of("NUMBER","DATE").contains(t)&&Set.of("GT","GTE","LT","LTE","BETWEEN").contains(op))throw new IllegalArgumentException("Operator not supported for field type");
  JsonNode v=n.get("value");if(v==null||v.isNull())throw new IllegalArgumentException("Value required");
  if(Set.of("IN","NOT_IN","BETWEEN").contains(op)){if(!v.isArray()||v.isEmpty()||(op.equals("BETWEEN")&&v.size()!=2))throw new IllegalArgumentException("Invalid range/list");for(JsonNode x:v)checkType(x,t);if(op.equals("BETWEEN")&&compare(v.get(0),v.get(1),t)>0)throw new IllegalArgumentException("Reversed range");}else checkType(v,t);
 }
 void checkType(JsonNode v,String t){boolean ok=switch(t){case "NUMBER"->v.isNumber()||v.isTextual()&&v.asText().matches("-?\\d+(\\.\\d+)?");case "BOOLEAN"->v.isBoolean();case "DATE"->{try{LocalDate.parse(v.asText());yield v.isTextual();}catch(Exception e){yield false;}}default->v.isTextual();};if(!ok)throw new IllegalArgumentException("Type mismatch: "+t);}
 int compare(JsonNode a,JsonNode b,String t){return t.equals("NUMBER")?new BigDecimal(a.asText()).compareTo(new BigDecimal(b.asText())):a.asText().compareTo(b.asText());}
 public Result evaluate(JsonNode root,Map<String,Object> facts,Map<String,String> qs){validate(root,qs);var ex=new ArrayList<Map<String,Object>>();var missing=new LinkedHashSet<String>();Truth truth=walk(root,facts,qs,ex,missing);return new Result(truth,ex,new ArrayList<>(missing));}
 Truth walk(JsonNode n,Map<String,Object> facts,Map<String,String> qs,List<Map<String,Object>> ex,Set<String> missing){
  if(n.path("type").asText().equals("GROUP")){boolean and=n.path("operator").asText().equals("AND"),unknown=false,definite=false;for(JsonNode c:n.path("children")){Truth t=walk(c,facts,qs,ex,missing);unknown|=t==Truth.UNKNOWN;definite|=t==(and?Truth.FALSE:Truth.TRUE);}return definite?(and?Truth.FALSE:Truth.TRUE):unknown?Truth.UNKNOWN:and?Truth.TRUE:Truth.FALSE;}
  String f=n.path("field").asText(),op=n.path("operator").asText(),t=NUM.contains(f)?"NUMBER":qs.getOrDefault(f,"TEXT");Object a=facts.get(f);JsonNode v=n.get("value");Truth r;
  if(a==null){missing.add(f);r=Truth.UNKNOWN;}else{JsonNode actual=json.valueToTree(a);boolean b;if(op.equals("IN")||op.equals("NOT_IN")){b=false;for(JsonNode x:v)b|=compare(actual,x,t)==0;if(op.equals("NOT_IN"))b=!b;}else if(op.equals("BETWEEN"))b=compare(actual,v.get(0),t)>=0&&compare(actual,v.get(1),t)<=0;else{int c=compare(actual,v,t);b=switch(op){case "EQ"->c==0;case "NE"->c!=0;case "GT"->c>0;case "GTE"->c>=0;case "LT"->c<0;case "LTE"->c<=0;default->throw new IllegalArgumentException("operator");};}r=b?Truth.TRUE:Truth.FALSE;}
  Map<String,Object> row=new LinkedHashMap<>();row.put("nodeId",n.path("nodeId").asText());row.put("fieldKey",f);row.put("operator",op);row.put("expectedValue",json.convertValue(v,Object.class));row.put("actualValue",a);row.put("result",r.name());ex.add(row);return r;
 }
 public static int age(LocalDate dob,LocalDate date){LocalDate anniversary=dob.withYear(date.getYear());if(dob.getMonthValue()==2&&dob.getDayOfMonth()==29&&!date.isLeapYear())anniversary=LocalDate.of(date.getYear(),3,1);return date.getYear()-dob.getYear()-(date.isBefore(anniversary)?1:0);}
}
