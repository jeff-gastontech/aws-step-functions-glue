package com.gastontechnologies.encrypt;

import java.io.IOException;
import java.util.function.Function;

import org.bouncycastle.openpgp.PGPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EncryptFileApplication {

    @Autowired
    private EncryptDecryptService encryptDecryptService;

    @Autowired
    private FileService fileService;

    /*
     * You need this main method (empty) or explicit <start-class>example.FunctionConfiguration</start-class>
     * in the POM to ensure boot plug-in makes the correct entry
     */
    public static void main(String[] args) {
        SpringApplication.run(EncryptFileApplication.class, args);
    }

    @Bean
    public Function<FunctionInput, FunctionOutput> encryptFile() {
        return value -> {
            try {
                System.out.println("Input: " + value.toString());
                String[] fileSplit = value.getFileName().split("/");
                String originalFileName = fileSplit[fileSplit.length - 1];
                String newFileName = fileSplit[fileSplit.length - 1].concat(".pgp");
                String originalFilePath = determineFilePath(fileSplit);
                String tenantName = getTenantName(fileSplit);
                fileService.uploadFile(
                        encryptDecryptService.encryptFile(
                                tenantName,
                                fileService.downloadFile(value.getFileName(), value.getBucket())
                        ),
                        originalFilePath + tenantName + "/" + newFileName, value.getBucket());

                return FunctionOutput.builder().bucket(value.getBucket()).originalFilePath(originalFilePath).originalFileName(originalFileName).newFilePath(originalFilePath).newFileName(newFileName).tenantName(tenantName).build();
            } catch (IOException | PGPException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Bean
    public Function<FunctionInput, FunctionOutput> decryptFile() {
        return value -> {
            try {
                System.out.println("Input: " + value.toString());
                String[] fileSplit = value.getFileName().split("/");
                String originalFileName = fileSplit[fileSplit.length - 1];
                String newFileName = fileSplit[fileSplit.length - 1].replace(".pgp", "");
                String originalFilePath = determineFilePath(fileSplit);
                String newFilePath = originalFilePath.replace("source/", "staging/");
                String tenantName = getTenantName(fileSplit);
                fileService.uploadFile(
                        encryptDecryptService.decryptFile(
                                getTenantName(fileSplit),
                                fileService.downloadFile(value.getFileName(), value.getBucket())
                        )
                        , newFilePath + tenantName + "/" + newFileName, value.getBucket()
                );
                return FunctionOutput.builder().bucket(value.getBucket()).originalFilePath(originalFilePath).originalFileName(originalFileName).newFilePath(newFilePath).newFileName(newFileName).tenantName(tenantName).build();
            } catch (IOException | PGPException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private String determineFilePath(String[] fileSplit) {
        String filePath = "";
        for (int i = 0; i < fileSplit.length - 2; i++) {
            filePath = filePath.concat(fileSplit[i]).concat("/");
        }
        System.out.println("Determined file path: " + filePath);
        return filePath;
    }

    private String getTenantName(String[] fileSplit) {
        return fileSplit[1];
    }
}