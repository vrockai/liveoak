/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.security.impl;

/**
 * TODO: Probably remove and init everything from JSON or something...
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthConstants {

    // auth token string
    public static final String ATTR_AUTHORIZATION_TOKEN = "_authorizationTokenString";

    // Default applicationId. Metadata and default policy are actually registered for application with this ID
    public static final String DEFAULT_APP_ID = "DEFAULT_APP_ID";

    // Name of realm and application and publicKey, which will be registered by default under DEFAULT_APP_ID
    public static final String DEFAULT_REALM_NAME = "realmName1";
    public static final String DEFAULT_APPLICATION_NAME = "appName1";

    // Some other realm and apps to use
    public static final String REALM_NAME2 = "realmName2";
    public static final String REALM2_APP_NAME2 = "realm2_appName2";
    public static final String REALM2_APP_NAME3 = "realm2_appName3";
    public static final String REALM2_APP_NAME4 = "realm2_appName4";
    public static final String REALM2_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCiraR2ePEwigubNnn68UcIS9R1J4DIUL4nvMclyPllTkGdaZ8FW642GrFm6hWiFeFQib+oKGolqhv0Pz3wd5vllJL0OQ4xP92R94xuuXNqzbgNRXOOy6QHrUh/mjhjaguZw+1pnU2IfemAFz1UHq9OAnPUv5cX6gR/NTFbd4ZupQIDAQAB";


}
