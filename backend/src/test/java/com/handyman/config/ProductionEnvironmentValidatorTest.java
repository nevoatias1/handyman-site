package com.handyman.config;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class ProductionEnvironmentValidatorTest {
 private MockEnvironment env(Map<String,Object> vars) {
  var env = new MockEnvironment() {
   @Override public Map<String,Object> getSystemEnvironment() { return vars; }
  };
  env.setActiveProfiles("prod");
  return env;
 }
 @Test void eachRequiredVariableIsCheckedBeforeBeansStart() {
  Map<String,Object> values=new HashMap<>();
  ProductionEnvironmentValidator.REQUIRED.forEach(key->values.put(key,"test-value"));
  var validator=new ProductionEnvironmentValidator();
  assertDoesNotThrow(()->validator.postProcessEnvironment(env(values),null));
  for(String key:ProductionEnvironmentValidator.REQUIRED) {
   var missing=new HashMap<>(values);missing.remove(key);
   var ex=assertThrows(IllegalStateException.class,()->validator.postProcessEnvironment(env(missing),null));
   assertTrue(ex.getMessage().contains(key));
   missing.put(key," ");
   assertThrows(IllegalStateException.class,()->validator.postProcessEnvironment(env(missing),null));
  }
 }
 @Test void devDoesNotRequireProductionVariables() {
  var environment=env(Map.of());environment.setActiveProfiles("dev");
  assertDoesNotThrow(()->new ProductionEnvironmentValidator().postProcessEnvironment(environment,null));
 }
}
