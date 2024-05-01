package pt.unl.fct.di.novasys.babel.core;

/**
 * Indicates that the protcol needs a contact. Should be used to annotate a
 * parameter of type Host in the protocol. Acompanying the anotation, the
 * parameter of type Host should also include a public getter and setter.
 * An aditional getter for the port where the first connection will be made
 * should also be provided.
 */
public @interface Contact {

}
