package com.task.tracker.tasktrackerapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.util.UUID;

@Configuration
@Slf4j
public class AWSCognitoConfig {

    @Value("${aws.cognito.region}")
    private String region;

    @Value("${aws.accessKeyId}")
    private String accessKeyId;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.ec2.iamRoleArn}")
    private String ec2IamRoleArn;

    @Value("${aws.ec2.roleSessionName}")
    private String ec2RoleSessionName;

    @Bean
    public CognitoIdentityProviderClient cognitoIdentityProviderClient() {
        log.info("Initializing Cognito Client with automatic STS refreshing...");
        try {
            // 1. Create the STS Client to perform the assumption
             StsClient stsClient = StsClient.builder()
                    .region(Region.of(region))
                    .build();

            // 2. Use a specialized provider that handles the session token AND expiration
            StsAssumeRoleCredentialsProvider credentialsProvider = StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(AssumeRoleRequest.builder()
                            .roleArn(ec2IamRoleArn)
                            .roleSessionName(ec2RoleSessionName + "-" + UUID.randomUUID())
                            .build())
                    .build();

            return CognitoIdentityProviderClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();
        } catch (Exception e) {
            log.info("Using default AWS Cognito Credentials Provider when sts role assumptions are missing");
            if (accessKeyId != null && !accessKeyId.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
                return CognitoIdentityProviderClient.builder().region(Region.of(region)).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretKey))).build();
            } else {
                log.info("Using default AWS Cognito Credentials Provider");
               return CognitoIdentityProviderClient.builder().region(Region.of(region)).credentialsProvider(DefaultCredentialsProvider.create()).build();
            }
        }
    }
}
