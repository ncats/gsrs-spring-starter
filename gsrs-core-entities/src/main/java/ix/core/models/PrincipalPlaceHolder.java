package ix.core.models;

/**
 * Legacy compatibility shim. Hibernate can create the discriminator column on Principal
 * directly without a fake mapping subclass; keeping this as a JPA entity creates a
 * synthetic FAK discriminator check that fails under H2 schema validation.
 */
public class PrincipalPlaceHolder extends Principal {
}
