package com.task.tracker.tasktrackerapp.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import java.util.UUID;

@Configuration
@Slf4j
public class AWSCognitoConfig {

    @Value("${aws.cognito.region}")
    private String region;

    @Value("${aws.accessKeyId}}")
    private String accessKeyId;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.ec2.iamRoleArn}")
    private String ec2IamRoleArn;

    @Value("${aws.ec2.roleSessionName}")
    private String ec2RoleSessionName;

    private CognitoIdentityProviderClient cognitoIdentityProviderClient;

    @PostConstruct
    private void initAwsCredentialsProvider() {
        AwsCredentialsProvider credentialsProvider;
        try {
            log.info("Initializing AWS Cognito Credentials Provider using sts Assume role");
            StsClient stsClient = StsClient.builder()
                    .region(Region.of(region))
                    .build();

            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                    .roleArn(ec2IamRoleArn)
                    .roleSessionName(ec2RoleSessionName + UUID.randomUUID()) // Ensure unique session name for each assumption
                    .build();

            AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
            Credentials sessionCredentials = assumeRoleResponse.credentials();

            credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(
                    sessionCredentials.accessKeyId(),
                    sessionCredentials.secretAccessKey()
            ));

        } catch (Exception e) {
            log.info("Using default AWS Cognito Credentials Provider when sts role assumptions are missing");
            if (accessKeyId != null && !accessKeyId.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
                credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretKey));
            } else {
                log.info("Using default AWS Cognito Credentials Provider");
                credentialsProvider = DefaultCredentialsProvider.create();
            }
        }
        log.info("AWS Cognito Credentials Provider initialized successfully");
        cognitoIdentityProviderClient = CognitoIdentityProviderClient.builder().region(Region.of(region)).credentialsProvider(credentialsProvider).build();
    }

    public CognitoIdentityProviderClient getCognitoClient() {
        if (cognitoIdentityProviderClient == null) {
            initAwsCredentialsProvider();
        }
        return cognitoIdentityProviderClient;
    }
}
