package pt.unl.fct.di.novasys.babel.core.security;

import java.security.spec.AlgorithmParameterSpec;

public record CipherData(byte[] data, AlgorithmParameterSpec param) {
}
