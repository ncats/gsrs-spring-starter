package ix.core;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.ImprovedNamingStrategy;

public class H2EbeanLikeImplicitNamingStategy extends ImplicitNamingStrategyLegacyJpaImpl {
    private static final long serialVersionUID = 1L;
    private static final ImprovedNamingStrategy STRATEGY_INSTANCE = new ImprovedNamingStrategy();

//
//    
    @Override
    protected Identifier toIdentifier(String stringForm,
            MetadataBuildingContext buildingContext) {
    	if(stringForm.equalsIgnoreCase("value")) {
    		stringForm = "term_"+stringForm;
//    		System.out.println("\n\nvalue to " + stringForm + "\n\n");
    	}
    	
        Identifier id = super.toIdentifier(STRATEGY_INSTANCE.columnName(stringForm), buildingContext);
//        System.out.println("ID from:" + stringForm + " is " + id.toString());
        return id;
    }

}
