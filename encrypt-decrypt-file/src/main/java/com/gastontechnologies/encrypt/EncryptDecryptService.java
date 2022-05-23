package com.gastontechnologies.encrypt;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.util.io.Streams;
import org.pgpainless.PGPainless;
import org.pgpainless.decryption_verification.ConsumerOptions;
import org.pgpainless.decryption_verification.DecryptionStream;
import org.pgpainless.encryption_signing.EncryptionOptions;
import org.pgpainless.encryption_signing.EncryptionStream;
import org.pgpainless.encryption_signing.ProducerOptions;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.pgpainless.util.Passphrase;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

@Service
public class EncryptDecryptService {

    private final SsmClient ssmClient = SsmClient.builder()
            .region(Region.of(System.getenv("AWS_REGION")))
            .build();


    public byte[] encryptFile(String tenantName, byte[] fileContents) throws IOException, PGPException {

        System.out.println("Tenant Name: " + tenantName);

        GetParameterRequest publicKeyRequest = GetParameterRequest.builder()
                .name(tenantName + ".pgp.publicKey")
                .withDecryption(true)
                .build();

        GetParameterResponse publicKeyResponse = ssmClient.getParameter(publicKeyRequest);
        System.out.println("The parameter value is "+publicKeyResponse.parameter().value());
        PGPPublicKeyRing encryptionCertificate = PGPainless.readKeyRing().publicKeyRing(publicKeyResponse.parameter().value());

        // plaintext message to encrypt
        ByteArrayOutputStream ciphertext = new ByteArrayOutputStream();
        // Encrypt and sign
        EncryptionStream encryptor = PGPainless.encryptAndOrSign()
                .onOutputStream(ciphertext)
                .withOptions(ProducerOptions.encrypt(EncryptionOptions.encryptCommunications()
                        .addRecipient(encryptionCertificate)).setAsciiArmor(true)
                );

        // Pipe data trough and CLOSE the stream (important)
        Streams.pipeAll(new ByteArrayInputStream(fileContents), encryptor);
        encryptor.close();

        byte[] result = ciphertext.toByteArray();

        ciphertext.close();

        return result;
    }

    public byte[] decryptFile(String tenantName, byte[] fileContents) throws IOException, PGPException {

        System.out.println("Tenant Name: " + tenantName);

        GetParameterRequest privateKeyRequest = GetParameterRequest.builder()
                .name(tenantName + ".pgp.privateKey")
                .withDecryption(true)
                .build();

        GetParameterResponse privateKeyResponse = ssmClient.getParameter(privateKeyRequest);
        System.out.println("The parameter value is "+privateKeyResponse.parameter().value());

        GetParameterRequest passphraseRequest = GetParameterRequest.builder()
                .name(tenantName + ".pgp.passphrase")
                .withDecryption(true)
                .build();

        GetParameterResponse passphraseResponse = ssmClient.getParameter(passphraseRequest);
        System.out.println("The parameter value is "+passphraseResponse.parameter().value());

        SecretKeyRingProtector decryptorProtector = SecretKeyRingProtector.unlockAnyKeyWith(Passphrase.fromPassword(passphraseResponse.parameter().value()));
        PGPSecretKeyRing decryptionKey = PGPainless.readKeyRing().secretKeyRing(privateKeyResponse.parameter().value());

        DecryptionStream decryptionStream = PGPainless.decryptAndOrVerify()
                .onInputStream(new ByteArrayInputStream(fileContents))
                .withOptions(new ConsumerOptions()
                        .addDecryptionKey(decryptionKey, decryptorProtector)
                );
        ByteArrayOutputStream plaintext = new ByteArrayOutputStream();

        Streams.pipeAll(decryptionStream, plaintext);
        decryptionStream.close();
        byte[] result = plaintext.toByteArray();
        plaintext.close();
        System.out.println(Arrays.toString(result));

        return result;
    }
}
