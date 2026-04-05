package com.generic4.itda.annotation;

import com.generic4.itda.config.JpaConfig;
import com.generic4.itda.config.QuerydslConfig;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@DataJpaTest
@ActiveProfiles({"test", "h2"})
@Import({JpaConfig.class, QuerydslConfig.class})
public @interface H2RepositoryTest {

}
