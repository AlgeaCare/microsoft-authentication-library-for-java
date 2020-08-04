// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSHeader.Builder;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

final class JwtHelper {

    static ClientAssertion buildJwt(String clientId, final ClientCertificate credential,
            final String jwtAudience) throws MsalClientException {
        if (StringHelper.isBlank(clientId)) {
            throw new IllegalArgumentException("clientId is null or empty");
        }

        if (credential == null) {
            throw new IllegalArgumentException("credential is null");
        }

        final long time = System.currentTimeMillis();

        final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .audience(Collections.singletonList(jwtAudience))
                .issuer(clientId)
                .jwtID(UUID.randomUUID().toString())
                .notBeforeTime(new Date(time))
                .expirationTime(new Date(time
                                + Constants.AAD_JWT_TOKEN_LIFETIME_SECONDS
                                * 1000))
                .subject(clientId)
                .build();

        SignedJWT jwt;
        try {
            List<Base64> certs = new ArrayList<>();
            for(String publicCertificate: credential.publicCertificates()) {
                certs.add(new Base64(publicCertificate));
            }

            JWSHeader.Builder builder = new Builder(JWSAlgorithm.RS256);
            builder.x509CertChain(certs);
            builder.x509CertThumbprint(new Base64URL(credential.publicCertificateHash()));

            jwt = new SignedJWT(builder.build(), claimsSet);
            final RSASSASigner signer = new RSASSASigner(credential.key());

            jwt.sign(signer);
        }
        catch (final Exception e) {
            throw new MsalClientException(e);
        }

        return new ClientAssertion(jwt.serialize());
    }
}
