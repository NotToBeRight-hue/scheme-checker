package in.project.schemes;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;
class RuleEngineTest {
 RuleEngine e=new RuleEngine();
 JsonNode leaf(String id,String field,String op,Object value){return e.json.valueToTree(Map.of("type","CONDITION","nodeId",id,"field",field,"operator",op,"value",value));}
 JsonNode group(String op,JsonNode...children){return e.json.valueToTree(Map.of("schemaVersion",1,"type","GROUP","operator",op,"children",children));}
 RuleEngine.Result run(String op,Object expected,Object actual){return e.evaluate(group("AND",leaf("a","annualIncome",op,expected)),actual==null?Map.of():Map.of("annualIncome",actual),Map.of());}
 void truth(RuleEngine.Truth t,RuleEngine.Result r){assertEquals(t,r.truth());}
 @Test void eqEqualTrue(){truth(RuleEngine.Truth.TRUE,run("EQ",10,10));}
 @Test void neUnequalTrue(){truth(RuleEngine.Truth.TRUE,run("NE",10,11));}
 @Test void gtEqualFalseGreaterTrue(){truth(RuleEngine.Truth.FALSE,run("GT",10,10));truth(RuleEngine.Truth.TRUE,run("GT",10,11));}
 @Test void gteEqualTrue(){truth(RuleEngine.Truth.TRUE,run("GTE",10,10));}
 @Test void ltEqualFalseLowerTrue(){truth(RuleEngine.Truth.FALSE,run("LT",10,10));truth(RuleEngine.Truth.TRUE,run("LT",10,9));}
 @Test void lteEqualTrue(){truth(RuleEngine.Truth.TRUE,run("LTE",10,10));}
 @Test void inMemberTrue(){truth(RuleEngine.Truth.TRUE,run("IN",List.of(10,20),10));}
 @Test void notInAbsentTrue(){truth(RuleEngine.Truth.TRUE,run("NOT_IN",List.of(10,20),30));}
 @Test void betweenInsideTrue(){truth(RuleEngine.Truth.TRUE,run("BETWEEN",List.of(10,20),15));}
 @Test void betweenInclusiveEndpoints(){truth(RuleEngine.Truth.TRUE,run("BETWEEN",List.of(10,20),10));truth(RuleEngine.Truth.TRUE,run("BETWEEN",List.of(10,20),20));}
 JsonNode yes(){return leaf("y","annualIncome","GTE",10);}JsonNode no(){return leaf("n","annualIncome","LT",10);}JsonNode unknown(){return leaf("u","stateCode","EQ","MH");}
 RuleEngine.Result eval(JsonNode n){return e.evaluate(n,Map.of("annualIncome",10),Map.of());}
 @Test void andAllTrue(){truth(RuleEngine.Truth.TRUE,eval(group("AND",yes(),leaf("z","annualIncome","EQ",10))));}
 @Test void andFalseDominates(){truth(RuleEngine.Truth.FALSE,eval(group("AND",unknown(),no())));}
 @Test void orTrueDominates(){truth(RuleEngine.Truth.TRUE,eval(group("OR",unknown(),yes())));}
 @Test void orAllFalse(){truth(RuleEngine.Truth.FALSE,eval(group("OR",no(),leaf("z","annualIncome","GT",11))));}
 @Test void andUnknownWithoutFalse(){truth(RuleEngine.Truth.UNKNOWN,eval(group("AND",yes(),unknown())));}
 @Test void orUnknownWithoutTrue(){truth(RuleEngine.Truth.UNKNOWN,eval(group("OR",no(),unknown())));}
 @Test void nestedTruthTable(){truth(RuleEngine.Truth.TRUE,eval(group("AND",group("OR",no(),yes()),leaf("z","annualIncome","EQ",10))));}
 @Test void threeGroupLevelsAccepted(){assertDoesNotThrow(()->eval(group("AND",group("OR",group("AND",yes())))));}
 @Test void fourGroupLevelsRejected(){assertThrows(IllegalArgumentException.class,()->eval(group("AND",group("OR",group("AND",group("AND",yes()))))));}
 @Test void unsupportedOperatorRejected(){assertThrows(IllegalArgumentException.class,()->run("APPROX",10,10));}
 @Test void typeMismatchRejected(){assertThrows(IllegalArgumentException.class,()->run("EQ",true,10));}
 @Test void exactBigDecimalBoundary(){truth(RuleEngine.Truth.TRUE,run("LTE","100000.10",new BigDecimal("100000.10")));truth(RuleEngine.Truth.FALSE,run("LTE","100000.10",new BigDecimal("100000.11")));}
 @Test void criticalMissingInputUnknown(){var r=run("EQ",10,null);truth(RuleEngine.Truth.UNKNOWN,r);assertEquals(List.of("annualIncome"),r.missing());}
 @Test void allLeavesExplainedAfterDefinitiveOutcome(){var r=eval(group("OR",yes(),unknown(),no()));assertEquals(3,r.explanations().size());truth(RuleEngine.Truth.TRUE,r);}
 @Test void leapDayAgeUsesMarchFirst(){assertEquals(24,RuleEngine.age(LocalDate.of(2000,2,29),LocalDate.of(2025,2,28)));assertEquals(25,RuleEngine.age(LocalDate.of(2000,2,29),LocalDate.of(2025,3,1)));}
 @Test void duplicateNodesRejected(){assertThrows(IllegalArgumentException.class,()->eval(group("AND",yes(),yes())));}
 @Test void emptyGroupsRejected(){assertThrows(IllegalArgumentException.class,()->eval(group("AND")));}
 @Test void multiSelectConditionsRejected(){assertThrows(IllegalArgumentException.class,()->e.evaluate(group("AND",leaf("q","question:1","EQ","a")),Map.of(),Map.of("question:1","MULTI_SELECT")));}
 @Test void unlinkedQuestionRejected(){assertThrows(IllegalArgumentException.class,()->eval(group("AND",leaf("q","question:1","EQ",true))));}
 @Test void frozenFixtureDate(){assertEquals(26,RuleEngine.age(LocalDate.of(2000,10,12),LocalDate.of(2026,10,12)));}

