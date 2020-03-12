package eu.thingwave.arrowheadmqtt;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;

import org.bouncycastle.jce.provider.*;
import org.bouncycastle.openssl.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.openssl.PEMKeyPair;

public class SslUtil
{
	static SSLSocketFactory getSocketFactory (final String caCrtFile, final String crtFile, final String keyFile, 
	                                          final String password) throws Exception
	{
		Security.addProvider(new BouncyCastleProvider());

		// load CA certificate
		PEMParser reader = new PEMParser(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(caCrtFile)))));
		JcaX509CertificateConverter conv = new JcaX509CertificateConverter();
		X509Certificate caCert = null; //(X509Certificate)reader.readObject();
		caCert = conv.getCertificate((X509CertificateHolder)reader.readObject());
		reader.close();

		// load client certificate
		reader = new PEMParser(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(crtFile)))));
		X509Certificate cert = null;
		cert = conv.getCertificate((X509CertificateHolder)reader.readObject());
		reader.close();

		// load client private key
		reader = new PEMParser(
				new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(keyFile))))/*,
				new PasswordFinder() {
					@Override
					public char[] getPassword() {
						return password.toCharArray();
					}
				}*/
		);
		KeyPair key = null; //(KeyPair)reader.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        PEMDecryptorProvider decProv = new     JcePEMDecryptorProviderBuilder().build(password.toCharArray());
    	PEMKeyPair pkp = null;
        pkp = (PEMKeyPair)reader.readObject();
        key = converter.getKeyPair(pkp);
		reader.close();

		// CA certificate is used to authenticate server
		KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
		caKs.load(null, null);
		caKs.setCertificateEntry("ca-certificate", caCert);
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(caKs);

		// client key and certificates are sent to server so it can authenticate us
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		ks.setCertificateEntry("certificate", cert);
		ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(), new java.security.cert.Certificate[]{cert});
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, password.toCharArray());

		// finally, create SSL socket factory
		SSLContext context = SSLContext.getInstance("TLSv1.3");
		context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		return context.getSocketFactory();
	}
}
