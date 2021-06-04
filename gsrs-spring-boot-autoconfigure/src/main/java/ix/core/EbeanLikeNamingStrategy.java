package ix.core;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class EbeanLikeNamingStrategy extends PhysicalNamingStrategyStandardImpl {
   private static final long serialVersionUID = 1L;
    private static final ImprovedNamingStrategy STRATEGY_INSTANCE = new ImprovedNamingStrategy();
//
//    @Override
//    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
//        return new Identifier(classToTableName(name.getText()), name.isQuoted());
//    }
////
////    @Override
////    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
////        
////        return new Identifier(STRATEGY_INSTANCE.columnName(name.getText()), name.isQuoted());
////    }
//
//    private String classToTableName(String className) {
//        return STRATEGY_INSTANCE.classToTableName(className);
//    }
}