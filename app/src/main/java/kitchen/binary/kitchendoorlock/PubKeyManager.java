package kitchen.binary.kitchendoorlock;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import javax.net.ssl.X509TrustManager;

public final class PubKeyManager implements X509TrustManager
{
    private String pin;

    public PubKeyManager(String pin) {
        this.pin = pin;
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
    {
        if (chain == null) {
            throw new IllegalArgumentException("X509Certificate array is null");
        }

        if (chain.length != 1) {
            throw new IllegalArgumentException("wrong chain length");
        }

        RSAPublicKey pubkey = (RSAPublicKey) chain[0].getPublicKey();
        String encoded = new BigInteger(1 /* positive */, pubkey.getEncoded()).toString(16);

        if (!pin.equals(encoded)) {
            throw new CertificateException("certificate check failed");
        }
    }

    public void checkClientTrusted(X509Certificate[] xcs, String string) {
        throw new UnsupportedOperationException("checkClientTrusted: Not supported yet.");
	}

	public X509Certificate[] getAcceptedIssuers() {
		throw new UnsupportedOperationException("getAcceptedIssuers: Not supported yet.");
		//return null;
	}

}