 JsonNode numericRule(String operator,String token){return e.parse("{\"schemaVersion\":1,\"type\":\"GROUP\",\"operator\":\"AND\",\"children\":[{\"type\":\"CONDITION\",\"nodeId\":\"exact\",\"field\":\"annualIncome\",\"operator\":\""+operator+"\",\"value\":"+token+"}]}");}
 @Test void precisionLargeDecimalIsParsedComparedAndCanonicalizedExactly(){
  JsonNode raw=numericRule("EQ","1000000000000000.11");
  assertEquals(new BigDecimal("1000000000000000.11"),raw.path("children").get(0).path("value").decimalValue());
  JsonNode stored=e.parse(e.canonicalRule(raw,Map.of()));
  assertEquals("1000000000000000.11",stored.path("children").get(0).path("value").textValue());
  var result=e.evaluate(stored,Map.of("annualIncome","1000000000000000.11"),Map.of());
  truth(RuleEngine.Truth.TRUE,result);
  assertEquals("1000000000000000.11",result.explanations().get(0).get("expectedValue"));
  truth(RuleEngine.Truth.FALSE,e.evaluate(stored,Map.of("annualIncome","1000000000000000.10"),Map.of()));
 }
 @Test void precisionDecimalStringsRetainAllDigitsAndScale(){
  JsonNode stored=e.parse(e.canonicalRule(numericRule("EQ","\"1000000000000000.1100\""),Map.of()));
  assertEquals("1000000000000000.1100",stored.path("children").get(0).path("value").textValue());
  truth(RuleEngine.Truth.TRUE,e.evaluate(stored,Map.of("annualIncome","1000000000000000.11"),Map.of()));
 }
 @Test void precisionCanonicalizationPreservesAllNineOperatorsAndExplanations(){
  Map<String,String> values=Map.of("EQ","1000000000000000.11","NE","1000000000000000.12",
      "GT","1000000000000000.10","GTE","1000000000000000.11","LT","1000000000000000.12",
      "LTE","1000000000000000.11","IN","[1000000000000000.11,1000000000000000.12]",
      "NOT_IN","[1000000000000000.12,1000000000000000.13]","BETWEEN","[1000000000000000.10,1000000000000000.11]");
  values.forEach((operator,token)->{
   JsonNode raw=numericRule(operator,token),stored=e.parse(e.canonicalRule(raw,Map.of()));
   var facts=Map.<String,Object>of("annualIncome","1000000000000000.11");
   var before=e.evaluate(raw,facts,Map.of());var after=e.evaluate(stored,facts,Map.of());
   truth(RuleEngine.Truth.TRUE,after);assertEquals(before.truth(),after.truth());
   assertEquals(1,after.explanations().size());
   for(String key:List.of("nodeId","fieldKey","operator","actualValue","result"))
    assertEquals(before.explanations().get(0).get(key),after.explanations().get(0).get(key));
  });
 }
 @Test void precisionSafeNumbersAndUnknownNestedGroupsRemainSupported(){
  for(String token:List.of("18","0.25","1.2345e5","9007199254740993")){
   JsonNode stored=e.parse(e.canonicalRule(numericRule("EQ",token),Map.of()));
   String actual=new BigDecimal(token).toPlainString();
   truth(RuleEngine.Truth.TRUE,e.evaluate(stored,Map.of("annualIncome",actual),Map.of()));
  }
  JsonNode nested=group("AND",group("OR",numericRule("EQ","0.25").path("children").get(0),leaf("false","annualIncome","GT",100)),unknown());
  var result=e.evaluate(e.parse(e.canonicalRule(nested,Map.of())),Map.of("annualIncome","0.25"),Map.of());
  truth(RuleEngine.Truth.UNKNOWN,result);assertEquals(3,result.explanations().size());assertEquals(List.of("stateCode"),result.missing());
 }
 @Test void precisionNonfiniteAndOversizedNumericRepresentationsAreRejected(){
  assertThrows(IllegalArgumentException.class,()->e.canonicalRule(group("AND",leaf("bad","annualIncome","EQ",Double.POSITIVE_INFINITY)),Map.of()));
  assertThrows(IllegalArgumentException.class,()->e.canonicalRule(numericRule("EQ","1e10000"),Map.of()));
  assertThrows(IllegalArgumentException.class,()->e.canonicalRule(numericRule("EQ","\"NaN\""),Map.of()));
 }
}
