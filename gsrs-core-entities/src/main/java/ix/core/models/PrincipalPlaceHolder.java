package ix.core.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * This is a fake subclass to trick Hibernate into
 * thinking there are multiple entities that
 * are written into the Principal table
 * so it will create DDL with a discriminator column
 * to be backwards compatible with old GSRS database schemas.
 */
@Entity
@DiscriminatorValue("FAK")
public class PrincipalPlaceHolder extends Principal{
}
