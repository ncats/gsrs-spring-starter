package ix.core;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.ImprovedNamingStrategy;

public class EbeanLikeImplicitNamingStategy extends ImplicitNamingStrategyLegacyJpaImpl {
    private static final long serialVersionUID = 1L;
    private static final ImprovedNamingStrategy STRATEGY_INSTANCE = new ImprovedNamingStrategy();

//
//    
    @Override
    protected Identifier toIdentifier(String stringForm,
            MetadataBuildingContext buildingContext) {
        return super.toIdentifier(STRATEGY_INSTANCE.columnName(stringForm), buildingContext);
    }
//
//    @Override
//    public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source) {
//        return toIdentifier( STRATEGY_INSTANCE.columnName(coltransformAttributePath( source.getAttributePath() )), source.getBuildingContext() );
//    }
//    
//    @Override
//    public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
//        this.
//        
//        // legacy JPA-based naming strategy preferred to use {TableName}_{ReferencedColumnName}
//        // where JPA was later clarified to prefer {EntityName}_{ReferencedColumnName}.
//        //
//        // The spec-compliant one implements the clarified {EntityName}_{ReferencedColumnName}
//        // naming.  Here we implement the older {TableName}_{ReferencedColumnName} naming
//        final String name;
//
//        
////      if ( source.getNature() == ImplicitJoinColumnNameSource.Nature.ENTITY
////              && source.getAttributePath() != null ) {
////          // many-to-one /  one-to-one
////          //
////          // legacy naming used the attribute name here, following suit with legacy hbm naming
////          //
////          // NOTE : attribute path being null here would be an error, so for now don't bother checking
////          name = transformAttributePath( source.getAttributePath() );
////      }
////      else if ( source.getNature() == ImplicitJoinColumnNameSource.Nature.ELEMENT_COLLECTION
//        
//        
//        if ( source.getNature() == ImplicitJoinColumnNameSource.Nature.ELEMENT_COLLECTION
//                || source.getAttributePath() == null) {
//            name = source.getReferencedTableName().getText()
//                    + '_'
//                    + source.getReferencedColumnName().getText();
//            
//        }
//        else {
////            System.out.println(source.getClass());
////            System.out.println(source.getAttributePath().getFullPath());
//            name = transformAttributePath( source.getAttributePath() )
//                    + '_'
//                    + source.getReferencedColumnName().getText();
//            
//        }
////        source.getAttributePath().
////        System.out.println(source.getNature() + "=>" + name );
//        return toIdentifier( name, source.getBuildingContext() );
//    }
}