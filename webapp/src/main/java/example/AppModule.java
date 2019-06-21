/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package example;

import static com.google.inject.multibindings.MapBinder.newMapBinder;
import static com.google.inject.name.Names.named;

import example.handlers.AJAXHandler;
import example.handlers.RecvHandler;
import example.handlers.SendHandler;

import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;

public class AppModule extends AbstractModule {
    @Override protected void configure() {
        bind(String.class).annotatedWith(named("keyIdEast"))
                          .toInstance(System.getenv("kms_key_id"));
        bind(String.class).annotatedWith(named("keyIdWest"))
                          .toInstance(System.getenv("cross_region_kms_key_id"));
        bind(String.class).annotatedWith(named("queueUrl"))
                          .toInstance(System.getenv("queue_url"));
        bind(String.class).annotatedWith(named("logGroupName"))
                          .toInstance(System.getenv("log_group_name"));
        bind(AmazonSQS.class).to(AmazonSQSClient.class).in(Singleton.class);

        bind(Region.class).toInstance(
                RegionUtils.getRegion(new DefaultAwsRegionProviderChain().getRegion())
        );

        MapBinder<String, AJAXHandler> handlerBinder = newMapBinder(binder(), String.class, AJAXHandler.class);
        handlerBinder.addBinding("send").to(SendHandler.class);
        handlerBinder.addBinding("recv").to(RecvHandler.class);
    }

    @Provides @Singleton public AWSKMS buildKMS(Region region) {
        AWSKMSClient kms = new AWSKMSClient();
        kms.setRegion(region);
        return kms;
    }

}