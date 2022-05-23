package com.gastontechnologies.encrypt;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class FunctionOutput {
    private String originalFilePath;
    private String originalFileName;
    private String newFilePath;
    private String newFileName;
    private String bucket;
    private String tenantName;
}
