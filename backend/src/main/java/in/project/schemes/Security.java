package in.project.schemes;

import jakarta.persistence.*;

import jakarta.servlet.*;

import jakarta.servlet.http.*;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.*;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.web.*;

import org.springframework.security.web.csrf.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.http.ResponseCookie;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;

import javax.crypto.spec.SecretKeySpec;

import java.util.*;

import java.time.*;

import java.nio.charset.StandardCharsets;



@Configuration

class Security {

 @Bean org.springframework.security.crypto.password.PasswordEncoder passwords(){return new org.springframework.security.crypto.password.PasswordEncoder(){
 final BCryptPasswordEncoder bcrypt=new BCryptPasswordEncoder();
 public String encode(CharSequence raw){return bcrypt.encode(prehash(raw));}
 public boolean matches(CharSequence raw,String encoded){return bcrypt.matches(prehash(raw),encoded);}
 String prehash(CharSequence raw){try{return Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(raw.toString().getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 };}
 @Bean SecurityFilterChain chain(HttpSecurity http,SessionTokens tokens) throws Exception{

  var repo=CookieCsrfTokenRepository.withHttpOnlyFalse();repo.setCookiePath("/");

  var handler=new CsrfTokenRequestAttributeHandler();handler.setCsrfRequestAttributeName(null);

  return http.httpBasic(x->x.disable()).formLogin(x->x.disable()).sessionManagement(x->x.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)).csrf(x->x.csrfTokenRepository(repo).csrfTokenRequestHandler(handler)).authorizeHttpRequests(x->x.requestMatchers("/api/v1/auth/csrf","/api/v1/auth/login","/api/v1/auth/register").permitAll().requestMatchers("/api/v1/admin/**").hasRole("ADMIN").anyRequest().authenticated()).exceptionHandling(x->x.authenticationEntryPoint((q,r,e)->error(r,401,"UNAUTHENTICATED")).accessDeniedHandler((q,r,e)->error(r,403,"FORBIDDEN"))).addFilterBefore(new OncePerRequestFilter(){protected void doFilterInternal(HttpServletRequest q,HttpServletResponse r,FilterChain c)throws java.io.IOException,ServletException{try{if(q.getCookies()!=null)for(Cookie k:q.getCookies())if(k.getName().equals("SESSION")){User u=tokens.verify(k.getValue());SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u.id.toString(),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.role))));}}catch(Exception ignored){SecurityContextHolder.clearContext();}c.doFilter(q,r);}},org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class).build();

 }

 static void error(HttpServletResponse r,int status,String code)throws java.io.IOException{r.setStatus(status);r.setContentType("application/json");r.getWriter().write("{\"error\":{\"code\":\""+code+"\",\"message\":\"Request rejected\",\"details\":{}},\"requestId\":\""+UUID.randomUUID()+"\"}");}

}

@org.springframework.stereotype.Component

class SessionTokens {

 @PersistenceContext EntityManager em;

 final byte[] secret;final RuleEngine engine;

 SessionTokens(@Value("${app.jwt-secret}") String key,RuleEngine engine){secret=key.getBytes(StandardCharsets.UTF_8);if(secret.length<32)throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");this.engine=engine;}

 String b64(byte[] x){return Base64.getUrlEncoder().withoutPadding().encodeToString(x);}

 byte[] sign(String x){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(secret,"HmacSHA256"));return m.doFinal(x.getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}

 String issue(User u){try{long now=Instant.now().getEpochSecond();String token=b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))+"."+b64(engine.json.writeValueAsBytes(Map.of("iss","scheme-selector","aud","scheme-selector-web","sub",u.id.toString(),"role",u.role,"iat",now,"exp",now+900)));return token+"."+b64(sign(token));}catch(Exception e){throw new IllegalStateException(e);}}

 User verify(String token)throws Exception{String[] p=token.split("\\.");if(p.length!=3||!java.security.MessageDigest.isEqual(sign(p[0]+"."+p[1]),Base64.getUrlDecoder().decode(p[2])))throw new IllegalArgumentException();var h=engine.json.readTree(Base64.getUrlDecoder().decode(p[0]));var c=engine.json.readTree(Base64.getUrlDecoder().decode(p[1]));long now=Instant.now().getEpochSecond();if(!h.path("alg").asText().equals("HS256")||!c.path("iss").asText().equals("scheme-selector")||!c.path("aud").asText().equals("scheme-selector-web")||c.path("exp").asLong()<=now||c.path("iat").asLong()>now||c.path("exp").asLong()-c.path("iat").asLong()!=900)throw new IllegalArgumentException();User u=em.find(User.class,Long.valueOf(c.path("sub").asText()));if(u==null||!"ACTIVE".equals(u.status)||!u.role.equals(c.path("role").asText()))throw new IllegalArgumentException();return u;}

 void cookie(HttpServletRequest q,HttpServletResponse r,String value){r.addHeader("Set-Cookie",ResponseCookie.from("SESSION",value).httpOnly(true).secure(q.isSecure()).sameSite("Lax").path("/api").maxAge(value.isEmpty()?0:900).build().toString());}

}

