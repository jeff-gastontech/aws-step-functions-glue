package com.gastontechnologies.encrypt;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.io.InputStream;

@Service
public class FileService {

    private final S3Client s3 = S3Client.builder().region(Region.US_EAST_1).build();


    public void uploadFile(byte[] fileContents, String fileName, String bucketName) {
        PutObjectResponse response = s3.putObject(PutObjectRequest.builder().bucket(bucketName).key(fileName).build(), RequestBody.fromBytes(fileContents));
    }

    public byte[] downloadFile(String fileName, String bucketName) throws IOException {
        return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(fileName).build()).asByteArray();
    }
}
