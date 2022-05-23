package com.gastontechnologies.encrypt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class FunctionInput {
    private String fileName;
    private String bucket;
    private String tenantName;
}
