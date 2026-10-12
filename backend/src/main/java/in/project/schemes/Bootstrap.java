package in.project.schemes;

import org.springframework.boot.*;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.core.env.Environment;

import jakarta.persistence.*;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import java.time.*;

@Component

class Bootstrap implements ApplicationRunner {

 @PersistenceContext EntityManager em;final Environment env;final PasswordEncoder passwords;final RuleEngine engine;final DemoSchemes demos;

 Bootstrap(Environment e,PasswordEncoder p,RuleEngine r,DemoSchemes d){env=e;passwords=p;engine=r;demos=d;}

 @Transactional public void run(ApplicationArguments args){String email=env.getProperty("ADMIN_EMAIL"),password=env.getProperty("ADMIN_PASSWORD");if(email!=null&&!email.isBlank()&&password!=null&&!password.isBlank()){if(password.length()<12||password.length()>128)throw new IllegalStateException("Provisioning password must be at least 12 characters and at most 128 characters");if(em.createQuery("select count(u) from User u where role='ADMIN'",Long.class).getSingleResult()==0){User u=new User();u.fullName="Local administrator";u.email=email.toLowerCase(Locale.ROOT);u.passwordHash=passwords.encode(password);u.role="ADMIN";u.status="ACTIVE";em.persist(u);Profile p=new Profile();p.userId=u.id;p.revision=0L;em.persist(p);Audit a=new Audit();a.actorId=u.id;a.action="ADMIN_PROVISIONED";a.targetId=u.id.toString();a.createdAt=Instant.now().toString();em.persist(a);}}

 if(!Arrays.asList(env.getActiveProfiles()).contains("dev"))return;

 if(em.createQuery("select count(s) from Scheme s",Long.class).getSingleResult()>0){demos.seed();return;}

 Category category=new Category();category.name="Synthetic demonstration";em.persist(category);

 Question q=new Question();q.label="Are you currently enrolled in a training course?";q.type="BOOLEAN";q.active=true;q.optionsJson="[]";q.revision=0L;em.persist(q);

 String[] names={"Learning Support","Training Access","Senior Wellbeing","Rural Enterprise","Student Equipment","Employment Readiness","Household Support","Skills Transition"};

 for(int i=0;i<8;i++){Scheme s=new Scheme();s.name="Synthetic Demo: "+names[i];s.categoryId=category.id;s.synthetic=true;s.reviewerApproved=false;s.sourceMapping="{}";s.description="Fictional development scheme used to demonstrate preliminary eligibility screening. It does not represent a real government programme.";s.benefits="Demonstration benefit only. No money, service or entitlement is offered.";s.guidance="Use the profile and eligibility questions to test the application. No application can be submitted for this fictional scheme.";s.revision=0L;em.persist(s);Link l=new Link();l.schemeId=s.id;l.questionId=q.id;em.persist(l);Rule r=new Rule();r.schemeId=s.id;r.status="PUBLISHED";r.revision=0L;r.createdAt=Instant.now().toString();r.ruleJson="{\"schemaVersion\":1,\"type\":\"GROUP\",\"operator\":\"AND\",\"children\":[{\"type\":\"CONDITION\",\"nodeId\":\"age\",\"field\":\"completedAgeYears\",\"operator\":\"GTE\",\"value\":"+(i==2?60:18)+"},{\"type\":\"CONDITION\",\"nodeId\":\"income\",\"field\":\"annualIncome\",\"operator\":\"LTE\",\"value\":\""+(100000+i*50000)+".00\"}"+(i==1?",{\"type\":\"CONDITION\",\"nodeId\":\"training\",\"field\":\"question:"+q.id+"\",\"operator\":\"EQ\",\"value\":true}":"")+"]}";em.persist(r);s.publishedVersionId=r.id;}

 demos.seed();
 }

}

